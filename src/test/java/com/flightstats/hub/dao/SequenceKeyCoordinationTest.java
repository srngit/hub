package com.flightstats.hub.dao;

import com.flightstats.hub.model.SequenceContentKey;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SequenceKeyCoordinationTest {
    private SequenceKeyCoordination keyCoordination;

    @Before
    public void setUp() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        keyCoordination = new SequenceKeyCoordination(curator);
    }

    @Test
    public void testLow() throws Exception {
        keyCoordination.insert("testlow", new SequenceContentKey(2000));
        keyCoordination.insert("testlow", new SequenceContentKey(1999));
        assertEquals(2000, keyCoordination.getFromZK("testlow").value);
    }

    @Test
    public void testSame() throws Exception {
        keyCoordination.insert("testSame", new SequenceContentKey(2000));
        keyCoordination.insert("testSame", new SequenceContentKey(2000));
        assertEquals(2000, keyCoordination.getFromZK("testSame").value);
    }

    @Test
    public void testHigh() throws Exception {

        keyCoordination.insert("testHigh", new SequenceContentKey(2000));
        keyCoordination.insert("testHigh", new SequenceContentKey(2001));
        assertEquals(2001, keyCoordination.getFromZK("testHigh").value);
    }



}
