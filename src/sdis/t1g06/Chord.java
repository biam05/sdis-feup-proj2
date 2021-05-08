package sdis.t1g06;

import java.net.InetSocketAddress;

public class Chord {

    protected Peer peer;

    protected int m = Settings.CHORD_MBITS; // number of bits in the key/node indentifiers
    protected Pair<Integer, InetSocketAddress> predecessor;
    protected Pair<Integer, InetSocketAddress> successor;
    protected Pair<Integer, InetSocketAddress>[] fingerTable;
    protected Pair<Integer, InetSocketAddress>[] successors;
    protected InetSocketAddress address; //Peer address

    public Chord(int port){

    }

    /**
     * as node n to find id's successor
     * @param id
     * @return successor
     */
    public int find_successor(int id) {
        Pair<Integer, InetSocketAddress> n = find_predecessor(id);
        return n.getLeft();
    }

    /**
     * ask node n to find id's predecessor
     *
     *      when node n executes find_predecessor, it contacts a series of nodes moving forward around
     *      the Chord circle towards id. If node n contacts a node n' such that id falls between n' and
     *      the successor of n', find_predecessor is done an returns n'. Otherwise node n asks n' for the
     *      node n' knows about that most closely precedes id
     *
     *      n' = n
     *      while (id !E (n', n'.successor])
     *          n' = n'.closest_preceding_finger(id)
     *      return n'
     *
     * @param id
     * @return predecessor
     */
    public Pair<Integer, InetSocketAddress> find_predecessor(int id) {
        // TODO

        return predecessor;
    }

    /**
     * return closest finger preceding id
     * @param id
     * @return closest finger preceding id
     */
    public Pair<Integer, InetSocketAddress> closest_preceding_finger(int id) {
        for(int i = m - 1; i >= 0; i--) {
            if(fingerTable[i].getLeft() == id)
                return fingerTable[i];
        }
        return null;
    }

    public Pair<Integer, InetSocketAddress> getPredecessor() {
        return predecessor;
    }

    public Pair<Integer, InetSocketAddress> getSuccessor() {
        return successor;
    }

    /**
     * node joins the network;
     * @param n arbitrary node in the network
     */
    public void join(Peer n){

    }

    /**
     * Initialize finger table of local node
     * @param n arbitrary node already in the network
     */
    public void init_finger_table(Peer n){

    }

    /**
     * update all nodes whose finger tables should refer to n
     */
    public void update_others(){

    }
}
