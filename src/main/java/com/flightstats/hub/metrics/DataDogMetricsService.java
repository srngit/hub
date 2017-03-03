package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class DataDogMetricsService implements MetricsService {
    private final static StatsDClient statsd = DataDog.statsd;

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {
        if (shouldLog(channel)) {
            time(channel, "channel", start, bytes, "type:" + type.toString());
            count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
        }
    }

    @Override
    public void event(String title, String text, String[] tags) {
        Event event = DataDog.getEventBuilder()
                .withTitle(title)
                .withText(text)
                .build();
        DataDog.statsd.recordEvent(event, tags);
    }

    @Override
    public void count(String name, long value, String... tags) {
        statsd.count(name, value, addTagExclusions(tags));
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        statsd.gauge(name, value, addTagExclusions(tags));
    }

    @Override
    public void time(String name, long start, String... tags) {
        statsd.time(name, System.currentTimeMillis() - start, addTagExclusions(tags));
    }

    private String[] addTagExclusions(String[] tags) {
        List<String> list = new ArrayList<>(Arrays.asList(tags));
        addTagExclusions(list);
        return list.toArray(new String[list.size()]);
    }

    private void addTagExclusions(List<String> list) {
        //todo - gfm - make this configurable
        list.add("availability-zone:");
        list.add("domain:");
        list.add("env:");
        list.add("environment:");
        list.add("group:");
        list.add("image:");
        list.add("instance-type:");
        list.add("kernel:");
        list.add("location:");
        //todo - gfm - we might use name
        //list.add("name:");
        list.add("product:");
        list.add("region:");
        list.add("role:");
        list.add("security-group:");
        list.add("team:");
        list.add("version:");
    }

    @Override
    public void time(String channel, String name, long start, String... tags) {
        if (shouldLog(channel)) {
            statsd.time(name, System.currentTimeMillis() - start, addChannelTag(channel, tags));
        }
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {
        if (shouldLog(channel)) {
            time(channel, name, start, tags);
            count(name + ".bytes", bytes, addChannelTag(channel, tags));
        }
    }

    String[] addChannelTag(String channel, String... tags) {
        List<String> list = Arrays.stream(tags).collect(Collectors.toList());
        list.add("channel:" + channel);
        addTagExclusions(list);
        return list.toArray(new String[list.size()]);
    }

}
