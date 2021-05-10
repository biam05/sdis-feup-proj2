package sdis.t1g06;

import java.net.InetSocketAddress;

public class Chord {

    protected Peer peer;

    protected int m = Settings.CHORD_MBITS; // number of bits in the key/node indentifiers
    protected Node predecessor;
    protected Node successor;
    protected Node[] fingerTable;

    public Chord() {
    }

    /**
     * if s is the ith finger of n, update n'ss finger table with s
     */
    public void update_finger_table(/*s, i*/){

    }
}
