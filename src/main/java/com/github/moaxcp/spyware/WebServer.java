package com.github.moaxcp.spyware;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Simple web service using JDK's built-in HttpServer that exposes endpoints,
 * including /screenshot which returns a PNG image captured by ScreenshotService.
 *
 * Also provides start/stop lifecycle methods compatible with Windows service wrappers.
 */
public class WebServer {
    private HttpServer server;

    /**
     * Starts the web server without blocking. Intended for Windows service wrappers.
     */
    public void start(int port) {
      try {
        server = HttpServer.create(new InetSocketAddress(port), 0);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      // Register handlers
        server.createContext("/screenshot", new ScreenshotHandler(new ScreenshotService()));
        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            String message = "Spyware web service is running. Try GET /screenshot to get a PNG screenshot.";
            byte[] body = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[Spyware] Web server started on port " + port + ". Endpoints: /, /screenshot");
    }

    /**
     * Stops the web server. Intended for Windows service wrappers.
     */
    public void stop() {
        if (server == null) {
            System.out.println("[Spyware] Server not running");
            return;
        }
        // 0-second delay: stop immediately, closing existing connections
        server.stop(0);
        server = null;
        System.out.println("[Spyware] Web server stopped");
    }
}
