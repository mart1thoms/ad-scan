package fr.reniti.adscan.member;

import static fr.reniti.adscan.database.jooq.Tables.MEMBER;

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

    public Member insert(String id, String firstName, String lastName, String email, String phone, String formation,
                          LocalDate startDate, LocalDate endDate, String accessToken, boolean confirmed) {
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

    private Member toMember(MemberRecord record) {
        return new Member(
                record.getId(),
                record.getFirstName(),
                record.getLastName(),
                record.getEmail(),
                record.getPhone(),
                record.getFormation(),
                record.getStartDate(),
                record.getEndDate(),
                record.getAccessToken(),
                Boolean.TRUE.equals(record.getConfirmed()),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
