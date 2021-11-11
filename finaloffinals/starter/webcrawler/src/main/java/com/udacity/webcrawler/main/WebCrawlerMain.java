package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiled;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class WebCrawlerMain {

    private final CrawlerConfiguration config;

    private WebCrawlerMain(CrawlerConfiguration config) {
        this.config = Objects.requireNonNull(config);
    }

    @Inject
    private WebCrawler crawler;

    @Inject
    private Profiler profiler;

    @Profiled
    private void run() throws Exception {
        Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

        CrawlResult result = crawler.crawl(config.getStartPages());
        CrawlResultWriter resultWriter = new CrawlResultWriter(result);

        if (!config.getResultPath().isEmpty()) {
            Path p = Paths.get(config.getResultPath());
            resultWriter.write(p);
        } else {
            OutputStream out = System.out;
            OutputStreamWriter o = new OutputStreamWriter(out);
            resultWriter.write(o);
        }

        if (!config.getProfileOutputPath().isEmpty()) {
            Path p = Paths.get(config.getProfileOutputPath());
            profiler.writeData(p);
        } else {
            OutputStream out = System.out;
            OutputStreamWriter o = new OutputStreamWriter(out);
            profiler.writeData(o);
        }
    }

    public static void main(String[] args) throws Exception {

        //set args.length to 0 and simple run the main method with any of the paths below for easy testing.
        if (args.length != 1) {
            System.out.println("Usage: WebCrawlerMain [starting-url]");
            return;
        }
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        Path path = Paths.get("C:\\Users\\Owner\\Desktop\\original crawler\\starter\\starter\\webcrawler\\src\\main\\config\\sample_config.json");
//        Path path = Paths.get("C:\\Users\\Owner\\Desktop\\crawler\\starter\\webcrawler\\src\\main\\config\\sample_config_sequential.json");
//        Path path = Paths.get("C:\\Users\\Owner\\Desktop\\original crawler\\starter\\starter\\webcrawler\\src\\main\\config\\written_question_1b.json");
//        Path path = Paths.get("C:\\Users\\Owner\\Desktop\\original crawler\\starter\\starter\\webcrawler\\src\\main\\config\\written_question_1a.json");
        CrawlerConfiguration config = new ConfigurationLoader(path).load();
        System.out.println("Please wait...");
        new WebCrawlerMain(config).run();
    }
}