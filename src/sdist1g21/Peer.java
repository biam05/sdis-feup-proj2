package sdist1g21;

import jdk.jshell.execution.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer implements ServiceInterface {

    /* For testing purposes, the SSLPort has to be different on all peers. In reality, this shouldn't be a problem because each peer is a different PC
    *
    * TestApp args: 127.0.0.1 500(peerID) <sub_protocol> <opnds>
    * Main peer args: 127.0.0.1 8001
    * Other peers args: peerID 230.0.0.(peerID) 600(peerID) 5000 127.0.0.1 8001
    *
    */

    private static Peer peer;

    private static SSLChannel sslChannel;

    private PeerContainer peerContainer;

    private PeerChannel peerToMainChannel, peerToPeerChannel;

    private String mainPeerAddress, peerAddress;
    private int mainPeerPort, peerPort;
    private static int peerID;
    private int SSLPort;

    public static void main(String[] args) {
        // check usage
        if (args.length != 6) {
            System.out.println("Usage: java Peer PeerID PeerAddress PeerPort SSLPort MainPeerAddress MainPeerPort");
            return;
        }

        Peer.peer = new Peer(args);
    }

    public Peer(String[] args){
        try {
            peerID = Integer.parseInt(args[0]);
            peerAddress = args[1];
            peerPort = Integer.parseInt(args[2]);
            SSLPort = Integer.parseInt(args[3]) + peerID;
            mainPeerAddress = args[4];
            mainPeerPort = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            System.err.println( "Peer: Either one of the ports or the peerID given is not a number!");
            return;
        }

        sslChannel = new SSLChannel(SSLPort);
        sslChannel.start();

        peerContainer = new PeerContainer(peerID, peerAddress, peerPort);

        peerToMainChannel = new PeerChannel(peerID, false, false, mainPeerAddress, mainPeerPort, peerContainer);
        peerToMainChannel.start();

        peerToPeerChannel = new PeerChannel(peerID, false, true, peerAddress, peerPort, null);
        peerToPeerChannel.start();

        createDirectories();
        startAutoSave();
    }

    public static String messageFromTestAppHandler(String message) {
        System.out.println("This peer got the message: " + message);
        String[] msg = message.split(":");
        String filename;
        int rep_deg;
        long max_disk_space;
        switch (msg[0].toUpperCase(Locale.ROOT)) {
            case "BACKUP" -> {
                if (msg.length < 3) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                    return "error";
                }
                filename = msg[1];
                rep_deg = Integer.parseInt(msg[2]);
                return peer.backup(filename, rep_deg);
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
        return "error";
    }

    static ConcurrentHashMap<String, Integer> backupOps = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, byte[]> backupOpFiles = new ConcurrentHashMap<>();

    public static String messageFromPeerHandler(byte[] msgBytes) {
        String message = new String(msgBytes).trim();
        System.out.println("Received " + msgBytes.length + " bytes.");
        String[] msg = message.split(":");
        String protocol = msg[0];

        switch (protocol) {
            case "BACKUP" -> {
                System.out.println("This peer got the message: " + msg[0] + " " + msg[1] + " " + msg[2]);

                int tmp = msg[0].length() + msg[1].length() + msg[2].length() + 3;

                long fileSize = Long.parseLong(msg[2]);

                System.out.println("LOOK: " + 0);

                if(fileSize > Utils.MAX_BYTE_MSG) {
                    System.out.println("LOOK: " + 1);
                    if(!backupOps.containsKey(msg[1])) backupOps.put(msg[1], (int) Math.ceil((double) fileSize / Utils.MAX_BYTE_MSG));
                    System.out.println("LOOK: " + 2);

                    byte[] fileBytes = new byte[Utils.MAX_BYTE_MSG];
                    System.arraycopy(msgBytes, tmp, fileBytes, 0, msgBytes.length - tmp);

                    System.out.println("LOOK: " + 3);

                    if(backupOpFiles.containsKey(msg[1])) backupOpFiles.put(msg[1], fileBytes);
                    else {
                        System.out.println("LOOK: " + 4);
                        System.arraycopy(backupOpFiles.get(msg[1]), 0, fileBytes, fileBytes.length-1, backupOpFiles.get(msg[1]).length);
                        backupOpFiles.put(msg[1], fileBytes);
                        backupOps.put(msg[1], backupOps.get(msg[1]) - 1);
                        if(backupOps.get(msg[1]) == 0) {
                            System.out.println("LOOK: " + 5);
                            Backup backupProtocol = new Backup(msg[1], fileBytes, peerID);
                            backupProtocol.performBackup();
                            backupOps.remove(msg[1]);
                            backupOpFiles.remove(msg[1]);
                        }
                    }
                } else {
                    System.out.println("LOOK: " + 6);
                    byte[] fileBytes = new byte[(int) fileSize];
                    System.arraycopy(msgBytes, tmp, fileBytes, 0, (int) fileSize);

                    Backup backupProtocol = new Backup(msg[1], fileBytes, peerID);
                    backupProtocol.performBackup();
                }
                System.out.println("LOOK: " + 7);
            }
            default -> System.err.println("> Peer " + peerID + " got the following basic message: " + message);
        }

        return "Protocol operation finished";
    }


    /**
     * Function used to create Peer Directories
     */
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get("peer " + peerID + "\\files"));
            Files.createDirectories(Paths.get("peer " + peerID + "\\backups"));
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
        String response = peerToMainChannel.sendMessageToMain(message, null);

        String[] peers = response.split("=");

        ArrayList<String> results = new ArrayList<>();

        for(String peer : peers) {
            String[] tmp = peer.split(":");
            String address = tmp[0];
            String port = tmp[1];
            try {
                results.add(peerToPeerChannel.sendMessageToPeer("BACKUP", address, port, file_name, filemanager.getFile().length(), Files.readAllBytes(Path.of(filemanager.getFile().getPath()))));
            } catch (IOException e) {
                System.err.println("> Peer " + peerID + " exception: failed to read bytes of file");
            }
        }

        int actualRepDegree = 0;
        for (String r : results) {
            if(!r.equals("error")) {
                actualRepDegree++;
            }
        }
        return "BACKUP operation finished with a replication degree of " + actualRepDegree;
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
