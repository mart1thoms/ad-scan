package fr.reniti.adscan;

import fr.reniti.adscan.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class AdScanApplication {

    public static void main(String[] args) {
        // SQLite won't create the parent directory of the db file on its own.
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        SpringApplication.run(AdScanApplication.class, args);
    }
}
