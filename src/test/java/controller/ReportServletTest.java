package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonReportRepository;
import security.SecurityFilter;
import storage.JsonStore;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ReportServletTest {
    @TempDir
    Path tempDirectory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listingAndMyReportsSerializeDatesAsIsoStrings() throws Exception {
        JsonReportRepository repository = repository();
        Report report = report("LOST", "owner-a");
        repository.create(report);
        HttpSession session = session(Map.of("userId", "owner-a"));

        ResponseCapture listing = new ResponseCapture();
        new ReportServlet(repository).doGet(
                request(Map.of(), null, "/view-items", "GET", session),
                listing.proxy()
        );
        JsonNode listingJson = objectMapper.readTree(listing.body());
        assertEquals(report.getReportDate().toString(),
                listingJson.get(0).get("lostDate").asText());
        assertEquals(report.getPrivateContact(), listingJson.get(0).get("contactInfo").asText());

        ResponseCapture myReports = new ResponseCapture();
        new MyItemsServlet(repository).doGet(
                request(Map.of(), null, "/my-items", "GET", session),
                myReports.proxy()
        );
        JsonNode myReportsJson = objectMapper.readTree(myReports.body());
        assertEquals(report.getReportDate().toString(),
                myReportsJson.get(0).get("reportDate").asText());
    }

    @Test
    void reportListingsRequireAuthentication() throws Exception {
        ResponseCapture response = new ResponseCapture();
        new ReportServlet(repository()).doGet(
                request(Map.of(), null, "/view-items", "GET", null),
                response.proxy()
        );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status());
        assertEquals(0, response.body().length);
    }

    @Test
    void editPreservesReturnedStatusAndContactWhenContactIsOmitted() throws Exception {
        JsonReportRepository repository = repository();
        Report saved = report("LOST", "owner-a");
        saved.setStatus("RETURNED");
        repository.create(saved);

        Map<String, String> params = new HashMap<>();
        params.put("type", "LOST");
        params.put("itemName", "Updated item");
        params.put("category", "Keys");
        params.put("description", "Updated description");
        params.put("lostLocation", "Library");
        params.put("lostDate", saved.getReportDate().toString());

        ResponseCapture response = new ResponseCapture();
        new ReportServlet(repository).doPost(
                request(params, "/" + saved.getId(), "/api/reports", "POST",
                        session(Map.of("userId", "owner-a"))),
                response.proxy()
        );

        Report updated = repository.find("LOST", saved.getId()).orElseThrow();
        assertEquals(201, response.status());
        assertEquals("RETURNED", updated.getStatus());
        assertEquals(saved.getPrivateContact(), updated.getPrivateContact());
        assertEquals("Updated item", updated.getTitle());
    }

    @Test
    void returnActionRequiresOwnerAndValidCsrfAndPreservesReportIdentity() throws Exception {
        JsonReportRepository repository = repository();
        Report saved = report("FOUND", "owner-a");
        repository.create(saved);
        ReportServlet servlet = new ReportServlet(repository);
        SecurityFilter filter = new SecurityFilter();

        ResponseCapture unauthorized = new ResponseCapture();
        servlet.doPost(
                request(Map.of("type", "FOUND"), "/" + saved.getId() + "/return",
                        "/api/reports", "POST", session(Map.of("userId", "owner-b"))),
                unauthorized.proxy()
        );
        assertEquals(HttpServletResponse.SC_FORBIDDEN, unauthorized.status());
        assertEquals("FOUND", repository.find("FOUND", saved.getId()).orElseThrow().getStatus());

        Map<String, String> editParameters = Map.of(
                "type", "FOUND",
                "itemName", "Tampered item",
                "category", "Other",
                "description", "Tampered",
                "foundLocation", "Elsewhere",
                "foundDate", saved.getReportDate().toString()
        );
        ResponseCapture unauthorizedEdit = new ResponseCapture();
        servlet.doPost(
                request(editParameters, "/" + saved.getId(), "/api/reports", "POST",
                        session(Map.of("userId", "owner-b"))),
                unauthorizedEdit.proxy()
        );
        assertEquals(HttpServletResponse.SC_FORBIDDEN, unauthorizedEdit.status());
        assertEquals("Campus item", repository.find("FOUND", saved.getId()).orElseThrow().getTitle());

        HttpSession ownerSession = session(Map.of("userId", "owner-a", "csrfToken", "valid-token"));
        HttpServletRequest missingTokenRequest = request(
                Map.of("type", "FOUND"), "/" + saved.getId() + "/return",
                "/api/reports", "POST", ownerSession
        );
        ResponseCapture missingToken = new ResponseCapture();
        AtomicBoolean missingTokenReachedServlet = new AtomicBoolean();
        filter.doFilter(missingTokenRequest, missingToken.proxy(), (request, response) -> {
            missingTokenReachedServlet.set(true);
            servlet.doPost((HttpServletRequest) request, (HttpServletResponse) response);
        });
        assertEquals(HttpServletResponse.SC_FORBIDDEN, missingToken.status());
        assertFalse(missingTokenReachedServlet.get());
        assertEquals("FOUND", repository.find("FOUND", saved.getId()).orElseThrow().getStatus());

        ResponseCapture invalidToken = new ResponseCapture();
        AtomicBoolean invalidTokenReachedServlet = new AtomicBoolean();
        filter.doFilter(
                request(Map.of("type", "FOUND", "csrfToken", "invalid-token"),
                        "/" + saved.getId() + "/return", "/api/reports", "POST", ownerSession),
                invalidToken.proxy(),
                (request, response) -> invalidTokenReachedServlet.set(true)
        );
        assertEquals(HttpServletResponse.SC_FORBIDDEN, invalidToken.status());
        assertFalse(invalidTokenReachedServlet.get());
        assertEquals("FOUND", repository.find("FOUND", saved.getId()).orElseThrow().getStatus());

        Map<String, String> params = Map.of("type", "FOUND", "csrfToken", "valid-token");
        ResponseCapture validToken = new ResponseCapture();
        filter.doFilter(
                request(params, "/" + saved.getId() + "/return",
                        "/api/reports", "POST", ownerSession),
                validToken.proxy(),
                (request, response) -> servlet.doPost(
                        (HttpServletRequest) request, (HttpServletResponse) response)
        );

        Report returned = repository.find("FOUND", saved.getId()).orElseThrow();
        assertEquals(HttpServletResponse.SC_NO_CONTENT, validToken.status());
        assertEquals("RETURNED", returned.getStatus());
        assertEquals(saved.getId(), returned.getId());
        assertEquals(saved.getOwnerId(), returned.getOwnerId());
        assertEquals(saved.getCreatedAt(), returned.getCreatedAt());
        assertEquals(saved.getPrivateContact(), returned.getPrivateContact());
        assertTrue(returned.getUpdatedAt().isAfter(saved.getUpdatedAt()));
    }

    @Test
    void unsupportedReportTypesReturnBadRequestForGetPostAndDelete() throws Exception {
        ReportServlet servlet = new ReportServlet(repository());
        HttpSession session = session(Map.of("userId", "owner-a"));

        ResponseCapture get = new ResponseCapture();
        servlet.doGet(request(Map.of("type", "OTHER"), null, "/api/reports", "GET", session), get.proxy());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, get.status());

        ResponseCapture post = new ResponseCapture();
        servlet.doPost(request(Map.of("type", "OTHER"), null, "/api/reports", "POST", session), post.proxy());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, post.status());

        ResponseCapture delete = new ResponseCapture();
        servlet.doDelete(request(Map.of("type", "OTHER"), "/some-id", "/api/reports", "DELETE", session),
                delete.proxy());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, delete.status());
    }

    @Test
    void returnedStatusCanBeFilteredInBrowseResults() throws Exception {
        JsonReportRepository repository = repository();
        Report returned = report("FOUND", "owner-a");
        returned.setStatus("RETURNED");
        repository.create(returned);
        repository.create(report("FOUND", "owner-b"));

        ResponseCapture response = new ResponseCapture();
        new ReportServlet(repository).doGet(
                request(Map.of("type", "FOUND", "status", "RETURNED"),
                        null, "/view-items", "GET", session(Map.of("userId", "owner-a"))),
                response.proxy()
        );

        JsonNode results = objectMapper.readTree(response.body());
        assertEquals(1, results.size());
        assertEquals("RETURNED", results.get(0).get("status").asText());
    }

    private JsonReportRepository repository() {
        return new JsonReportRepository(new JsonStore(tempDirectory));
    }

    private Report report(String type, String owner) {
        Report report = new Report();
        report.setId(UUID.randomUUID().toString());
        report.setOwnerId(owner);
        report.setType(type);
        report.setTitle("Campus item");
        report.setCategory("Other");
        report.setDescription("Description");
        report.setLocation("Library");
        report.setReportDate(LocalDate.of(2026, 4, 23));
        report.setStatus(type);
        report.setPrivateContact(owner + "@example.edu");
        report.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        report.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        return report;
    }

    private HttpServletRequest request(
            Map<String, String> parameters,
            String pathInfo,
            String servletPath,
            String method,
            HttpSession session
    ) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, called, arguments) -> switch (called.getName()) {
                    case "getParameter" -> parameters.get(arguments[0]);
                    case "getParameterMap" -> parameterMap(parameters);
                    case "getPathInfo" -> pathInfo;
                    case "getServletPath" -> servletPath;
                    case "getContextPath" -> "";
                    case "getMethod" -> method;
                    case "getSession" -> session;
                    case "getHeader" -> null;
                    case "toString" -> "test-request";
                    default -> defaultValue(called.getReturnType());
                }
        );
    }

    private Map<String, String[]> parameterMap(Map<String, String> parameters) {
        Map<String, String[]> result = new HashMap<>();
        parameters.forEach((key, value) -> result.put(key, new String[]{value}));
        return result;
    }

    private HttpSession session(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, called, arguments) -> switch (called.getName()) {
                    case "getAttribute" -> attributes.get(arguments[0]);
                    case "toString" -> "test-session";
                    default -> defaultValue(called.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        return 0d;
    }

    private static final class ResponseCapture {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final ServletOutputStream output = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int value) {
                bytes.write(value);
            }
        };
        private int status = HttpServletResponse.SC_OK;

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, called, arguments) -> switch (called.getName()) {
                        case "setStatus" -> {
                            status = (int) arguments[0];
                            yield null;
                        }
                        case "sendError" -> {
                            status = (int) arguments[0];
                            yield null;
                        }
                        case "sendRedirect" -> {
                            status = HttpServletResponse.SC_FOUND;
                            yield null;
                        }
                        case "getOutputStream" -> output;
                        case "toString" -> "test-response";
                        default -> defaultValue(called.getReturnType());
                    }
            );
        }

        int status() {
            return status;
        }

        byte[] body() {
            return bytes.toByteArray();
        }
    }
}
