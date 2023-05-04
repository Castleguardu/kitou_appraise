package com.ktar.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ktar.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.ktar.utils.RedisConstants.*;
import static com.ktar.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * Description:这是redis缓存封装类
 * Author: KIKI
 * Date: 2023-05-02
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public <T,ID> T queryWithPassThrough(ID id, String pre, Class<T> type, Function<ID,T> dbFallback,
                                         Long time,TimeUnit unit) {   //缓存穿透
        String key = pre + id;
        String Json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(Json)) {
            return JSONUtil.toBean(Json,type);
        }
        if (Json !=null) {
            return null;
        }
        T t = dbFallback.apply(id);
        if (t==null) {
//            将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(t),time, unit);
        return t;
    }

    public void set(String key,Object value,Long time,TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),time, unit);
    }

    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));//默认时间单位为秒

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData)) ;
    }


    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R queryWithLogicalExpire(String pre,ID id,Class<R> type,String lockPre,
                                           Function<ID,R> dbFallback,Long time,TimeUnit unit) {
        System.out.println("进来了吗？");
        String key = pre + id;
        String Json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(Json)) {
            return null;
        }
//        命中的话，需要先把json对象反序列化为对象
        RedisData redisData = JSONUtil.toBean(Json,RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
//        判断有没有过期
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }

        String lockKey = lockPre +id;
        boolean isLock = tryLock(lockKey);
//        判断是否成功
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(()->{
                        try{
                            R newR = dbFallback.apply(id);
                            this.setWithLogicalExpire(key,newR,time,unit);
                        }catch (Exception e) {
                            throw new RuntimeException(e);
                        }finally {
                            unlock(lockKey);
                        }

                    }
            );
        }
        return r;  //返回过期信息

    }

    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.判断是否获取成功
            if (!isLock) {
                // 4.3.获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.获取锁成功，根据id查询数据库
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6.存在，写入redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.释放锁
            unlock(lockKey);
        }
        // 8.返回
        return r;
    }




    //    尝试获取锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    //    释放锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }



}
