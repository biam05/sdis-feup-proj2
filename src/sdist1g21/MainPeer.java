package sdist1g21;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MainPeer {

    /* For testing purposes, the SSLPort has to be different on all peers. In reality, this shouldn't be a problem because each peer is a different PC
     *
     * TestApp args: 127.0.0.1 500(peerID) <sub_protocol> <opnds>
     * Main peer args: 127.0.0.1 8000
     * Other peers args: peerID 5000 127.0.0.1 8000
     *
     */

    private static ConcurrentHashMap<Integer, PeerContainer> peerContainers = new ConcurrentHashMap<>();

    private static PeerChannel peerChannel;

    private String peerAddress;
    private int peerPort;

    private static final ScheduledThreadPoolExecutor mainPeerExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    public static void main(String[] args) {
        // check usage
        if (args.length != 2) {
            System.out.println("Usage: java MainPeer PeerAddress PeerPort");
            return;
        }

        new MainPeer(args);
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

        createDirectory();
        loadState();

        peerChannel = new PeerChannel(0, true, true, peerAddress, peerPort, null);
        peerChannel.start();
    }

    public static String messageFromPeerHandler(byte[] msgBytes) {
        String message = new String(msgBytes).trim();
        String[] msg = message.split(":");

        String filename;
        int initiatorPeerID, rep_deg;
        long fileSize, max_disk_space;
        switch (msg[1].toUpperCase(Locale.ROOT)) {
            case "PEERCONTAINER" -> {
                if (msg.length < 3) {
                    System.err.println("> Main peer exception: invalid message received");
                    return "error";
                }
                initiatorPeerID = Integer.parseInt(msg[0]);
                PeerContainer peerContainer;

                int tmp = msg[0].length() + msg[1].length() + msg[2].length() + 3;

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
                saveState();
                return "GOTPEERCONTAINER";
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
                            peers.put(peerID,tmp2);
                            rep_deg--;
                            if(rep_deg == 0) break;
                        }
                }

                return formPeersList(peers);
            }
            case "RESTORE" -> {
                initiatorPeerID = Integer.parseInt(msg[0]);
                filename = msg[1];
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
                return "something";
            }
            case "DELETE" -> {
                if (msg.length < 3) {
                    System.err.println("> Main peer exception: invalid message received");
                }
                initiatorPeerID = Integer.parseInt(msg[0]);
                filename = msg[2];

                HashMap<Integer, HashMap<String,Long>> peers = new HashMap<>();

                for(int peerID : peerContainers.keySet()) {
                    if(peerID == initiatorPeerID) continue;
                    PeerContainer tmp = peerContainers.get(peerID);
                    for(FileManager fileManager : tmp.getBackedUpFiles()) {
                        if(fileManager.getFile().getName().equals(filename)) {
                            HashMap<String, Long> tmp2 = new HashMap<>();
                            tmp2.put(tmp.getPeerAdress(), tmp.getPeerPort());
                            peers.put(peerID, tmp2);
                            break;
                        }
                    }
                }

                return formPeersList(peers);
            }
            case "RECLAIM" -> {
                initiatorPeerID = Integer.parseInt(msg[0]);
                max_disk_space = Long.parseLong(msg[1]);
                if (msg.length < 2) {
                    System.err.println("> Main peer exception: invalid message received");
                }
                return "something";
            }
            default -> {
                System.out.println("> Main peer got the following basic message: " + message);
                return "nothing";
            }
        }
    }

    private static String formPeersList(HashMap<Integer, HashMap<String, Long>> peers) {
        StringBuilder response = new StringBuilder();

        for(int peer : peers.keySet()) {
            for(String address : peers.get(peer).keySet()){
                response.append(address);
                response.append(":");
                response.append(peers.get(peer).get(address));
            }
            response.append("=");
        }

        if(!response.isEmpty()) response.deleteCharAt(response.length()-1);
        else response.append("empty");

        return response.toString();
    }

    /**
     * Function used to create MainPeer Directory
     */
    private void createDirectory() {
        try {
            Files.createDirectories(Paths.get("MainPeer"));
        } catch (Exception e) {
            System.err.println("> MainPeer exception: failed to create MainPeer directory");
        }
    }

    /**
     * Function used to save the state of the MainPeer
     */
    public static synchronized void saveState() {
        mainPeerExecutors.execute(() -> {
            try {
                FileOutputStream stateFileOut = new FileOutputStream("MainPeer/state.ser");
                ObjectOutputStream out = new ObjectOutputStream(stateFileOut);
                out.writeObject(peerContainers);
                out.close();
                stateFileOut.close();
            } catch (IOException i) {
                System.err.println("> MainPeer: Failed to save serialized state");
                i.printStackTrace();
            }
        });
    }

    /**
     * Function used to load the state of the MainPeer
     */
    public synchronized void loadState() {
        mainPeerExecutors.execute(() -> {
            ConcurrentHashMap<Integer, PeerContainer> peerContainers;
            try {
                FileInputStream stateFileIn = new FileInputStream("MainPeer/state.ser");
                ObjectInputStream in = new ObjectInputStream(stateFileIn);
                peerContainers = (ConcurrentHashMap<Integer, PeerContainer>) in.readObject();
                in.close();
                stateFileIn.close();
                System.out.println("> MainPeer: Serialized state of peer loaded successfully");
            } catch (Exception i) {
                System.out.println("> MainPeer: State file of peer not found, a new one will be created");
                saveState();
                return;
            }

            MainPeer.peerContainers = peerContainers;
        });
    }
}
