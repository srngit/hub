package com.flightstats.hub.dao.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.dynamo.DynamoChannelConfigurationDao;
import com.flightstats.hub.dao.dynamo.DynamoUtils;
import com.flightstats.hub.dao.encryption.AuditChannelService;
import com.flightstats.hub.dao.encryption.BasicChannelService;
import com.flightstats.hub.dao.s3.ContentDaoImpl;
import com.flightstats.hub.dao.s3.S3Config;
import com.flightstats.hub.dao.s3.S3IndexDao;
import com.flightstats.hub.dao.timeIndex.TimeIndexCoordinator;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.group.DynamoGroupDao;
import com.flightstats.hub.group.GroupCallback;
import com.flightstats.hub.group.GroupCallbackImpl;
import com.flightstats.hub.group.GroupValidator;
import com.flightstats.hub.metrics.GraphiteReporting;
import com.flightstats.hub.metrics.HostedGraphiteReporting;
import com.flightstats.hub.replication.*;
import com.flightstats.hub.service.ChannelValidator;
import com.flightstats.hub.service.HubHealthCheck;
import com.flightstats.hub.service.HubHealthCheckImpl;
import com.flightstats.hub.util.ContentKeyGenerator;
import com.flightstats.hub.util.CuratorKeyGenerator;
import com.flightstats.hub.websocket.WebsocketPublisher;
import com.flightstats.hub.websocket.WebsocketPublisherImpl;
import com.flightstats.jerseyguice.metrics.MethodTimingAdapterProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class AwsModule extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(AwsModule.class);

    private final Properties properties;

    public AwsModule(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);
        bind(HubHealthCheck.class).to(HubHealthCheckImpl.class).asEagerSingleton();
        bind(ZooKeeperState.class).asEagerSingleton();
        bind(ReplicationService.class).to(ReplicationServiceImpl.class).asEagerSingleton();
        bind(Replicator.class).to(ReplicatorImpl.class).asEagerSingleton();
        bind(TimeIndexCoordinator.class).asEagerSingleton();
        bind(ChannelUtils.class).asEagerSingleton();
        bind(CuratorLock.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(AwsConnectorFactory.class).asEagerSingleton();

        if (Boolean.parseBoolean(properties.getProperty("app.encrypted"))) {
            logger.info("using encrypted hub");
            bind(ChannelService.class).annotatedWith(BasicChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
            bind(ChannelService.class).to(AuditChannelService.class).asEagerSingleton();
        } else {
            logger.info("using normal hub");
            bind(ChannelService.class).to(ChannelServiceImpl.class).asEagerSingleton();
        }
        bind(ChannelConfigurationDao.class).to(CachedChannelConfigurationDao.class).asEagerSingleton();
        bind(ChannelConfigurationDao.class)
                .annotatedWith(Names.named(CachedChannelConfigurationDao.DELEGATE))
                .to(DynamoChannelConfigurationDao.class);
        bind(WebsocketPublisher.class).to(WebsocketPublisherImpl.class).asEagerSingleton();
        bind(ReplicationDao.class).to(CachedReplicationDao.class).asEagerSingleton();
        bind(ReplicationDao.class)
                .annotatedWith(Names.named(CachedReplicationDao.DELEGATE))
                .to(DynamoReplicationDao.class);

        bind(ContentService.class).to(ContentServiceImpl.class).asEagerSingleton();
        bind(ContentDao.class).to(ContentDaoImpl.class).asEagerSingleton();
        bind(TimeIndexDao.class).to(S3IndexDao.class).asEagerSingleton();
        bind(LastUpdatedDao.class).to(SequenceLastUpdatedDao.class).asEagerSingleton();
        bind(ContentKeyGenerator.class).to(CuratorKeyGenerator.class).asEagerSingleton();

        bind(DynamoUtils.class).asEagerSingleton();
        bind(DynamoGroupDao.class).asEagerSingleton();
        bind(ChannelValidator.class).asEagerSingleton();
        bind(GroupValidator.class).asEagerSingleton();
        bind(GroupCallback.class).to(GroupCallbackImpl.class).asEagerSingleton();
        bind(WatchManager.class).asEagerSingleton();

        bind(MetricRegistry.class).in(Singleton.class);
        bind(HostedGraphiteReporting.class).asEagerSingleton();
        bind(GraphiteReporting.class).asEagerSingleton();
        bind(InstrumentedResourceMethodDispatchAdapter.class).toProvider(MethodTimingAdapterProvider.class).in(Singleton.class);
    }

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDBClient buildDynamoClient(AwsConnectorFactory factory) throws IOException {
        return factory.getDynamoClient();
    }

    @Inject
    @Provides
    @Singleton
    public AmazonS3 buildS3Client(AwsConnectorFactory factory) throws IOException {
        return factory.getS3Client();
    }
}
