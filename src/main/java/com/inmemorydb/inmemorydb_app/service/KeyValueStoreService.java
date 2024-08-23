package com.inmemorydb.inmemorydb_app.service;

import org.springframework.scheduling.annotation.Scheduled;


import org.springframework.stereotype.Service;

import com.inmemorydb.inmemorydb_app.model.KeyValuePair;

import jakarta.annotation.PostConstruct;

import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class KeyValueStoreService {
    private final Map<String, KeyValuePair> store = new ConcurrentHashMap<>();
    private final PriorityQueue<ExpirationEntry> expirationHeap = new PriorityQueue<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::checkExpiration, 0, 1, TimeUnit.SECONDS);
        loadData(); // Load data when the service starts
    }

    public void create(String key, String value, Instant expirationDate) {
        lock.writeLock().lock();
        try {
            KeyValuePair pair = new KeyValuePair(key, value, expirationDate, Instant.now());
            store.put(key, pair);
            expirationHeap.offer(new ExpirationEntry(key, expirationDate));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String read(String key) {
        lock.readLock().lock();
        try {
            KeyValuePair pair = store.get(key);
            if (pair != null && !pair.isExpired()) {
                return pair.getValue();
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String key, String value, Instant expirationDate) {
        lock.writeLock().lock();
        try {
            KeyValuePair pair = store.get(key);
            if (pair != null) {
                pair.setValue(value);
                pair.setExpirationDate(expirationDate);
                pair.setInsertTimestamp(Instant.now());
                expirationHeap.offer(new ExpirationEntry(key, expirationDate));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void delete(String key) {
        lock.writeLock().lock();
        try {
            store.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkExpiration() {
        lock.writeLock().lock();
        try {
            Instant now = Instant.now();
            while (!expirationHeap.isEmpty() && expirationHeap.peek().expirationDate.isBefore(now)) {
                ExpirationEntry entry = expirationHeap.poll();
                store.remove(entry.key);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void persistData() {
        lock.readLock().lock();
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("keyvalue_store.dat"))) {
                oos.writeObject(new HashMap<>(store));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void loadData() {
        lock.writeLock().lock();
        try {
            File file = new File("keyvalue_store.dat");
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    Map<String, KeyValuePair> loadedStore = (Map<String, KeyValuePair>) ois.readObject();
                    store.putAll(loadedStore);
                    
                    // Rebuild the expiration heap
                    for (Map.Entry<String, KeyValuePair> entry : loadedStore.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            expirationHeap.offer(new ExpirationEntry(entry.getKey(), entry.getValue().getExpirationDate()));
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    private static class ExpirationEntry implements Comparable<ExpirationEntry> {
        String key;
        Instant expirationDate;

        public ExpirationEntry(String key, Instant expirationDate) {
            this.key = key;
            this.expirationDate = expirationDate;
        }

        @Override
        public int compareTo(ExpirationEntry other) {
            return this.expirationDate.compareTo(other.expirationDate);
        }
    }
}