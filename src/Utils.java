package sdist1g21;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Utils {
    public static final int MAX_THREADS = 20;
    public static final int MAX_BYTE_MSG = 10000;
    public static final int MAX_STORAGE_SPACE = 10000000;
    public static final int CHORD_MBITS = 128; // number of bits in the key/node indentifiers

    /**
     * SHA256 Encoding Function
     * @param originalString Orignal String before encoding
     * @return String after Encoding
     *
     * Reutilized from Project 1
     */
    public synchronized static String sha256(String originalString){
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

    public static int calculatePathSpaceSize(int nFile, String space, StringBuilder state, FileManager fileManager) {
        int spaceNum;
        state.append("::: PATH: ").append(fileManager.getFile().getPath());
        spaceNum = 68 - fileManager.getFile().getPath().length();
        state.append(space.repeat(spaceNum)).append(":::[n]");
        state.append("::: FILE_ID: ").append(fileManager.getFileID()).append(" :::[n]");
        nFile++;
        return nFile;
    }

    public static String peerState(int peerID, PeerContainer peerContainer) {
        int nFile = 1, spaceNum;
        String space = " ";
        StringBuilder state = new StringBuilder();
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                                   PEER ").append(peerID).append("                                  :::[n]");
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                                 OWN FILES                                 :::[n]");
        // each file whose backup has initiated
        for(FileManager fileManager : peerContainer.getStoredFiles()){
            if(fileManager.isAlreadyBackedUp()) {
                state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
                state.append("::: FILE ").append(nFile).append("                                                                    :::[n]");
                state.append(":::                                                                           :::[n]");
                state.append("::: IS BACKED UP: YES").append("                                                         :::[n]");
                state.append("::: PATH: ").append(fileManager.getFile().getPath());
                spaceNum = 68 - fileManager.getFile().getPath().length();
                state.append(space.repeat(spaceNum)).append(":::[n]");
                state.append("::: FILE_ID: ").append(fileManager.getFileID()).append(" :::[n]");
                state.append("::: DESIRED REPLICATION DEGREE: ").append(fileManager.getDesiredReplicationDegree()).append("                                             :::[n]");
                state.append("::: CURRENT REPLICATION DEGREE: ").append(fileManager.getActualReplicationDegree()).append("                                             :::[n]");
                nFile++;
            } else {
                state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
                state.append("::: FILE ").append(nFile).append("                                                                    :::[n]");
                state.append(":::                                                                           :::[n]");
                state.append("::: IS BACKED UP: NO").append("                                                          :::[n]");
                nFile = Utils.calculatePathSpaceSize(nFile, space, state, fileManager);
            }
        }
        if(nFile == 1){
            state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
            state.append("::: NO STORED FILES                                                           :::[n]");
        }
        nFile = 1;

        // backed up files from other peers
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                           OTHER BACKED UP FILES                           :::[n]");
        for(FileManager fileManager : peerContainer.getBackedUpFiles()){
            state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
            state.append("::: FILE ").append(nFile).append("                                                                    :::[n]");
            state.append(":::                                                                           :::[n]");
            nFile = Utils.calculatePathSpaceSize(nFile, space, state, fileManager);
        }
        if(nFile == 1){
            state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
            state.append("::: NO BACKED UP FILES FROM OTHER PEERS                                       :::[n]");
        }

        // peer storage capacity
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append(":::                           PEER STORAGE CAPACITY                           :::[n]");
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::[n]");
        state.append("::: TOTAL DISK SPACE: ").append(peerContainer.getMaxSpace()).append(" Bytes");
        spaceNum = 50 - String.valueOf(peerContainer.getMaxSpace()).length();
        state.append(space.repeat(spaceNum)).append(":::[n]");
        state.append("::: FREE SPACE: ").append(peerContainer.getFreeSpace()).append(" Bytes");
        spaceNum = 56 - String.valueOf(peerContainer.getFreeSpace()).length();
        state.append(space.repeat(spaceNum)).append(":::[n]");
        state.append(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");

        return state.toString();
    }
}
