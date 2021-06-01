package sdist1g21;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;


public class TestApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java TestApp <peerAddress> <peerPort> <sub_protocol> <opnds>");
            return;
        }

        //set the type of trust store
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        //set the password with which the truststore is encripted
        System.setProperty("javax.net.ssl.trustStorePassword", "12345678");
        //set the name of the trust store containing the server's public key and certificate
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        //set the password with which the client keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword","12345678");
        //set the name of the keystore containing the client's private and public keys
        System.setProperty("javax.net.ssl.keyStore","keys/keystore");

        String request, response = "ERROR";
        String peerAddress = args[0];
        int peerPort;
        try {
            peerPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println( "Peer: The port given is not a number!");
            return;
        }
        SSLSocket clientSocket;
        SSLSocketFactory sf;
        sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        PrintWriter out;
        BufferedReader in;

        try {
            // build Socket
            clientSocket = (SSLSocket) sf.createSocket(InetAddress.getByName(peerAddress), peerPort);
            // get OutputStream
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            // get InputStream
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + peerAddress);
            System.exit(1);
            return;
        } catch( IOException e) {
            System.out.println("SSLClient - Failed to create SSLSocket");
            e.printStackTrace();
            return;
        }

        clientSocket.startHandshake();

        // prepare and send request
        request = args[2];

        // Different Commands
        switch (request.toUpperCase(Locale.ROOT)) {
            // Backup a File
            case "BACKUP" -> {
                System.out.println("> TestApp: BACKUP Operation");
                if (args.length != 5) {
                    System.err.println("Wrong number of arguments given for BACKUP operation");
                    return;
                }
                String file_name = args[3];
                int replicationDegree;
                try {
                    replicationDegree = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    System.err.println("Replication degree given is not a number!");
                    return;
                }
                out.println(request.toUpperCase(Locale.ROOT) + ":" + file_name + ":" + replicationDegree);
            }
            // Restore a File
            case "RESTORE" -> {
                System.out.println("> TestApp: RESTORE Operation");
                if (args.length != 4) {
                    System.err.println("Wrong number of arguments given for RESTORE operation");
                    return;
                }
                String file_name = args[3];
                out.println(request.toUpperCase(Locale.ROOT) + ":" + file_name);
            }
            // Delete a File
            case "DELETE" -> {
                System.out.println("> TestApp: DELETE Operation");
                if (args.length != 4) {
                    System.err.println("Wrong number of arguments given for DELETE operation");
                    return;
                }
                String file_name = args[3];
                out.println(request.toUpperCase(Locale.ROOT) + ":" + file_name);
            }
            // Reclaim Space
            case "RECLAIM" -> {
                System.out.println("> TestApp: RECLAIM Operation");
                if (args.length != 4){
                    System.err.println("Wrong number of arguments given for RECLAIM operation");
                    return;
                }
                int space = Integer.parseInt(args[2]);
                out.println(request.toUpperCase(Locale.ROOT) + ":" + space);
            }
            // Get Internal State
            case "STATE" -> {
                System.out.println("> TestApp: STATE Operation");
                if (args.length != 3){
                    System.err.println("Wrong number of arguments given for STATE operation");
                    return;
                }
                out.println(request.toUpperCase(Locale.ROOT));
            }
            default -> System.err.println("TestApp: Invalid operation requested");
        }

        try {
            response = in.readLine();
        } catch (IOException e) {
            System.err.println("SSLClient: Failed to read response!");
            e.printStackTrace();
        }

        response = response.replace("[n]", "\n");

        System.out.println("SSLClient: " + request.toUpperCase(Locale.ROOT) + " protocol got response: \n" + response);

        clientSocket.close();
    }
}

