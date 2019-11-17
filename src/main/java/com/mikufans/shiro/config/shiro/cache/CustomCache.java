package com.mikufans.shiro.config.shiro.cache;

import com.mikufans.shiro.model.common.Constant;
import com.mikufans.shiro.util.JwtUtil;
import com.mikufans.shiro.util.RedisUtil;
import com.mikufans.shiro.util.common.PropertiesUtil;
import com.mikufans.shiro.util.common.SerializableUtil;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;

import java.util.*;

public class CustomCache<K, V> implements Cache<K, V>
{

    /**
     * 缓存的key名称获取为shiro:cache:account
     *
     * @param key
     * @return java.lang.String
     * @author dolyw.com
     * @date 2018/9/4 18:33
     */
    private String getKey(Object key)
    {
        return Constant.PREFIX_SHIRO_CACHE + JwtUtil.getClaim(key.toString(), Constant.ACCOUNT);
    }

    /**
     * 获取缓存
     */
    @Override
    public Object get(Object key) throws CacheException
    {
        if (!RedisUtil.exists(this.getKey(key)))
        {
            return null;
        }
        return RedisUtil.getObject(this.getKey(key));
    }

    /**
     * 保存缓存
     */
    @Override
    public Object put(Object key, Object value) throws CacheException
    {
        // 读取配置文件，获取Redis的Shiro缓存过期时间
        PropertiesUtil.readProperties("config.properties");
        String shiroCacheExpireTime = PropertiesUtil.getProperty("shiroCacheExpireTime");
        // 设置Redis的Shiro缓存
        return RedisUtil.setObject(this.getKey(key), value, Integer.parseInt(shiroCacheExpireTime));
    }

    /**
     * 移除缓存
     */
    @Override
    public Object remove(Object key) throws CacheException
    {
        if (!RedisUtil.exists(this.getKey(key)))
        {
            return null;
        }
        RedisUtil.delKey(this.getKey(key));
        return null;
    }

    /**
     * 清空所有缓存
     */
    @Override
    public void clear() throws CacheException
    {
        Objects.requireNonNull(RedisUtil.getJedis()).flushDB();
    }

    /**
     * 缓存的个数
     */
    @Override
    public int size()
    {
        Long size = Objects.requireNonNull(RedisUtil.getJedis()).dbSize();
        return size.intValue();
    }

    /**
     * 获取所有的key
     */
    @Override
    public Set keys()
    {
        Set<byte[]> keys = Objects.requireNonNull(RedisUtil.getJedis()).keys("*".getBytes());
        Set<Object> set = new HashSet<Object>();
        for (byte[] bs : keys)
        {
            set.add(SerializableUtil.unserializable(bs));
        }
        return set;
    }

    /**
     * 获取所有的value
     */
    @Override
    public Collection values()
    {
        Set keys = this.keys();
        List<Object> values = new ArrayList<Object>();
        for (Object key : keys)
        {
            values.add(RedisUtil.getObject(this.getKey(key)));
        }
        return values;
    }
}