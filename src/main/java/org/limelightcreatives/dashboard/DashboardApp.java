package org.limelightcreatives.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * The Limelight Creatives dashboard: launchpads for participants and staff, each event's dashboard and
 * the staff tools. People sign in through the auth app.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DashboardApp {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApp.class, args);
    }

    /** What "now" is, so tests can set it (an event's run of show, booking windows, deadlines). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
