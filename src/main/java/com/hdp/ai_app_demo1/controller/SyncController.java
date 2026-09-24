package com.hdp.ai_app_demo1.controller;

import com.hdp.ai_app_demo1.service.SchemaIngestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SyncController {

    private SchemaIngestionService ingestService;

    public SyncController(SchemaIngestionService ingestService){
        this.ingestService = ingestService;
    }


    @GetMapping("/sync")
    public String model() {
        System.out.println("sync start ....");

        ingestService.syncAllSchemaToVectorDB();
        System.out.println("sync end ....");
        return "OK";
    }
}
