package com.chejh5.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

/**
 * Created by wupeng1 on 2017/3/28.
 */
public class RedisUtil {

    private static final Log log = LogFactory.getLog(RedisUtil.class);

    public static Jedis getJedisClient(String host, int port) {
        Jedis jedis = null;
        try {
            jedis = new Jedis(host, port, 300);
        } catch (Exception ex) {
            log.error("Get jedis instance failed:" + ex.getMessage());
        }
        return jedis;
    }

    public static void returnResource(Jedis jedis) {
        if (jedis != null) {
            jedis.disconnect();
        }
    }

    public static String get(String host, int port, int dbNum, String key) {
        Jedis jedis = getJedisClient(host, port);
        try {
            jedis.select(dbNum);
            return jedis.get(key);
        } catch (Exception e) {
            log.error(" get-string-failed:" + key, e);
            return null;
        } finally {
            returnResource(jedis);
        }
    }



}
