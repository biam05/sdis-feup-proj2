package sdist1g21;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Utils {
    public static final int MAX_THREADS = 20;
    public static final int MAX_BYTE_MSG = 20000;
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
}
