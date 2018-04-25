package com.feizi.distribute.lock.redis.ioc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * redis实现分布式锁
 * Created by feizi on 2018/4/24.
 */
@Component
public class DistributedRedisLockIoc {
    private final static Logger LOGGER = LoggerFactory.getLogger(DistributedRedisLockIoc.class);

    private final static String LOCK_SUCCESS = "OK";
    private final static Long RELEASE_SUCCESS = 1L;
    private final static String SET_IF_NOT_EXIST = "NX";
    private final static String SET_WITH_EXPIRE_TIME = "PX";

    @Resource
    private JedisPool jedisPool;

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁标识key
     * @param requestId 请求标识
     * @param expireTime 超时时间
     * @return 是否获取成功
     */
    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime){
        //获取jedis客户端连接
        Jedis conn = jedisPool.getResource();
        String result = conn.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
//        LOGGER.info("===加锁结果result：" + result);

        if(LOCK_SUCCESS.equals(result)){
            return true;
        }
        return false;
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁标识key
     * @param requestId 请求标识
     * @return
     */
    public boolean releaseDistributedLock(String lockKey, String requestId){
        //获取jedis客户端连接
        Jedis conn = jedisPool.getResource();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = conn.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
//        LOGGER.info("===解锁结果result：" + result);
        if(RELEASE_SUCCESS.equals(result)){
            return true;
        }
        return false;
    }
}
