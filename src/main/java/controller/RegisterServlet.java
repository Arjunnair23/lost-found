package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AccountValidation;
import service.AccountValidationException;
import exception.InvalidUserException;
import service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private UserService service;
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public void init() {
        service = new UserService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmation = request.getParameter("confirmPassword");

        try {
            AccountValidation.validateRegistration(username, email, password, confirmation);
        } catch (AccountValidationException exception) {
            validationFailure(request, response, exception.getField(), exception.getMessage(),
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            service.register(username, email, password);
        } catch (InvalidUserException exception) {
            if ("Username is already registered.".equals(exception.getMessage())) {
                validationFailure(request, response, "username", exception.getMessage(),
                        HttpServletResponse.SC_CONFLICT);
            } else {
                validationFailure(request, response, "invalid", exception.getMessage(),
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            return;
        } catch (RuntimeException exception) {
            getServletContext().log("Registration failed", exception);
            if (isJsonRequest(request)) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to create your account right now.");
            } else {
                redirectWithError(request, response, "database");
            }
            return;
        }

        if (isJsonRequest(request)) {
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(), Map.of("redirect", "login.html?success=registered"));
        } else {
            response.sendRedirect(request.getContextPath() + "/login.html?success=registered");
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        return request.getHeader("Accept") != null
                && request.getHeader("Accept").contains("application/json");
    }

    private void validationFailure(HttpServletRequest request, HttpServletResponse response,
                                   String field, String message, int status) throws IOException {
        if (isJsonRequest(request)) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(), Map.of("field", field, "message", message));
        } else {
            redirectWithError(request, response,
                    status == HttpServletResponse.SC_CONFLICT ? "duplicate" : field);
        }
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/register.html?error="
                + URLEncoder.encode(error, StandardCharsets.UTF_8));
    }
}
