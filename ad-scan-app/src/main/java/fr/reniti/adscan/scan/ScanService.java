package fr.reniti.adscan.scan;

import static fr.reniti.adscan.database.jooq.Tables.MEMBER;
import static fr.reniti.adscan.database.jooq.Tables.PARTICIPATION;
import static fr.reniti.adscan.database.jooq.Tables.SCAN_LOG;

import fr.reniti.adscan.database.jooq.tables.records.ParticipationRecord;
import fr.reniti.adscan.event.Event;
import fr.reniti.adscan.member.FormationOptions;
import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.member.MemberService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanService {

    private static final DateTimeFormatter CLOSED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DSLContext dsl;
    private final MemberService memberService;

    public ScanService(DSLContext dsl, MemberService memberService) {
        this.dsl = dsl;
        this.memberService = memberService;
    }

    @Transactional
    public ScanResult scan(Event event, String scannedKey) {
        Integer eventId = event.id();
        if (event.isClosed()) {
            return new ScanResult(ScanStatus.EVENT_CLOSED, null, null, null, null, null, null, countEntries(eventId),
                    "Soirée fermée : les entrées ne sont plus acceptées.");
        }

        Optional<Member> memberOpt = memberService.findByAccessToken(scannedKey);
        if (memberOpt.isEmpty()) {
            Optional<Member> invalidatedOwner = memberService.findByInvalidatedAccessToken(scannedKey);
            if (invalidatedOwner.isPresent()) {
                Member owner = invalidatedOwner.get();
                logScan(eventId, owner.id(), scannedKey, ScanStatus.TOKEN_INVALIDATED);
                return new ScanResult(ScanStatus.TOKEN_INVALIDATED, owner.fullName(), owner.email(), owner.provenance(),
                        null, null, null, countEntries(eventId),
                        "Badge invalidé : un nouveau badge a été généré pour " + owner.fullName() + ".");
            }
            logScan(eventId, null, scannedKey, ScanStatus.UNKNOWN_KEY);
            return new ScanResult(ScanStatus.UNKNOWN_KEY, null, null, null, null, null, null, countEntries(eventId),
                    "Clé inconnue : aucun adhérent ne correspond à ce badge.");
        }

        Member member = memberOpt.get();
        String fullName = member.fullName();
        String startDate = member.startDate() != null ? member.startDate().toString() : null;
        String endDate = member.endDate() != null ? member.endDate().toString() : null;

        if (member.isExpired(LocalDate.now())) {
            logScan(eventId, member.id(), scannedKey, ScanStatus.EXPIRED);
            return new ScanResult(ScanStatus.EXPIRED, fullName, member.email(), member.provenance(), startDate, endDate,
                    null, countEntries(eventId), "Adhésion expirée le " + endDate + ".");
        }

        // Unconfirmed = membership fee not validated by staff yet: refused, and NOT counted as an entry.
        if (!member.confirmed()) {
            logScan(eventId, member.id(), scannedKey, ScanStatus.PENDING_PAYMENT);
            return new ScanResult(ScanStatus.PENDING_PAYMENT, fullName, member.email(), member.provenance(), startDate,
                    endDate, null, countEntries(eventId), "Paiement de l'adhésion en attente pour " + fullName + ".");
        }

        Optional<ParticipationRecord> existing = dsl.selectFrom(PARTICIPATION)
                .where(PARTICIPATION.EVENT_ID.eq(eventId).and(PARTICIPATION.MEMBER_ID.eq(member.id())))
                .fetchOptional();

        if (existing.isEmpty()) {
            dsl.insertInto(PARTICIPATION)
                    .set(PARTICIPATION.EVENT_ID, eventId)
                    .set(PARTICIPATION.MEMBER_ID, member.id())
                    .set(PARTICIPATION.FIRST_SCANNED_AT, LocalDateTime.now())
                    .set(PARTICIPATION.SCAN_COUNT, 1)
                    .execute();
            logScan(eventId, member.id(), scannedKey, ScanStatus.OK);
            return new ScanResult(ScanStatus.OK, fullName, member.email(), member.provenance(), startDate, endDate, 1,
                    countEntries(eventId), "Entrée validée pour " + fullName + ".");
        }

        int newScanCount = existing.get().getScanCount() + 1;
        dsl.update(PARTICIPATION)
                .set(PARTICIPATION.SCAN_COUNT, newScanCount)
                .where(PARTICIPATION.ID.eq(existing.get().getId()))
                .execute();
        logScan(eventId, member.id(), scannedKey, ScanStatus.DUPLICATE);
        return new ScanResult(ScanStatus.DUPLICATE, fullName, member.email(), member.provenance(), startDate, endDate,
                newScanCount, countEntries(eventId), fullName + " a déjà été scanné (" + newScanCount + " fois).");
    }

    /** Number of validated entries (distinct admitted members) for the event. */
    public int countEntries(Integer eventId) {
        return dsl.fetchCount(PARTICIPATION, PARTICIPATION.EVENT_ID.eq(eventId));
    }

    /** Validated entries per event id, for the events list. */
    public Map<Integer, Integer> entryCountsByEvent() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        dsl.select(PARTICIPATION.EVENT_ID, DSL.count())
                .from(PARTICIPATION)
                .groupBy(PARTICIPATION.EVENT_ID)
                .fetch()
                .forEach(record -> counts.put(record.value1(), record.value2()));
        return counts;
    }

    public EventStats stats(Event event) {
        List<Record3<String, String, Boolean>> entries = dsl
                .select(MEMBER.FORMATION, MEMBER.FILIERE, MEMBER.ALTERNANT)
                .from(PARTICIPATION)
                .join(MEMBER).on(PARTICIPATION.MEMBER_ID.eq(MEMBER.ID))
                .where(PARTICIPATION.EVENT_ID.eq(event.id()))
                .fetch();

        int ensim = 0;
        int personnel = 0;
        int exterieur = 0;
        int unknownOrigin = 0;
        int alternants = 0;
        int nonAlternants = 0;
        Map<String, Integer> years = new LinkedHashMap<>();
        FormationOptions.STUDENT_YEARS.forEach(year -> years.put(year, 0));
        Map<String, Integer> filieres = new LinkedHashMap<>();
        FormationOptions.FILIERES.forEach(filiere -> filieres.put(filiere, 0));

        for (Record3<String, String, Boolean> entry : entries) {
            String formation = entry.value1();
            if (FormationOptions.isStudent(formation)) {
                ensim++;
                years.merge(formation, 1, Integer::sum);
                if (FormationOptions.hasFiliere(formation)) {
                    String filiere = entry.value2();
                    if (filiere != null && !filiere.isBlank()) {
                        filieres.merge(filiere, 1, Integer::sum);
                    }
                    if (Boolean.TRUE.equals(entry.value3())) {
                        alternants++;
                    } else {
                        nonAlternants++;
                    }
                }
            } else if (FormationOptions.PERSONNEL_ENSIM.equals(formation)) {
                personnel++;
            } else if (FormationOptions.EXTERIEUR.equals(formation)) {
                exterieur++;
            } else {
                unknownOrigin++;
            }
        }

        int pendingPayment = dsl.select(DSL.countDistinct(SCAN_LOG.MEMBER_ID))
                .from(SCAN_LOG)
                .where(SCAN_LOG.EVENT_ID.eq(event.id())
                        .and(SCAN_LOG.STATUS.eq(ScanStatus.PENDING_PAYMENT.name()))
                        // Only members still refused: once admitted they no longer count as pending.
                        .and(SCAN_LOG.MEMBER_ID.notIn(dsl.select(PARTICIPATION.MEMBER_ID)
                                .from(PARTICIPATION)
                                .where(PARTICIPATION.EVENT_ID.eq(event.id())))))
                .fetchOne(0, int.class);
        int duplicateScans = dsl.fetchCount(SCAN_LOG,
                SCAN_LOG.EVENT_ID.eq(event.id()).and(SCAN_LOG.STATUS.eq(ScanStatus.DUPLICATE.name())));

        return new EventStats(
                event.isClosed(),
                event.closedAt() != null ? event.closedAt().format(CLOSED_AT_FORMAT) : null,
                entries.size(),
                pendingPayment,
                duplicateScans,
                ensim,
                personnel,
                exterieur,
                unknownOrigin,
                years,
                filieres,
                alternants,
                nonAlternants
        );
    }

    public List<ScanLogEntry> history(Integer eventId) {
        return dsl.select(SCAN_LOG.SCANNED_AT, SCAN_LOG.STATUS, SCAN_LOG.SCANNED_KEY, MEMBER.FIRST_NAME, MEMBER.LAST_NAME)
                .from(SCAN_LOG)
                .leftJoin(MEMBER).on(SCAN_LOG.MEMBER_ID.eq(MEMBER.ID))
                .where(SCAN_LOG.EVENT_ID.eq(eventId))
                .orderBy(SCAN_LOG.SCANNED_AT.desc(), SCAN_LOG.ID.desc())
                .fetch(record -> {
                    String firstName = record.get(MEMBER.FIRST_NAME);
                    String lastName = record.get(MEMBER.LAST_NAME);
                    String memberName = firstName != null ? firstName + " " + lastName : null;
                    return new ScanLogEntry(
                            record.get(SCAN_LOG.SCANNED_AT),
                            memberName,
                            record.get(SCAN_LOG.STATUS),
                            record.get(SCAN_LOG.SCANNED_KEY)
                    );
                });
    }

    private void logScan(Integer eventId, String memberId, String scannedKey, ScanStatus status) {
        dsl.insertInto(SCAN_LOG)
                .set(SCAN_LOG.EVENT_ID, eventId)
                .set(SCAN_LOG.MEMBER_ID, memberId)
                .set(SCAN_LOG.SCANNED_KEY, scannedKey)
                .set(SCAN_LOG.STATUS, status.name())
                .set(SCAN_LOG.SCANNED_AT, LocalDateTime.now())
                .execute();
    }
}
