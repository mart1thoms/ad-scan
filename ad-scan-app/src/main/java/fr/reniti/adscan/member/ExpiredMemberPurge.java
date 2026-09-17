package fr.reniti.adscan.member;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automatically deletes members once their membership end date has passed. Runs at startup (so a
 * server that was off during the night still catches up) and then every day shortly after midnight.
 */
@Component
public class ExpiredMemberPurge {

    private static final Logger log = LoggerFactory.getLogger(ExpiredMemberPurge.class);

    private final MemberService memberService;

    public ExpiredMemberPurge(MemberService memberService) {
        this.memberService = memberService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        purge();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void nightly() {
        purge();
    }

    public void purge() {
        int removed = memberService.purgeExpired(LocalDate.now());
        if (removed > 0) {
            log.info("Purge des adhésions expirées : {} adhérent(s) supprimé(s)", removed);
        }
    }
}
