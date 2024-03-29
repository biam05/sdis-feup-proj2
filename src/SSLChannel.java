package sdist1g21;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class SSLChannel extends Thread {

    private SSLServerSocket socket;

    public SSLChannel(int port) {

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
            socket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        } catch (IOException e) {
            System.out.println("> Failed to Start SSLChannel : Port " + port);
            e.printStackTrace();
            return;
        }

        System.out.println("> Started SSLChannel : Port " + port);
    }

    @Override
    public void run() {
        while(true) {
            try {
                SSLSocket clientSocket = (SSLSocket) socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                String message = in.readLine();
                String response = Peer.messageFromTestAppHandler(message);
                out.println(response);
            } catch(IOException e) {
                System.err.println("> SSLChannel: Failed to receive message!");
                e.printStackTrace();
                return;
            }
        }
    }
}