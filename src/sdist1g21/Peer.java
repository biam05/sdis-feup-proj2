package sdist1g21;

public class Peer implements ServiceInterface {

    /*
    TestApp args: 127.0.0.1 5001 REGISTER ipteste dnsteste
    Peer 1 args: 5001 127.0.0.1 8000
    Peer 2 args: 5001 230.0.0.1 5002 127.0.0.1 8000
     */

    private static SSLChannel sslChannel;

    private static Peer instance;
    private String ownAddress, friendAddress;
    private int ownPort, friendPort, SSLPort;

    private Chord chord;

    public static void main(String[] args) {
        // check usage
        if (args.length != 3 && args.length != 5) {
            System.out.println("Usage: java Peer SSLPort NodeAddress NodePort [FriendNodeAddress FriendNodePort]");
            return;
        }

        Peer.instance = new Peer(args);
    }

    public Peer(String[] args){
        try {
            SSLPort = Integer.parseInt(args[0]);
            ownAddress = args[1];
            ownPort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println( "Peer: One of the ports given is not a number");
            e.printStackTrace();
            return;
        }

        if(args.length == 5) {
            try {
                friendAddress = args[3];
                friendPort = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                System.err.println("Peer: friendPort given is not a number");
                e.printStackTrace();
                return;
            }
        } else {
            friendAddress = ownAddress;
            friendPort = ownPort;
        }

        sslChannel = new SSLChannel(SSLPort);
        sslChannel.start();

        chord = new Chord(instance, ownAddress, ownPort, friendAddress, friendPort);
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
