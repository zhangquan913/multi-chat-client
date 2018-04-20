package com.qzhang.chat.client;

import java.io.*;
import java.net.*;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

public class ChatClient implements Runnable {

    private static final int DEFAULT_PORT = 8989;
    private static final String DEFAULT_HOST = "localhost";
    private static final String PORT = "port";
    private static final String HOST = "host";
    private static final String JAR_NAME = "chat-client-<version>.jar";
    private static Logger LOGGER = Logger.getLogger(ChatClient.class);
    
    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;
    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void main(String[] args) {

        Options options = new Options();

        Option port = new Option("p", "port", true, "the server port");
        port.setRequired(false);
        options.addOption(port);
        
        Option host = new Option("h", "host", true, "the server host");
        host.setRequired(false);
        options.addOption(host);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar " + JAR_NAME, options);
            System.exit(1);
            return;
        }

        String hostName = DEFAULT_HOST;
        int portNumber = DEFAULT_PORT;
        if (cmd.hasOption(HOST)) {
            hostName = cmd.getOptionValue(HOST);
        }
        else {
            System.out.println("Now using default host: " + DEFAULT_HOST);
        }
        if (cmd.hasOption(PORT)) {
            portNumber = Integer.parseInt(cmd.getOptionValue(PORT));
        }
        else {
            System.out.println("Now using default port: " + DEFAULT_PORT);
        }
        System.out.println("Usage: java -jar " + JAR_NAME + " -h <host> -p <port>");
        
        /*
         * Open a socket on a given host and port. Open input and output streams.
         */
        try {
            clientSocket = new Socket(hostName, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
            LOGGER.error("Don't know about host " + host, e);
        } catch (IOException e) {
            LOGGER.error("Couldn't get I/O for the connection to the host " + host, e);
        }
        LOGGER.info("Client socket opened.");

        /*
         * If everything has been initialized then we want to write some data to the
         * socket we have opened a connection to on the port portNumber.
         */
        if (clientSocket != null && os != null && is != null) {
            try {

                /* Create a thread to read from the server. */
                new Thread(new ChatClient()).start();
                while (!closed) {
                    os.println(inputLine.readLine().trim());
                }
                /*
                 * Close the output stream, close the input stream, close the socket.
                 */
                os.close();
                is.close();
                clientSocket.close();
                LOGGER.info("Connection closed.");
            } catch (IOException e) {
                LOGGER.error("IOException: " + e);
            }
        }
    }

    /*
     * Create a thread to read from the server.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        /*
         * Keep on reading from the socket till we receive "Bye" from the server. Once
         * we received that then we want to break.
         */
        String responseLine;
        try {
            while ((responseLine = new BufferedReader(new InputStreamReader(is)).readLine()) != null) {
                System.out.println(responseLine);
                if (responseLine.indexOf("*** Bye") != -1)
                    break;
            }
            closed = true;
        } catch (IOException e) {
            LOGGER.error("IOException: " + e);
        }
    }
}