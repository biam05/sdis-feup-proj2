package sdist1g21;

public class Chord {

    protected Peer peer;

    public Chord(Peer peer, String ownAddress, int ownPort) {
        this.peer = peer;
        Node node = new Node(ownAddress, ownPort);
    }

    public Chord(Peer peer, String ownAddress, int ownPort, String friendAddress, String friendPortStr) {
        this.peer = peer;
        int friendPort;
        try {
            friendPort = Integer.parseInt(friendPortStr);
        } catch (NumberFormatException e) {
            System.err.println( "Peer: NodePort given is not a number");
            e.printStackTrace();
            return;
        }
        Node node = new Node(ownAddress, ownPort, friendAddress, friendPort);
    }
}
