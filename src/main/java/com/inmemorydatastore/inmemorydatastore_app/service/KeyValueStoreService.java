package com.inmemorydatastore.inmemorydatastore_app.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.inmemorydatastore.inmemorydatastore_app.model.KeyValuePair;

import jakarta.annotation.PostConstruct;


import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class KeyValueStoreService {
    private final ConsistentHash<String> consistentHash;
    private final Map<String, Map<String, KeyValuePair>> nodeStores;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final PriorityQueue<ExpirationEntry> expirationHeap = new PriorityQueue<>();

    public KeyValueStoreService() {
        List<String> nodes = Arrays.asList("node1", "node2", "node3"); // Example nodes
        this.consistentHash = new ConsistentHash<>(new MD5Hash(), 100, nodes);
        this.nodeStores = new HashMap<>();
        for (String node : nodes) {
            nodeStores.put(node, new ConcurrentHashMap<>());
        }
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::checkExpiration, 0, 1, TimeUnit.SECONDS);
        loadData();
    }

    public void create(String key, String value, Instant expirationDate) {
        lock.writeLock().lock();
        try {
            String node = consistentHash.get(key);
            KeyValuePair pair = new KeyValuePair(key, value, expirationDate, Instant.now());
            nodeStores.get(node).put(key, pair);
            expirationHeap.offer(new ExpirationEntry(key, expirationDate));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String read(String key) {
        lock.readLock().lock();
        try {
            String node = consistentHash.get(key);
            KeyValuePair pair = nodeStores.get(node).get(key);
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
            String node = consistentHash.get(key);
            KeyValuePair pair = nodeStores.get(node).get(key);
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
            String node = consistentHash.get(key);
            nodeStores.get(node).remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getAllKeys() {
        lock.readLock().lock();
        try {
            List<String> allKeys = new ArrayList<>();
            for (Map<String, KeyValuePair> nodeStore : nodeStores.values()) {
                allKeys.addAll(nodeStore.keySet());
            }
            return allKeys;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void checkExpiration() {
        lock.writeLock().lock();
        try {
            Instant now = Instant.now();
            while (!expirationHeap.isEmpty() && expirationHeap.peek().expirationDate.isBefore(now)) {
                ExpirationEntry entry = expirationHeap.poll();
                String node = consistentHash.get(entry.key);
                nodeStores.get(node).remove(entry.key);
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
                Map<String, KeyValuePair> allData = new HashMap<>();
                for (Map<String, KeyValuePair> nodeStore : nodeStores.values()) {
                    allData.putAll(nodeStore);
                }
                oos.writeObject(allData);
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
                    for (Map.Entry<String, KeyValuePair> entry : loadedStore.entrySet()) {
                        String node = consistentHash.get(entry.getKey());
                        nodeStores.get(node).put(entry.getKey(), entry.getValue());
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

    private static class ConsistentHash<T> {
        private final HashFunction hashFunction;
        private final int numberOfReplicas;
        private final SortedMap<Integer, T> circle = new TreeMap<>();

        public ConsistentHash(HashFunction hashFunction, int numberOfReplicas, Collection<T> nodes) {
            this.hashFunction = hashFunction;
            this.numberOfReplicas = numberOfReplicas;
            for (T node : nodes) {
                add(node);
            }
        }

        public void add(T node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.put(hashFunction.hash(node.toString() + i), node);
            }
        }

        public void remove(T node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.remove(hashFunction.hash(node.toString() + i));
            }
        }

        public T get(Object key) {
            if (circle.isEmpty()) {
                return null;
            }
            int hash = hashFunction.hash(key);
            if (!circle.containsKey(hash)) {
                SortedMap<Integer, T> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            return circle.get(hash);
        }
    }

    private static class MD5Hash implements HashFunction {
        public int hash(Object key) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] bytes = md.digest(key.toString().getBytes());
                return ByteBuffer.wrap(bytes).getInt();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private interface HashFunction {
        int hash(Object key);
    }
}