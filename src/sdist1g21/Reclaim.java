package sdist1g21;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Reclaim Class
 */
public class Reclaim {
    private final String fileId;
    private final double protocol_version;
    private final int pID;
    private final PeerContainer peerContainer;
    private final ScheduledThreadPoolExecutor peerExecutors;
    private final ConcurrentHashMap<String, ScheduledFuture<String>> activeOps;

    /**
     * Reclaim Constructor
     * 
     * @param fileId           File ID
     * @param protocol_version Protocol version
     * @param pId              Peer ID
     * @param peerContainer    Peer container
     * @param peerExecutors    Peer executor pool
     * @param activeOps        active putchunk operations
     */
    public Reclaim(String fileId, double protocol_version, int pId, PeerContainer peerContainer,
            ScheduledThreadPoolExecutor peerExecutors, ConcurrentHashMap<String, ScheduledFuture<String>> activeOps) {
        this.fileId = fileId;
        this.protocol_version = protocol_version;
        this.pID = pId;
        this.peerContainer = peerContainer;
        this.peerExecutors = peerExecutors;
        this.activeOps = activeOps;
    }

    /**
     * Function used to perform the space reclaim
     */
    public synchronized void performReclaim() {
        boolean ownedFile = false;
        for (FileManager file : peerContainer.getStoredFiles()) {
            if (file.getFileID().equals(fileId)) {
                ownedFile = true;
                file.setReplicationDegree(file.getReplicationDegree() - 1);

            }
        }

    }

}
