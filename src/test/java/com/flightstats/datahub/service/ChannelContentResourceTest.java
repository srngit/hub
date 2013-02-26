package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelContentResourceTest {

    @Test
    public void testGetValue() throws Exception {

        String channelName = "canal4";
        byte[] expected = new byte[]{55, 66, 77, 88};
        String contentType = "text/plain";
        DataHubKey key = new DataHubKey(new Date(11), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, expected);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent());

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        assertEquals(MediaType.TEXT_PLAIN_TYPE, result.getMetadata().getFirst("Content-Type"));
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueNoContentTypeSpecified() throws Exception {
        String channelName = "canal4";
        DataHubKey key = new DataHubKey(new Date(11), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();
        byte[] expected = new byte[]{55, 66, 77, 88};
        DataHubCompositeValue value = new DataHubCompositeValue("", expected);
        Optional<DataHubKey> previous = Optional.absent();


        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(Optional.of(new LinkedDataHubCompositeValue(value, previous)));

        ChannelContentResource testClass = new ChannelContentResource(null, dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        assertNull(result.getMetadata().getFirst("Content-Type"));      //null, and the framework defaults to application/octet-stream
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueNotFound() throws Exception {

        String channelName = "canal4";
        DataHubKey key = new DataHubKey(new Date(11), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(Optional.<LinkedDataHubCompositeValue>absent());

        ChannelContentResource testClass = new ChannelContentResource(null, dao, dataHubKeyRenderer);
        try {
            testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));
            fail("Should have thrown exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void testCreationDateHeaderInResponse() throws Exception {
        String channelName = "woo";
        DataHubKey key = new DataHubKey(new Date(1123456678922L), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();
        DataHubCompositeValue value = new DataHubCompositeValue(null, "found it!".getBytes());
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent());

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        String creationDateString = (String) result.getMetadata().getFirst(CustomHttpHeaders.CREATION_DATE_HEADER.getHeaderName());
        assertEquals("2005-08-07T23:17:58.922Z", creationDateString);
    }

    @Test
    public void testPreviousLink() throws Exception {
        String channelName = "woo";
        DataHubKey previousKey = new DataHubKey(new Date(1123456678921L), (short) 0);
        DataHubKey key = new DataHubKey(new Date(1123456678922L), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();
        DataHubCompositeValue value = new DataHubCompositeValue(null, "found it!".getBytes());
        Optional<DataHubKey> previous = Optional.of(previousKey);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, previous);

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo uriInfo = mock(UriInfo.class);

        when(dao.getValue(channelName, key)).thenReturn(Optional.of(linkedValue));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/thisitem123"));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        String link = (String) result.getMetadata().getFirst("Link");
        assertEquals("<http://path/to/000021CJ7HU0I000>;rel=\"previous\"", link);
    }

    @Test
    public void testPreviousLink_none() throws Exception {
        String channelName = "woo";
        DataHubKey key = new DataHubKey(new Date(1123456678922L), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();
        DataHubCompositeValue value = new DataHubCompositeValue(null, "found it!".getBytes());
        Optional<DataHubKey> previous = Optional.absent();
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, previous);

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo uriInfo = mock(UriInfo.class);

        when(dao.getValue(channelName, key)).thenReturn(Optional.of(linkedValue));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/thisitem123"));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        String link = (String) result.getMetadata().getFirst("Link");
        assertNull(link);
    }
}
