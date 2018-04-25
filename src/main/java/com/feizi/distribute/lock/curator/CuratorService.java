package com.feizi.distribute.lock.curator;

/**
 * 业务逻辑
 * Created by feizi on 2018/4/19.
 */
public class CuratorService {
    //模拟秒杀500件商品
    private int n = 500;

    /**
     * 打印流水号
     */
    public void createSerialNumber(){
        System.out.println(System.currentTimeMillis());
    }

    /**
     * 秒杀（在具体业务执行逻辑里面加锁）
     */
    public void seckillInner(){
        DistributedCuratorLock lock = null;

        try {
            lock = new DistributedCuratorLock();
            //加锁
            if(lock.lock()){
                System.out.println(--n);
                System.out.println(Thread.currentThread().getName() + " 正在运行...");
            }
        } finally {
            //解锁
            lock.unLock();
        }
    }

    /**
     * 秒杀（在调用具体业务逻辑之前加锁）
     */
    public void seckill(){
        System.out.println(--n);
        System.out.println(Thread.currentThread().getName() + " 正在运行...");
    }
}
