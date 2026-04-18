package com.github.moaxcp.spyware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ScreenshotHandler implements HttpHandler {
    private final ScreenshotService screenshotService;

    public ScreenshotHandler(ScreenshotService screenshotService) {
        this.screenshotService = Objects.requireNonNull(screenshotService);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().add("Allow", "GET");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            byte[] jpeg;
            if (params.containsKey("x") && params.containsKey("y") && params.containsKey("width") && params.containsKey("height")) {
                int x = Integer.parseInt(params.get("x"));
                int y = Integer.parseInt(params.get("y"));
                int width = Integer.parseInt(params.get("width"));
                int height = Integer.parseInt(params.get("height"));
                jpeg = screenshotService.captureJpeg(new Rectangle(x, y, width, height));
            } else {
                jpeg = screenshotService.captureJpeg();
            }
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, jpeg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jpeg);
            }
        } catch (NumberFormatException e) {
            byte[] msg = ("Invalid parameter format: " + e.getMessage()).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(400, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        } catch (IllegalArgumentException e) {
            byte[] msg = ("Invalid screenshot area: " + e.getMessage()).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(400, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        } catch (IllegalStateException e) {
            byte[] msg = ("Cannot capture screenshot: " + e.getMessage()).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(503, msg.length); // Service Unavailable
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        } catch (AWTException e) {
            byte[] msg = ("AWT error capturing screenshot: " + e.getMessage()).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(500, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        } catch (IOException e) {
            byte[] msg = ("I/O error processing screenshot: " + e.getMessage()).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(500, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
