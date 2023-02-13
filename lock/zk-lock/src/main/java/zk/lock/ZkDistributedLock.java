package zk.lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZkDistributedLock {
    private static final String ROOT_PATH = "/distributed";
    private String path;
    private ZooKeeper zooKeeper;
    public ZkDistributedLock(ZooKeeper zooKeeper, String lockName){
        this.zooKeeper = zooKeeper;
        this.path = ROOT_PATH + "/" + lockName;
    }
    public void lock(){
        try {
            zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            // 重试
            try {
                Thread.sleep(200);
                lock();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    public void unlock(){
        try {
            this.zooKeeper.delete(path, 0);
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
