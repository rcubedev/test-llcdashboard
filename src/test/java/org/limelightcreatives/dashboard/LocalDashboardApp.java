package org.limelightcreatives.dashboard;

import org.limelightcreatives.dashboard.sheets.Spreadsheets;
import org.springframework.boot.SpringApplication;
import org.springframework.context.support.GenericApplicationContext;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Runs the dashboard locally with everything it needs: {@code ./gradlew bootTestRun}, then open
 * http://localhost:3000.
 *
 * <ul>
 *     <li>its own PostgreSQL, keeping its data in build/dev-database between runs ({@code ./gradlew clean}
 *     empties it);</li>
 *     <li>the Google Sheets in memory ({@link FakeSpreadsheets}): three participants and an empty booking grid,
 *     fresh every run;</li>
 *     <li>equipment booking on today and tomorrow, so its page has slots to reserve.</li>
 * </ul>
 */
public class LocalDashboardApp {

    public static void main(String[] args) {
        // auth's bootTestRun uses 54329
        String url = TestPostgres.persistent(Path.of("build/dev-database"), 54330);
        System.setProperty("spring.datasource.url", url);
        System.setProperty("spring.profiles.active", "dev");
        // equipment booking as if the event were on: today is open, tomorrow opens at 9
        LocalDate today = LocalDate.now(Times.SYDNEY);
        System.setProperty("equipment.days[0].date", today.toString());
        System.setProperty("equipment.days[0].label", "Today");
        System.setProperty("equipment.days[0].opens", today + "T00:00");
        System.setProperty("equipment.days[1].date", today.plusDays(1).toString());
        System.setProperty("equipment.days[1].label", "Tomorrow");
        System.setProperty("equipment.days[1].opens", today.plusDays(1) + "T09:00");

        var app = new SpringApplication(DashboardApp.class);
        app.addInitializers(context -> ((GenericApplicationContext) context).registerBean(Spreadsheets.class,
                FakeSpreadsheets::new, definition -> definition.setPrimary(true)));
        app.run(args);
    }
}
