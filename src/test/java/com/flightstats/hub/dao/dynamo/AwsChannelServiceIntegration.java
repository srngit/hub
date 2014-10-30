package com.flightstats.hub.dao.dynamo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Any test in this class will cause a channel to be expected in testChannels()
 * If the test doesn't create a channel, call channelNames.remove(channelName);
 * <p>
 * AwsChannelServiceIntegration can use DynamoDBLocal to speed up running tests locally.
 * To use it, set dynamo.endpoint=localhost:8000 in src/main/resources/default_local.properties
 * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html
 * start with java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar
 */
public class AwsChannelServiceIntegration {
    private final static Logger logger = LoggerFactory.getLogger(AwsChannelServiceIntegration.class);

    /*protected static Injector injector;
    protected static List<String> channelNames = new ArrayList<>();
    protected ChannelService channelService;
    protected String channelName;
    private CuratorFramework curator;*/

    /*@BeforeClass
    public static void setupClass() throws Exception {
        injector = Integration.startRealHub();
        channelNames = new ArrayList<>();
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        ChannelService service = injector.getInstance(ChannelService.class);
        for (String channelName : channelNames) {
            try {
                service.delete(channelName);
            } catch (Exception e) {
                logger.warn("unable to delete " + channelName, e);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        channelService = injector.getInstance(ChannelService.class);
        channelName = Integration.getRandomChannel();
        channelNames.add(channelName);
        curator = injector.getInstance(CuratorFramework.class);
    }*/

    //todo - gfm - 10/30/14 -
    /*@Test
    public void testChannelCreateDelete() throws Exception {
        channelNames.remove(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        ChannelConfiguration createdChannel = channelService.createChannel(configuration);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelService.getChannelConfiguration(channelName));
        assertNotNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
        channelService.delete(channelName);
        assertNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
    }*/

    //todo - gfm - 10/30/14 -
   /* @Test
    public void testChannelWriteReadDelete() throws Exception {
        channelNames.remove(channelName);
        createLocksPath("/TimeIndexLock/");
        createLocksPath("/ChannelReplicator/");
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        channelService.createChannel(configuration);
        Request request = Request.builder().channel(channelName).id(new ContentKey(1000).keyToString()).build();
        assertFalse(channelService.getValue(request).isPresent());
        byte[] bytes = "some data".getBytes();
        Content content = Content.builder().withData(bytes).build();
        InsertedContentKey insert = channelService.insert(channelName, content);
        Request request2 = Request.builder().channel(channelName).id(insert.getKey().keyToString()).build();
        Optional<LinkedContent> value = channelService.getValue(request2);
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());

        assertNotNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
        assertNotNull(curator.checkExists().forPath("/lastUpdated/" + channelName));
        assertNotNull(curator.checkExists().forPath("/TimeIndex/" + channelName));
        assertNotNull(curator.checkExists().forPath("/TimeIndexLock/" + channelName));
        assertNotNull(curator.checkExists().forPath("/ChannelReplicator/" + channelName));
        channelService.delete(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        assertNull(curator.checkExists().forPath("/keyGenerator/" + channelName));
        assertNull(curator.checkExists().forPath("/lastUpdated/" + channelName));
        assertNull(curator.checkExists().forPath("/TimeIndex/" + channelName));
        assertNull(curator.checkExists().forPath("/TimeIndexLock/" + channelName));
    }*/

   /* private void createLocksPath(String root) throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(root + channelName + "/locks");
        curator.create().creatingParentsIfNeeded().forPath(root + channelName + "/leases");
    }
*/
    /*@Test
    public void testChannelOptionals() throws Exception {

        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        channelService.createChannel(configuration);
        byte[] bytes = "testChannelOptionals".getBytes();
        Content content = Content.builder().withData(bytes).withContentLanguage("lang").withContentType("content").build();
        InsertedContentKey insert = channelService.insert(channelName, content);

        Request request = Request.builder().channel(channelName).id(insert.getKey().keyToString()).build();
        Optional<LinkedContent> value = channelService.getValue(request);
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertEquals("content", compositeValue.getContentType().get());
        assertEquals("lang", compositeValue.getValue().getContentLanguage().get());
    }*/

    /**
     * If this test fails, make sure all new tests either create a channel, or call channelNames.remove(channelName);
     */
    /*@Test
    public void testChannels() throws Exception {
        Set<String> existing = new HashSet<>(channelNames);
        existing.remove(channelName);
        Set<String> found = new HashSet<>();
        Iterable<ChannelConfiguration> foundChannels = channelService.getChannels();
        Iterator<ChannelConfiguration> iterator = foundChannels.iterator();
        while (iterator.hasNext()) {
            ChannelConfiguration configuration = iterator.next();
            found.add(configuration.getName());
        }
        logger.info("existing " + existing);
        logger.info("found " + found);
        assertTrue(found.containsAll(existing));

    }

    private ChannelConfiguration getChannelConfig(ChannelConfiguration.ChannelType series) {
        return ChannelConfiguration.builder()
                .withName(channelName)
                .withTtlDays(36000L)
                .withType(series)
                .withContentKiloBytes(16)
                .withDescription("descriptive")
                .withTags(Arrays.asList("one", "two", "three"))
                .build();
    }*/

/*    @Test
    public void testSequenceTimeIndexChannelWriteReadDelete() throws Exception {
        ChannelConfiguration configuration = getChannelConfig(ChannelConfiguration.ChannelType.Sequence);
        timeIndexWork(configuration, true);
    }*/
/*
    private void timeIndexWork(ChannelConfiguration configuration, boolean callGetKeys) {
        channelService.createChannel(configuration);

        byte[] bytes = "some data".getBytes();
        InsertedContentKey insert1 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        InsertedContentKey insert2 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        InsertedContentKey insert3 = channelService.insert(channelName, Content.builder().withData(bytes).build());
        HashSet<ContentKey> createdKeys = Sets.newHashSet(insert1.getKey(), insert2.getKey(), insert3.getKey());
        Request request = Request.builder().channel(channelName).id(insert1.getKey().keyToString()).build();
        Optional<LinkedContent> value = channelService.getValue(request);
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());

        HashSet<ContentKey> foundKeys = Sets.newHashSet();
        //this fails using DynamoDBLocal
        if (callGetKeys) {
            DateTime expectedDate = new DateTime(insert1.getDate());
            foundKeys.addAll(channelService.getKeys(channelName, expectedDate));
            foundKeys.addAll(channelService.getKeys(channelName, expectedDate.plusMinutes(1)));
        }

        assertEquals(createdKeys, foundKeys);

        channelService.delete(channelName);
        assertNull(channelService.getChannelConfiguration(channelName));
        channelNames.remove(channelName);
    }*/

}
