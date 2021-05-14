package sdist1g21;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class SSLChannel extends Thread {

    private int port;
    private SSLServerSocket socket;
    private SSLSocket clientSocket;
    private String message;
    private PrintWriter out;

    public SSLChannel(int port) {
        this.port = port;

        //set the type of trust store
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        //set the password with which the truststore is encripted
        System.setProperty("javax.net.ssl.trustStorePassword", "12345678");
        //set the name of the trust store containing the client public key and certificate
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        //set the password with which the server keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword","12345678");
        //set the name of the keystore containing the server's private and public keys
        System.setProperty("javax.net.ssl.keyStore","keys/keystore");

        // start sslchannel
        SSLServerSocketFactory serverSocketFactory;
        serverSocketFactory=(SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            socket = (SSLServerSocket) serverSocketFactory.createServerSocket(this.port);
        } catch (IOException e) {
            System.out.println("> Failed to Start SSLChannel : Port " + this.port);
            e.printStackTrace();
            return;
        }

        System.out.println("> Started SSLChannel : Port " + this.port);
    }

    @Override
    public void run() {
        while(true) {
            try {
                clientSocket = (SSLSocket) socket.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                message = br.readLine();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                Peer.messageHandler(message);
            } catch(IOException e) {
                System.err.println("> SSLChannel: Failed to receive message!");
                e.printStackTrace();
                return;
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}