package com.github.moaxcp.spyware;

import java.util.Objects;

public class Main {

  private static WebServer server;

  private static int resolvePort() {
    String sysProp = System.getProperty("port");
    String env = System.getenv("PORT");
    String value = Objects.requireNonNullElseGet(sysProp, () -> Objects.requireNonNullElse(env, "8080"));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 8080;
    }
  }

  public static void main(String[] args) {
    if (server == null) {
      server = new WebServer();
    }

    int port = resolvePort();
    server.start(port);
    int actualPort = server.getPort();

    javax.swing.SwingUtilities.invokeLater(() -> {
      SpywareGui gui = new SpywareGui(server);
      gui.setPort(actualPort);
    });
  }
}
