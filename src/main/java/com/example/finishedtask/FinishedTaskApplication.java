package com.example.finishedtask;

import com.example.finishedtask.services.CrawlerService;
import com.example.finishedtask.services.CrawlerServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class FinishedTaskApplication {
	public static void main(String[] args) throws IOException, InterruptedException {
		SpringApplication.run(FinishedTaskApplication.class, args);
			CrawlerService crawlerService = new CrawlerServiceImpl();
			crawlerService.inquireResults(null);
	}
}
