package fr.reniti.adscan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String name,
        String illustrationImage,
        String publicBaseUrl,
        Security security,
        WalletBranding wallet,
        GoogleWallet googleWallet,
        AppleWallet appleWallet
) {

    public record Security(String password) {
    }

    /** Branding shared by both the Google and Apple membership cards. Image files live at fixed
     * paths under branding/ (see branding/README.md) — not configurable, so there's nothing to
     * mismatch between the two wallets. */
    public record WalletBranding(String name, String backgroundColor) {
    }

    /** Enabled as soon as issuerId is set — no separate on/off flag to forget to flip. */
    public record GoogleWallet(String issuerId, String classSuffix, String serviceAccountKeyPath) {
    }

    /** Enabled as soon as both passTypeIdentifier and teamIdentifier are set — no separate on/off flag. */
    public record AppleWallet(String passTypeIdentifier, String teamIdentifier, String p12Path, String p12Password,
                               String wwdrCertPath) {
    }
}
