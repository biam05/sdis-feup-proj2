package sdist1g21;

public class Peer implements ServiceInterface {

    /*
    Peer 1 args: 127.0.0.1 8000 5001
    Peer 2 args: 230.0.0.1 5002 127.0.0.1 8000
     */

    private static SSLChannel sslChannel;

    private static Peer instance;
    private String address;
    private int chordPort, SSLPort;

    private Chord chord;

    public static void main(String[] args) {
        // check usage
        if (args.length < 3) {
            System.out.println("Usage: java Peer NodeAddress NodePort [SSLPort] [FriendNodeAddress FriendNodePort]");
            return;
        }

        Peer.instance = new Peer(args);
    }

    public Peer(String[] args){
        if(args.length == 3) {
            try {
                address = args[0];
                chordPort = Integer.parseInt(args[1]);
                SSLPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println( "Peer: NodePort given is not a number");
                e.printStackTrace();
                return;
            }

            sslChannel = new SSLChannel(SSLPort);
            sslChannel.start();

            chord = new Chord(instance, address, chordPort);
        } else {
            try {
                address = args[0];
                chordPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println( "Peer: NodePort given is not a number");
                e.printStackTrace();
                return;
            }

            chord = new Chord(instance, address, chordPort, args[2], args[3]);
        }
    }

    public static void messageHandler(String message) {
        System.out.println("This peer got the message: " + message);
    }

    @Override
    public String backup(String file_name, int replicationDegree) {
        return null;
    }

    @Override
    public String restore(String file_name) {
        return null;
    }

    @Override
    public String delete(String file_name) {
        return null;
    }

    @Override
    public String reclaim(long max_disk_space) {
        return null;
    }

    @Override
    public String state() {
        return null;
    }
}
