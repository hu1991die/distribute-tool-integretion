package com.feizi.distribute.lock.zookeeper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by feizi on 2018/4/19.
 */
public class TestZookeeperLock {

    public static void main(String[] args) {
        TestZookeeperLock testZookeeperLock = new TestZookeeperLock();
        //服务
        ZookeeperService zookeeperService = new ZookeeperService();

        //test1
//        testZookeeperLock.test1(zookeeperService);

        //test2
        testZookeeperLock.test2(zookeeperService);
    }

    /**
     * 第一种方式
     * @param zookeeperService
     */
    public void test1(final ZookeeperService zookeeperService){
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    zookeeperService.seckill();
                }
            });
            t.start();
        }
    }

    /**
     * 第二种方式
     * @param zookeeperService
     */
    public void test2(final ZookeeperService zookeeperService){
        //线程池启动
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    zookeeperService.seckill();
                }
            });
        }
        executorService.shutdown();
    }
}
