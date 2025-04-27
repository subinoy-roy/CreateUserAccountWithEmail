package org.roy.utils;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.hash.BloomFilter;
import redis.clients.jedis.JedisPooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RedisUtil {
    public RedisUtil() {
    }

    /**
     * Connects to Redis using the provided host and port.
     *
     * @param host The Redis host
     * @param port The Redis port
     * @return A JedisPooled instance connected to the Redis server
     */
    public JedisPooled connectToRedis(String host, int port) {
        return new JedisPooled(host, port);
    }

    /**
     * Writes the bloom filter to Redis.
     *
     * @param context     The AWS Lambda context
     * @param jedis       The JedisPooled instance
     * @param bloomFilter The bloom filter to write
     * @return true if there was an error, false otherwise
     */
    public boolean writeToRedis(Context context, JedisPooled jedis, BloomFilter<CharSequence> bloomFilter, String redisKey) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            bloomFilter.writeTo(baos);
            jedis.set(redisKey.getBytes(), baos.toByteArray());
        } catch (IOException e) {
            context.getLogger().log("Error writing bloom filter to redis: " + e.getMessage());
            return true;
        }
        return false;
    }
}