package sdis.t1g06;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class SSLChannel implements Runnable {

    private int port;
    private SSLServerSocket socket;
    private SSLSocket clientSocket;
    private String message;
    private PrintWriter out;

    public SSLChannel(int port) {
        this.port = port;

        // start sslchannel
        SSLServerSocketFactory serverSocketFactory;
        serverSocketFactory=(SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            socket= (SSLServerSocket) serverSocketFactory.createServerSocket(this.port);
        } catch (IOException e) {
            System.out.println("> Failed to Start SLLChannel : Port " + this.port);
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
