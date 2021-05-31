package sdist1g21;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Path;

public class Restore {
    private final String fileId;
    private final int pID;
    private final PeerContainer peerContainer;

    /**
     * Restore Constructor
     * 
     * @param fileId        File ID
     * @param pId           Peer ID
     * @param peerContainer Container of the Peer
     */
    public Restore(String fileId, int pId, PeerContainer peerContainer) {
        this.fileId = fileId;
        this.pID = pId;
        this.peerContainer = peerContainer;
    }

    /**
     * Function used to perform the Restore of a File
     * 
     * @param packet message with information about the file that is gonna be
     *               restored
     */
    public synchronized void performRestore(DatagramPacket packet) {
        for (FileManager file : peerContainer.getStoredFiles()) {
            if (file.getFileID().equals(fileId)) {
                boolean fileRestorationHasAlreadyStarted = false;
                FileManager restored_file = new FileManager(
                        Peer.getPeerPath(pID) + "files/restored" + file.getFile().getName(), 0);
                for (FileManager tmp : peerContainer.getStoredFiles()) {
                    if (tmp.getFile().getName().equals(restored_file.getFile().getName())) {
                        fileRestorationHasAlreadyStarted = true;
                        restored_file = tmp;
                        break;
                    }
                }
                if (!fileRestorationHasAlreadyStarted) {
                    peerContainer.addStoredFile(restored_file);
                }
                try {
                    // missing content
                    restored_file.createFile(
                            Path.of(Peer.getPeerPath(pID) + "files/restored_" + file.getFile().getName()), pID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(
                        "> Peer " + pID + ": RESTORE of file " + restored_file.getFile().getName() + " finished");

                /*
                 * boolean chunkAlreadyReceived = false; for(FileChunk chunk :
                 * restored_file.getChunks()) { if(chunk.getChunkNo() == chunkNo) {
                 * chunkAlreadyReceived = true; System.out.println("> Peer " + pID +
                 * ": Chunk nº" + chunkNo + " already processed, ignoring repeat"); break; } }
                 * if(!chunkAlreadyReceived) { byte[] content = Peer.getBody(packet); FileChunk
                 * chunk = new FileChunk(restored_file.getFileID(), chunkNo, content,
                 * content.length); restored_file.getChunks().add(chunk);
                 * if(restored_file.getChunks().size() == file.getChunks().size()) {
                 * restored_file.createFile(Path.of(Peer.getPeerPath(pID) + "files/restored_" +
                 * file.getFile().getName()), pID); System.out.println("> Peer " + pID +
                 * ": RESTORE of file " + restored_file.getFile().getName() + " finished"); } }
                 */

                break;
            }
        }
    }
}