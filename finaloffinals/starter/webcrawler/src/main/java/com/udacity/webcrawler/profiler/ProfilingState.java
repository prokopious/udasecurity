package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Helper class that records method performance data from the method interceptor.
 */
final class ProfilingState {
    private final Map<String, Duration> data = new ConcurrentHashMap<>();

    @Override
    public String toString() {
        StringJoiner string = new StringJoiner("\n");
        for (Map.Entry<String, Duration> entry : data.entrySet()) {
            string.add("Method: " + entry.getKey() + " Duration: " + entry.getValue().toMinutesPart() + " minutes " + entry.getValue().toSecondsPart() + " seconds " + entry.getValue().toMillisPart() + " milliseconds ");
        }
        return "\n" + string + "\n";
    }

    void record(Class<?> callingClass, Method method, Duration elapsed) {
        Objects.requireNonNull(callingClass);
        Objects.requireNonNull(method);
        Objects.requireNonNull(elapsed);
        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("negative elapsed time");
        }
        String key = formatMethodCall(callingClass, method);
        data.compute(key, (k, v) -> (v == null) ? elapsed : v.plus(elapsed));
    }

    /**
     * Writes the method invocation data to the given {@link Writer}.
     *
     * <p>Recorded data is aggregated across calls to the same method. For example, suppose
     * {@link #record(Class, Method, Duration) record} is called three times for the same method
     * {@code M()}, with each invocation taking 1 second. The total {@link Duration} reported by
     * this {@code write()} method for {@code M()} should be 3 seconds.
     */
    void write(Writer writer) throws IOException {
        List<String> entries =
                data.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + " took " + formatDuration(e.getValue()) + System.lineSeparator())
                        .collect(Collectors.toList());

        // We have to use a for-loop here instead of a Stream API method because Writer#write() can
        // throw an IOException, and lambdas are not allowed to throw checked exceptions.
        for (String entry : entries) {
            writer.write(entry);
        }
    }

    /**
     * Formats the given method call for writing to a text file.
     *
     * @param callingClass the Java class of the object whose method was invoked.
     * @param method       the Java method that was invoked.
     * @return a string representation of the method call.
     */
    private static String formatMethodCall(Class<?> callingClass, Method method) {
        return String.format("%s#%s", callingClass.getName(), method.getName());
    }

    /**
     * Formats the given {@link Duration} for writing to a text file.
     */
    private static String formatDuration(Duration duration) {
        return String.format(
                "%sm %ss %sms", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
    }
}
