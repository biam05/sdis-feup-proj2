package sdis.t1g06;

public interface ServiceInterface {
    String backup(String file_name, int replicationDegree);
    String restore(String file_name);
    String delete(String file_name);
    String reclaim(long max_disk_space);
    String state();
}
