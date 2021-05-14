package sdist1g21;

import java.io.Serializable;
import java.util.ArrayList;

public class PeerContainer implements Serializable {
    private Peer peer;
    private int storage_space;
    private ArrayList<FileManager> files;

    public PeerContainer(Peer peer) {
        this.peer = peer;
        storage_space = Settings.MAX_STORAGE_SPACE;
        files = new ArrayList<>();
    }
}
