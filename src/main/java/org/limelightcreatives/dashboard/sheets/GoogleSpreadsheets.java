package org.limelightcreatives.dashboard.sheets;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/** The real Google Sheets, from the settings in application.yml. */
@Component
public class GoogleSpreadsheets implements Spreadsheets {

    private final Spreadsheet attendees;
    private final Spreadsheet equipment;

    @Autowired
    public GoogleSpreadsheets(DashboardProperties properties, Clock clock) {
        this(properties, RestClient.builder(), clock);
    }

    GoogleSpreadsheets(DashboardProperties properties, RestClient.Builder http, Clock clock) {
        this.attendees = open(properties.attendeeSheet(), "GOOGLE_SERVICE_ACCOUNT_EMAIL, GOOGLE_PRIVATE_KEY_B64 and GOOGLE_SHEET_ID", http, clock);
        this.equipment = open(properties.equipmentSheet(), "GOOGLE_SERVICE_EQUIPMENT_EMAIL, GOOGLE_EQUIPMENT_PRIVATE_KEY_B64 and EQUIPMENT_SHEET_ID", http, clock);
    }

    private static Spreadsheet open(DashboardProperties.Sheet sheet, String settings, RestClient.Builder http, Clock clock) {
        if (!sheet.configured()) return new MissingSpreadsheet(settings);
        return new GoogleSpreadsheet(http.clone(),
                new ServiceAccount(sheet.serviceAccountEmail(), ServiceAccount.parsePrivateKey(sheet.privateKey()), clock),
                sheet.sheetId());
    }

    @Override
    public Spreadsheet attendees() {
        return attendees;
    }

    @Override
    public Spreadsheet equipment() {
        return equipment;
    }
}
