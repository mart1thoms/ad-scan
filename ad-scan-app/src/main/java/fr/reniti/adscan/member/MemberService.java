package fr.reniti.adscan.member;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private static final String ACCESS_TOKEN_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ACCESS_TOKEN_LENGTH = 38;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Membership years run September 1st -> August 31st (e.g. "2026-2027" starts 2026-09-01 and
    // ends 2027-08-31; from 2027-09-01 onward it's "2027-2028").
    private static final int MEMBERSHIP_YEAR_START_MONTH = 9;
    private static final int MEMBERSHIP_YEAR_START_DAY = 1;

    private final MemberRepository memberRepository;
    private final MemberAccessTokenRepository memberAccessTokenRepository;

    public MemberService(MemberRepository memberRepository, MemberAccessTokenRepository memberAccessTokenRepository) {
        this.memberRepository = memberRepository;
        this.memberAccessTokenRepository = memberAccessTokenRepository;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Optional<Member> findById(String id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> findByAccessToken(String accessToken) {
        return memberRepository.findByAccessToken(accessToken);
    }

    /** Resolves the member an INVALIDATED (regenerated-away) token used to belong to, if any. */
    public Optional<Member> findByInvalidatedAccessToken(String token) {
        return memberAccessTokenRepository.findMemberIdByInvalidatedToken(token)
                .flatMap(this::findById);
    }

    public Optional<LocalDateTime> findActiveTokenIssuedAt(String memberId) {
        return memberAccessTokenRepository.findActiveTokenIssuedAt(memberId);
    }

    /** Created by staff from the back-office: trusted (confirmed) immediately. */
    @Transactional
    public Member create(String firstName, String lastName, String email, String phone, String formation,
                          String filiere, Boolean alternant, LocalDate startDate, LocalDate endDate) {
        String id = UUID.randomUUID().toString();
        String accessToken = generateAccessToken();
        Member member = memberRepository.insert(id, firstName, lastName, email, phone, formation,
                normalizeFiliere(formation, filiere), normalizeAlternant(formation, alternant), startDate, endDate,
                accessToken, true);
        memberAccessTokenRepository.recordIssued(id, accessToken);
        return member;
    }

    /**
     * Public self-registration (see /register): created unconfirmed ("non cotisant") until a staff
     * member validates it from the back-office. The access token is issued immediately so the
     * registrant can download their wallet/PDF card right away.
     */
    @Transactional
    public Member registerPublic(String firstName, String lastName, String email, String phone, String formation,
                                  String filiere, Boolean alternant) {
        if (isEmailAlias(email)) {
            throw new IllegalArgumentException("Les alias d'email (contenant un « + ») ne sont pas autorisés.");
        }
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
        String id = UUID.randomUUID().toString();
        String accessToken = generateAccessToken();
        LocalDate startDate = currentMembershipYearStart(LocalDate.now());
        LocalDate endDate = startDate.plusYears(1).minusDays(1);
        Member member = memberRepository.insert(id, firstName, lastName, email, phone, formation,
                normalizeFiliere(formation, filiere), normalizeAlternant(formation, alternant), startDate, endDate,
                accessToken, false);
        memberAccessTokenRepository.recordIssued(id, accessToken);
        return member;
    }

    // Filière / alternance only make sense for 3A-5A students; anything else is stored as "not applicable".
    private static String normalizeFiliere(String formation, String filiere) {
        if (!FormationOptions.hasFiliere(formation) || filiere == null || filiere.isBlank()) {
            return null;
        }
        return filiere.trim();
    }

    private static Boolean normalizeAlternant(String formation, Boolean alternant) {
        return FormationOptions.hasFiliere(formation) ? alternant : null;
    }

    // Blocks the "local+tag@domain" alias syntax (supported by Gmail, Outlook, ProtonMail, etc.) so
    // a single real mailbox can't be used to register more than once under the unique-email rule.
    private static boolean isEmailAlias(String email) {
        int atIndex = email.indexOf('@');
        String localPart = atIndex >= 0 ? email.substring(0, atIndex) : email;
        return localPart.contains("+");
    }

    private static LocalDate currentMembershipYearStart(LocalDate today) {
        LocalDate septemberFirstThisYear = LocalDate.of(today.getYear(), MEMBERSHIP_YEAR_START_MONTH, MEMBERSHIP_YEAR_START_DAY);
        return today.isBefore(septemberFirstThisYear) ? septemberFirstThisYear.minusYears(1) : septemberFirstThisYear;
    }

    /** Validates a self-registered member from the back-office: they now count as a paying member. */
    @Transactional
    public Member confirm(String memberId) {
        findById(memberId).orElseThrow(() -> new IllegalArgumentException("Adhérent introuvable"));
        memberRepository.updateConfirmed(memberId, true);
        return findById(memberId).orElseThrow(() -> new IllegalStateException("Adhérent disparu pendant la confirmation"));
    }

    /** Extends (or shortens) the member's adhesion by changing its end date. */
    @Transactional
    public Member extendMembership(String memberId, LocalDate newEndDate) {
        Member member = findById(memberId).orElseThrow(() -> new IllegalArgumentException("Adhérent introuvable"));
        if (newEndDate == null || newEndDate.isBefore(member.startDate())) {
            throw new IllegalArgumentException("La nouvelle date de fin doit être postérieure à la date de début");
        }
        memberRepository.updateEndDate(memberId, newEndDate);
        return findById(memberId).orElseThrow(() -> new IllegalStateException("Adhérent disparu pendant la prolongation"));
    }

    /** Invalidates the member's current token (and any wallet pass built from it) and issues a new one. */
    @Transactional
    public Member regenerateAccessToken(String memberId) {
        Member member = findById(memberId).orElseThrow(() -> new IllegalArgumentException("Adhérent introuvable"));
        memberAccessTokenRepository.invalidate(member.accessToken());
        String newToken = generateAccessToken();
        memberRepository.updateAccessToken(memberId, newToken);
        memberAccessTokenRepository.recordIssued(memberId, newToken);
        return findById(memberId).orElseThrow(() -> new IllegalStateException("Adhérent disparu pendant la régénération"));
    }

    /** Permanently removes the member together with their scan history and tokens. */
    @Transactional
    public void delete(String memberId) {
        findById(memberId).orElseThrow(() -> new IllegalArgumentException("Adhérent introuvable"));
        memberRepository.delete(memberId);
    }

    /**
     * Removes every member whose membership end date is already past (strictly before today), and
     * returns how many were removed. Run automatically once a day and at startup, see
     * {@link ExpiredMemberPurge}.
     */
    @Transactional
    public int purgeExpired(LocalDate today) {
        List<Member> expired = memberRepository.findExpired(today);
        for (Member member : expired) {
            memberRepository.delete(member.id());
        }
        return expired.size();
    }

    private static String generateAccessToken() {
        StringBuilder token = new StringBuilder(ACCESS_TOKEN_LENGTH);
        for (int i = 0; i < ACCESS_TOKEN_LENGTH; i++) {
            token.append(ACCESS_TOKEN_CHARS.charAt(RANDOM.nextInt(ACCESS_TOKEN_CHARS.length())));
        }
        return token.toString();
    }
}
