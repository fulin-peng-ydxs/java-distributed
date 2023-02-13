package zk.lock;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;



/** 使用阻塞监听的方式，有利于减少高并发下请求自旋的cpu资源消耗
 * @author PengFuLin
 * @date 22:29 2023/2/12
 **/
public class ZkDistributedLockForWait {

    private static final String ROOT_PATH = "/distributed";

    private String path;

    private ZooKeeper zooKeeper;

    public ZkDistributedLockForWait(ZooKeeper zooKeeper, String lockName){
        try {
            this.zooKeeper = zooKeeper;
            //创建序列化节点，所有的节点创建请求将会按顺序创建
            this.path = zooKeeper.create(ROOT_PATH + "/" + lockName + "-", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** 每个客户端应该对刚好在它之前的子节点设置事件监听，
     * 例如子节点列表为/locks/lock-0000000000、/locks/lock-0000000001、/locks/lock-0000000002，
     * 序号为1的客户端监听序号为0的子节点删除消息，序号为2的监听序号为1的子节点删除消息。**/
    public void lock(){
        try {
            //获取该节点的前一个节点
            String preNode = getPreNode(path);
            // 如果该节点没有前一个节点，说明该节点时最小节点，放行执行业务逻辑
            if (StringUtils.isEmpty(preNode)){
                return ;
            } else { //如果不为第一个，则对前面的那个节点删除状态进行监听，删除时就会被触发监听从而获取锁
                CountDownLatch countDownLatch = new CountDownLatch(1);
                if (this.zooKeeper.exists(ROOT_PATH + "/" + preNode, new Watcher(){
                    @Override
                    public void process(WatchedEvent event) {
                        countDownLatch.countDown();
                    }
                }) == null) {  //刚好获取到锁：前面节点刚好被释放
                    return;
                }
                // 阻塞前面的节点被释放。。。。
                countDownLatch.await();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void unlock(){
        try {
            //删除当前的节点
            this.zooKeeper.delete(path, 0);
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
