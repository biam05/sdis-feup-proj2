package sdist1g21;

public class Chord {

    private Peer peer;
    private Node node;

    public Chord(Peer peer, String ownAddress, int ownPort, String friendAddress, int friendPort) {
        this.peer = peer;
        this.node = new Node(ownAddress, ownPort, friendAddress, friendPort);
    }
}
