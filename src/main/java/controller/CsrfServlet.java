package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@WebServlet("/csrf-token")
public class CsrfServlet extends HttpServlet {
    private static final Object TOKEN_INITIALIZATION_LOCK = new Object();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        String token;
        synchronized (TOKEN_INITIALIZATION_LOCK) {
            Object existingToken = session.getAttribute("csrfToken");
            if (existingToken instanceof String value && !value.isBlank()) {
                token = value;
            } else {
                token = UUID.randomUUID().toString();
                session.setAttribute("csrfToken", token);
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        OBJECT_MAPPER.writeValue(response.getWriter(), Map.of("token", token));
    }
}
