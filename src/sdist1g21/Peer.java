package sdist1g21;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer implements ServiceInterface {

    /*
     * For testing purposes, the SSLPort has to be different on all peers. In
     * reality, this shouldn't be a problem because each peer is a different PC
     *
     * TestApp args: 127.0.0.1 500(peerID) <sub_protocol> <opnds>
     * Main peer args:127.0.0.1 8001
     * Other peers args: peerID(começa em 2) 230.0.0.(peerID) 600(peerID) 5000 127.0.0.1 8001
     *
     */

    private static Peer peer;

    private static SSLChannel sslChannel;

    private static PeerContainer peerContainer;

    private static PeerChannel peerToMainChannel;
    private PeerChannel peerToPeerChannel;

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

    public Peer(String[] args) {
        try {
            peerID = Integer.parseInt(args[0]);
            peerAddress = args[1];
            peerPort = Integer.parseInt(args[2]);
            SSLPort = Integer.parseInt(args[3]) + peerID;
            mainPeerAddress = args[4];
            mainPeerPort = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            System.err.println("Peer: Either one of the ports or the peerID given is not a number!");
            return;
        }

        sslChannel = new SSLChannel(SSLPort);
        sslChannel.start();

        createDirectories();
        peerContainer = new PeerContainer(peerID, peerAddress, peerPort);
        peerContainer.loadState();

        peerToMainChannel = new PeerChannel(peerID, false, false, mainPeerAddress, mainPeerPort, peerContainer);
        peerToMainChannel.start();

        peerToPeerChannel = new PeerChannel(peerID, false, true, peerAddress, peerPort, null);
        peerToPeerChannel.start();

        startAutoSave();
    }

    public static String getPeerPath(int pID) {
        return "peer " + pID + "/";
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
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
                filename = msg[1];
                return peer.delete(filename);
            }
            case "RECLAIM" -> {
                max_disk_space = Long.parseLong(msg[1]);
                peer.reclaim(max_disk_space);
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
            }
            case "STATE" -> {
                return peer.state();
            }

            default -> System.err.println("> Peer " + peerID + " got the following basic message: " + message);
        }
        return "error";
    }

    static ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> backupOpFiles = new ConcurrentHashMap<>();

    public static String messageFromPeerHandler(byte[] msgBytes) {
        String message = new String(msgBytes).trim();
        System.out.println("Received " + msgBytes.length + " bytes.");
        String[] msg = message.split(":");
        String protocol = msg[0];

        switch (protocol) {
            case "BACKUP" -> {
                System.out.println("This peer got the message: " + msg[0] + " " + msg[1] + " " + msg[2] + " " + msg[3]);
                int tmp = msg[0].length() + msg[1].length() + msg[2].length() + msg[3].length() + 4;
                long fileSize = Long.parseLong(msg[2]);
                int numMsg = Integer.parseInt(msg[3]);
                if (fileSize + tmp > Utils.MAX_BYTE_MSG) {
                    byte[] fileBytes;

                    long finalSize = fileSize;
                    if((numMsg + 1) == (int) Math.ceil((double) fileSize / Utils.MAX_BYTE_MSG)) {
                        while(finalSize > Utils.MAX_BYTE_MSG) finalSize -= Utils.MAX_BYTE_MSG;
                        fileBytes = new byte[(int) finalSize + (tmp * numMsg)];
                        System.arraycopy(msgBytes, tmp, fileBytes, 0, (int) finalSize + (tmp * numMsg));
                    } else {
                        fileBytes = new byte[msgBytes.length - tmp];
                        System.arraycopy(msgBytes, tmp, fileBytes, 0, msgBytes.length - tmp);
                    }

                    int response_time = 50 * numMsg;

                    try {
                        Thread.sleep(response_time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    ConcurrentHashMap<Integer, byte[]> tmpMap;
                    if(backupOpFiles.containsKey(msg[2])) tmpMap = backupOpFiles.get(msg[2]);
                    else tmpMap = new ConcurrentHashMap<>();

                    tmpMap.put(numMsg, fileBytes);

                    backupOpFiles.put(msg[2], tmpMap);

                    if(backupOpFiles.get(msg[2]).size() == (int) Math.ceil((double) fileSize / Utils.MAX_BYTE_MSG)) {
                        byte[] finalFileBytes = new byte[backupOpFiles.get(msg[2]).size() * Utils.MAX_BYTE_MSG];
                        for(int i = 0; i < backupOpFiles.get(msg[2]).size(); i++) {
                            System.arraycopy(backupOpFiles.get(msg[2]).get(i), 0, finalFileBytes, (msgBytes.length - tmp) * i, backupOpFiles.get(msg[2]).get(i).length);
                        }
                        Backup backupProtocol = new Backup(msg[1], finalFileBytes, fileSize, peerID);
                        backupProtocol.performBackup(peerContainer);
                        backupOpFiles.remove(msg[2]);

                        updatePeerContainerToMain();

                        return "Protocol operation finished";
                    }

                    return "Awaiting further messages with file information";
                } else {
                    byte[] fileBytes = new byte[(int) fileSize];
                    System.arraycopy(msgBytes, tmp, fileBytes, 0, (int) fileSize);

                    Backup backupProtocol = new Backup(msg[1], fileBytes, fileSize, peerID);
                    backupProtocol.performBackup(peerContainer);
                    return "Protocol operation finished";
                }
            }
            case "DELETE" -> {
                System.out.println("This peer got the message: " + msg[0] + " " + msg[1]);
                String filename = msg[1];
                Delete deleteProtocol = new Delete(filename, peerContainer);
                deleteProtocol.performDelete();
                return "Protocol operation finished";
            }
            default -> {
                System.err.println("> Peer " + peerID + " got the following basic message: " + message);
                return "No response necessary";
            }
        }
    }

    private static void updatePeerContainerToMain() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(peerContainer);
            out.flush();
            byte[] peerContainerBytes = bos.toByteArray();

            bos.close();
            peerToMainChannel.sendMessageToMain(peerID + ":PEERCONTAINER:", peerContainerBytes);
        } catch (IOException e) {
            System.err.println("PeerChannel exception: Failed to encode peerContainer!");
            e.printStackTrace();
        }
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
     * Function used to load the state of the peer from a file state.ser, update it
     * with any changes on the physical file system, and initialize a thread that
     * auto-saves the state of the peer every 3 seconds
     */
    private synchronized void startAutoSave() {
        peerContainer.updateState();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> peerContainer.saveState(), 0, 3,
                TimeUnit.SECONDS);
    }

    private String formMessage(String[] content) {
        StringBuilder msg = new StringBuilder();
        for (String part : content) {
            msg.append(part);
            if (!content[content.length - 1].equals(part))
                msg.append(":");
        }
        return msg.toString();
    }

    @Override
    public String backup(String file_name, int desiredReplicationDegree) {
        FileManager filemanager = null;
        for (FileManager file : peerContainer.getStoredFiles())
            if (file.getFile().getName().equals(file_name))
                filemanager = file;
        if (filemanager == null)
            return "Unsuccessful BACKUP of file " + file_name + ", this file does not exist on this peer's file system";
        if (filemanager.isAlreadyBackedUp()) {
            System.out.println("This file is already backed up, ignoring command");
            return "Unsuccessful BACKUP of file " + file_name + ", backup of this file already exists";
        }
        filemanager.setDesiredReplicationDegree(desiredReplicationDegree);
        String[] args = { String.valueOf(peerID), "BACKUP", file_name, String.valueOf(desiredReplicationDegree),
                String.valueOf(filemanager.getFile().length()) };
        String message = formMessage(args);
        String response = peerToMainChannel.sendMessageToMain(message, null);

        String[] peers = response.split("=");

        ArrayList<String> results = new ArrayList<>();

        for (String peer : peers) {
            String[] tmp = peer.split(":");
            String address = tmp[0];
            String port = tmp[1];
            try {
                results.add(peerToPeerChannel.sendMessageToPeer("BACKUP", address, port, file_name,
                        filemanager.getFile().length(), Files.readAllBytes(Path.of(filemanager.getFile().getPath()))));
            } catch (IOException e) {
                System.err.println("> Peer " + peerID + " exception: failed to read bytes of file");
            }
        }

        int actualRepDegree = 0;
        for (String r : results) {
            if (!r.equals("error")) {
                actualRepDegree++;
            }
        }
        if(actualRepDegree > 0) filemanager.setAlreadyBackedUp(true);
        filemanager.setActualReplicationDegree(actualRepDegree);

        updatePeerContainerToMain();

        return "BACKUP operation finished with a replication degree of " + actualRepDegree;
    }

    @Override
    public String restore(String file_name) {
        return null;
    }

    @Override
    public String delete(String file_name) {
        FileManager filemanager = null;
        for (FileManager file : peerContainer.getStoredFiles())
            if (file.getFile().getName().equals(file_name))
                filemanager = file;
        if (filemanager == null)
            return "Unsuccessful DELETE of file " + file_name + ", this file does not exist on this peer's file system";
        if (!filemanager.isAlreadyBackedUp()) {
            System.out.println("This file is not backed up, ignoring command");
            return "Unsuccessful DELETE of file " + file_name + ", this file has no backups in the network!";
        }

        String[] args = { String.valueOf(peerID), "DELETE", file_name };
        String message = formMessage(args);
        String response = peerToMainChannel.sendMessageToMain(message, null);

        String[] peers = response.split("=");

        ArrayList<String> results = new ArrayList<>();

        for (String peer : peers) {
            String[] tmp = peer.split(":");
            String address = tmp[0];
            String port = tmp[1];
            results.add(peerToPeerChannel.sendMessageToPeer("DELETE", address, port, file_name, -1, null));
        }

        int failedOps = 0;
        for (String r : results) {
            if (r.equals("error")) {
                failedOps++;
            }
        }

        updatePeerContainerToMain();

        if(failedOps > 0) return "DELETE operation finished but failed on " + failedOps + " peers";
        else return "DELETE operation finished";
    }


    @Override
    public String reclaim(long max_disk_space) {
        return null;
    }

    @Override
    public String state() {
        int nFile = 1, spaceNum = 0;
        String space = " ";
        StringBuilder state = new StringBuilder();
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                                   PEER ").append(peerID).append("                                  :::[n]");
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                            OWN BACKED UP FILES                            :::[n]");
        // each file whose backup has initiated
        for(FileManager fileManager : peerContainer.getStoredFiles()){
            if(fileManager.isAlreadyBackedUp()) {
                state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
                state.append("::: FILE ").append(nFile).append("                                                                    :::[n]");
                state.append(":::                                                                           :::[n]");
                state.append("::: IS BACKED UP: YES").append("                                                         :::[n]");
                state.append("::: PATH: ").append(fileManager.getFile().getPath());
                spaceNum = 68 - fileManager.getFile().getPath().length();
                state.append(space.repeat(spaceNum)).append(":::[n]");
                state.append("::: FILE_ID: ").append(fileManager.getFileID()).append(" :::[n]");
                state.append("::: DESIRED REPLICATION DEGREE: ").append(fileManager.getDesiredReplicationDegree()).append("                                             :::[n]");
                state.append("::: CURRENT REPLICATION DEGREE: ").append(fileManager.getActualReplicationDegree()).append("                                             :::[n]");
                nFile++;
            } else {
                state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
                state.append("::: FILE ").append(nFile).append("                                                                    :::[n]");
                state.append(":::                                                                           :::[n]");
                state.append("::: IS BACKED UP: NO").append("                                                          :::[n]");
                nFile = calculatePathSpaceSize(nFile, space, state, fileManager);
            }
        }
        if(nFile == 1){
            state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
            state.append("::: NONE OF MY FILES HAVE BEEN BACKED UP YET                                  :::[n]");
        }
        nFile = 1;

        // backed up files from other peers
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                           OTHER BACKED UP FILES                           :::[n]");
        for(FileManager fileManager : peerContainer.getBackedUpFiles()){
            state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
            state.append("::: FILE ").append(nFile).append("                                                                    :::[n]");
            state.append(":::                                                                           :::[n]");
            nFile = calculatePathSpaceSize(nFile, space, state, fileManager);
        }
        if(nFile == 1){
            state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
            state.append("::: I DON'T HAVE BACKED UP FILES FROM OTHER PEERS YET                         :::[n]");
        }

        // peer storage capacity
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                           PEER STORAGE CAPACITY                           :::[n]");
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append("::: TOTAL DISK SPACE: ").append(peerContainer.getMaxSpace()).append(" B");
        spaceNum = 53 - String.valueOf(peerContainer.getMaxSpace()).length();
        state.append(space.repeat(spaceNum)).append(":::[n]");
        state.append("::: FREE SPACE: ").append(peerContainer.getFreeSpace()).append(" B");
        spaceNum = 59 - String.valueOf(peerContainer.getFreeSpace()).length();
        state.append(space.repeat(spaceNum)).append(":::[n]");
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");

        System.out.println(state);
        return state.toString();
    }

    private int calculatePathSpaceSize(int nFile, String space, StringBuilder state, FileManager fileManager) {
        int spaceNum;
        state.append("::: PATH: ").append(fileManager.getFile().getPath());
        spaceNum = 68 - fileManager.getFile().getPath().length();
        state.append(space.repeat(spaceNum)).append(":::[n]");
        state.append("::: FILE_ID: ").append(fileManager.getFileID()).append(" :::[n]");
        nFile++;
        return nFile;
    }

}
