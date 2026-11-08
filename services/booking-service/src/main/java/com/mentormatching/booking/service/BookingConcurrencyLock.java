package com.mentormatching.booking.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingConcurrencyLock {

    private final EntityManager entityManager;
    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    /**
     * Executes the supplier while holding a lock for (mentorId, bookingDate).
     * Combines an in-JVM striped lock (for multi-threaded concurrency) with
     * MSSQL sp_getapplock (for distributed database-level concurrency when running on SQL Server).
     */
    public <T> T executeWithLock(Long mentorId, LocalDate bookingDate, LockCallback<T> action) {
        String lockKey = "booking_slot_" + mentorId + "_" + bookingDate;
        ReentrantLock lock = localLocks.computeIfAbsent(lockKey, k -> new ReentrantLock(true));

        lock.lock();
        try {
            tryAcquireMssqlAppLock(lockKey);
            return action.doInLock();
        } finally {
            lock.unlock();
        }
    }

    private void tryAcquireMssqlAppLock(String resource) {
        try {
            // Check if we are running against MSSQL before calling sp_getapplock
            String dbProductName = entityManager.getEntityManagerFactory().getProperties()
                    .getOrDefault("hibernate.dialect", "").toString();
            if (dbProductName.toLowerCase().contains("sqlserver")) {
                entityManager.createNativeQuery(
                        "EXEC sp_getapplock @Resource = :resource, @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 5000")
                        .setParameter("resource", resource)
                        .executeUpdate();
            }
        } catch (Exception e) {
            log.debug("MSSQL sp_getapplock skipped or not supported by current dialect: {}", e.getMessage());
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T doInLock();
    }
}

