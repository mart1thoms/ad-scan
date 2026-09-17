package fr.reniti.adscan.member;

import static fr.reniti.adscan.database.jooq.Tables.EVENT;
import static fr.reniti.adscan.database.jooq.Tables.SCAN_LOG;

import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Cross-event scan history for a single member (as opposed to ScanService.history, which is per-event). */
@Repository
public class MemberScanHistoryRepository {

    private final DSLContext dsl;

    public MemberScanHistoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<MemberScanHistoryEntry> findForMember(String memberId) {
        return dsl.select(EVENT.NAME, EVENT.EVENT_DATE, SCAN_LOG.SCANNED_AT, SCAN_LOG.STATUS)
                .from(SCAN_LOG)
                .join(EVENT).on(SCAN_LOG.EVENT_ID.eq(EVENT.ID))
                .where(SCAN_LOG.MEMBER_ID.eq(memberId))
                .orderBy(SCAN_LOG.SCANNED_AT.desc())
                .fetch(record -> new MemberScanHistoryEntry(
                        record.get(EVENT.NAME),
                        record.get(EVENT.EVENT_DATE),
                        record.get(SCAN_LOG.SCANNED_AT),
                        record.get(SCAN_LOG.STATUS)
                ));
    }
}
