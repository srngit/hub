package com.flightstats.hub.metrics;

import org.junit.Test;

public class DataDogMetricsServiceTest {

    //todo - gfm -
    @Test
    public void test() {

    }
    /*@Test
    public void testChannelTag() throws Exception {
        DataDogMetricsService metricsService = new DataDogMetricsService();
        String[] tags = metricsService.addChannelTag("stuff", "name:one");
        assertArrayEquals(new String[]{"name:one", "channel:stuff"}, tags);
    }

    @Test
    public void testChannelTagThree() throws Exception {
        DataDogMetricsService metricsService = new DataDogMetricsService();
        String[] input = new String[]{"name:one", "name:two", "name:3"};
        String[] tags = metricsService.addChannelTag("stuff", input);
        List<String> strings = new ArrayList<>(Arrays.asList(input));
        strings.add("channel:stuff");
        assertArrayEquals(strings.toArray(new String[strings.size()]), tags);
    }

    @Test
    public void testChannelNone() throws Exception {
        DataDogMetricsService metricsService = new DataDogMetricsService();
        String[] tags = metricsService.addChannelTag("stuff");
        assertArrayEquals(new String[]{"channel:stuff"}, tags);
    }*/
}