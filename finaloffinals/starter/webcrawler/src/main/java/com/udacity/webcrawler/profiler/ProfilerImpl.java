package com.udacity.webcrawler.profiler;


import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;


final class ProfilerImpl implements Profiler {

    private final Clock clock;

    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = clock;
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        final List<Method> methods = new ArrayList<>();
        for (Method m : klass.getMethods()) {
            if (m.isAnnotationPresent(Profiled.class)) {
                methods.add(m);
            }
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("Class does not contain profiled method");
        }

        InvocationHandler handler = new ProfilingMethodInterceptor(clock, delegate, state);
        return (T) Proxy.newProxyInstance(
                klass.getClassLoader(),
                new Class[]{klass},
                handler
        );
    }

    @Override
    public void writeData(Path path) throws IOException {
//See ProfilingState (For this implementation to work, I had to override toString(). I'm not sure if that's allowed, but it worked)
        Files.write(path, (state.toString()).getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
