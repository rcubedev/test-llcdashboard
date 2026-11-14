package org.limelightcreatives.dashboard.equipment;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Everything the equipment page needs: per category its units, limit and slots, and every booking. */
public record EquipmentSnapshot(Map<String, CategoryState> categories, List<Booking> bookings) {

    /**
     * @param maxSessions how many slots a team may book per day
     * @param days        slots by date ("YYYY-MM-DD")
     */
    public record CategoryState(List<String> units, int maxSessions, Map<String, List<Slots.Slot>> days) {}

    /**
     * @param team the team's name, as written into the sheet cell
     * @param unit e.g. "Gimbal 2"
     */
    public record Booking(String team, String categoryId, String unit, String date, String slotId) {}

    public static boolean sameTeam(String a, String b) {
        return a.strip().toLowerCase(Locale.ROOT).equals(b.strip().toLowerCase(Locale.ROOT));
    }
}
