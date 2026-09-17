package fr.reniti.adscan.member;

import static fr.reniti.adscan.database.jooq.Tables.ACCESS_REQUEST;
import static fr.reniti.adscan.database.jooq.Tables.MEMBER;
import static fr.reniti.adscan.database.jooq.Tables.MEMBER_ACCESS_TOKEN;
import static fr.reniti.adscan.database.jooq.Tables.PARTICIPATION;
import static fr.reniti.adscan.database.jooq.Tables.SCAN_LOG;

import fr.reniti.adscan.database.jooq.tables.records.MemberRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    private final DSLContext dsl;

    public MemberRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Member> findAll() {
        return dsl.selectFrom(MEMBER)
                .orderBy(MEMBER.LAST_NAME, MEMBER.FIRST_NAME)
                .fetch(this::toMember);
    }

    public Optional<Member> findById(String id) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.ID.eq(id))
                .fetchOptional(this::toMember);
    }

    public Optional<Member> findByAccessToken(String accessToken) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.ACCESS_TOKEN.eq(accessToken))
                .fetchOptional(this::toMember);
    }

    public Optional<Member> findByEmail(String email) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.EMAIL.eq(email))
                .fetchOptional(this::toMember);
    }

    /** Members whose end date is strictly before {@code today}. */
    public List<Member> findExpired(LocalDate today) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.END_DATE.isNotNull().and(MEMBER.END_DATE.lessThan(today)))
                .orderBy(MEMBER.LAST_NAME, MEMBER.FIRST_NAME)
                .fetch(this::toMember);
    }

    public Member insert(String id, String firstName, String lastName, String email, String phone, String formation,
                          String filiere, Boolean alternant, LocalDate startDate, LocalDate endDate, String accessToken,
                          boolean confirmed) {
        // Set explicitly (rather than relying on the column's DEFAULT CURRENT_TIMESTAMP, which SQLite
        // evaluates in UTC) so created_at/updated_at agree with the JVM's local time zone used everywhere
        // else in the app (scan timestamps, etc.) instead of drifting apart by the local UTC offset.
        LocalDateTime now = LocalDateTime.now();
        MemberRecord record = dsl.insertInto(MEMBER)
                .set(MEMBER.ID, id)
                .set(MEMBER.FIRST_NAME, firstName)
                .set(MEMBER.LAST_NAME, lastName)
                .set(MEMBER.EMAIL, email)
                .set(MEMBER.PHONE, phone)
                .set(MEMBER.FORMATION, formation)
                .set(MEMBER.FILIERE, filiere)
                .set(MEMBER.ALTERNANT, alternant)
                .set(MEMBER.START_DATE, startDate)
                .set(MEMBER.END_DATE, endDate)
                .set(MEMBER.ACCESS_TOKEN, accessToken)
                .set(MEMBER.CONFIRMED, confirmed)
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning()
                .fetchOne();
        return toMember(record);
    }

    /** Also bumps updated_at via the member table's trigger. */
    public void updateConfirmed(String memberId, boolean confirmed) {
        dsl.update(MEMBER)
                .set(MEMBER.CONFIRMED, confirmed)
                .where(MEMBER.ID.eq(memberId))
                .execute();
    }

    /** Also bumps updated_at via the member table's trigger. */
    public void updateAccessToken(String memberId, String newAccessToken) {
        dsl.update(MEMBER)
                .set(MEMBER.ACCESS_TOKEN, newAccessToken)
                .where(MEMBER.ID.eq(memberId))
                .execute();
    }

    /** Also bumps updated_at via the member table's trigger. */
    public void updateEndDate(String memberId, LocalDate newEndDate) {
        dsl.update(MEMBER)
                .set(MEMBER.END_DATE, newEndDate)
                .where(MEMBER.ID.eq(memberId))
                .execute();
    }

    /**
     * Removes the member and every row that references it (scan history, participations, token
     * history, pending access requests). SQLite doesn't enforce foreign keys by default, so the
     * dependent rows are deleted explicitly rather than relying on ON DELETE CASCADE.
     */
    public void delete(String memberId) {
        dsl.deleteFrom(SCAN_LOG).where(SCAN_LOG.MEMBER_ID.eq(memberId)).execute();
        dsl.deleteFrom(PARTICIPATION).where(PARTICIPATION.MEMBER_ID.eq(memberId)).execute();
        dsl.deleteFrom(ACCESS_REQUEST).where(ACCESS_REQUEST.MEMBER_ID.eq(memberId)).execute();
        dsl.deleteFrom(MEMBER_ACCESS_TOKEN).where(MEMBER_ACCESS_TOKEN.MEMBER_ID.eq(memberId)).execute();
        dsl.deleteFrom(MEMBER).where(MEMBER.ID.eq(memberId)).execute();
    }

    private Member toMember(MemberRecord record) {
        return new Member(
                record.getId(),
                record.getFirstName(),
                record.getLastName(),
                record.getEmail(),
                record.getPhone(),
                record.getFormation(),
                record.getFiliere(),
                record.getAlternant(),
                record.getStartDate(),
                record.getEndDate(),
                record.getAccessToken(),
                Boolean.TRUE.equals(record.getConfirmed()),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
