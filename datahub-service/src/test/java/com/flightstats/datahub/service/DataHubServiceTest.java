package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DataHubServiceTest {

	@Test
	public void testGetChannels() throws Exception {
		ChannelConfiguration channel1 = new ChannelConfiguration("channel1", new Date(), 1000L);
		ChannelConfiguration channel2 = new ChannelConfiguration("channel2", new Date(), 2000L);
		List<ChannelConfiguration> channelConfigurations = Arrays.asList(channel1, channel2);

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.getChannels()).thenReturn(channelConfigurations);

		DataHubService testClass = new DataHubService(channelDao, null, null);

		Iterable<ChannelConfiguration> channels = testClass.getChannels();
		assertEquals(channelConfigurations, channels);
	}

	@Test
	public void testCreateChannel() throws Exception {
		ChannelDao channelDao = mock(ChannelDao.class);

		DataHubService testClass = new DataHubService(channelDao, null, null);

		testClass.createChannel("channelName", 1000L);

		verify(channelDao).createChannel("channelName", 1000L);
	}

	@Test
	public void testChannelExists() throws Exception {
		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.channelExists("channelName")).thenReturn(true);

		DataHubService testClass = new DataHubService(channelDao, null, null);
		boolean result = testClass.channelExists("channelName");
		assertTrue(result);
	}

	@Test
	public void testGetChannelConfiguration() throws Exception {
		ChannelConfiguration channelConfiguration = new ChannelConfiguration("channel1", new Date(), 1000L);

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.getChannelConfiguration("channelName")).thenReturn(channelConfiguration);

		DataHubService testClass = new DataHubService(channelDao, null, null);
		ChannelConfiguration result = testClass.getChannelConfiguration("channelName");
		assertEquals(channelConfiguration, result);
	}

	@Test
	public void testFindLastUpdatedKey() throws Exception {
		DataHubKey dataHubKey = new DataHubKey((short) 1033);

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.findLastUpdatedKey("channelName")).thenReturn(Optional.of(dataHubKey));

		DataHubService testClass = new DataHubService(channelDao, null, null);

		Optional<DataHubKey> result = testClass.findLastUpdatedKey("channelName");
		assertEquals(dataHubKey, result.get());
	}

	@Test
	public void testInsert() throws Exception {
		String channelName = "channelName";
		byte[] data = {'h', 'e', 'l', 'l', 'o'};
		Optional<String> contentType = Optional.of("contentType");
		Optional<String> contentEncoding = Optional.of("contentEncoding");
		Optional<String> contentLanguage = Optional.of("contentLanguage");
		DataHubKey dataHubKey = new DataHubKey((short) 3000);

		ChannelDao channelDao = mock(ChannelDao.class);
		ChannelLockExecutor channelLockExecutor = mock(ChannelLockExecutor.class);
		ChannelInsertionPublisher channelInsertionPublisher = mock(ChannelInsertionPublisher.class);

		WriteAndDispatch expectedDispatch = new WriteAndDispatch(channelDao, channelInsertionPublisher, channelName, data, contentType,
                contentLanguage);

		when(channelLockExecutor.execute(channelName, expectedDispatch)).thenReturn(new ValueInsertionResult(dataHubKey));
		when(channelDao.insert(channelName, contentType, contentLanguage, data)).thenReturn(new ValueInsertionResult(dataHubKey));

		DataHubService testClass = new DataHubService(channelDao, channelLockExecutor, channelInsertionPublisher);
		ValueInsertionResult result = testClass.insert(channelName, data, contentType, contentLanguage);

		assertEquals(dataHubKey, result.getKey());
	}

	@Test
	public void testGetValue() throws Exception {
		DataHubKey dataHubKey = new DataHubKey((short) 1033);
		byte[] data = {'h', 'e', 'l', 'l', 'o'};
		Optional<String> contentType = Optional.of("contentType");
		Optional<String> contentEncoding = Optional.of("contentEncoding");
		Optional<String> contentLanguage = Optional.of("contentLanguage");
		LinkedDataHubCompositeValue compositeValue = new LinkedDataHubCompositeValue(new DataHubCompositeValue(contentType, contentLanguage, data),
				Optional.<DataHubKey>absent(), Optional.<DataHubKey>absent());

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.getValue("channelName", dataHubKey)).thenReturn(Optional.of(compositeValue));
		DataHubService testClass = new DataHubService(channelDao, null, null);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue("channelName", dataHubKey);
		assertEquals(compositeValue, result.get());
	}

	@Test
	public void testUpdateChannelMetadata() throws Exception {
		ChannelConfiguration channelConfiguration = new ChannelConfiguration("channel1", new Date(), 1000L);

		ChannelDao channelDao = mock(ChannelDao.class);
		DataHubService testClass = new DataHubService(channelDao, null, null);

		testClass.updateChannelMetadata(channelConfiguration);
		verify(channelDao).updateChannelMetadata(channelConfiguration);
	}
}
