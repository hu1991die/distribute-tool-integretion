package com.feizi.distribute.lock.zookeeper;

/**
 * Created by feizi on 2018/4/19.
 */
public class ZookeeperService {

    //模拟秒杀500件商品
    private int n = 500;

    /**
     * 模拟秒杀场景
     */
    public void seckill(){
        //zookeeper分布式锁
        DistributedZookeeperLock lock = null;
        try {
            lock = new DistributedZookeeperLock("127.0.0.1:2181", "testZookeeperLock");
            //加锁
            lock.lock();
            System.out.println(Thread.currentThread().getName() + " 正在运行...");
            System.out.println(--n);
        } finally {
            //解锁
            if(null != lock){
                lock.unlock();
            }
        }
    }
}
