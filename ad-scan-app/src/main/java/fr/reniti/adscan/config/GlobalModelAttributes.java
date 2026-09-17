package fr.reniti.adscan.config;

import fr.reniti.adscan.wallet.AppleWalletService;
import fr.reniti.adscan.wallet.GoogleWalletService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@ControllerAdvice
public class GlobalModelAttributes {

    private final AppProperties appProperties;
    private final GoogleWalletService googleWalletService;
    private final AppleWalletService appleWalletService;

    public GlobalModelAttributes(AppProperties appProperties, GoogleWalletService googleWalletService,
                                  AppleWalletService appleWalletService) {
        this.appProperties = appProperties;
        this.googleWalletService = googleWalletService;
        this.appleWalletService = appleWalletService;
    }

    @ModelAttribute("appName")
    public String appName() {
        return appProperties.name();
    }

    @ModelAttribute("appIllustration")
    public String appIllustration() {
        return appProperties.illustrationImage();
    }

    @ModelAttribute("googleWalletEnabled")
    public boolean googleWalletEnabled() {
        return googleWalletService.isEnabled();
    }

    @ModelAttribute("appleWalletEnabled")
    public boolean appleWalletEnabled() {
        return appleWalletService.isEnabled();
    }

    // Prefers the configured public-base-url (the real address members would actually reach) and
    // falls back to whatever host the current admin request used, so the link is never blank.
    @ModelAttribute("registerUrl")
    public String registerUrl() {
        String publicBaseUrl = appProperties.publicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl + "/register";
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/register").toUriString();
    }
}
