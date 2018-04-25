package com.feizi.distribute.lock.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 使用Apache Curator实现分布式锁
 * Created by feizi on 2018/4/19.
 */
public class DistributedCuratorLock {
    private final static Logger LOGGER = LoggerFactory.getLogger(DistributedCuratorLock.class);

    /*根节点路径*/
    private final static String ROOT_NODE_PATH = "/lock_path";
    /*重试策略：重试间隔时间（默认间隔1秒）*/
    private final static int RETRY_INTERVAL = 1000;
    /*重试策略：重试次数（默认重试3次）*/
    private final static int RETRY_TIMES = 3;
    /*session会话超时时间，默认设置为5秒*/
    private final static int SESSION_TIMEOUT = 5000;
    /*连接超时时间，默认设置为3秒*/
    private final static int CONNECT_TIMEOUT = 3000;
    /* 加锁最大等待超时时间, 默认设置为10秒，如果超时则加锁失败 */
    private static final int LOCK_MAX_WAIT_TIMITOUT = 100000;
    /*zk连接地址，默认是本机*/
    private final static String ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    /*命名空间（可以修改）*/
    private final static String NAMESPACE = "distributedCuratorLock";
    /*curator客户端 */
    private CuratorFramework curatorClient;
    /*分布式锁 */
    private InterProcessMutex lock;

    public DistributedCuratorLock() {
        this(ZOOKEEPER_ADDRESS, NAMESPACE);
    }

    public DistributedCuratorLock(String zkAddress, String nameSpace) {
        init(zkAddress, nameSpace);
    }

    /**
     * 初始化
     * @param zkAddress zk地址
     * @param nameSpace 命名空间
     */
    private void init(String zkAddress, String nameSpace){
        if(null == zkAddress || zkAddress.trim().length() == 0){
            //如果没传，就取默认
            zkAddress = ZOOKEEPER_ADDRESS;
        }

        if(null == nameSpace || nameSpace.trim().length() == 0){
            nameSpace = NAMESPACE;
        }

        //获取客户端连接
        this.curatorClient = getClient(zkAddress, nameSpace);
        LOGGER.info("========Curator client build SUCCESS, the lock state is: {}", curatorClient == null ? "" : curatorClient.getState());
        if(null != curatorClient && curatorClient.getState() == CuratorFrameworkState.STARTED){
            //根据客户端连接和根节点路径创建分布式锁
            this.lock = new InterProcessMutex(this.curatorClient, ROOT_NODE_PATH);
        }
    }

    /**
     * 加锁
     * @param maxWaitTime 最大等待超时时间
     * @param waitUnit 最大等待超时时间单位
     * @return true-成功，false-失败
     */
    public boolean lock(long maxWaitTime, TimeUnit waitUnit){
        if(null == lock){
            return false;
        }

        try {
            //加锁
            return lock.acquire(maxWaitTime, waitUnit);
        } catch (Exception e) {
            //加锁失败
            LOGGER.error("加锁失败： " + e.getMessage());
        }
        return false;
    }

    /**
     * 加锁
     * @return
     */
    public boolean lock(){
        return lock(LOCK_MAX_WAIT_TIMITOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * 解锁
     * @return
     */
    public void unLock(){
        if(null == lock){
            return;
        }

        try {
            lock.release();
        } catch (Exception e) {
            //解锁失败
            LOGGER.error("解锁失败： " + e.getMessage());
        }
    }

    /**
     * 获取客户端连接
     * @param zkAddress zk连接地址，多个集群地址使用逗号分隔
     * @param nameSpace 命名空间
     * @return
     */
    private CuratorFramework getClient(String zkAddress, String nameSpace){
        //1、重试策略（默认: 最多重试3次，每次间隔1秒）
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(RETRY_INTERVAL, RETRY_TIMES);
        //2、通过工厂模式创建客户端连接
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .retryPolicy(retryPolicy)
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .connectionTimeoutMs(CONNECT_TIMEOUT)
                .namespace(nameSpace)
                .build();


        //3、开启连接
        client.start();
        return client;
    }
}
