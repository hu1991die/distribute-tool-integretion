package com.feizi.distribute.lock.redis.code;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by feizi on 2018/4/18.
 */
public class TestRedisLock {
    public static void main(String[] args) {
        final RedisLockService service = new RedisLockService();

        //线程池启动
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 500; i++){
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    service.secKill();
                }
            });
        }
        executorService.shutdown();
    }
}
