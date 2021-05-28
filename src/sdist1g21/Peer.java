package sdist1g21;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer implements ServiceInterface {

    /* For testing purposes, the SSLPort has to be different on all peers. In reality, this shouldn't be a problem because each peer is a different PC
    *
    * TestApp args: 127.0.0.1 500(peerID) <sub_protocol> <opnds>
    * Main peer args: 127.0.0.1 8000
    * Other peers args: peerID 5000 127.0.0.1 8000
    *
    */

    private static Peer peer;

    private static SSLChannel sslChannel;

    private PeerContainer peerContainer;

    private PeerChannel peerChannel;

    private String mainPeerAddress;
    private int mainPeerPort;
    private static int peerID;
    private int SSLPort;

    public static void main(String[] args) {
        // check usage
        if (args.length != 4) {
            System.out.println("Usage: java Peer PeerID SSLPort MainPeerAddress MainPeerPort");
            return;
        }

        Peer.peer = new Peer(args);
    }

    public Peer(String[] args){
        try {
            peerID = Integer.parseInt(args[0]);
            SSLPort = Integer.parseInt(args[1]) + peerID;
            mainPeerAddress = args[2];
            mainPeerPort = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.err.println( "Peer: Either one of the ports or the peerID given is not a number!");
            return;
        }

        sslChannel = new SSLChannel(SSLPort);
        sslChannel.start();

        peerChannel = new PeerChannel(peerID, false, mainPeerAddress, mainPeerPort);
        peerChannel.start();

        peerContainer = new PeerContainer(peerID);
        createDirectories();
        startAutoSave();
    }

    public static void messageHandler(String message) {
        System.out.println("This peer got the message: " + message);
        String[] msg = message.split(":");
        String filename;
        int rep_deg;
        long max_disk_space;
        switch (msg[0].toUpperCase(Locale.ROOT)) {
            case "BACKUP" -> {
                if (msg.length < 3) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                    return;
                }
                filename = msg[1];
                rep_deg = Integer.parseInt(msg[2]);
                peer.backup(filename, rep_deg);
            }
            case "RESTORE" -> {
                filename = msg[1];
                peer.restore(filename);
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
            }
            case "DELETE" -> {
                filename = msg[1];
                peer.delete(filename);
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
            }
            case "RECLAIM" -> {
                max_disk_space = Long.parseLong(msg[1]);
                peer.reclaim(max_disk_space);
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
            }
            case "STATE" -> peer.state();
            default -> System.err.println("> Peer " + peerID + " got the following basic message: " + message);
        }
    }

    /**
     * Function used to create Peer Directories
     */
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get("peer " + peerID + "\\files"));
        } catch (Exception e) {
            System.err.println("> Peer " + peerID + " exception: failed to create peer directory");
        }
    }

    /**
     * Function used to load the state of the peer from a file state.ser, update it with any changes on the physical file system,
     * and initialize a thread that auto-saves the state of the peer every 3 seconds
     */
    private synchronized void startAutoSave() {
        peerContainer.loadState();
        peerContainer.updateState();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> peerContainer.saveState(), 0, 3, TimeUnit.SECONDS);
    }

    private String formMessage(String[] content) {
        StringBuilder msg = new StringBuilder();
        for (String part : content) {
            msg.append(part);
            if(!content[content.length-1].equals(part)) msg.append(":");
        }
        return msg.toString();
    }

    @Override
    public String backup(String file_name, int replicationDegree) {
        FileManager filemanager = null;
        for(FileManager file : peerContainer.getStoredFiles())
            if(file.getFile().getName().equals(file_name)) filemanager = file;
        if(filemanager == null) return "Unsuccessful BACKUP of file " + file_name + ", this file does not exist on this peer's file system";
        if(filemanager.isAlreadyBackedUp()) {
            System.out.println("This file is already backed up, ignoring command");
            return "Unsuccessful BACKUP of file " + file_name + ", backup of this file already exists";
        }
        String[] args = {String.valueOf(peerID), "BACKUP", file_name, String.valueOf(replicationDegree), String.valueOf(filemanager.getFile().length())};
        String message = formMessage(args);
        String response = peerChannel.sendMessageToMain(message);
        System.out.println("RESPONSE: " + response);

        Backup backup = new Backup();

        return "null;";
    }

    @Override
    public String restore(String file_name) {
        return null;
    }

    @Override
    public String delete(String file_name) {
        return null;
    }

    @Override
    public String reclaim(long max_disk_space) {
        return null;
    }

    @Override
    public String state() {
        return null;
    }
}
