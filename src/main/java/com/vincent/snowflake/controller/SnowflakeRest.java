package com.vincent.snowflake.controller;

import com.vincent.snowflake.service.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SnowflakeRest {

    @Autowired
    SnowFlake snowFlake;

    @GetMapping("/id")
    public Long generateId(){
        long id = snowFlake.nextId();
        log.info("id:{}",id);
        return id;
    }
}
