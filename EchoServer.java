package edu.seg2105.edu.server.backend;
// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import edu.seg2105.client.common.ChatIF;
import ocsf.server.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 */
public class EchoServer extends AbstractServer 
{
  //Class variables *************************************************
  
  /**
   * The default port to listen on.
   */
  final public static int DEFAULT_PORT = 5555;

  // Keep nice human labels for connections (since this OCSF version
  // does not provide setInfo/getInfo on ConnectionToClient)
  private final ConcurrentHashMap<ConnectionToClient, String> labels = new ConcurrentHashMap<>();

  // Server state flags for Exercise 2 commands
  private volatile boolean closed = true;   // before listen() we consider closed
  private volatile boolean stopped = false;

  public boolean isClosed() { return closed; }
  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port) 
  {
    super(port);
  }

  
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient(Object msg, ConnectionToClient client)
  {
    String label = labels.getOrDefault(client, String.valueOf(client));
    System.out.println("Message received: " + msg + " from " + label);
    this.sendToAllClients(msg);
  }

  /** Exercise 2: allow server operator to type messages that
   *  are echoed locally and broadcast to all clients with prefix.
   */
  public void handleMessageFromServerUI(String msg) {
    String formatted = "SERVER MSG> " + msg;
    System.out.println(formatted);
    this.sendToAllClients(formatted);
  }
    
  /**
   * This method overrides the one in the superclass.  Called
   * when the server starts listening for connections.
   */
  protected void serverStarted()
  {
    closed = false; 
    stopped = false;
    System.out.println("Server listening for connections on port " + getPort());
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when the server stops listening for connections.
   */
  protected void serverStopped()
  {
    stopped = true;
    System.out.println("Server has stopped listening for connections.");
  }

  /** Called when the server is fully closed (after close()). */
  protected void serverClosed() {
    closed = true;
    stopped = false;
    System.out.println("Server closed.");
  }

  // ---- Exercise 1.c: print connect/disconnect nicely ----

  @Override
  protected void clientConnected(ConnectionToClient client) {
    String host = client.getInetAddress() != null ? client.getInetAddress().getHostName() : "unknown";
    String ip   = client.getInetAddress() != null ? client.getInetAddress().getHostAddress() : "?";
    String label = host + " (" + ip + ")";
    labels.put(client, label);
    System.out.println("Client connected: " + label);
  }

  @Override
  synchronized protected void clientDisconnected(ConnectionToClient client) {
    String label = labels.getOrDefault(client, String.valueOf(client));
    System.out.println("Client disconnected: " + label);
    labels.remove(client);
    // superclass will also remove from its connection table
    super.clientDisconnected(client);
  }

  @Override
  synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
    String label = labels.getOrDefault(client, String.valueOf(client));
    System.out.println("Client exception from " + label + " : " + exception);
    try { client.close(); } catch (Exception ignore) {}
  }
  
  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of 
   * the server instance and the server-side console.
   *
   * @param args[0] The port number to listen on.  Defaults to 5555 
   *          if no argument is entered.
   */
  public static void main(String[] args) 
  {
    int port = 0; //Port to listen on

    try { port = Integer.parseInt(args[0]); } 
    catch(Throwable t) { port = DEFAULT_PORT; }
	
    EchoServer sv = new EchoServer(port);
    
    try 
    {
      sv.listen(); //Start listening for connections
    } 
    catch (Exception ex) 
    {
      System.out.println("ERROR - Could not listen for clients!");
    }

    // Start server-side console (implements ChatIF like ClientConsole)
    new ServerConsole(sv).accept();
  }

  // ====== Inner class: ServerConsole (Exercise 2 requirement) ======
  // Warning: Some of the code here is cloned from ClientConsole

  private static class ServerConsole implements ChatIF, Runnable {
    private final EchoServer server;
    private final Scanner in = new Scanner(System.in);

    ServerConsole(EchoServer server) { this.server = server; }

    public void accept() { run(); }

    @Override public void run() {
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
            server.close(); // also disconnects all clients
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
            server.close(); // stop + disconnect all clients
            display("Server closed.");
            break;

          case "#setport":
            if (parts.length < 2) { display("Usage: #setport <port>"); break; }
            if (!server.isClosed()) {
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
}
//End of EchoServer class
