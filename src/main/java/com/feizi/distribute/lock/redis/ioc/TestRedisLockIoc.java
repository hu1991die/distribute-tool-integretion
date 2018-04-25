package com.feizi.distribute.lock.redis.ioc;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by feizi on 2018/4/18.
 */
public class TestRedisLockIoc {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring/applicationContext-redis.xml");
        final RedisLockServiceIoc service = (RedisLockServiceIoc) applicationContext.getBean("redisLockServiceIoc");

        //线程池启动
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 500; i++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    service.secKill();
                }
            });
        }
        executorService.shutdown();
    }
}
