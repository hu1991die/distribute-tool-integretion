package com.feizi.distribute.lock.redis.ioc;


import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * Created by feizi on 2018/4/18.
 */
@Service
public class RedisLockServiceIoc {

    //锁key值
    private final static String LOCK_KEY = "touna_resources";

    //redis分布式锁
    @Resource
    private DistributedRedisLockIoc lock;

    //模拟500件商品库存
    private int n = 500;

    /**
     * 模拟秒杀
     */
    public void secKill(){
        //模拟不加锁操作
        /*System.out.println(Thread.currentThread().getName());
        System.out.println(--n);*/

        //模拟加锁操作
        String requestId = null;
        try {
            requestId = UUID.randomUUID().toString();

            //加锁
            boolean flag = lock.tryGetDistributedLock(LOCK_KEY, requestId, 1000);
            if(flag){
                System.out.println(Thread.currentThread().getName() + "根据requestId: " + requestId + "加锁成功...");
                System.out.println(--n);
            }else {
                System.out.println(Thread.currentThread().getName() + "根据requestId: " + requestId + "加锁失败...");
            }
        } finally {
            //解锁
            boolean flag = lock.releaseDistributedLock(LOCK_KEY, requestId);
            if(flag){
                System.out.println(Thread.currentThread().getName() + "根据requestId: " + requestId + "解锁成功...");
            }else {
                System.out.println(Thread.currentThread().getName() + "根据requestId: " + requestId + "解锁失败...");
            }
        }
    }
}
