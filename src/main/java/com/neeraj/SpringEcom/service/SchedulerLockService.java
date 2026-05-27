package com.neeraj.SpringEcom.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class SchedulerLockService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerLockService.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final String ownerId = UUID.randomUUID().toString();

    public SchedulerLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void runWithLock(String lockName, Duration lockTtl, Runnable task) {
        String lockKey = "springecom:scheduler-lock:" + lockName;
        String lockValue = ownerId + ":" + Thread.currentThread().getName();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, lockTtl);

        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Skipping scheduled job {} because another server owns the lock", lockName);
            return;
        }

        try {
            task.run();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockValue);
        } catch (Exception e) {
            log.warn("Failed to release scheduler lock {}", lockKey, e);
        }
    }
}