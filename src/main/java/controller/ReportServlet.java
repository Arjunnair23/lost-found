package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import model.Report;
import repository.*;
import validation.ReportValidation;
import validation.ReportValidationException;

@WebServlet(urlPatterns = {
    "/api/reports",
    "/api/reports/*"
})
public class ReportServlet extends HttpServlet {

    private final ReportRepository repo;

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ReportServlet() {
        this(new JsonReportRepository());
    }

    ReportServlet(ReportRepository repo) {
        this.repo = repo;
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p)
            throws IOException {

        HttpSession s = q.getSession(false);

        if (s == null || s.getAttribute("userId") == null) {
            p.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String type;
        try {
            type = type(q.getParameter("type"), q.getServletPath());
        } catch (IllegalArgumentException e) {
            p.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported report type");
            return;
        }

        boolean mine = "true".equals(q.getParameter("mine"));

        String owner = (String) s.getAttribute("userId");

        List<Report> rs = mine
                ? repo.mine(type, owner)
                : repo.list(type);

        String keyword = norm(q.getParameter("q"));
        String cat = norm(q.getParameter("category"));
        String loc = norm(q.getParameter("location"));
        String status = norm(q.getParameter("status"));

        rs = rs.stream()
                .filter(r -> keyword.isEmpty()
                        || norm(r.getTitle() + " " + r.getDescription())
                                .contains(keyword))
                .filter(r -> cat.isEmpty()
                        || norm(r.getCategory()).equals(cat))
                .filter(r -> loc.isEmpty()
                        || norm(r.getLocation()).contains(loc))
                .filter(r -> status.isEmpty()
                        || norm(r.getStatus()).equals(status))
                .sorted(Comparator.comparing(
                        Report::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        p.setContentType("application/json;charset=UTF-8");
        p.setHeader("Cache-Control", "no-store");

        json.writeValue(
                p.getOutputStream(),
                rs.stream().map(this::publicView).toList()
        );
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p)
            throws IOException {

        HttpSession s = q.getSession(false);

        if (s == null || s.getAttribute("userId") == null) {
            p.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String path = q.getPathInfo();

        try {

            String servletPath = q.getServletPath();

            String type = type(
                    q.getParameter("type"),
                    servletPath
            );

            boolean markReturned = path != null && path.endsWith("/return");
            String id;
            if (path == null || path.isBlank() || "/".equals(path)) {
                id = null;
            } else if (markReturned) {
                id = path.substring(1, path.length() - "/return".length());
                if (id.isBlank() || id.contains("/")) {
                    p.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            } else {
                id = path.startsWith("/") ? path.substring(1) : path;
                if (id.isBlank() || id.contains("/")) {
                    p.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }

            if (markReturned) {
                markReturned(p, type, id, s);
                return;
            }

            Report old = null;
            if (id != null) {
                old = repo.find(type, id).orElse(null);

                if (old == null) {
                    p.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                if (!canModify(old, s)) {
                    p.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }

            Report r = from(q, type, old);
            r.setUpdatedAt(Instant.now());

            if (old != null) {
                r.setId(old.getId());
                r.setOwnerId(old.getOwnerId());
                r.setCreatedAt(old.getCreatedAt());

                if (!repo.update(r)) {
                    p.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

            } else {

                r.setId(UUID.randomUUID().toString());
                r.setOwnerId((String) s.getAttribute("userId"));
                r.setCreatedAt(Instant.now());

                repo.create(r);
            }

            // Browser form submission: redirect after successful saving.
            if ("/add-item".equals(servletPath)
                    || "/report-found".equals(servletPath)) {

                String success = type.equals("LOST")
                        ? "lost"
                        : "found";

                p.sendRedirect(
                        q.getContextPath()
                                + "/my-items.html?success="
                                + success
                );

                return;
            }

            // API requests continue to receive JSON.
            p.setStatus(HttpServletResponse.SC_CREATED);
            p.setContentType("application/json;charset=UTF-8");

            json.writeValue(p.getOutputStream(), publicView(r));

        } catch (ReportValidationException e) {
            writeValidationError(p, e.getField(), e.getMessage());
        } catch (DateTimeParseException e) {
            writeValidationError(p, "reportDate", "Enter a real date in YYYY-MM-DD format.");
        } catch (IllegalArgumentException e) {
            writeValidationError(p, "report", "Invalid report fields.");

        } catch (RuntimeException e) {

            getServletContext().log("Report operation failed", e);

            p.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to save report"
            );
        }
    }

    @Override
    protected void doDelete(HttpServletRequest q, HttpServletResponse p)
            throws IOException {

        HttpSession s = q.getSession(false);

        if (s == null || s.getAttribute("userId") == null) {
            p.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String path = q.getPathInfo();

        if (path == null || path.length() < 2) {
            p.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String type;
        try {
            type = type(q.getParameter("type"), q.getServletPath());
        } catch (IllegalArgumentException e) {
            p.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported report type");
            return;
        }

        String id = path.substring(1);

        Report r = repo.find(type, id).orElse(null);

        if (r == null) {
            p.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!canModify(r, s)) {
            p.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!repo.delete(type, id)) {
            p.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        p.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void markReturned(
            HttpServletResponse response,
            String type,
            String id,
            HttpSession session
    ) throws IOException {
        Report report = repo.find(type, id).orElse(null);
        if (report == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canModify(report, session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!type.equals(report.getStatus())) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Report is not eligible to be returned");
            return;
        }
        if (!repo.markReturned(type, id, Instant.now())) {
            Report current = repo.find(type, id).orElse(null);
            response.sendError(current == null
                    ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_CONFLICT);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private boolean canModify(Report report, HttpSession session) {
        return Objects.equals(report.getOwnerId(), session.getAttribute("userId"))
                || "ADMIN".equals(session.getAttribute("role"));
    }

    private Report from(HttpServletRequest request, String type, Report existing) {
        Map<String, String> values = new HashMap<>();
        request.getParameterMap().forEach((name, parameters) ->
                values.put(name, parameters == null || parameters.length == 0 ? null : parameters[0]));
        return ReportValidation.create(values, type, existing);
    }

    private void writeValidationError(HttpServletResponse response, String field, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getOutputStream(), Map.of("field", field, "message", message));
    }

    private String type(String t, String path) {

        if (t == null && path != null
                && path.endsWith("report-found")) {

            t = "FOUND";
        }

        if ("FOUND".equalsIgnoreCase(t)) {
            return "FOUND";
        }

        if ("LOST".equalsIgnoreCase(t)
                || t == null || t.isBlank()) {

            return "LOST";
        }

        throw new IllegalArgumentException();
    }

    private String norm(String s) {

        return s == null
                ? ""
                : s.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> publicView(Report r) {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("id", r.getId());
        m.put("type", r.getType());
        m.put("itemName", r.getTitle());
        m.put("category", r.getCategory());
        m.put("description", r.getDescription());

        m.put(
                r.getType().equals("LOST")
                        ? "lostLocation"
                        : "foundLocation",
                r.getLocation()
        );

        m.put(
                r.getType().equals("LOST")
                        ? "lostDate"
                        : "foundDate",
                r.getReportDate()
        );

        m.put("status", r.getStatus());
        m.put("createdAt", r.getCreatedAt());
        m.put("contactInfo", r.getPrivateContact());

        return m;
    }
}