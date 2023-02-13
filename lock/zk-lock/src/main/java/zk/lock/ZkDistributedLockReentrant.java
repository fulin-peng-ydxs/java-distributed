package zk.lock;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZkDistributedLockReentrant {

    private static final String ROOT_PATH = "/distributed";
    private static final ThreadLocal<Integer> THREAD_LOCAL = new ThreadLocal<>();

    private String path;

    private ZooKeeper zooKeeper;

    public ZkDistributedLockReentrant(ZooKeeper zooKeeper, String lockName){
        try {
            this.zooKeeper = zooKeeper;
            if (THREAD_LOCAL.get() == null || THREAD_LOCAL.get() == 0){
                this.path = zooKeeper.create(ROOT_PATH + "/" + lockName + "-", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void lock(){
        Integer flag = THREAD_LOCAL.get();
        if (flag != null && flag > 0) {
            THREAD_LOCAL.set(flag + 1);
            return;
        }
        try {
            String preNode = getPreNode(path);
            // 如果该节点没有前一个节点，说明该节点时最小节点，放行执行业务逻辑
            if (StringUtils.isEmpty(preNode)){
                THREAD_LOCAL.set(1);
                return ;
            } else {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                if (this.zooKeeper.exists(ROOT_PATH + "/" + preNode, new Watcher(){
                    @Override
                    public void process(WatchedEvent event) {
                        countDownLatch.countDown();
                    }
                }) == null) {
                    THREAD_LOCAL.set(1);
                    return;
                }
                // 阻塞。。。。
                countDownLatch.await();
                THREAD_LOCAL.set(1);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unlock(){
        try {
            THREAD_LOCAL.set(THREAD_LOCAL.get() - 1);
            if (THREAD_LOCAL.get() == 0) {
                this.zooKeeper.delete(path, 0);
                THREAD_LOCAL.remove();
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    private String getPreNode(String path){

        try {
            // 获取当前节点的序列化号
            Long curSerial = Long.valueOf(StringUtils.substringAfterLast(path, "-"));
            // 获取根路径下的所有序列化子节点
            List<String> nodes = this.zooKeeper.getChildren(ROOT_PATH, false);

            // 判空
            if (CollectionUtils.isEmpty(nodes)){
                return null;
            }

            // 获取前一个节点
            Long flag = 0L;
            String preNode = null;
            for (String node : nodes) {
                // 获取每个节点的序列化号
                Long serial = Long.valueOf(StringUtils.substringAfterLast(node, "-"));
                if (serial < curSerial && serial > flag){
                    flag = serial;
                    preNode = node;
                }
            }

            return preNode;
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
