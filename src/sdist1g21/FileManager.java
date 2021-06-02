package sdist1g21;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

public class FileManager implements Serializable {

    private final String fileID;
    private final File file;

    private int desiredReplicationDegree;
    private int actualReplicationDegree;

    private boolean alreadyBackedUp;

    /**
     * FileManager Constructor
     * 
     * @param path              Path of the File
     * @param desiredReplicationDegree Desired Replication Degree of the File
     */
    public FileManager(String path, int desiredReplicationDegree) {
        this.file = new File(path);
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.fileID = id();
        this.alreadyBackedUp = false;
    }

    /**
     * Desired Replication Degree Getter
     * 
     * @return Desired Replication Degree
     */
    public int getDesiredReplicationDegree() {
        return desiredReplicationDegree;
    }

    public void setDesiredReplicationDegree(int r) {
        this.desiredReplicationDegree = r;
    }

    /**
     * Actual Replication Degree Getter
     *
     * @return Actual Replication Degree
     */
    public int getActualReplicationDegree() {
        return actualReplicationDegree;
    }

    public void setActualReplicationDegree(int r) {
        this.actualReplicationDegree = r;
    }

    /**
     * File ID Getter
     * 
     * @return File ID
     */
    public String getFileID() {
        return fileID;
    }

    /**
     * File Getter
     * 
     * @return File
     */
    public File getFile() {
        return file;
    }

    /**
     * Check if a file was already backed up
     * 
     * @return True if the file was already backed up. False otherwise
     */
    public boolean isAlreadyBackedUp() {
        return alreadyBackedUp;
    }

    /**
     * Change if the file was already backed up
     * 
     * @param alreadyBackedUp new value
     */
    public void setAlreadyBackedUp(boolean alreadyBackedUp) {
        this.alreadyBackedUp = alreadyBackedUp;
    }

    /**
     * Function used to get the File ID
     * 
     * @return File ID (after SHA256 encoding)
     *
     *         Reutilized from Project 1
     */
    private synchronized String id() {
        String filename = this.file.getName(); // file name
        String filedate = String.valueOf(this.file.lastModified()); // date modified
        String fileowner = this.file.getParent(); // owner

        String originalString = filename + ":" + filedate + ":" + fileowner;
        return Utils.sha256(originalString); // sha-256 encryption
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileManager)) {
            return false;
        }
        FileManager fm = (FileManager) o;
        return this.fileID.equals(fm.fileID);
    }

}
