package com.inmemorydatastore.inmemorydatastore_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inmemorydatastore.inmemorydatastore_app.service.KeyValueStoreService;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/kv")
public class KeyValueStoreController {

    @Autowired
    private KeyValueStoreService storeService;

    @PostMapping("/{key}")
    public ResponseEntity<String> create(@PathVariable String key, @RequestBody KeyValueRequest request) {
        storeService.create(key, request.getValue(), Instant.now().plusSeconds(request.getExpirationSeconds()));
        return new ResponseEntity<>("Created", HttpStatus.CREATED);
    }

    @GetMapping("/{key}")
    public ResponseEntity<String> read(@PathVariable String key) {
        String value = storeService.read(key);
        if (value != null) {
            return new ResponseEntity<>(value, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<String> update(@PathVariable String key, @RequestBody KeyValueRequest request) {
        storeService.update(key, request.getValue(), Instant.now().plusSeconds(request.getExpirationSeconds()));
        return new ResponseEntity<>("Updated", HttpStatus.OK);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> delete(@PathVariable String key) {
        storeService.delete(key);
        return new ResponseEntity<>("Deleted", HttpStatus.OK);
    }

    @GetMapping("/keys")
    public ResponseEntity<List<String>> getAllKeys() {
        List<String> keys = storeService.getAllKeys();
        return new ResponseEntity<>(keys, HttpStatus.OK);
    }

    static class KeyValueRequest {
        private String value;
        private long expirationSeconds = 3600; // Default 1 hour

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
    }
}