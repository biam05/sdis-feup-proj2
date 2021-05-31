package sdist1g21;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Delete {
    private final String fileId;
    private final PeerContainer peerContainer;

    /**
     * Delete Constructor
     * 
     * @param fileId        File ID
     * @param peerContainer Peer Container
     */
    public Delete(String fileId, PeerContainer peerContainer) {
        this.fileId = fileId;
        this.peerContainer = peerContainer;
    }

    /**
     * Function used to perform the deletion of a chunk
     */
    public synchronized void performDelete() {
        ArrayList<FileManager> toBeDeleted = new ArrayList<>();
        for (FileManager file : peerContainer.getStoredFiles()) {
            if (file.getFileID().equals(fileId)) {
                peerContainer.incFreeSpace(file.getFile().length());
                Executors.newScheduledThreadPool(5).schedule(() -> {
                    peerContainer.deleteStoredFile(file);
                }, 0, TimeUnit.SECONDS);
                toBeDeleted.add(file);
            }
        }
        // Delete From Memory
        for (FileManager file : toBeDeleted) {
            peerContainer.getStoredFiles().removeIf(f -> f.equals(file));
        }
    }
}
