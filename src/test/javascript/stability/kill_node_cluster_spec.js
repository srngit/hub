// const moment = require('moment');
const { exec, execSync } = require('child_process');
const NodeSSH = require('node-ssh');

const ssh = new NodeSSH();
const {
    closeServer,
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    // hubClientChannelRefresh,
    hubClientDelete,
    hubClientGet,
    hubClientPostTestItem,
    hubClientPut,
    itSleeps,
    randomChannelName,
    randomString,
    startServer,
    waitForCondition,
} = require('../lib/helpers');
const {
    getCallBackDomain,
    getCallBackPort,
    getHubUrlBase,
    getChannelUrl,
} = require('../lib/config');

const port = getCallBackPort();
const channelName = randomChannelName();
const webhookName = randomChannelName();
const callbackDomain = getCallBackDomain();
const callbackPath = `/${randomString(5)}`;
const callbackUrl = `${callbackDomain}:${port}${callbackPath}`;
const channelUrl = getChannelUrl();
const channelResource = `${channelUrl}/${channelName}`;
const testContext = {
    [channelName]: {
        postedItemHistory: [],
        callbackItemHistory: [],
        serversToRestart: [],
        zookeepersToRestart: [],
        connections: {},
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
        execSync(`docker stop ${containerId}`, execOptions);
        do {
            console.log(' ***** ***** ***** ');
        } while (isHubUp());
    } catch (ex) {
        console.log('failed at ', ex);
    }
};

const getClusterHubNodes = async () => {
    const url = `${getHubUrlBase()}/internal/deploy`;
    const response = await hubClientGet(url, headers);
    return getProp('body', response) || [];
};

describe('behavior of webhook when a single node cluster dies during delivery', () => {
    beforeAll(async () => {
        // spin up the instance if needed
        pending();
        if (!isHubUp()) {
            exec('docker run -p 80:80 flightstats/hub', execOptions);
        }
        // wait for the hub to be stable
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
    });

    xit('kills the node and posts an item to a dead hub', async () => {
        await hubClientKillNodeAndPostItem();
    });
});

describe('ssh ability', () => {
    beforeAll(async () => {
        pending();
        // create channel
        const response2 = await createChannel(channelName, channelUrl, 'stability test: kill node');
        const channelStart = getProp('statusCode', response2) === 201;
        expect(channelStart).toBe(true);
    });

    xit('ssh to the server', async () => {
        const nodes = await getClusterHubNodes();
        console.log('nodes ::::: ', nodes);
        const {
            HOME,
            SSH_PASS,
            SSH_USER,
            USER,
        } = process.env;
        for (const node of nodes) {
            if (node) {
                try {
                    const connection = await ssh.connect({
                        host: node,
                        username: SSH_USER || USER,
                        passphrase: SSH_PASS,
                        privateKey: `${HOME}/.ssh/id_rsa`,
                    });
                    testContext[channelName].connections[node] = connection;
                } catch (ex) {
                    console.log('failed ssh connection', node, ex);
                }
            }
        }
    });

    it('posts an item to the hub', async () => {
        const response = await hubClientPostTestItem(channelResource, headers);
        const item = fromObjectPath(['body', '_links', 'self', 'href'], response);
        testContext[channelName].postedItem = item;
    });

    it('finds the server that got the post', async () => {
        const nodes = await getClusterHubNodes();
        const nodesUsed = [];
        for (const node of nodes) {
            if (node) {
                const url = `${getHubUrlBase()}/internal/traces`;
                const response = await hubClientGet(url, headers);
                const recent = fromObjectPath(['body', 'recent'], response) || [];
                const active = fromObjectPath(['body', 'active'], response) || [];
                console.log('%%%%%%%%%%%%%', Object.keys(getProp('body', response)));
                const { postedItem } = testContext[channelName];
                const itemId = (postedItem && postedItem.split('/').pop()) || '';
                const found = [...active, ...recent].filter(req =>
                    req &&
                    JSON.stringify(req).includes(itemId));
                console.log('found *&*&*&*&*&');
                console.log(found);
                console.log('found *&*&*&*&*&');
                nodesUsed.push({ server: node, item: found[0] });
            }
        }
        const used = nodesUsed
            .filter(({ item }) => !!item)
            .map(u => getProp('server', u));
        console.log(`\n\u001b[1m\u001b[36mUSED NODE :::::: ${used}\u001b[0m`);
    });

    it('posts an item to the hub', async () => {
        const response = await hubClientPostTestItem(channelResource, headers);
        const item = fromObjectPath(['body', '_links', 'self', 'href'], response);
        testContext[channelName].postedItem = item;
    });

    it('finds the server that got the post', async () => {
        const nodes = await getClusterHubNodes();
        const nodesUsed = [];
        for (const node of nodes) {
            if (node) {
                const url = `${getHubUrlBase()}/internal/traces`;
                const response = await hubClientGet(url, headers);
                const recent = fromObjectPath(['body', 'recent'], response) || [];
                const { postedItem } = testContext[channelName];
                const itemId = (postedItem && postedItem.split('/').pop()) || '';
                const found = recent.find(req => req && Object.values(req).some(value => value && `${value}`.includes(itemId)));
                nodesUsed.push({ server: node, item: found });
            }
        }
        const used = nodesUsed
            .filter(({ item }) => !!item)
            .map(u => getProp('server', u));
        console.log(`\n\u001b[1m\u001b[36mUSED NODE :::::: ${used}\u001b[0m`);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    //     const { connections } = testContext[channelName];
    //     for (const node of Object.keys(connections)) {
    //         const connection = connections[node];
    //         if (connection) {
    //             try {
    //                 await connection.dispose();
    //             } catch (ex) {
    //                 console.log('failure closing ssh connection', node, ex);
    //             }
    //         }
    //     }
    });
});

