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
    /*private final String filename;
    private final byte[] content;
    private final int pID;*/

    /**
     * Backup Constructor
     */
    public Backup(){
        /*this.fileId = chunk.getFileID();
        this.chunkNo = chunk.getChunkNo();
        this.content = chunk.getContent();
        this.pID = pId;*/
    }

    /**
     * Function used to perform the backup of a chunk
     */
    public synchronized void performBackup() {
        /*try {
            Path path = Path.of("peer " + pID + "\\" + "chunks\\" + fileId + "_" + chunkNo);
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            fileChannel.write(ByteBuffer.wrap(content), 0);
            fileChannel.close();
            System.out.println("> Peer " + pID + ": saved chunk nº" + chunkNo + " of file with fileID: " + fileId);
        } catch (IOException e) {
            System.err.println("> Peer " + pID + " exception: failed to save chunk " +  chunkNo);
            e.printStackTrace();
        }*/
    }
}
