package sdist1g21;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Restore {
    private final String filename;
    private final byte[] content;
    private final int pID;

    /**
     * Restore Constructor
     *
     * @param filename      Name of the file
     * @param filecontent   Content of the file
     * @param fileSize      Size of the file
     * @param pID           Peer ID
     */
    public Restore(String filename, byte[] filecontent, long fileSize, int pID) {
        this.filename = filename;
        this.content = new byte[(int) fileSize];
        System.arraycopy(filecontent, 0, this.content, 0, (int) fileSize);
        this.pID = pID;
    }

    /**
     * Function used to perform the Restore of a File
     *
     * @param peerContainer Peer container to be updated with the new file
     */
    public synchronized void performRestore(PeerContainer peerContainer) {
        try {
            FileManager restoredFile = new FileManager("peer " + pID + "/" + "files/" + filename, -1);
            Path path = Path.of("peer " + pID + "/" + "files/" + filename);
            Backup.saveFile(peerContainer, restoredFile, path, content, pID, filename);
        } catch (IOException e) {
            System.err.println("> Peer " + pID + " exception: failed to save file " +  filename);
            e.printStackTrace();
        }
    }
}