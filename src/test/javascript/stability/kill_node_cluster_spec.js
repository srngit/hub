// const moment = require('moment');
const { exec, execSync } = require('child_process');
const {
    // closeServer,
    createChannel,
    // deleteWebhook,
    fromObjectPath,
    getProp,
    // getWebhookUrl,
    // hubClientChannelRefresh,
    // hubClientDelete,
    hubClientGet,
    hubClientPostTestItem,
    // hubClientPut,
    itSleeps,
    randomChannelName,
    // randomString,
    // startServer,
    // waitForCondition,
} = require('../lib/helpers');
const {
    // getCallBackDomain,
    // getCallBackPort,
    getHubUrlBase,
    getChannelUrl,
} = require('../lib/config');

// const port = getCallBackPort();
const channelName = randomChannelName();
// const webhookName = randomChannelName();
// const callbackDomain = getCallBackDomain();
// const callbackPath = `/${randomString(5)}`;
// const callbackUrl = `${callbackDomain}:${port}${callbackPath}`;
const channelUrl = getChannelUrl();
const channelResource = `${channelUrl}/${channelName}`;
const testContext = {
    [channelName]: {
        postedItemHistory: [],
        callbackItemHistory: [],
        serversToRestart: [],
        zookeepersToRestart: [],
    },
};
const execOptions = { encoding: 'utf8' };
const headers = { 'Content-Type': 'application/json' };

const pendingIfNotReady = () => {
    if (!testContext[channelName].ready) {
        return pending('test configuration failed in before block');
    }
};

const isHubUp = () => {
    const container = execSync('docker ps', execOptions);
    return container.includes('hub');
};

const hubClientKillNodeAndPostItem = async () => {
    const handlePostItem = async () => {
        const response = await hubClientPostTestItem(channelResource, headers);
        const item = fromObjectPath(['body', '_links', 'self', 'href'], response);
        process.stdout.write(`
        ‹‹‹‹‹‹‹‹startItem››››››››
        ${item}
        ‹‹‹‹‹‹‹‹‹‹‹‹‹›››››››››››››`
        );
        testContext[channelName].firstItem = item;
    };
    try {
        setTimeout(handlePostItem, 0);
        const containers = execSync('docker ps', execOptions);
        console.log('containers', containers);
        const rows = containers && containers.split('\n');
        const hubString = rows && rows.find(str => str.includes('hub'));
        const hubPropertyArray = (hubString && hubString.split(' ')) || [];
        const containerId = hubPropertyArray[0];
        const restart = execSync(`docker stop ${containerId}`, execOptions);
        do {
            console.log(' ***** ***** ***** ');
        } while (isHubUp());
        console.log('*************', restart);
    } catch (ex) {
        console.log('failed at ', ex);
    }
};

describe('behavior of webhook when a single node cluster dies during delivery', () => {
    it('runs', async () => {
        if (!isHubUp()) {
            exec('docker run -p 80:80 flightstats/hub', execOptions);
        }
        // make a call to the hub to clarify it is alive
        const response1 = await hubClientGet(`${getHubUrlBase()}/channel`);
        let stableStart = getProp('statusCode', response1) === 200;
        let tries = 0;
        do {
            await itSleeps(3000);
            tries += 1;
            const res = await hubClientGet(`${getHubUrlBase()}/channel`);
            stableStart = getProp('statusCode', res) === 200;
        } while (stableStart === false && tries < 5);
        // create channel
        const response2 = await createChannel(channelName, channelUrl, 'stability test: kill node');
        const channelStart = getProp('statusCode', response2) === 201;
        // tag all as ready to roll
        testContext[channelName].ready = [stableStart, channelStart]
            .every(t => t);
        await hubClientKillNodeAndPostItem();
    });

    xit('posts an item to the channel', async () => {
        pendingIfNotReady();
        await hubClientKillNodeAndPostItem();
        expect(true).toBe(true);
        // const response = await hubClientPostTestItem(channelResource, headers);
        // const item = fromObjectPath(['body', '_links', 'self', 'href'], response);
        // process.stdout.write(`
        // ‹‹‹‹‹‹‹‹startItem››››››››
        // ${item}
        // ‹‹‹‹‹‹‹‹‹‹‹‹‹›››››››››››››`
        // );
        // testContext[channelName].firstItem = item;
    });
});
