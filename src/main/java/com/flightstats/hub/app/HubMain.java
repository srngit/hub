package com.flightstats.hub.app;

import com.conducivetech.services.common.util.PropertyConfiguration;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.hub.app.config.GuiceContext;
import com.flightstats.hub.dao.aws.AwsModule;
import com.flightstats.jerseyguice.jetty.JettyConfig;
import com.flightstats.jerseyguice.jetty.JettyConfigImpl;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class HubMain {

    private static final Logger logger = LoggerFactory.getLogger(HubMain.class);
    private static Injector injector;
    private static final DateTime startTime = new DateTime();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, or 'useDefault'");
        }
        final Properties properties = loadProperties(args[0]);
        logger.info(properties.toString());

        startZookeeperIfSingle(properties);

        JettyServer server = startServer(properties, new AwsModule(properties));

        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("Jetty Server shutting down...");
                latch.countDown();
            }
        });
        latch.await();
        server.halt();
        HubServices.stopAll();
        logger.info("Server shutdown complete.  Exiting application.");
    }

    private static void startZookeeperIfSingle(final Properties properties) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String zkConfigFile = properties.getProperty("zookeeper.cfg", "");
                if ("singleNode".equals(zkConfigFile)) {
                    startSingleZookeeper();
                }
            }

            private void startSingleZookeeper() {
                try {
                    warn("using zookeeper single node config file");
                    QuorumPeerConfig config = new QuorumPeerConfig();
                    Properties zkProperties = new Properties();
                    zkProperties.setProperty("clientPort", "2181");
                    zkProperties.setProperty("initLimit", "5");
                    zkProperties.setProperty("syncLimit", "2");
                    zkProperties.setProperty("maxClientCnxns", "0");
                    zkProperties.setProperty("tickTime", "2000");
                    Path tempZookeeper = Files.createTempDirectory("zookeeper_");
                    zkProperties.setProperty("dataDir", tempZookeeper.toString());
                    zkProperties.setProperty("dataLogDir", tempZookeeper.toString());
                    config.parseProperties(zkProperties);
                    ServerConfig serverConfig = new ServerConfig();
                    serverConfig.readFrom(config);

                    new ZooKeeperServerMain().runFromConfig(serverConfig);
                } catch (Exception e) {
                    logger.warn("unable to start zookeeper", e);
                }
            }
        }).start();
    }

    private static void warn(String message) {
        logger.warn("**********************************************************");
        logger.warn("*** " + message);
        logger.warn("**********************************************************");
    }

    public static JettyServer startServer(Properties properties, Module module) throws IOException, ConstraintException {
        JettyConfig jettyConfig = new JettyConfigImpl(properties);
        GuiceContext.HubGuiceServlet guice = GuiceContext.construct(properties, module);
        injector = guice.getInjector();
        JettyServer server = new JettyServer(jettyConfig, guice);
        HubServices.startAll();
        server.start();
        logger.info("Jetty server has been started.");
        return server;
    }

    public  static Properties loadProperties(String fileName) throws IOException {
        if (fileName.equals("useDefault")) {
            return getLocalProperties("default");
        } else if (fileName.equals("useEncryptedDefault")) {
            return getLocalProperties("defaultEncrypted");
        }
        return PropertyConfiguration.loadProperties(new File(fileName), true, logger);
    }

    private static Properties getLocalProperties(String fileNameRoot) throws IOException {
        warn("using " + fileNameRoot + " properties file");
        Properties defaultProperties = getProperties("/" + fileNameRoot + ".properties", true);
        Properties localProperties = getProperties("/" + fileNameRoot + "_local.properties", false);
        for (String localKey : localProperties.stringPropertyNames()) {
            String newVal = localProperties.getProperty(localKey);
            logger.info("overriding " + localKey + " using '" + newVal +
                    "' instead of '" + defaultProperties.getProperty(localKey, "") + "'");
            defaultProperties.setProperty(localKey, newVal);
        }
        return defaultProperties;
    }

    private static Properties getProperties(String name, boolean required) throws IOException {
        URL resource = HubMain.class.getResource(name);
        return PropertyConfiguration.loadProperties(resource, required, logger);
    }

    @VisibleForTesting
    public static Injector getInjector() {
        return injector;
    }

    public static DateTime getStartTime() {
        return startTime;
    }
}
