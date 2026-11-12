package controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Report;
import repository.JsonReportRepository;
import repository.ReportRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/my-items")
public class MyItemsServlet extends HttpServlet {
    private final ReportRepository repo;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public MyItemsServlet() {
        this(new JsonReportRepository());
    }

    MyItemsServlet(ReportRepository repo) {
        this.repo = repo;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        List<Map<String, Object>> reports = new ArrayList<>();
        for (String type : List.of("LOST", "FOUND")) {
            for (Report report : repo.mine(type, (String) session.getAttribute("userId"))) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", report.getId());
                item.put("type", type);
                item.put("itemName", report.getTitle());
                item.put("category", report.getCategory());
                item.put("description", report.getDescription());
                item.put("location", report.getLocation());
                item.put("reportDate", report.getReportDate());
                item.put("status", report.getStatus());
                reports.add(item);
            }
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), reports);
    }
}