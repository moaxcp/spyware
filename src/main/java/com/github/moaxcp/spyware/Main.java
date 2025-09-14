package com.github.moaxcp.spyware;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
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

  public static void main(String[] args) throws IOException {
    if (server == null) {
      server = new WebServer();
    }
    String command = "start";
    if (args.length > 0) {
      command = args[0];
    }
    if ("start".equals(command)) {
      new Thread(() -> server.start(resolvePort())).start();
    } else {
      server.stop();
    }
  }
}
