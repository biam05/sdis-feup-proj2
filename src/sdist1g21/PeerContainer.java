package sdist1g21;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PeerContainer implements Serializable {
    private int peerID;
    private final String peerAddress;
    private final int peerPort;
    private ArrayList<FileManager> storedFiles;
    private ArrayList<FileManager> backedUpFiles;

    private long maxSpace;
    private long freeSpace;

    private static final ScheduledThreadPoolExecutor peerContainerExecutors = new ScheduledThreadPoolExecutor(
            Utils.MAX_THREADS);

    public PeerContainer(int peerID, String peerAddress, int peerPort) {
        this.peerID = peerID;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        storedFiles = new ArrayList<>();
        backedUpFiles = new ArrayList<>();
        this.maxSpace = Utils.MAX_STORAGE_SPACE;
        this.freeSpace = Utils.MAX_STORAGE_SPACE;
    }

    /**
     * Function used to save the state of the Peer Container
     */
    public synchronized void saveState() {
        peerContainerExecutors.execute(() -> {
            try {
                FileOutputStream stateFileOut = new FileOutputStream("peer " + peerID + "/state.ser");
                ObjectOutputStream out = new ObjectOutputStream(stateFileOut);
                out.writeObject(this);
                out.close();
                stateFileOut.close();
            } catch (IOException i) {
                System.err.println("> Peer " + peerID + ": Failed to save serialized state");
                i.printStackTrace();
            }
        });
    }

    /**
     * Function used to load the state of the Peer Container
     */
    public synchronized void loadState() {
        PeerContainer peerContainer;
        try {
            FileInputStream stateFileIn = new FileInputStream("peer " + peerID + "/state.ser");
            ObjectInputStream in = new ObjectInputStream(stateFileIn);
            peerContainer = (PeerContainer) in.readObject();
            in.close();
            stateFileIn.close();
            System.out.println("> Peer " + peerID + ": Serialized state of peer loaded successfully");
        } catch (Exception i) {
            System.out.println("> Peer " + peerID + ": State file of peer not found, a new one will be created");
            updateState();
            saveState();
            return;
        }

        peerID = peerContainer.getPeerID();
        storedFiles = peerContainer.getStoredFiles();
        backedUpFiles = peerContainer.getBackedUpFiles();
        maxSpace = peerContainer.getMaxSpace();
        freeSpace = peerContainer.getFreeSpace();
    }

    /**
     * Function used to read the physical state of the Peer's Filesystem and update
     * the Peer's container with it
     */
    public synchronized void updateState() {
        // Register all files
        try {
            Files.walk(Paths.get("peer " + peerID + "/files")).forEach(filePath -> {
                if (!filePath.toFile().isDirectory()) {
                    FileManager fileManager = new FileManager(
                            "peer " + peerID + "/files/" + filePath.getFileName().toString(), 0);
                    if (!storedFiles.contains(fileManager)) {
                        storedFiles.add(fileManager);
                        /*try {
                            freeSpace -= Files.size(filePath);
                        } catch (IOException e) {
                            System.err.println("> Peer " + peerID + ": Failed to get size of file: "
                                    + fileManager.getFile().getName());
                        }*/
                    }
                }
            });
            Files.walk(Paths.get("peer " + peerID + "/backups")).forEach(filePath -> {
                if (!filePath.toFile().isDirectory()) {
                    FileManager fileManager = new FileManager(
                            "peer " + peerID + "/backups/" + filePath.getFileName().toString(), 0);
                    if (!backedUpFiles.contains(fileManager)) {
                        backedUpFiles.add(fileManager);
                        try {
                            freeSpace -= Files.size(filePath);
                        } catch (IOException e) {
                            System.err.println("> Peer " + peerID + ": Failed to get size of file: "
                                    + fileManager.getFile().getName());
                        }
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("> Peer " + peerID + ": Failed to iterate files of peer");
        }
    }

    public void setMaxSpace(long maxSpace) {
        this.maxSpace = maxSpace;
    }

    public void setFreeSpace(long freeSpace) {
        this.freeSpace = freeSpace;
    }

    public int getPeerID() {
        return peerID;
    }

    public ArrayList<FileManager> getStoredFiles() {
        return storedFiles;
    }

    public ArrayList<FileManager> getBackedUpFiles() {
        return backedUpFiles;
    }

    public long getMaxSpace() {
        return maxSpace;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public long getPeerPort() {
        return peerPort;
    }

    /**
     * Function used to change the free space
     * 
     * @param size amount of space that will be added
     */
    public synchronized void addFreeSpace(long size) {
        this.freeSpace += size;
        saveState();
    }

    /**
     * Function used to add a FileManager to the Stored FileManager array
     * 
     * @param file FileManager that is gonna be stored
     */
    public synchronized void addStoredFile(FileManager file) {
        for (FileManager storedFile : this.storedFiles) {
            if (file.equals(storedFile))
                return; // cant store equal files
        }
        this.storedFiles.add(file);
        saveState();
    }

    /**
     * Function used to add a FileManager to the BackedUp FileManager array
     *
     * @param file FileManager that is gonna be stored
     */
    public synchronized void addBackedUpFile(FileManager file) {
        for (FileManager backedUpFile : this.backedUpFiles) {
            if (file.equals(backedUpFile))
                return; // cant store equal files
        }
        this.backedUpFiles.add(file);
        saveState();
    }

    /**
     * Function used to delete a File from the Stored BackedUp Files Array
     * 
     * @param file file that is gonna be deleted
     */
    public synchronized void deleteStoredBackupFile(FileManager file) {
        try {
            Files.deleteIfExists(Path.of("peer " + peerID + "\\" + "backups\\" + file.getFile().getName()));
            System.out.println("> Peer " + peerID + ": DELETE of file " + file.getFile().getName() + " finished");
        } catch (IOException e) {
            System.err.println("> Peer " + peerID + ": Failed to delete file " + file.getFile().getName());
            e.printStackTrace();
        }
    }

}
