package com.inmemorydatastore.inmemorydatastore_app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Welcome {
    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to In-memory Data Store application";
    }
}
