package org.limelightcreatives.dashboard;

import org.limelightcreatives.dashboard.sheets.Spreadsheet;
import org.limelightcreatives.dashboard.sheets.Spreadsheets;

import java.util.ArrayList;
import java.util.List;

/**
 * The two Google Sheets, in memory, laid out like the real ones: Attendees and Check-ins tabs, and an
 * equipment booking tab with "Gimbal Day 1" style tables. Used by tests and by {@code ./gradlew bootTestRun}.
 */
public class FakeSpreadsheets implements Spreadsheets {

    public static final String EQUIPMENT_TAB = "Bookings";
    /** Each booking table: a header row, then one row per hour from 10 AM to 4 PM. */
    public static final List<String> SLOTS = List.of("10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM");

    private InMemorySpreadsheet attendees;
    private InMemorySpreadsheet equipment;

    public FakeSpreadsheets() {
        reset();
    }

    /** Back to the starting data: three participants, nobody checked in, nothing booked. */
    public synchronized void reset() {
        attendees = new InMemorySpreadsheet()
                .tab("Attendees", List.of(
                        List.of("Name", "Email", "Phone No.", "Emergency No.", "Registration ID"),
                        List.of("Sam Lee", "sam@example.com", "0400 000 001", "0400 000 101", "LC-001"),
                        List.of("Alex Kim", "alex@example.com", "0400 000 002", "0400 000 102", "LC-002"),
                        List.of("Jordan Wu", "jordan@example.com", "0400 000 003", "0400 000 103", "LC-003")))
                .tab("Check-ins", List.of(List.of("Participant Name", "Action", "Timestamp", "Registration ID")));

        equipment = new InMemorySpreadsheet().tab(EQUIPMENT_TAB, List.of());
        String[][] config = {
                {"Gimbal Quantity", "3"}, {"Max Gimbal Sessions", "2"},
                {"Microphone Quantity", "2"}, {"Max Microphone Sessions", "2"},
                {"Tripod Quantity", "4"}, {"Max Tripod Sessions", "1"}};
        for (int i = 0; i < config.length; i++) {
            equipment.set(EQUIPMENT_TAB, i, 0, config[i][0]);
            equipment.set(EQUIPMENT_TAB, i, 1, config[i][1]);
        }
        String[] prefixes = {"Gimbal", "Microphone", "Tripod"};
        int[] units = {3, 2, 4};
        for (int c = 0; c < prefixes.length; c++) {
            for (int day = 0; day < 2; day++) {
                int top = 8 + c * (SLOTS.size() + 3);
                int left = day * (units[c] + 2);
                List<String> header = new ArrayList<>(List.of("Timeslot"));
                for (int u = 1; u <= units[c]; u++) header.add("Slot " + u);
                for (int j = 0; j < header.size(); j++) equipment.set(EQUIPMENT_TAB, top, left + j, header.get(j));
                for (int s = 0; s < SLOTS.size(); s++) equipment.set(EQUIPMENT_TAB, top + 1 + s, left, SLOTS.get(s));
                equipment.table(EQUIPMENT_TAB, new Spreadsheet.Table(prefixes[c] + " Day " + (day + 1),
                        top, top + 1 + SLOTS.size(), left, left + 1 + units[c]));
            }
        }
    }

    @Override
    public synchronized InMemorySpreadsheet attendees() {
        return attendees;
    }

    @Override
    public synchronized InMemorySpreadsheet equipment() {
        return equipment;
    }
}
