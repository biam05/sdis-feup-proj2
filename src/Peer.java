package sdist1g21;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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

    private static PeerContainer peerContainer;

    private static PeerChannel peerToMainChannel;
    private static PeerChannel peerToPeerChannel;

    private static final ScheduledThreadPoolExecutor peerExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    private String peerAddress;
    private int peerPort;
    private static int peerID;

    public static void main(String[] args) {
        // check usage
        if (args.length != 6) {
            System.out.println("Usage: java Peer PeerID PeerAddress PeerPort SSLPort MainPeerAddress MainPeerPort");
            return;
        }

        Peer.peer = new Peer(args);
    }

    public Peer(String[] args) {
        String mainPeerAddress;
        int mainPeerPort;
        int SSLPort;
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

        SSLChannel sslChannel = new SSLChannel(SSLPort);
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
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
                filename = msg[1];
                return peer.restore(filename);
            }
            case "DELETE" -> {
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
                filename = msg[1];
                return peer.delete(filename);
            }
            case "RECLAIM" -> {
                if (msg.length < 2) {
                    System.err.println("> Peer " + peerID + " exception: invalid message received");
                }
                max_disk_space = Long.parseLong(msg[1]);
                return peer.reclaim(max_disk_space);
            }
            case "STATE" -> {
                return peer.state();
            }
            case "MAINSTATE" -> {
                return peer.mainState();
            }
            case "INVALID PROTOCOL" -> {
                return "INVALID PROTOCOL";
            }
            default -> System.err.println("> Peer " + peerID + " got the following basic message: " + message);
        }
        return "error";
    }

    static ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> backupOpFiles = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> restoreOpFiles = new ConcurrentHashMap<>();

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
                        byte[] finalFileBytes = getFinalFileBytes(msgBytes, msg, tmp, backupOpFiles);

                        Backup backupProtocol = new Backup(msg[1], finalFileBytes, fileSize, peerID);
                        backupProtocol.performBackup(peerContainer);
                        backupOpFiles.remove(msg[2]);
                        peerContainer.updateState();

                        updatePeerContainerToMain();

                        return "Protocol operation finished";
                    }

                    return "Awaiting further messages with file information";
                } else {
                    byte[] fileBytes = new byte[(int) fileSize];
                    System.arraycopy(msgBytes, tmp, fileBytes, 0, (int) fileSize);

                    Backup backupProtocol = new Backup(msg[1], fileBytes, fileSize, peerID);
                    backupProtocol.performBackup(peerContainer);
                    peerContainer.updateState();

                    updatePeerContainerToMain();

                    return "Protocol operation finished";
                }
            }
            case "REQUESTRESTORE" -> {
                System.out.println("This peer got the message: " + msg[0] + " " + msg[1] + " " + msg[2] + " " + msg[3]);
                String filename = msg[1];
                String address = msg[2];
                String port = msg[3];

                FileManager filemanager = null;
                for (FileManager file : peerContainer.getBackedUpFiles())
                    if (file.getFile().getName().equals(filename))
                        filemanager = file;
                if (filemanager == null)
                    return "Unsuccessful RESTORE of file " + filename + ", this file does not exist on this peer's backup system";

                return sendRestoreFile(address, port, filemanager);
            }
            case "GETRESTORE" -> {
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
                    if(restoreOpFiles.containsKey(msg[2])) tmpMap = restoreOpFiles.get(msg[2]);
                    else tmpMap = new ConcurrentHashMap<>();

                    tmpMap.put(numMsg, fileBytes);

                    restoreOpFiles.put(msg[2], tmpMap);

                    if(restoreOpFiles.get(msg[2]).size() == (int) Math.ceil((double) fileSize / Utils.MAX_BYTE_MSG)) {
                        byte[] finalFileBytes = getFinalFileBytes(msgBytes, msg, tmp, restoreOpFiles);
                        Restore restoreProtocol = new Restore("restored_" + msg[1], finalFileBytes, fileSize, peerID);
                        restoreProtocol.performRestore(peerContainer);
                        restoreOpFiles.remove(msg[2]);
                        peerContainer.updateState();

                        updatePeerContainerToMain();

                        return "Protocol operation finished";
                    }

                    return "Awaiting further messages with file information";
                } else {
                    byte[] fileBytes = new byte[(int) fileSize];
                    System.arraycopy(msgBytes, tmp, fileBytes, 0, (int) fileSize);

                    Restore restoreProtocol = new Restore("restored_" + msg[1], fileBytes, fileSize, peerID);
                    restoreProtocol.performRestore(peerContainer);

                    updatePeerContainerToMain();

                    return "Protocol operation finished";
                }
            }
            case "DELETE" -> {
                System.out.println("This peer got the message: " + msg[0] + " " + msg[1]);
                String filename = msg[1];
                FileManager file = null;

                for (FileManager f : peerContainer.getBackedUpFiles()) {
                    if (f.getFile().getName().equals(filename)) {
                        file = f;
                        break;
                    }
                }

                if(file == null)
                    return "Failed to delete file " + filename + ". This file does not exist on this peer!";

                peerContainer.addFreeSpace(file.getFile().length());

                Delete deleteProtocol = new Delete(filename, peerContainer);
                deleteProtocol.performDelete();

                updatePeerContainerToMain();

                return "Protocol operation finished";
            }
            case "DECREP" -> {
                System.out.println("This peer got the message: " + msg[0] + " " + msg[1]);
                String filename = msg[1];

                FileManager file = null;

                for(FileManager f : peerContainer.getStoredFiles()) {
                    if(f.getFile().getName().equals(filename))
                        file = f;
                }

                if(file == null)
                    return "Failed to decrement replication degree of file " + filename + ". This file does not exist on this peer!";

                file.setActualReplicationDegree(file.getActualReplicationDegree() - 1);

                if(file.getActualReplicationDegree() == 0)
                    file.setAlreadyBackedUp(false);

                updatePeerContainerToMain();

                return "Protocol operation finished";
            }
            default -> {
                System.err.println("> Peer " + peerID + " got the following basic message: " + message);
                return "No response necessary";
            }
        }
    }

    private static byte[] getFinalFileBytes(byte[] msgBytes, String[] msg, int tmp, ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> restoreOpFiles) {
        byte[] finalFileBytes = new byte[restoreOpFiles.get(msg[2]).size() * Utils.MAX_BYTE_MSG];
        for(int i = 0; i < restoreOpFiles.get(msg[2]).size(); i++) {
            System.arraycopy(restoreOpFiles.get(msg[2]).get(i), 0, finalFileBytes, (msgBytes.length - tmp) * i, restoreOpFiles.get(msg[2]).get(i).length);
        }
        return finalFileBytes;
    }

    private static void updatePeerContainerToMain() {
        peerExecutors.execute(() -> {
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
        });
    }

    /**
     * Function used to create Peer Directories
     */
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get("peer " + peerID + "/files"));
            Files.createDirectories(Paths.get("peer " + peerID + "/backups"));
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
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                    peerContainer.updateState();
                    peerContainer.saveState();
                }, 0, 3,
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

        if(response.equals("empty"))
            return "BACKUP operation failed, there are no peers available to backup the file";

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
                return "error";
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
        FileManager filemanager = null;
        for (FileManager file : peerContainer.getStoredFiles())
            if (file.getFile().getName().equals(file_name))
                filemanager = file;
        if (filemanager == null)
            return "Unsuccessful RESTORE of file " + file_name + ", this file does not exist on this peer's file system";
        if (!filemanager.isAlreadyBackedUp()) {
            System.out.println("This file is not backed up, ignoring command");
            return "Unsuccessful RESTORE of file " + file_name + ", this file has no backups in the network!";
        }

        String[] args = { String.valueOf(peerID), "RESTORE", file_name };
        String message = formMessage(args);
        String response = peerToMainChannel.sendMessageToMain(message, null);

        if(response.equals("empty"))
            return "RESTORE operation failed, no peers that hold the file have been found";

        String[] peers = response.split("=");

        for (String peer : peers) {
            String[] tmp = peer.split(":");
            String address = tmp[0];
            String port = tmp[1];
            String result = peerToPeerChannel.sendRestoreMessageToPeer("REQUESTRESTORE", address, port, file_name, peerAddress, peerPort);
            if(result.equals("Protocol operation finished")) return result;
        }

        updatePeerContainerToMain();

        return "Unsuccessful RESTORE of file " + file_name + ", no peers were able to successfully reply with the file!";
    }

    public static String sendRestoreFile(String address, String port, FileManager file) {
        try {
            return peerToPeerChannel.sendMessageToPeer("GETRESTORE", address, port, file.getFile().getName(),
                    file.getFile().length(), Files.readAllBytes(Path.of(file.getFile().getPath())));
        } catch (IOException e) {
            System.err.println("> Peer " + peerID + " exception: failed to read bytes of file");
            return "error";
        }
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

        if(response.equals("empty"))
            return "DELETE operation failed, none of the peers that hold the file are online";

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
            if (r.equals("error"))
                failedOps++;
             else
                filemanager.setActualReplicationDegree(filemanager.getActualReplicationDegree() - 1);
        }

        if(failedOps > 0) {
            filemanager.setDesiredReplicationDegree(0);
            updatePeerContainerToMain();
            return "DELETE operation finished but failed on " + failedOps + " peer(s)";
        } else {
            filemanager.setDesiredReplicationDegree(-1);
            filemanager.setAlreadyBackedUp(false);
            updatePeerContainerToMain();
            return "DELETE operation finished";
        }
    }

    @Override
    public String reclaim(long max_disk_space) {
        peerContainer.setFreeSpace(max_disk_space - (peerContainer.getMaxSpace() - peerContainer.getFreeSpace()));
        peerContainer.setMaxSpace(max_disk_space);

        while(peerContainer.getFreeSpace() < 0) {
            FileManager file = peerContainer.getBackedUpFiles().get(0);

            String[] args = { String.valueOf(peerID), "RECLAIM", file.getFile().getName(), String.valueOf(file.getFile().length())};
            String message = formMessage(args);
            String response = peerToMainChannel.sendMessageToMain(message, null);

            String[] separate = response.split("\\|");

            if(separate.length == 1) { // No avaliable peers for backup operation
                String ownerPeerAddress = separate[0].split("&")[0];
                String ownerPeerPort = separate[0].split("&")[1];

                String result;

                result = peerToPeerChannel.sendMessageToPeer("DECREP", ownerPeerAddress, ownerPeerPort,
                        file.getFile().getName(), 0, null);

                Delete deleteProtocol = new Delete(file.getFile().getName(), peerContainer);
                deleteProtocol.performDelete();

                peerContainer.addFreeSpace(file.getFile().length());

                if(result.equals("Protocol operation finished")) System.out.println("File " + file.getFile().getName() + " deleted from Peer "
                        + peerID + " but no avaliable peers were found to save the file, owner peer was warned to decrement replication degree");
                else System.out.println("File " + file.getFile().getName() + " deleted from Peer "
                        + peerID + " but no avaliable peers were found to save the file, tried to warn owner peer unsuccessfully");
            } else {
                response = response.substring(response.indexOf("|") + 1);
                String[] peers = response.split("=");

                for (String peer : peers) {
                    String[] tmp = peer.split(":");
                    String address = tmp[0];
                    String port = tmp[1];
                    String result;
                    try {
                        result = peerToPeerChannel.sendMessageToPeer("BACKUP", address, port,
                                file.getFile().getName(), file.getFile().length(), Files.readAllBytes(Path.of(file.getFile().getPath())));
                    } catch (IOException e) {
                        System.err.println("> Peer " + peerID + " exception: failed to read bytes of file");
                        return "error";
                    }
                    if(result.equals("Protocol operation finished")) break;
                }

                Delete deleteProtocol = new Delete(file.getFile().getName(), peerContainer);
                deleteProtocol.performDelete();

                peerContainer.addFreeSpace(file.getFile().length());
            }
        }

        updatePeerContainerToMain();

        return "RECLAIM operation finished";
    }

    @Override
    public String state() {
        return Utils.peerState(peerID, peerContainer);
    }

    @Override
    public String mainState() {
        String[] args = { String.valueOf(peerID), "STATE" };
        String message = formMessage(args);
        return peerToMainChannel.sendMessageToMain(message, null);
    }
}
