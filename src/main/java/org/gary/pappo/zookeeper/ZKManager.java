package org.gary.pappo.zookeeper;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ZKManager implements Watcher {

    private static final int ZK_SESSION_TIMEOUT = 5000;
    static final String ZK_REGISTRY_PATH = "/nettyrpc";
    private static final Logger LOGGER = Logger.getLogger(ZKManager.class);
    private String zkAddress;
    private ZooKeeper zk;
    private final CountDownLatch connectedSignal = new CountDownLatch(1);
    private ConcurrentHashMap<String, List<CountDownLatch>> wakeupMap = new ConcurrentHashMap<>();

    public ZKManager(String zkAddress) {
        try {
            this.zkAddress = zkAddress;
            //必须添加Watcher
            ZooKeeper zooKeeper = new ZooKeeper(zkAddress, ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    LOGGER.debug(" receive event : " + event.getType().name());
                }
            });
            if (zooKeeper.exists(ZK_REGISTRY_PATH, false) == null)
                zooKeeper.create(ZK_REGISTRY_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.close();
        } catch (Exception e) {
            LOGGER.error("fail to create persistent node " + ZK_REGISTRY_PATH);
            e.printStackTrace();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == Event.KeeperState.SyncConnected) {
            //connect()操作需要等待连接真正建立后才能进行后序的操作
            if (event.getType() == Event.EventType.None && event.getPath() == null) {
                connectedSignal.countDown();
                //唤醒所有正在等待服务消费者线程
            } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
                try {
                    List<CountDownLatch> list = wakeupMap.get(event.getPath());
                    if (list != null) {
                        while (list.size() > 0) {
                            CountDownLatch countDownLatch = list.get(0);
                            countDownLatch.countDown();
                            list.remove(0);
                        }
                        wakeupMap.put(event.getPath(), list);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void connect() {
        try {
            zk = new ZooKeeper(zkAddress, ZK_SESSION_TIMEOUT, this);
            connectedSignal.await();
            LOGGER.info("successfully connected to " + zkAddress);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("fail to connect to " + zkAddress);
            e.printStackTrace();
        }
    }

    void createPersistentNode(String nodePath) {
        try {
            if (zk.exists(nodePath, false) == null)
                zk.create(nodePath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            ;
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("fail to create persistent node " + nodePath);
            e.printStackTrace();
        }
    }

    void createEphemeralNode(String nodePath) {
        try {
            if (zk.exists(nodePath, false) == null)
                zk.create(nodePath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("fail to create ephemeral node " + nodePath);
            e.printStackTrace();
        }
    }

    // 此方法要求nodePath是完整的,所以要以/origin开始
    List<String> listChildren(String nodePath, String exclude) {
        List<String> children = null;
        try {
            // 监听若是available则通知阻塞的进程继续
            children = zk.getChildren(nodePath, true);
            children.remove(exclude);
            while (children.size() == 0) {
                System.out.println(Thread.currentThread() + "wait for available server");
                CountDownLatch countDownLatch = new CountDownLatch(1);
                List<CountDownLatch> list = wakeupMap.get(nodePath);
                if (list == null)
                    list = new LinkedList<>();
                list.add(countDownLatch);
                wakeupMap.put(nodePath, list);
                countDownLatch.await();
                children = zk.getChildren(nodePath, true);
            }

        } catch (Exception e) {
            LOGGER.error("fail to list node " + nodePath);
            e.printStackTrace();
        }
        return children;
    }

    // 删除本节点和所有子孙节点
    private void deleteNode(String nodePath) {
        try {
            List<String> children = zk.getChildren(nodePath, true);
            if (children != null)
                for (String c : children)
                    deleteNode(nodePath + "/" + c);
            zk.delete(nodePath, -1);
        } catch (Exception e) {
            LOGGER.error("fail to delete node " + nodePath);
            e.printStackTrace();
        }
    }

    void closeConnect() {
        try {
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
