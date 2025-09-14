package com.github.moaxcp.spyware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
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
            byte[] png = screenshotService.capturePng();
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, png.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(png);
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
}
