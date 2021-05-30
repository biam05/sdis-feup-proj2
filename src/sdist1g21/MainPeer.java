package sdist1g21;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class MainPeer {

    /* For testing purposes, the SSLPort has to be different on all peers. In reality, this shouldn't be a problem because each peer is a different PC
     *
     * TestApp args: 127.0.0.1 500(peerID) <sub_protocol> <opnds>
     * Main peer args: 127.0.0.1 8000
     * Other peers args: peerID 5000 127.0.0.1 8000
     *
     */

    private static MainPeer instance;

    private static ConcurrentHashMap<Integer, PeerContainer> peerContainers = new ConcurrentHashMap<>();

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

        peerChannel = new PeerChannel(0, true, true, peerAddress, peerPort, null);
        peerChannel.start();
    }

    public static String messageFromPeerHandler(byte[] msgBytes) {
        String message = new String(msgBytes).trim();
        System.out.println("This peer got the message: " + message);
        String[] msg = message.split(":");

        String filename;
        int initiatorPeerID, rep_deg;
        long fileSize, max_disk_space;
        switch (msg[1].toUpperCase(Locale.ROOT)) {
            case "WELCOME" -> {
                if (msg.length < 3) {
                    System.err.println("> Main peer exception: invalid message received");
                    return "error";
                }
                initiatorPeerID = Integer.parseInt(msg[0]);
                PeerContainer peerContainer;

                int tmp = msg[0].length() + msg[1].length() + 2;

                byte[] peerContainerBytes = new byte[msgBytes.length - tmp];
                System.arraycopy(msgBytes, tmp, peerContainerBytes, 0, msgBytes.length - tmp);

                ByteArrayInputStream bis = new ByteArrayInputStream(peerContainerBytes);
                ObjectInput in;
                try {
                    in = new ObjectInputStream(bis);
                    peerContainer = (PeerContainer) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    return "error";
                }

                peerContainers.put(initiatorPeerID, peerContainer);
                return "GOTWELCOME";
            }
            case "BACKUP" -> {
                if (msg.length < 5) {
                    System.err.println("> Main peer exception: invalid message received");
                    return "error";
                }
                initiatorPeerID = Integer.parseInt(msg[0]);
                filename = msg[2];
                rep_deg = Integer.parseInt(msg[3]);
                fileSize = Long.parseLong(msg[4]);

                HashMap<Integer, HashMap<String,Long>> peers = new HashMap<>();

                boolean ignoreFile;

                for(int peerID : peerContainers.keySet()) {
                    if(peerID == initiatorPeerID) continue;
                    ignoreFile = false;
                    PeerContainer tmp = peerContainers.get(peerID);
                    for(FileManager fileManager : tmp.getStoredFiles()) {
                        if(fileManager.getFile().getName().equals(filename)) {
                            ignoreFile = true;
                            break;
                        }
                    }
                    if(ignoreFile) continue;
                    if(tmp.getFreeSpace() >= fileSize)
                        if(rep_deg > 0) {
                            HashMap<String, Long> tmp2 = new HashMap<>();
                            tmp2.put(tmp.getPeerAdress(), tmp.getPeerPort());
                            tmp2.put(tmp.getPeerAdress(), tmp.getPeerPort());
                            peers.put(peerID,tmp2);
                            rep_deg--;
                            if(rep_deg == 0) break;
                        }
                }

                StringBuilder response = new StringBuilder();
                
                for(int peer : peers.keySet()) {
                    for(String address : peers.get(peer).keySet()){
                        response.append(address);
                        response.append(":");
                        response.append(peers.get(peer).get(address));
                    }
                    response.append("=");
                }

                System.out.println(response);
                if(!response.isEmpty()) response.deleteCharAt(response.length()-1);
                else response.append("empty");
                System.out.println(response);
                
                return response.toString();
            }
            case "RESTORE" -> {
                filename = msg[1];
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
                return "something";
            }
            case "DELETE" -> {
                filename = msg[1];
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
                return "something";
            }
            case "RECLAIM" -> {
                max_disk_space = Long.parseLong(msg[1]);
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
                return "something";
            }
            //case "STATE" -> peer.state();
            default -> {
                System.out.println("> Main peer got the following basic message: " + message);
                return "nothing";
            }
        }
    }
}
