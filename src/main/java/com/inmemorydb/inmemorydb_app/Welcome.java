package com.inmemorydb.inmemorydb_app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Welcome {
    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to In-memory data Store app";
    }
}
