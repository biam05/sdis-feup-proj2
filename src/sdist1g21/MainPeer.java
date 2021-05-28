package sdist1g21;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainPeer {

    /* For testing purposes, the SSLPort has to be different on all peers. In reality, this shouldn't be a problem because each peer is a different PC
     *
     * TestApp args: 127.0.0.1 500(peerID) <sub_protocol> <opnds>
     * Main peer args: 127.0.0.1 8000
     * Other peers args: peerID 5000 127.0.0.1 8000
     *
     */

    private static MainPeer instance;

    private PeerContainer peerContainer;

    private static PeerChannel peerChannel;

    private String peerAddress;
    private int peerPort, SSLPort;

    public static void main(String[] args) {
        // check usage
        if (args.length != 2) {
            System.out.println("Usage: java MainPeer PeerAddress PeerPort");
            return;
        }

        MainPeer.instance = new MainPeer(args);
    }

    public MainPeer(String[] args){
        try {
            peerAddress = args[0];
            peerPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println( "Peer: The peer port given is not a number");
            e.printStackTrace();
            return;
        }

        peerChannel = new PeerChannel(0, true, peerAddress, peerPort);
        peerChannel.start();
    }

    public static void messageHandler(String message, AsynchronousSocketChannel clientChannel, ByteBuffer buffer) {
        System.out.println("This peer got the message: " + message);
        String[] msg = message.split(":");

        String filename;
        int initiatorPeerID, rep_deg;
        long fileSize, max_disk_space;
        switch (msg[1].toUpperCase(Locale.ROOT)) {
            case "BACKUP" -> {
                if (msg.length < 5) {
                    System.err.println("> Main peer exception: invalid message received");
                    return;
                }
                initiatorPeerID = Integer.parseInt(msg[0]);
                filename = msg[2];
                rep_deg = Integer.parseInt(msg[3]);
                fileSize = Long.parseLong(msg[4]);
                byte[] msgbytes = "teste".getBytes();
                buffer = ByteBuffer.wrap(msgbytes);
                Future<Integer> writeResult = clientChannel.write(buffer);
                try {
                    writeResult.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            case "RESTORE" -> {
                filename = msg[1];
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
            }
            case "DELETE" -> {
                filename = msg[1];
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
            }
            case "RECLAIM" -> {
                max_disk_space = Long.parseLong(msg[1]);
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
            }
            //case "STATE" -> peer.state();
            default -> System.out.println("> Main peer got the following basic message: " + message);
        }
    }
}
