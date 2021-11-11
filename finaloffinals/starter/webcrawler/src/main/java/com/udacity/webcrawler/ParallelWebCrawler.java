package com.udacity.webcrawler;


import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

//Citations and notes: For the constructor I got guidance here: https://knowledge.udacity.com/questions/640295.
//My design is basically the same thing as the sequential crawler, albeit with RecursiveAction/ForkJoinPool replacing crawlInternal to handle parallel
//tasks. I also implemented the builder pattern below to avoid long constructor calls,
//which I did on the advice of the instructor. Other relevant citations can be found in the code blocks below.

final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final int maxDepth;
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @MaxDepth int maxDepth,
            @IgnoredUrls List<Pattern> ignoredUrls,
            PageParserFactory parserFactory) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
        for (String url : startingUrls) {
            pool.invoke(new CrawlInternal(url, deadline, maxDepth, counts, visitedUrls, clock, parserFactory, ignoredUrls));
        }
        if (counts.isEmpty()) {
            return new CrawlResult.Builder().setWordCounts(counts).setUrlsVisited(visitedUrls.size()).build();
        }
        return new CrawlResult.Builder().setWordCounts(WordCounts.sort(counts, popularWordCount)).setUrlsVisited(visitedUrls.size()).build();
    }

    public static class CrawlInternal extends RecursiveAction {

        //Citations: http://tutorials.jenkov.com/java-util-concurrent/java-fork-and-join-forkjoinpool.html

        private String url;
        private final Instant deadline;
        private final int maxDepth;
        private final ConcurrentMap<String, Integer> counts;
        private final ConcurrentSkipListSet<String> visitedUrls;
        private final Clock clock;
        private final PageParserFactory parserFactory;
        private final List<Pattern> ignoredUrls;

        public CrawlInternal(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls, Clock clock,
                             PageParserFactory parserFactory, List<Pattern> ignoredUrls) {
            this.url = url;
            this.deadline = deadline;
            this.maxDepth = maxDepth;
            this.counts = counts;
            this.visitedUrls = visitedUrls;
            this.clock = clock;
            this.parserFactory = parserFactory;
            this.ignoredUrls = ignoredUrls;
        }

        public static final class Builder {

            //Citations: https://dzone.com/articles/the-builder-pattern-for-class-with-many-constructo  https://java-design-patterns.com/patterns/builder/

            private String url;
            private Instant deadline;
            private int maxDepth;
            private ConcurrentMap<String, Integer> counts;
            private ConcurrentSkipListSet<String> visitedUrls;
            private Clock clock;
            private PageParserFactory parserFactory;
            private List<Pattern> ignoredUrls;

            public Builder setUrl(String url) {
                this.url = url;
                return this;
            }

            public Builder setMaxDepth(int maxDepth) {
                this.maxDepth = maxDepth;
                return this;
            }

            public Builder setDeadline(Instant deadline) {
                this.deadline = deadline;
                return this;
            }

            public Builder setCounts(ConcurrentMap<String, Integer> counts) {
                this.counts = counts;
                return this;
            }

            public Builder setClock(Clock clock) {
                this.clock = clock;
                return this;
            }

            public Builder setParserFactory(PageParserFactory parserFactory) {
                this.parserFactory = parserFactory;
                return this;
            }

            public Builder setVisitedUrls(ConcurrentSkipListSet<String> visitedUrls) {
                this.visitedUrls = visitedUrls;
                return this;
            }

            public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
                this.ignoredUrls = ignoredUrls;
                return this;
            }

            public CrawlInternal build() {
                return new CrawlInternal(
                        url,
                        deadline,
                        maxDepth,
                        counts,
                        visitedUrls,
                        clock,
                        parserFactory,
                        ignoredUrls);
            }
        }

        @Override
        protected void compute() {
            if (visitedUrls.contains(url)) {
                return;
            }

            if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
                return;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return;
                }
            }
            visitedUrls.add(url);
            PageParser.Result result = parserFactory.get(url).parse();
            for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
                if (counts.containsKey(e.getKey())) {
                    counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
                } else {
                    counts.put(e.getKey(), e.getValue());
                }
            }
            List<CrawlInternal> subtasks = new ArrayList<>();
            for (String link : result.getLinks()) {
                subtasks.add(new CrawlInternal.Builder().setParserFactory(parserFactory).setVisitedUrls(visitedUrls).setIgnoredUrls(ignoredUrls).setUrl(link).setDeadline(deadline).setCounts(counts).setMaxDepth(maxDepth - 1).setClock(clock).build());
            }
            invokeAll(subtasks);
        }
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

}
