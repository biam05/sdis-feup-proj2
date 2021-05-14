package sdist1g21;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class TestApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println(
                    "Usage: java TestApp <host> <port> <oper> <opnd>*");
            System.exit(1);
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

        String reqStr, response = "ERROR";
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        SSLSocket clientSocket;
        SSLSocketFactory sf;
        sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        PrintWriter out;
        BufferedReader in;

        try {
            // build Socket
            clientSocket = (SSLSocket) sf.createSocket(InetAddress.getByName(hostName), portNumber);
            // get OutputStream
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            // get InputStream
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
            return;
        } catch( IOException e) {
            System.out.println("SSLClient - Failed to create SSLSocket");
            e.printStackTrace();
            return;
        }

        // prepare and send request
        reqStr = buildReqString(args);
        out.println(reqStr);
        System.out.println("SSLClient: Request \"" + reqStr + "\" sent!");

        try {
            response = in.readLine();
        } catch (IOException e) {
            System.err.println("SSLClient: Failed to read response!");
            e.printStackTrace();
        }

        System.out.println("SSLClient: " + args[2] + " " + args[3] + " : " + response);

        clientSocket.close();
    }

    protected static String buildReqString(String[] args){
        StringBuilder reqStr = new StringBuilder();
        for(int i = 2; i < args.length; i++){
            reqStr.append(args[i]).append(" ");
        }
        return reqStr.toString();

    }
}

