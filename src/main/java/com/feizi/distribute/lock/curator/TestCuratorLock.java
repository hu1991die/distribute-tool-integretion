package com.feizi.distribute.lock.curator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by feizi on 2018/4/19.
 */
public class TestCuratorLock {
    public static void main(String[] args) {
        TestCuratorLock testCuratorLock = new TestCuratorLock();
        //业务服务
        CuratorService curatorService = new CuratorService();

        //test1
//        testCuratorLock.test1(curatorService);

        //test2
//        testCuratorLock.test2(curatorService);

        //test3
//        testCuratorLock.test3(curatorService);

        //test4
        testCuratorLock.test4(curatorService);
    }

    /**
     * 第一种方式
     * @param curatorService
     */
    public void test1(final CuratorService curatorService){
        //创建分布式锁
        final DistributedCuratorLock lock = new DistributedCuratorLock();

        //创建计数器
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();
        //通过线程池启动线程
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownLatch.await();
                        //加锁
                        if(lock.lock()){
                            //打印流水号
                            curatorService.createSerialNumber();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        //解锁
                        lock.unLock();
                    }
                }
            });
        }
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        System.out.println("创建线程花费时间:" + (endTime - startTime) + "ms");
        countDownLatch.countDown();
    }

    /**
     * 第二种方式（需要多次初始化，显然不太合理）
     * @param curatorService
     */
    public void test2(final CuratorService curatorService){
        //线程池启动
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    curatorService.seckillInner();
                }
            });
        }
        executorService.shutdown();
    }

    /**
     * 第三种方式
     * @param curatorService
     */
    public void test3(final CuratorService curatorService){
        //创建分布式锁
        final DistributedCuratorLock lock = new DistributedCuratorLock();

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        //加锁
                        if(lock.lock()){
                            curatorService.seckill();
                        }
                    } finally {
                        //解锁
                        lock.unLock();
                    }
                }
            });
        }
        executorService.shutdown();
    }

    /**
     * 第四种方式（引入计数器）
     * @param curatorService
     */
    public void test4(final CuratorService curatorService){
        //创建分布式锁
        final DistributedCuratorLock lock = new DistributedCuratorLock();

        //创建计数器
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownLatch.await();
                        //加锁
                        if(lock.lock()){
                            curatorService.seckill();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        //解锁
                        lock.unLock();
                    }
                }
            });
        }
        countDownLatch.countDown();
        executorService.shutdown();
    }
}
