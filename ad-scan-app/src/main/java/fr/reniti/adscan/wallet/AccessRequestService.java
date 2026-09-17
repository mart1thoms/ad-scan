package fr.reniti.adscan.wallet;

import static fr.reniti.adscan.database.jooq.Tables.ACCESS_REQUEST;

import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.member.MemberService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short-lived tokens letting an unauthenticated phone fetch a member's Google Wallet link, Apple
 * Wallet pass, or access-card PDF without logging in. Two flavors:
 * <ul>
 *     <li>single-use (staff "share access" QR): 30 minutes, one token shared by all three actions
 *     — whichever is consumed first invalidates the other two;</li>
 *     <li>reusable (post-registration card page): longer-lived and usable for all three actions
 *     repeatedly until it expires.</li>
 * </ul>
 */
@Service
public class AccessRequestService {

    private static final Duration SINGLE_USE_VALIDITY = Duration.ofMinutes(30);

    private final DSLContext dsl;
    private final MemberService memberService;

    public AccessRequestService(DSLContext dsl, MemberService memberService) {
        this.dsl = dsl;
        this.memberService = memberService;
    }

    @Transactional
    public String createToken(String memberId) {
        return createToken(memberId, SINGLE_USE_VALIDITY, true);
    }

    @Transactional
    public String createReusableToken(String memberId, Duration validity) {
        return createToken(memberId, validity, false);
    }

    private String createToken(String memberId, Duration validity, boolean singleUse) {
        String token = UUID.randomUUID().toString();
        dsl.insertInto(ACCESS_REQUEST)
                .set(ACCESS_REQUEST.ID, token)
                .set(ACCESS_REQUEST.MEMBER_ID, memberId)
                .set(ACCESS_REQUEST.EXPIRES_AT, LocalDateTime.now().plus(validity))
                .set(ACCESS_REQUEST.SINGLE_USE, singleUse)
                .execute();
        return token;
    }

    /**
     * Resolves the token to its member if still valid. Single-use tokens are atomically claimed
     * (marked used) on the first successful call; reusable tokens are left untouched and can be
     * resolved again until they expire.
     */
    @Transactional
    public Optional<Member> consume(String token) {
        var row = dsl.selectFrom(ACCESS_REQUEST)
                .where(ACCESS_REQUEST.ID.eq(token))
                .fetchOptional();
        if (row.isEmpty() || row.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(row.get().getSingleUse())) {
            return memberService.findById(row.get().getMemberId());
        }

        String memberId = dsl.update(ACCESS_REQUEST)
                .set(ACCESS_REQUEST.USED_AT, LocalDateTime.now())
                .where(ACCESS_REQUEST.ID.eq(token)
                        .and(ACCESS_REQUEST.USED_AT.isNull())
                        .and(ACCESS_REQUEST.EXPIRES_AT.greaterThan(LocalDateTime.now())))
                .returning(ACCESS_REQUEST.MEMBER_ID)
                .fetchOne(ACCESS_REQUEST.MEMBER_ID);

        if (memberId == null) {
            return Optional.empty();
        }
        return memberService.findById(memberId);
    }
}
