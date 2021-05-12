package sdis.t1g06;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Chord {

    protected Peer peer;

    public Chord(Peer peer, InetAddress ownAddress, int ownPort, String friendAddressStr, String friendPortStr) {
        this.peer = peer;
        InetAddress friendAddress;
        int friendPort;
        try {
            friendAddress = InetAddress.getByName(friendAddressStr);
            friendPort = Integer.parseInt(friendPortStr);
        } catch (UnknownHostException e) {
            System.err.println( "Peer: UnknownHostException occurred for the given NodeAddress");
            e.printStackTrace();
            return;
        } catch (NumberFormatException e) {
            System.err.println( "Peer: NodePort given is not a number");
            e.printStackTrace();
            return;
        }
        Node node = new Node(ownAddress, ownPort, friendAddress, friendPort);
    }
}
