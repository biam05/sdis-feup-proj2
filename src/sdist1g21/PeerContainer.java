package sdist1g21;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PeerContainer implements Serializable {
    private int peerID;
    private String peerAddress;
    private int peerPort;
    private ArrayList<FileManager> storedFiles;

    private long maxSpace;
    private long freeSpace;

    private static final ScheduledThreadPoolExecutor peerContainerExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    public PeerContainer(int peerID, String peerAddress, int peerPort) {
        this.peerID = peerID;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        storedFiles = new ArrayList<>();
        this.maxSpace = this.freeSpace = Utils.MAX_STORAGE_SPACE;
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
                //System.out.println("> Peer " + peerID + ": Serialized state saved in /peer " + peerID + "/state.ser");
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
        maxSpace = peerContainer.getMaxSpace();
        freeSpace = peerContainer.getFreeSpace();
    }

    /**
     * Function used to read the physical state of the Peer's Filesystem and update the Peer's container with it
     */
    public synchronized void updateState() {
        // Register all files
        try {
            Files.walk(Paths.get("peer " + peerID + "/files")).forEach(filePath -> {
                if (!filePath.toFile().isDirectory()) {
                    FileManager fileManager = new FileManager("peer " + peerID + "/files/" + filePath.getFileName().toString(), 0);
                    if(!storedFiles.contains(fileManager)) {
                        storedFiles.add(fileManager);
                        try {
                            freeSpace -= Files.size(filePath);
                        } catch (IOException e) {
                            System.err.println("> Peer " + peerID + ": Failed to get size of file: " + fileManager.getFile().getName());
                        }
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("> Peer " + peerID + ": Failed to iterate files of peer");
        }
    }

    public int getPeerID() {
        return peerID;
    }

    public ArrayList<FileManager> getStoredFiles() {
        return storedFiles;
    }

    public long getMaxSpace() {
        return maxSpace;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public String getPeerAdress() {
        return peerAddress;
    }

    public long getPeerPort() {
        return peerPort;
    }
}
