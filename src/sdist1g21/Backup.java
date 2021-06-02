package sdist1g21;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Backup Class
 */
public class Backup {
    private final String filename;
    private final byte[] content;
    private final int pID;

    /**
     * Backup Constructor
     *
     * @param filename      Name of the file
     * @param filecontent   Content of the file
     * @param fileSize      Size of the file
     * @param pID           Peer ID
     */
    public Backup(String filename, byte[] filecontent, long fileSize, int pID) {
        this.filename = filename;
        this.content = new byte[(int) fileSize];
        System.arraycopy(filecontent, 0, this.content, 0, (int) fileSize);
        this.pID = pID;
    }

    /**
     * Function used to perform the backup of a file
     *
     * @param peerContainer Peer container to be updated with the new file
     */
    public synchronized void performBackup(PeerContainer peerContainer) {
        try {
            FileManager backedUpFile = new FileManager("peer " + pID + "/" + "backups/" + filename, -1);
            peerContainer.addBackedUpFile(backedUpFile);
            Path path = Path.of("peer " + pID + "/" + "backups/" + filename);
            saveFile(peerContainer, backedUpFile, path, content, pID, filename);
        } catch (IOException e) {
            System.err.println("> Peer " + pID + " exception: failed to save file " +  filename);
            e.printStackTrace();
        }
    }

    static void saveFile(PeerContainer peerContainer, FileManager restoredFile, Path path, byte[] content, int pID, String filename) throws IOException {
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        fileChannel.write(ByteBuffer.wrap(content), 0);
        peerContainer.addFreeSpace(-restoredFile.getFile().length());
        fileChannel.close();
        System.out.println("> Peer " + pID + ": saved file " + filename);
    }
}
