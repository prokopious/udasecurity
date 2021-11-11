package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object o;
    private final ProfilingState profilingState;

    ProfilingMethodInterceptor(Clock clock, Object o, ProfilingState profilingState) {

        this.clock = Objects.requireNonNull(clock);
        this.o = o;
        this.profilingState = profilingState;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // Citation: https://www.baeldung.com/java-measure-elapsed-time  (the Java 8 section)

        if (method.isAnnotationPresent(Profiled.class)) {
            Instant start = clock.instant();
            try {
                method.invoke(o, args);
                System.out.println(method.getName() + "start");
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                Instant finish = clock.instant();
                Duration d = Duration.between(start, finish);
                System.out.println(method.getName() + d.toMillis());
                profilingState.record(o.getClass(), method, d);
            }
        }
        return method.invoke(o, args);
    }
}




