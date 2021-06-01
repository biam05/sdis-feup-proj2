package sdist1g21;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Delete {
    private final String filename;
    private final PeerContainer peerContainer;

    /**
     * Delete Constructor
     * 
     * @param filename        File ID
     * @param peerContainer Peer Container
     */
    public Delete(String filename, PeerContainer peerContainer) {
        this.filename = filename;
        this.peerContainer = peerContainer;
    }

    /**
     * Function used to perform the deletion of a chunk
     */
    public synchronized void performDelete() {
        ArrayList<FileManager> toBeDeleted = new ArrayList<>();
        for (FileManager file : peerContainer.getBackedUpFiles()) {
            if (file.getFile().getName().equals(filename)) {
                peerContainer.addFreeSpace(file.getFile().length());
                Executors.newScheduledThreadPool(5).schedule(() -> {
                    peerContainer.deleteStoredBackupFile(file);
                }, 0, TimeUnit.SECONDS);
                toBeDeleted.add(file);
            }
        }
        // Delete From Memory
        for (FileManager file : toBeDeleted) {
            peerContainer.getBackedUpFiles().removeIf(f -> f.equals(file));
        }
    }
}
