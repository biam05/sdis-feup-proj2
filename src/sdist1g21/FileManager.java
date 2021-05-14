package sdist1g21;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

public class FileManager {

    private final String fileID;
    private final File file;

    private final int replicationDegree;

    private boolean alreadyBackedUp;

    /**
     * FileManager Constructor
     * @param path Path of the File
     * @param replicationDegree Replication Degree of the File
     */
    public FileManager(String path, int replicationDegree){
        this.file = new File(path);
        this.replicationDegree = replicationDegree;
        this.fileID = id();
        this.alreadyBackedUp = false;
    }

    /**
     * Replication Degree Getter
     * @return Replication Degree
     */
    public int getReplicationDegree() {
        return replicationDegree;
    }

    /**
     * File ID Getter
     * @return File ID
     */
    public String getFileID(){
        return fileID;
    }

    /**
     * File Getter
     * @return File
     */
    public File getFile(){
        return file;
    }

    /**
     * Check if a file was already backed up
     * @return True if the file was already backed up. False otherwise
     */
    public boolean isAlreadyBackedUp() {
        return alreadyBackedUp;
    }

    /**
     * Change if the file was already backed up
     * @param alreadyBackedUp new value
     */
    public void setAlreadyBackedUp(boolean alreadyBackedUp) {
        this.alreadyBackedUp = alreadyBackedUp;
    }

    /**
     * Function used to get the File ID
     * @return File ID (after SHA256 encoding)
     *
     * Reutilized from Project 1
     */
    private synchronized String id(){
        String filename = this.file.getName();                      // file name
        String filedate = String.valueOf(this.file.lastModified()); // date modified
        String fileowner = this.file.getParent();                   // owner

        String originalString = filename + ":" + filedate + ":" + fileowner;
        return sha256(originalString); // sha-256 encryption
    }

    /**
     * SHA256 Encoding Function
     * @param originalString Orignal String before encoding
     * @return String after Encoding
     *
     * Reutilized from Project 1
     */
    private synchronized static String sha256(String originalString){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(originalString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            // convert a byte array to a string of hex digits
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0'); // 1 digit hexadecimal
                hexString.append(hex);
            }
            return hexString.toString();

        }catch(Exception e){
            System.err.println("Error in SHA-256 Encryptation.\n");
            throw new RuntimeException(e);
        }
    }

    /**
     * Function Used to Create a File
     * @param path Path of the File that is gonna be Created
     * @param pID ID of the Peer that is creating the File
     */
    public synchronized void createFile(Path path, int pID) throws IOException {
        try {
            byte[] content = Files.readAllBytes(path);

            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            fileChannel.write(ByteBuffer.wrap(content), 0);
            fileChannel.close();
            System.out.println("> Peer " + pID + ": File at " + path + " was created successfully");
        } catch (IOException e) {
            System.err.println("> Peer " + pID + ": File at " + path + " was not created successfully");
            e.printStackTrace();
        }
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
