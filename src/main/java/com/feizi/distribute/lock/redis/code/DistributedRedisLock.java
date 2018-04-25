package com.feizi.distribute.lock.redis.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * 使用redis实现分布式锁
 * Created by feizi on 2018/4/18.
 */
public class DistributedRedisLock {

    private final static Logger LOGGER = LoggerFactory.getLogger(DistributedRedisLock.class);

    //jedisPool，后续可以通过ioc方式注入
    private final JedisPool jedisPool;

    //锁key的前缀
    private final static String LOCK_KEY_PREFIX = "LOCK:";

    //加锁成功
    private final static int LOCK_SUCCESS = 1;

    //没有设置超时时间
    private final static int LOCK_NO_EXPIRE = -1;

    public DistributedRedisLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 加锁（可设置过期时间）
     * @param key 锁的key值
     * @param acquireTimeout 获取锁的超时时间
     * @param expires 锁的过期有效时间
     * @return 锁标识
     */
    public String lockExpires(String key, long acquireTimeout, long expires){
        Jedis conn = null;
        String retIdentifier = null;

        try {
            //获取连接
            conn = jedisPool.getResource();
            //随机生成一个value
            String identifier = UUID.randomUUID().toString();
            //锁名，即key值
            String lockKey = LOCK_KEY_PREFIX + key;
            //过期时间，上锁后如果超过过期时间则自动释放锁
            int lockExpire = (int) (expires / 1000);

            //获取锁的超时时间，超过这个时间则放弃获取锁
            long end = System.currentTimeMillis() + acquireTimeout;
            while (System.currentTimeMillis() < end){
                //当且仅当key不存在时，set一个key为val的字符串，返回1；若key存在，则什么都不做，返回0。
                if(conn.setnx(lockKey, identifier) == LOCK_SUCCESS){
                    LOGGER.info("=============SetNX加锁成功");

                    //为key设置一个超时时间，单位为second，超过这个时间锁会自动释放，避免死锁。
                    conn.expire(lockKey, lockExpire);

                    //返回value值，用于释放锁时进行确认
                    retIdentifier = identifier;
                    return retIdentifier;
                }

                //返回-1代表key没有设置超时时间，为key设置一个超时时间
                if(conn.ttl(lockKey) == LOCK_NO_EXPIRE){
                    LOGGER.info("=========返回-1代表key没有设置超时时间，为key设置一个超时时间...");
                    conn.expire(lockKey, lockExpire);
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    LOGGER.error("加锁出现异常，当前线程进行中断操作：" + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            LOGGER.error("加锁出现异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if(null != conn){
                //关闭连接，释放资源
                conn.close();
            }
        }
        return retIdentifier;
    }

    /**
     * 解锁
     * @param key 锁的key值
     * @param identifier 释放锁的标识
     * @return
     */
    public boolean releaseLock(String key, String identifier){
        if(null == key || null == identifier){
            return false;
        }

        Jedis conn = null;
        String lockKey = LOCK_KEY_PREFIX + key;
        boolean retFlag = false;

        try {
            //获取连接
            conn = jedisPool.getResource();
            while (true){
                //监视lock，准备开始事务
                conn.watch(lockKey);

                //通过前面返回的value值判断是不是该锁，若是该锁，则删除。释放锁
                if(identifier.equals(conn.get(lockKey))){
                    LOGGER.info("=========通过锁标识判断是否是该锁，如果是该锁则删除，释放锁资源...");
                    Transaction transaction = conn.multi();
                    transaction.del(lockKey);

                    List<Object> resultList = transaction.exec();
                    if(null == resultList || resultList.size() == 0){
                        continue;
                    }
                    retFlag = true;
                }

                //取消监视
                conn.unwatch();
                break;
            }
        } catch (Exception e) {
            LOGGER.error("解锁出现异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if(null != conn){
                //关闭连接，释放资源
                conn.close();
            }
        }
        LOGGER.info("=====释放锁是否成功retFlag: " + retFlag);
        return retFlag;
    }
}
