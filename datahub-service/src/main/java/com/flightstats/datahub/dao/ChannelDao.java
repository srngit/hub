package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

public interface ChannelDao {

	void createChannel(ChannelConfiguration configuration);

	ValueInsertionResult insert(ChannelConfiguration configuration, Optional<String> contentType, Optional<String> contentLanguage, byte[] data);

	Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key);

	Optional<DataHubKey> findLastUpdatedKey(String channelName);

}
