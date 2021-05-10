package sdis.t1g06;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class Node {
    private long id; // key
    private Node sucessor;
    private Node predecessor;
    private InetSocketAddress address;
    private HashMap<Integer, Node> fingerTable;

    /**
     * ask node to find id's successor
     * @param id key of the node whose successor will be searched
     * @return successor
     */
    public Node find_sucessor(long id) {
        Node predecessor = find_predecessor(id);
        if(predecessor == null) return this;
        else return predecessor.sucessor;
    }

    /**
     * ask node to find id's predecessor
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
     * @param id key of the node whose predecessor will be searched
     * @return predecessor
     */
    public Node find_predecessor(long id) {
        Node node = this;
        while(id != node.id || id != node.sucessor.id) {
            node = closest_preceding_finger(id);
        }
        return node;
    }

    /**
     * return closest finger preceding id
     * @param id key of the node whose closet finger will be searched
     * @return closest finger preceding id
     */
    public Node closest_preceding_finger(long id) {
        for(int i = Settings.CHORD_MBITS; i >= 1; i--) {
            if(fingerTable.get(i).id == id)
                return fingerTable.get(i);
        }
        return this;
    }

    /**
     * node joins the network;
     * @param node arbitrary node in the network
     */
    public void join(Node node) {
        if(node != null){ // Join table through my friend "node"
            init_finger_table(node);
            update_others();
        } else { // I'm the first one
            for(int i = 1; i <= Settings.CHORD_MBITS; i++) {
                fingerTable.put(i, this);
            }
            predecessor = this;
        }
    }

    /**
     * Initialize finger table of local node
     * @param node arbitrary node already in the network
     */
    public void init_finger_table(Node node) {
        fingerTable.put(1, node.find_sucessor(fingerTable.get(1).id));
        predecessor = sucessor.predecessor;
        sucessor.predecessor = this;
        for(int i = 1; i < Settings.CHORD_MBITS; i++) {
            if(fingerTable.get(i+1).id >= node.id || fingerTable.get(i+1).id <= fingerTable.get(i).id)
                fingerTable.put(i+1, fingerTable.get(i));
            else
                fingerTable.put(i+1, node.find_sucessor(fingerTable.get(i+1).id));
        }
    }

    /**
     * Update all nodes whose finger tables should refer to myself
     */
    public void update_others() {
        for(int i = 1; i <= Settings.CHORD_MBITS; i++) {
            Node p = find_predecessor((long) (this.id - (Math.pow(2, i-1))));
            p.update_finger_table(this, i);
        }
    }

    /**
     * If node is i'th finger of myself, update my finger table with node
     * @param node that will be used to update
     * @param i position of the finger in relation to myself
     */
    private void update_finger_table(Node node, int i) {
        if(node.id >= this.id || node.id <= fingerTable.get(i).id) {
            fingerTable.put(i, node);
            Node p = predecessor;
            p.update_finger_table(node, i);
        }
    }
}
