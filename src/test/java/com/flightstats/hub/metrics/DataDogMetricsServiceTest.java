package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;

public class DataDogMetricsServiceTest {

    private final static List<String> DEFAULT_TAGS = HubProperties.getDatadogTagsToIgnore();

    private String[] combineExpectedWithDefaults(String... expected) {
        return Stream.concat(Stream.of(expected), DEFAULT_TAGS.stream())
                .collect(Collectors.toList()).toArray(new String[0]);
    }

    @Test
    public void testChannelTag() throws Exception {
        DataDogMetricsService metricsService = new DataDogMetricsService();
        String[] actual = metricsService.addChannelTag("stuff", "foo:one");
        String[] expected = new String[]{"foo:one", "channel:stuff"};
        assertArrayEquals(combineExpectedWithDefaults(expected), actual);
    }

    @Test
    public void testChannelTagThree() throws Exception {
        DataDogMetricsService metricsService = new DataDogMetricsService();
        String[] input = new String[]{"foo:one", "foo:two", "foo:3"};
        String[] actual = metricsService.addChannelTag("stuff", input);
        List<String> strings = new ArrayList<>(Arrays.asList(input));
        strings.add("channel:stuff");
        String[] expected = strings.toArray(new String[strings.size()]);
        assertArrayEquals(combineExpectedWithDefaults(expected), actual);
    }

    @Test
    public void testChannelNone() throws Exception {
        DataDogMetricsService metricsService = new DataDogMetricsService();
        String[] actual = metricsService.addChannelTag("stuff");
        String[] expected = new String[]{"channel:stuff"};
        assertArrayEquals(combineExpectedWithDefaults(expected), actual);
    }
}