package edu.seg2105.edu.server.ui;

import java.util.Scanner;
import edu.seg2105.client.common.ChatIF;
import edu.seg2105.edu.server.backend.EchoServer;

public class ServerConsole implements ChatIF, Runnable {

  private final EchoServer server;
  private final Scanner in = new Scanner(System.in);

  public ServerConsole(EchoServer server) {
    this.server = server;
  }

  public void accept() { run(); }

  @Override
  public void run() {
    try {
      while (true) {
        String line = in.nextLine();
        if (line.startsWith("#")) {
          handleCommand(line.trim());
        } else {
          server.handleMessageFromServerUI(line);
        }
      }
    } catch (Exception e) {
      display("Server console error: " + e.getMessage());
    }
  }

  private void handleCommand(String line) {
    String[] parts = line.split("\\s+");
    String cmd = parts[0].toLowerCase();

    try {
      switch (cmd) {
        case "#quit":
          server.close(); // 會同時斷所有 client
          System.exit(0);
          break;

        case "#stop":
          if (server.isListening()) {
            server.stopListening();
            display("Server stopped listening.");
          } else {
            display("Server is already stopped.");
          }
          break;

        case "#close":
          server.close(); // 停止並斷所有 client
          display("Server closed.");
          break;

        case "#setport":
          if (parts.length < 2) { display("Usage: #setport <port>"); break; }
          if (!server.isClosed()) { // 僅在關閉狀態允許
            display("Error: You must close the server before changing port.");
            break;
          }
          int p = Integer.parseInt(parts[1]);
          server.setPort(p);
          display("Port set to " + server.getPort());
          break;

        case "#start":
          if (!server.isListening()) {
            server.listen();
          } else {
            display("Server is already listening.");
          }
          break;

        case "#getport":
          display("Current port: " + server.getPort());
          break;

        default:
          display("Unknown command: " + cmd);
      }
    } catch (Exception e) {
      display("Command failed: " + e.getMessage());
    }
  }

  @Override
  public void display(String message) {
    System.out.println("> " + message);
  }
}
