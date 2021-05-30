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
     */
    public Backup(String filename, byte[] filecontent, int pID) {
        this.filename = filename;
        this.content = filecontent;
        this.pID = pID;
    }

    /**
     * Function used to perform the backup of a file
     */
    public synchronized void performBackup() {
        try {
            Path path = Path.of("peer " + pID + "/" + "backups/" + filename);
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            fileChannel.write(ByteBuffer.wrap(content), 0);
            fileChannel.close();
            System.out.println("> Peer " + pID + ": saved file " + filename);
        } catch (IOException e) {
            System.err.println("> Peer " + pID + " exception: failed to save file " +  filename);
            e.printStackTrace();
        }
    }
}
