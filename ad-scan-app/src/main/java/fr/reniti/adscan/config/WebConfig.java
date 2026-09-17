package fr.reniti.adscan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the repo-root branding/ directory (versioned logo/icon/banner images, see
 * branding/README.md) over HTTP, so Google Wallet — which fetches images from its own
 * servers rather than reading a local file like Apple does — can be pointed at them.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/branding/**")
                .addResourceLocations("file:branding/");
    }
}
