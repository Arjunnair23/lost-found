package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrfServletTest {
    @Test
    void simultaneousTokenRequestsForNewSessionReceiveTheSameToken() throws Exception {
        Map<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAttribute" -> attributes.get(arguments[0]);
                    case "setAttribute" -> attributes.put((String) arguments[0], arguments[1]);
                    default -> null;
                }
        );
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, arguments) -> method.getName().equals("getSession") ? session : null
        );

        int requestCount = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<String>> responses = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                responses.add(executor.submit(() -> {
                    StringWriter body = new StringWriter();
                    HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                            HttpServletResponse.class.getClassLoader(),
                            new Class<?>[]{HttpServletResponse.class},
                            (proxy, method, arguments) -> method.getName().equals("getWriter")
                                    ? new PrintWriter(body)
                                    : null
                    );
                    start.await();
                    new CsrfServlet().doGet(request, response);
                    return new ObjectMapper().readTree(body.toString()).get("token").asText();
                }));
            }

            start.countDown();
            List<String> tokens = new ArrayList<>();
            for (Future<String> response : responses) {
                tokens.add(response.get());
            }
            assertEquals(1, new HashSet<>(tokens).size());
            assertEquals(tokens.get(0), attributes.get("csrfToken"));
        } finally {
            executor.shutdownNow();
        }
    }
}
