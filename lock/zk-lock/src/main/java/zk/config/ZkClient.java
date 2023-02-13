package zk.config;

import org.apache.zookeeper.*;
import org.springframework.stereotype.Component;
import zk.lock.ZkDistributedLock;
import zk.lock.ZkDistributedLockForWait;
import zk.lock.ZkDistributedLockReentrant;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class ZkClient {
    private static final String connectString = "172.16.116.100:2181";
    private static final String ROOT_PATH = "/distributed";
    private ZooKeeper zooKeeper;
    @PostConstruct
    public void init(){
        try {
            // 连接zookeeper服务器
            this.zooKeeper = new ZooKeeper(connectString, 30000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    System.out.println("获取链接成功！！");
                }
            });
            // 创建分布式锁根节点
            if (this.zooKeeper.exists(ROOT_PATH, false) == null)
                this.zooKeeper.create(ROOT_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            System.out.println("获取链接失败！");
            e.printStackTrace();
        }
    }
    @PreDestroy
    public void destroy(){
        try {
            if (zooKeeper != null)
                zooKeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public ZkDistributedLock getZkDistributedLock(String lockName){
        return new ZkDistributedLock(zooKeeper, lockName);
    }


    public ZkDistributedLockForWait getZkDistributedLockForWait(String lockName){
        return new ZkDistributedLockForWait(zooKeeper, lockName);
    }

    public ZkDistributedLockReentrant getZkDistributedLockReentrant(String lockName){
        return new ZkDistributedLockReentrant(zooKeeper, lockName);
    }

}
