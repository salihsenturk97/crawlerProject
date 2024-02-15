package com.example.finishedtask.services;

import com.example.finishedtask.entities.Result;

import java.io.IOException;
import java.util.List;

public interface CrawlerService {
    List<Result> inquireResults(Long page) throws IOException;

    Result inquireResultById(String id) throws IOException;
}
