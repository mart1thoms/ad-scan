package fr.reniti.adscan.member;

import static fr.reniti.adscan.database.jooq.Tables.MEMBER_ACCESS_TOKEN;

import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Full history of every access token ever issued to a member (see V2 migration for why). */
@Repository
public class MemberAccessTokenRepository {

    private final DSLContext dsl;

    public MemberAccessTokenRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void recordIssued(String memberId, String token) {
        // Explicit local time rather than the column's DEFAULT CURRENT_TIMESTAMP (UTC in SQLite) —
        // see MemberRepository.insert for why.
        dsl.insertInto(MEMBER_ACCESS_TOKEN)
                .set(MEMBER_ACCESS_TOKEN.MEMBER_ID, memberId)
                .set(MEMBER_ACCESS_TOKEN.TOKEN, token)
                .set(MEMBER_ACCESS_TOKEN.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    public void invalidate(String token) {
        dsl.update(MEMBER_ACCESS_TOKEN)
                .set(MEMBER_ACCESS_TOKEN.INVALIDATED_AT, LocalDateTime.now())
                .where(MEMBER_ACCESS_TOKEN.TOKEN.eq(token).and(MEMBER_ACCESS_TOKEN.INVALIDATED_AT.isNull()))
                .execute();
    }

    public Optional<LocalDateTime> findActiveTokenIssuedAt(String memberId) {
        return dsl.select(MEMBER_ACCESS_TOKEN.CREATED_AT)
                .from(MEMBER_ACCESS_TOKEN)
                .where(MEMBER_ACCESS_TOKEN.MEMBER_ID.eq(memberId).and(MEMBER_ACCESS_TOKEN.INVALIDATED_AT.isNull()))
                .fetchOptional(MEMBER_ACCESS_TOKEN.CREATED_AT);
    }

    public Optional<String> findMemberIdByInvalidatedToken(String token) {
        return dsl.select(MEMBER_ACCESS_TOKEN.MEMBER_ID)
                .from(MEMBER_ACCESS_TOKEN)
                .where(MEMBER_ACCESS_TOKEN.TOKEN.eq(token).and(MEMBER_ACCESS_TOKEN.INVALIDATED_AT.isNotNull()))
                .fetchOptional(MEMBER_ACCESS_TOKEN.MEMBER_ID);
    }
}
