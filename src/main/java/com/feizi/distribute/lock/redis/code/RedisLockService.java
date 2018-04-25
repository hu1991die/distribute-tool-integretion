package com.feizi.distribute.lock.redis.code;

import com.feizi.distribute.lock.redis.code.DistributedRedisLock;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by feizi on 2018/4/18.
 */
public class RedisLockService {
    //锁key值
    private final static String LOCK_KEY = "touna_resources";

    private static JedisPool pool = null;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        //设置最大连接数
        config.setMaxTotal(200);
        //设置最大空闲数
        config.setMaxIdle(8);
        //设置最大等待时间
        config.setMaxWaitMillis(60 * 1000);
        //在borrow一个jedis连接实例时，是否需要验证，若为true，则所有jedis连接实例均是可用的
        config.setTestOnBorrow(true);

        pool = new JedisPool(config, "127.0.0.1", 6379, 3000);
    }

    //redis分布式锁
    private DistributedRedisLock lock = new DistributedRedisLock(pool);

    //模拟500件商品库存
    private int n = 500;

    /**
     * 模拟秒杀
     */
    public void secKill(){
        /*模拟不加锁操作*/
        /*System.out.println(Thread.currentThread().getName());
        System.out.println(--n);*/


        /*模拟加锁操作*/
        String identifier = null;
        try {
            //返回锁的value值作为锁标识，供释放锁的时候进行判断
            identifier = lock.lockExpires(LOCK_KEY, 5000, 1000);
            if(null == identifier || identifier.trim().length() == 0){
                System.out.println(Thread.currentThread().getName() + "获取锁资源失败...");
                return;
            }

            System.out.println(Thread.currentThread().getName() + "获得了锁资源..., 锁的标识identifier： " + identifier);
            System.out.println(--n);
        } finally {
            lock.releaseLock(LOCK_KEY, identifier);
        }
    }
}
