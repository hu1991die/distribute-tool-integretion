package com.feizi.distribute.lock.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 使用zookeeper实现分布式锁
 * Created by feizi on 2018/4/18.
 */
public class DistributedZookeeperLock implements Lock, Watcher{
    private final static Logger LOGGER = LoggerFactory.getLogger(DistributedZookeeperLock.class);

    private ZooKeeper zk = null;
    //zk的根节点
    private final static String ROOT_LOCK = "/zkLocks";
    //zk节点分隔符
    private final static String NODE_SEPARATOR = "/";
    //zk节点分割串
    private final static String NODE_SPLIT_STR = "_LOCK_";
    //竞争的资源
    private String lockKey;
    //等待的前一个锁
    private String WAIT_LOCK;
    //当前锁
    private String CURRENT_LOCK;
    //计数器
    private CountDownLatch countDownLatch;
    //session有效期 30s
    private final static int sessionTimeout = 30000;
    //异常列表集合
    private List<Exception> exceptionList = new ArrayList<>();

    /**
     * 配置分布式锁，通过创建zookeeper临时有序节点的方式
     * @param url 连接的url地址
     * @param lockKey 竞争资源key值
     */
    public DistributedZookeeperLock(String url, String lockKey) {
        this.lockKey = lockKey;

        try {
            //获取zk连接
            zk = new ZooKeeper(url, sessionTimeout, this);
            Stat stat = zk.exists(ROOT_LOCK, false);
            if(null == stat){
                LOGGER.info("根节点不存在，开始创建根节点...");
                //如果根节点不存在，则创建根节点
                zk.create(ROOT_LOCK, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        //节点监视器
        if(this.countDownLatch != null){
            this.countDownLatch.countDown();
        }
    }

    @Override
    public void lock() {
        if(exceptionList.size() > 0){
            throw new LockException(exceptionList.get(0));
        }

        try {
            if(this.tryLock()){
                LOGGER.info(Thread.currentThread().getName() + " 已经获得了锁资源， 当前锁CURRENT_LOCK是: " + CURRENT_LOCK + ", lockKey是： " + lockKey);
                return;
            }else {
                //等待锁
                waitForLock(WAIT_LOCK, sessionTimeout);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lock();
    }

    @Override
    public boolean tryLock() {
        try {
            if(lockKey.contains(NODE_SPLIT_STR)){
                throw new LockException("锁名有误...");
            }

            //创建临时有序节点
            CURRENT_LOCK = zk.create(ROOT_LOCK + NODE_SEPARATOR + lockKey + NODE_SPLIT_STR, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

            //取所有子节点
            List<String> subNodeList = zk.getChildren(ROOT_LOCK, false);
            //取出所有lockKey的锁
            List<String> lockObjectList = new ArrayList<>();
            //迭代子节点列表集合
            for (String node : subNodeList){
                String _node = node.split(NODE_SPLIT_STR)[0];
                if(_node.equals(lockKey)){
                    lockObjectList.add(node);
                }
            }

            //排序子节点
            Collections.sort(lockObjectList);
            LOGGER.info(Thread.currentThread().getName() + "创建了锁CURRENT_LOCK：" + CURRENT_LOCK);

            //判断如果当前节点为最小节点，则表明获取锁成功
            if(CURRENT_LOCK.equals(ROOT_LOCK + NODE_SEPARATOR + lockObjectList.get(0))){
                return true;
            }

            //如果当前节点不是最小节点，则取当前节点的前（上）一个节点
            String currentNode = CURRENT_LOCK.substring(CURRENT_LOCK.lastIndexOf(NODE_SEPARATOR) + 1);
            LOGGER.info(Thread.currentThread().getName() + "所在的当前节点currentNode: " + currentNode);

            WAIT_LOCK = lockObjectList.get(Collections.binarySearch(lockObjectList, currentNode) - 1);
            LOGGER.info(Thread.currentThread().getName() + "等待的前一个锁WAIT_LOCK是： " + WAIT_LOCK);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            if(this.tryLock()){
                return true;
            }
            return waitForLock(WAIT_LOCK, time);
        } catch (KeeperException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void unlock() {
        try {
            LOGGER.info(Thread.currentThread().getName() + " 释放锁: " + CURRENT_LOCK);
            zk.delete(CURRENT_LOCK, -1);
            CURRENT_LOCK = null;
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    /**
     * 等待锁
     * @param prevNode 前一个节点
     * @param waitTime 等待时间
     * @return
     */
    private boolean waitForLock(String prevNode, long waitTime) throws KeeperException, InterruptedException {
        //锁资源
        String lockName = ROOT_LOCK + NODE_SEPARATOR + prevNode;
        Stat stat = zk.exists(lockName, true);
        if(null != stat){
            LOGGER.info(Thread.currentThread().getName() + " 等待锁: " + lockName);
            this.countDownLatch = new CountDownLatch(1);

            //计数等待，若等到前一个节点消失，则process中进行countDown,停止等待，获取锁
            this.countDownLatch.await(waitTime, TimeUnit.MILLISECONDS);
            this.countDownLatch = null;
            LOGGER.info(Thread.currentThread().getName() + " 等到了锁: " + lockName);
        }
        return true;
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    public class LockException extends RuntimeException{

        private static final long serialVersionUID = -514944413033206310L;

        public LockException(String message) {
            super(message);
        }

        public LockException(Throwable cause) {
            super(cause);
        }

        public LockException(Exception e) {
            super(e);
        }
    }
}
