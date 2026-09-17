package fr.reniti.adscan.scan;

import static fr.reniti.adscan.database.jooq.Tables.MEMBER;
import static fr.reniti.adscan.database.jooq.Tables.PARTICIPATION;
import static fr.reniti.adscan.database.jooq.Tables.SCAN_LOG;

import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.member.MemberService;
import fr.reniti.adscan.database.jooq.tables.records.ParticipationRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanService {

    private final DSLContext dsl;
    private final MemberService memberService;

    public ScanService(DSLContext dsl, MemberService memberService) {
        this.dsl = dsl;
        this.memberService = memberService;
    }

    @Transactional
    public ScanResult scan(Integer eventId, String scannedKey) {
        Optional<Member> memberOpt = memberService.findByAccessToken(scannedKey);
        if (memberOpt.isEmpty()) {
            Optional<Member> invalidatedOwner = memberService.findByInvalidatedAccessToken(scannedKey);
            if (invalidatedOwner.isPresent()) {
                Member owner = invalidatedOwner.get();
                String ownerName = owner.firstName() + " " + owner.lastName();
                logScan(eventId, owner.id(), scannedKey, ScanStatus.TOKEN_INVALIDATED);
                return new ScanResult(ScanStatus.TOKEN_INVALIDATED, ownerName, owner.email(), null, null, null,
                        "Badge invalidé : un nouveau badge a été généré pour " + ownerName + ".");
            }
            logScan(eventId, null, scannedKey, ScanStatus.UNKNOWN_KEY);
            return new ScanResult(ScanStatus.UNKNOWN_KEY, null, null, null, null, null,
                    "Clé inconnue : aucun adhérent ne correspond à ce badge.");
        }

        Member member = memberOpt.get();
        String fullName = member.firstName() + " " + member.lastName();
        String startDate = member.startDate() != null ? member.startDate().toString() : null;
        String endDate = member.endDate() != null ? member.endDate().toString() : null;

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
            return new ScanResult(ScanStatus.OK, fullName, member.email(), startDate, endDate, 1,
                    "Entrée validée pour " + fullName + ".");
        }

        int newScanCount = existing.get().getScanCount() + 1;
        dsl.update(PARTICIPATION)
                .set(PARTICIPATION.SCAN_COUNT, newScanCount)
                .where(PARTICIPATION.ID.eq(existing.get().getId()))
                .execute();
        logScan(eventId, member.id(), scannedKey, ScanStatus.DUPLICATE);
        return new ScanResult(ScanStatus.DUPLICATE, fullName, member.email(), startDate, endDate, newScanCount,
                fullName + " a déjà été scanné (" + newScanCount + " fois).");
    }

    public List<ScanLogEntry> history(Integer eventId) {
        return dsl.select(SCAN_LOG.SCANNED_AT, SCAN_LOG.STATUS, SCAN_LOG.SCANNED_KEY, MEMBER.FIRST_NAME, MEMBER.LAST_NAME)
                .from(SCAN_LOG)
                .leftJoin(MEMBER).on(SCAN_LOG.MEMBER_ID.eq(MEMBER.ID))
                .where(SCAN_LOG.EVENT_ID.eq(eventId))
                .orderBy(SCAN_LOG.SCANNED_AT.desc())
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
