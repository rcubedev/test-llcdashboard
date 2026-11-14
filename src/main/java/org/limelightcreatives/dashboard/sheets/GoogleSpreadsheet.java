package org.limelightcreatives.dashboard.sheets;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One spreadsheet through the Google Sheets REST API (v4), signed in as a service account. Share the
 * spreadsheet with the service account's email as an Editor.
 */
class GoogleSpreadsheet implements Spreadsheet {

    private final RestClient google;
    private final ServiceAccount account;
    private final String spreadsheetId;
    private final String sheetsApi;
    private final String tokenUrl;

    private String token;
    private Instant tokenExpires = Instant.EPOCH;

    GoogleSpreadsheet(RestClient.Builder http, ServiceAccount account, String spreadsheetId) {
        this(http, account, spreadsheetId, "https://sheets.googleapis.com", ServiceAccount.TOKEN_URL);
    }

    /** For tests: Google's addresses replaced with a local server's. */
    GoogleSpreadsheet(RestClient.Builder http, ServiceAccount account, String spreadsheetId, String sheetsApi, String tokenUrl) {
        this.sheetsApi = sheetsApi;
        this.tokenUrl = tokenUrl;
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(Duration.ofSeconds(20));
        this.google = http.requestFactory(timeouts).build();
        this.account = account;
        this.spreadsheetId = spreadsheetId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<List<String>> values(String range) {
        Map<String, Object> response = google.get()
                .uri(sheetsApi + "/v4/spreadsheets/{id}/values/{range}?valueRenderOption=FORMATTED_VALUE",
                        spreadsheetId, range)
                .header("Authorization", "Bearer " + token())
                .retrieve().body(Map.class);
        List<List<String>> rows = new ArrayList<>();
        if (response != null && response.get("values") instanceof List<?> values) {
            for (Object row : values) {
                List<String> cells = new ArrayList<>();
                if (row instanceof List<?> list) for (Object cell : list) cells.add(cell == null ? "" : String.valueOf(cell));
                rows.add(cells);
            }
        }
        return rows;
    }

    @Override
    public void append(String range, List<String> row) {
        google.post()
                // RAW, so a scanned value like "=IMPORTDATA(...)" is stored as text, not run as a formula.
                .uri(sheetsApi + "/v4/spreadsheets/{id}/values/{range}:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS",
                        spreadsheetId, range)
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("values", List.of(row)))
                .retrieve().toBodilessEntity();
    }

    @Override
    public void update(String range, List<List<String>> rows) {
        google.put()
                // RAW, so formulas aren't run and phone numbers keep their leading zeros.
                .uri(sheetsApi + "/v4/spreadsheets/{id}/values/{range}?valueInputOption=RAW", spreadsheetId, range)
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("values", rows))
                .retrieve().toBodilessEntity();
    }

    @Override
    public void clear(String range) {
        google.post()
                .uri(sheetsApi + "/v4/spreadsheets/{id}/values/{range}:clear", spreadsheetId, range)
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve().toBodilessEntity();
    }

    @Override
    public void deleteRow(String tab, int row) {
        int sheetId = tabs().stream().filter(t -> t.title().equals(tab)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Tab \"" + tab + "\" not found"))
                .sheetId();
        var range = Map.of("sheetId", sheetId, "dimension", "ROWS", "startIndex", row - 1, "endIndex", row);
        google.post()
                .uri(sheetsApi + "/v4/spreadsheets/{id}:batchUpdate", spreadsheetId)
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requests", List.of(Map.of("deleteDimension", Map.of("range", range)))))
                .retrieve().toBodilessEntity();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Tab> tabs() {
        Map<String, Object> response = google.get()
                .uri(sheetsApi + "/v4/spreadsheets/{id}?fields={fields}", spreadsheetId,
                        "sheets(properties(sheetId,title),tables(name,range))")
                .header("Authorization", "Bearer " + token())
                .retrieve().body(Map.class);
        List<Tab> tabs = new ArrayList<>();
        if (response == null || !(response.get("sheets") instanceof List<?> sheets)) return tabs;
        for (Object s : sheets) {
            if (!(s instanceof Map<?, ?> sheet) || !(sheet.get("properties") instanceof Map<?, ?> properties)) continue;
            List<Table> tables = new ArrayList<>();
            if (sheet.get("tables") instanceof List<?> list) {
                for (Object t : list) {
                    if (!(t instanceof Map<?, ?> table)) continue;
                    Map<?, ?> range = table.get("range") instanceof Map<?, ?> r ? r : Map.of();
                    tables.add(new Table(String.valueOf(table.get("name") == null ? "" : table.get("name")),
                            number(range, "startRowIndex"), number(range, "endRowIndex"),
                            number(range, "startColumnIndex"), number(range, "endColumnIndex")));
                }
            }
            tabs.add(new Tab(number(properties, "sheetId"), String.valueOf(properties.get("title")), tables));
        }
        return tabs;
    }

    private static int number(Map<?, ?> map, String key) {
        return map.get(key) instanceof Number n ? n.intValue() : 0;
    }

    /** An access token, reused until a few minutes before it expires. */
    @SuppressWarnings("unchecked")
    private synchronized String token() {
        Instant now = account.clock().instant();
        if (token != null && now.isBefore(tokenExpires)) return token;

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("assertion", account.assertion());
        Map<String, Object> response = google.post().uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(Map.class);
        if (response == null || !(response.get("access_token") instanceof String accessToken)) {
            throw new IllegalStateException("Google didn't return an access token");
        }
        long lifetime = response.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        token = accessToken;
        tokenExpires = now.plusSeconds(Math.max(60, lifetime - 300));
        return token;
    }
}