describe('test some other shit', () => {
    beforeAll(async () => {
        // make a call to the hub to clarify it is alive
        const response1 = await hubClientGet(`${getHubUrlBase()}/channel`);
        const stableStart = getProp('statusCode', response1) === 200;
        // create channel
        const response2 = await createChannel(channelName, channelUrl, 'stability test: kill node');
        const channelStart = getProp('statusCode', response2) === 201;
        // tag all as ready to roll
        testContext[channelName].ready = [stableStart, channelStart]
            .every(t => t);
    });

    it('starts a callback server', async () => {
        // start a callback server
        const callback = (item) => {
            console.log('callback: ', item);
            testContext[channelName].callbackItemHistory.push(item);
        };
        const response3 = await startServer(port, callback, callbackPath);
        testContext[channelName].callbackServer = response3;
        expect(response3).toBeTruthy();
    });

    it('creates a webhook pointing with startItem set to earliest posted', async () => {
        pendingIfNotReady();
        const url = `${getWebhookUrl()}/${webhookName}`;
        console.log('webhookUrl****', url);
        const body = {
            callbackUrl,
            channelUrl: channelResource,
        };

        const response = await hubClientPut(url, headers, body);
        expect(getProp('statusCode', response)).toBe(201);
    });

    it('posts an item', async () => {
        pendingIfNotReady();
        const response = await hubClientPostTestItem(channelResource, headers);
        const item = fromObjectPath(['body', '_links', 'self', 'href'], response);
        process.stdout.write(`
        ‹‹‹‹‹‹‹‹startItem››››››››
        ${item}
        ‹‹‹‹‹‹‹‹‹‹‹‹‹›››››››››››››`
        );
        testContext[channelName].firstItem = item;
    });

    it('waits for the callback', async () => {
        const { firstItem, callbackItemHistory } = testContext[channelName];
        const condition = () => (callbackItemHistory.length && callbackItemHistory[0] === firstItem);
        await waitForCondition(condition);
    });

    afterAll(async () => {
        await closeServer(testContext[channelName].callbackServer);
        await hubClientDelete(channelResource);
        await deleteWebhook(webhookName);
    });
});
