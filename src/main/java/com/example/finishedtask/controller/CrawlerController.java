package com.example.finishedtask.controller;

import com.example.finishedtask.entities.Result;
import com.example.finishedtask.services.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class CrawlerController {
@Autowired
    private CrawlerService crawlerService;

    @GetMapping("/articles")
    public ResponseEntity<List<Result>> getArticles(@RequestParam(required = false) Long page) throws IOException {
        List<Result> results = this.crawlerService.inquireResults(page);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<Result> getArticlesById(@PathVariable String id) throws IOException {
        Result result = this.crawlerService.inquireResultById(id);
        return ResponseEntity.ok(result);
    }


}
