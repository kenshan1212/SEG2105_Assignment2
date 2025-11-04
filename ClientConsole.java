package edu.seg2105.client.ui;
// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import java.util.Scanner;

import edu.seg2105.client.backend.ChatClient;
import edu.seg2105.client.common.*;

/**
 * This class constructs the UI for a chat client.  It implements the
 * chat interface in order to activate the display() method.
 * Warning: Some of the code here is cloned in ServerConsole 
 *
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Dr Timothy C. Lethbridge  
 * @author Dr Robert Lagani&egrave;re
 */
public class ClientConsole implements ChatIF 
{
  //Class variables *************************************************
  
  /**
   * The default port to connect on.
   */
  final public static int DEFAULT_PORT = 5555;
  
  //Instance variables **********************************************
  
  /**
   * The instance of the client that created this ConsoleChat.
   */
  ChatClient client;
  
  /**
   * Scanner to read from the console
   */
  Scanner fromConsole; 

  
  //Constructors ****************************************************

  /**
   * Constructs an instance of the ClientConsole UI.
   *
   * @param host The host to connect to.
   * @param port The port to connect on.
   */
  public ClientConsole(String host, int port) 
  {
    try 
    {
      client= new ChatClient(host, port, this);
    } 
    catch(IOException exception) 
    {
      System.out.println("Error: Can't setup connection!"
                + " Terminating client.");
      System.exit(1);
    }
    
    // Create scanner object to read from console
    fromConsole = new Scanner(System.in); 
  }

  
  //Instance methods ************************************************
  
  /**
   * This method waits for input from the console.  Once it is 
   * received, it sends it to the client's message handler.
   */
  public void accept() 
  {
    try
    {
      String line;
      while (true) 
      {
        line = fromConsole.nextLine();
        if (line.startsWith("#")) {
          handleCommand(line.trim());
        } else {
          client.handleMessageFromClientUI(line);
        }
      }
    } 
    catch (Exception ex) 
    {
      System.out.println("Unexpected error while reading from console!");
    }
  }

  /** Exercise 2: support #-commands on client side */
  private void handleCommand(String line) {
    String[] parts = line.split("\\s+");
    String cmd = parts[0].toLowerCase();

    try {
      switch (cmd) {
        case "#quit":   // quit gracefully
          client.quit();
          break;

        case "#logoff": // disconnect but do not quit
          if (client.isConnected()) {
            client.closeConnection();
            display("Logged off.");
          } else {
            display("Already logged off.");
          }
          break;

        case "#sethost": // only when logged off
          if (parts.length < 2) { display("Usage: #sethost <host>"); break; }
          if (client.isConnected()) { display("Error: You must log off before setting host."); break; }
          client.setHost(parts[1]);
          display("Host set to " + client.getHost());
          break;

        case "#setport": // only when logged off
          if (parts.length < 2) { display("Usage: #setport <port>"); break; }
          if (client.isConnected()) { display("Error: You must log off before setting port."); break; }
          try {
            int p = Integer.parseInt(parts[1]);
            client.setPort(p);
            display("Port set to " + client.getPort());
          } catch (NumberFormatException nfe) {
            display("Port must be an integer.");
          }
          break;

        case "#login": // connect when not already connected
          if (client.isConnected()) { display("Error: Already connected."); break; }
          client.openConnection();
          display("Connected to " + client.getHost() + ":" + client.getPort());
          break;

        case "#gethost":
          display("Current host: " + client.getHost());
          break;

        case "#getport":
          display("Current port: " + client.getPort());
          break;

        default:
          display("Unknown command: " + cmd);
      }
    } catch (IOException ioe) {
      display("Command failed: " + ioe.getMessage());
    }
  }

  /**
   * This method overrides the method in the ChatIF interface.  It
   * displays a message onto the screen.
   *
   * @param message The string to be displayed.
   */
  public void display(String message) 
  {
    System.out.println("> " + message);
  }

  
  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of the Client UI.
   *
   * @param args[0] The host to connect to.
   */
  public static void main(String[] args) 
  {
    String host;
    int port;

    // host
    try { host = args[0]; }
    catch(ArrayIndexOutOfBoundsException e) { host = "localhost"; }

    // optional port
    try { port = Integer.parseInt(args[1]); }
    catch(Exception e) { port = DEFAULT_PORT; }

    ClientConsole chat= new ClientConsole(host, port);
    chat.accept();  //Wait for console data
  }
}
//End of ConsoleChat class
