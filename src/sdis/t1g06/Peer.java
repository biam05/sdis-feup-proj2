package sdis.t1g06;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peer implements ServiceInterface {

    private static SSLChannel sslChannel;

    private static Peer instance;
    private InetAddress address;
    private int port;

    private Chord chord;

    public static void main(String[] args) {
        // check usage
        if (args.length < 2) {
            System.out.println( "Usage: java Peer NodeAddress NodePort [FriendNodeAddress FriendNodePort]");
            return;
        }

        Peer.instance = new Peer(args);
    }

    public Peer(String[] args){
        try {
            address = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);
        } catch (UnknownHostException e) {
            System.err.println( "Peer: UnknownHostException occurred for the given NodeAddress");
            e.printStackTrace();
            return;
        } catch (NumberFormatException e) {
            System.err.println( "Peer: NodePort given is not a number");
            e.printStackTrace();
            return;
        }

        sslChannel = new SSLChannel(port);
        sslChannel.start();

        //if(args.length > 2) chord = new Chord(instance, address, port, args[2], args[3]);
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
