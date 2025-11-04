package edu.seg2105.edu.server.backend;
// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import edu.seg2105.client.common.ChatIF;
import ocsf.server.*;
import edu.seg2105.edu.server.ui.ServerConsole;


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

  // Exercise 3: store login ids per connection
  private final ConcurrentHashMap<ConnectionToClient, String> loginIds = new ConcurrentHashMap<>();

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

    if (msg instanceof String) {
      String s = (String) msg;

      // Exercise 3: recognize #login <loginId>
      if (s.startsWith("#login")) {
        String[] parts = s.split("\\s+", 2);
        if (loginIds.containsKey(client)) {
          // already logged in -> error and disconnect
          sendErrorAndClose(client, "Already logged in.");
          return;
        }
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
          sendErrorAndClose(client, "Missing login id.");
          return;
        }
        String id = parts[1].trim();
        loginIds.put(client, id);
        System.out.println("[" + id + "] logged in from " + label);
        try { client.sendToClient("SERVER MSG> login accepted for '" + id + "'"); } catch (Exception ignore) {}
        return;
      }

      // normal text message: prefix with login id
      String id = loginIds.get(client);
      if (id == null) id = "ANON";
      System.out.println("Message received: " + s + " from " + label + " as " + id);
      this.sendToAllClients(id + "> " + s);
      return;
    }

    // Fallback for non-string messages
    System.out.println("Message received (object) from " + label);
    this.sendToAllClients(msg);
  }

  private void sendErrorAndClose(ConnectionToClient client, String err) {
    try { client.sendToClient("SERVER MSG> ERROR: " + err); } catch (Exception ignore) {}
    try { client.close(); } catch (Exception ignore) {}
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
    String id = loginIds.get(client);
    System.out.println("Client disconnected: " + (id != null ? "["+id+"] " : "") + label);
    labels.remove(client);
    loginIds.remove(client);
    super.clientDisconnected(client);
  }

  @Override
  synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
    String label = labels.getOrDefault(client, String.valueOf(client));
    String id = loginIds.get(client);
    System.out.println("Client exception from " + (id != null ? "["+id+"] " : "") + label + " : " + exception);
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
}
//End of EchoServer class
