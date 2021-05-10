package sdis.t1g06;

public class Peer implements ServiceInterface {

    private static SSLChannel sslChannel;

    private static Peer instance;

    private Chord chord;

    public static void main(String[] args) {
        // check usage
        if (args.length < 3) {
            System.out.println( "Usage: java Peer <peer id> <SSL port> <Chord port> [<ConnectionPeer address> <ConnectionPeer port>]");
            return;
        }

        Peer.instance = new Peer(args);

        sslChannel = new SSLChannel(Integer.parseInt(args[2]));



    }

    public Peer(String[] args){

        int port = Integer.parseInt(args[1]);
        //this.chord = new Chord(Integer.parseInt(args[2]));
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
