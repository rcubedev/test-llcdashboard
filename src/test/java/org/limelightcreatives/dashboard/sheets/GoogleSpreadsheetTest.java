package org.limelightcreatives.dashboard.sheets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Google Sheets client against a pretend Google (a local HTTP server): signs in as the service account,
 * and sends the requests the Sheets API expects.
 */
class GoogleSpreadsheetTest {

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final Map<String, String> bodies = new ConcurrentHashMap<>();
    private String base;
    private RSAPublicKey publicKey;
    private GoogleSpreadsheet sheet;

    @BeforeEach
    void start() throws Exception {
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        var pair = keys.generateKeyPair();
        publicKey = (RSAPublicKey) pair.getPublic();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String line = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath()
                    + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery());
            synchronized (requests) {
                requests.add(line + " auth=" + exchange.getRequestHeaders().getFirst("Authorization"));
            }
            bodies.put(line, body);
            String response;
            if (exchange.getRequestURI().getPath().equals("/token")) {
                response = "{\"access_token\":\"ya29.token\",\"expires_in\":3599,\"token_type\":\"Bearer\"}";
            } else if (exchange.getRequestURI().getPath().endsWith("/sheet-1")) {
                response = """
                        {"sheets":[{"properties":{"sheetId":0,"title":"Attendees"}},
                                   {"properties":{"sheetId":42,"title":"Bookings"},
                                    "tables":[{"name":"Gimbal Day 1","range":{"sheetId":42,"startRowIndex":8,"endRowIndex":16,"startColumnIndex":0,"endColumnIndex":4}}]}]}""";
            } else if (exchange.getRequestMethod().equals("GET")) {
                response = "{\"range\":\"Attendees!A2:E3\",\"values\":[[\"Sam\",\"\",\"0400\"],[],[\"Alex\",3]]}";
            } else {
                response = "{}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();

        var clock = Clock.fixed(Instant.parse("2026-10-24T00:30:00Z"), ZoneOffset.UTC);
        var account = new ServiceAccount("bot@project.iam.gserviceaccount.com", pair.getPrivate(), clock);
        sheet = new GoogleSpreadsheet(RestClient.builder(), account, "sheet-1", base, base + "/token");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void readsValuesAsText() {
        assertThat(sheet.values("'Attendees'!A2:E")).containsExactly(List.of("Sam", "", "0400"), List.of(), List.of("Alex", "3"));
        assertThat(requests.get(0)).startsWith("POST /token");
        assertThat(requests.get(1)).isEqualTo(
                "GET /v4/spreadsheets/sheet-1/values/'Attendees'!A2:E?valueRenderOption=FORMATTED_VALUE auth=Bearer ya29.token");
    }

    @Test
    void signsInWithAServiceAccountJwt() throws Exception {
        sheet.values("'Attendees'!A2:E");
        sheet.values("'Attendees'!A2:E");
        assertThat(requests).filteredOn(r -> r.startsWith("POST /token")).hasSize(1); // the token is reused

        String form = bodies.entrySet().stream().filter(e -> e.getKey().startsWith("POST /token")).findFirst().orElseThrow().getValue();
        Map<String, String> fields = new java.util.HashMap<>();
        for (String pair : form.split("&")) {
            String[] kv = pair.split("=", 2);
            fields.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        assertThat(fields.get("grant_type")).isEqualTo("urn:ietf:params:oauth:grant-type:jwt-bearer");
        String[] jwt = fields.get("assertion").split("\\.");
        assertThat(new String(Base64.getUrlDecoder().decode(jwt[0]))).isEqualTo("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        assertThat(new String(Base64.getUrlDecoder().decode(jwt[1]))).isEqualTo(
                "{\"iss\":\"bot@project.iam.gserviceaccount.com\",\"scope\":\"https://www.googleapis.com/auth/spreadsheets\","
                        + "\"aud\":\"https://oauth2.googleapis.com/token\",\"iat\":1792801800,\"exp\":1792805400}");
        var verify = Signature.getInstance("SHA256withRSA");
        verify.initVerify(publicKey);
        verify.update((jwt[0] + "." + jwt[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verify.verify(Base64.getUrlDecoder().decode(jwt[2]))).isTrue();
    }

    @Test
    void writesAsTyped() {
        sheet.append("'Check-ins'!A:D", List.of("Sam", "CHECK IN", "11:30:00 24/10/26", "=1+1"));
        sheet.update("'Bookings'!B10", List.of(List.of("The Grips")));
        sheet.clear("'Bookings'!B10");
        assertThat(requests).anySatisfy(r -> assertThat(r).startsWith("POST /v4/spreadsheets/sheet-1/values/'Check-ins'!A:D:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS"));
        assertThat(requests).anySatisfy(r -> assertThat(r).startsWith("PUT /v4/spreadsheets/sheet-1/values/'Bookings'!B10?valueInputOption=RAW"));
        assertThat(requests).anySatisfy(r -> assertThat(r).startsWith("POST /v4/spreadsheets/sheet-1/values/'Bookings'!B10:clear"));
        assertThat(bodies.values()).contains("{\"values\":[[\"Sam\",\"CHECK IN\",\"11:30:00 24/10/26\",\"=1+1\"]]}", "{\"values\":[[\"The Grips\"]]}");
    }

    @Test
    void tabsTablesAndDeletingRows() {
        var tabs = sheet.tabs();
        assertThat(tabs).extracting(Spreadsheet.Tab::title).containsExactly("Attendees", "Bookings");
        assertThat(tabs.get(1).tables()).containsExactly(new Spreadsheet.Table("Gimbal Day 1", 8, 16, 0, 4));
        assertThat(URLDecoder.decode(requests.get(1), StandardCharsets.UTF_8)).contains("fields=sheets(properties(sheetId,title),tables(name,range))");

        sheet.deleteRow("Bookings", 5);
        assertThat(bodies.entrySet()).anySatisfy(e -> {
            assertThat(e.getKey()).startsWith("POST /v4/spreadsheets/sheet-1:batchUpdate");
            assertThat(e.getValue()).contains("\"deleteDimension\"", "\"sheetId\":42", "\"dimension\":\"ROWS\"", "\"startIndex\":4", "\"endIndex\":5");
        });
    }

    @Test
    void privateKeysHoweverTheyWerePasted() throws Exception {
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        var key = (RSAPrivateKey) keys.generateKeyPair().getPrivate();
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";

        assertThat(ServiceAccount.parsePrivateKey(pem).getEncoded()).isEqualTo(key.getEncoded());
        assertThat(ServiceAccount.parsePrivateKey("\"" + pem.replace("\n", "\\n") + "\"").getEncoded()).isEqualTo(key.getEncoded());
        assertThat(ServiceAccount.parsePrivateKey(Base64.getEncoder().encodeToString(pem.getBytes())).getEncoded()).isEqualTo(key.getEncoded());
    }
}
