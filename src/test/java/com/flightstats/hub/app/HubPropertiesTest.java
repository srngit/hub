package com.flightstats.hub.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HubPropertiesTest {

    @Test
    public void testIsReadOnly() {
        assertFalse(HubProperties.isReadOnly());
        HubProperties.setProperty("hub.read.only", "somewhere," + HubHost.getLocalName());
        assertTrue(HubProperties.isReadOnly());
        HubProperties.setProperty("hub.read.only", "");
    }

    @Test
    public void testDatadogTagsToIgnoreIsEmpty() {
        HubProperties.setProperty("data_dog.ignore_tags", "");
        assertEquals(Collections.emptyList(), HubProperties.getDatadogTagsToIgnore());
    }

    @Test
    public void testDatadogTagsToIgnoreIsSingular() {
        HubProperties.setProperty("data_dog.ignore_tags", "foo");
        assertEquals(Collections.singletonList("foo:"), HubProperties.getDatadogTagsToIgnore());
    }

    @Test
    public void testDatadogTagsToIgnoreIsMultiple() {
        HubProperties.setProperty("data_dog.ignore_tags", "foo,bar");
        assertEquals(Arrays.asList("foo:", "bar:"), HubProperties.getDatadogTagsToIgnore());
    }

    @Test
    public void testDatadogTagsToIgnoreHasWhitespace() {
        HubProperties.setProperty("data_dog.ignore_tags", " foo ,  bar,baz, qux ");
        assertEquals(Arrays.asList("foo:", "bar:", "baz:", "qux:"), HubProperties.getDatadogTagsToIgnore());
    }

}