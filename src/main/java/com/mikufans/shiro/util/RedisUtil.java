package com.mikufans.shiro.util;

import com.mikufans.shiro.exception.CustomExcetption;
import com.mikufans.shiro.model.common.Constant;
import com.mikufans.shiro.util.common.SerializableUtil;
import com.mikufans.shiro.util.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.swing.undo.CannotUndoException;
import java.io.Serializable;
import java.util.Set;

@Component
@Slf4j
public class RedisUtil
{
    private static JedisPool jedisPool;

    @Autowired
    public void setJedisPool(JedisPool jedisPool)
    {
        RedisUtil.jedisPool = jedisPool;
    }

    public static synchronized Jedis getJedis()
    {
        if (jedisPool != null)
            return jedisPool.getResource();

        else return null;
    }

    public static void closeJedis()
    {
        jedisPool.close();
    }

    public static Object getObject(String key)
    {
        Jedis jedis = jedisPool.getResource();
        byte[] bytes = jedis.get(key.getBytes());

        if (StringUtil.isNotNull(bytes))
            return SerializableUtil.unserializable(bytes);

        log.error("获取redis异常：key={}", key);
        return null;
    }

    public static String setObject(String key, Object value)
    {
        Jedis jedis = jedisPool.getResource();

        return jedis.set(key.getBytes(), SerializableUtil.serializable(value));
    }

    public static String setObject(String key, Object value, int expiretime)
    {
        String result;
        Jedis jedis = jedisPool.getResource();

        result = jedis.set(key.getBytes(), SerializableUtil.serializable(value));
        if (Constant.OK.equals(result))
            jedis.expire(key, expiretime);

        return result;
    }

    public static String getJson(String key)
    {
        Jedis jedis = jedisPool.getResource();
        return jedis.get(key);
    }

    public static String setJson(String key, String value)
    {
        Jedis jedis = jedisPool.getResource();
        return jedis.set(key, value);
    }

    public static String setJson(String key, String value, int expiretime)
    {
        String result;
        Jedis jedis = jedisPool.getResource();
        result = jedis.set(key, value);
        if (result.equals(Constant.OK))
            jedis.expire(key, expiretime);

        return result;
    }

    public static Long delKey(String key)
    {
        Jedis jedis = jedisPool.getResource();
        return jedis.del(key);
    }

    public static Boolean exists(String key)
    {
        Jedis jedis = jedisPool.getResource();
        return jedis.exists(key.getBytes());

    }

    /**
     * 模糊查询  jedis.keys()
     *
     * @param key
     * @return
     */
    public static Set<String> keysS(String key)
    {
        try (Jedis jedis = jedisPool.getResource())
        {
            return jedis.keys(key);
        } catch (Exception e)
        {
            throw new CustomExcetption("模糊查询Redis的键集合keysS方法异常:key=" + key + " cause=" + e.getMessage());
        }
    }

    public static Set<byte[]> keysB(String key)
    {
        try (Jedis jedis = jedisPool.getResource())
        {
            return jedis.keys(key.getBytes());
        } catch (Exception e)
        {
            throw new CustomExcetption("模糊查询Redis的键集合keysB方法异常:key=" + key + " cause=" + e.getMessage());
        }
    }

    public static Long ttl(String key)
    {
        Long result = -2L;
        try (Jedis jedis = jedisPool.getResource())
        {
            result = jedis.ttl(key);
            return result;
        } catch (Exception e)
        {
            throw new CustomExcetption("获取Redis键过期剩余时间ttl方法异常:key=" + key + " cause=" + e.getMessage());
        }
    }

}
