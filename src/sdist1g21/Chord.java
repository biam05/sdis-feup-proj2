package sdist1g21;

public class Chord {

    private Peer peer;
    private Node node;

    public Chord(Peer peer, String ownAddress, int ownPort, int nodeID, String friendAddress, int friendPort) {
        this.peer = peer;
        this.node = new Node(ownAddress, ownPort, nodeID, friendAddress, friendPort);
    }
}
