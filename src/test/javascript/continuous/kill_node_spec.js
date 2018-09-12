const express = require('express');
const bodyParser = require('body-parser');
const NodeSSH = require('node-ssh');

const {
    getCallBackDomain,
    getCallBackPort,
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');
const {
    closeServer,
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    hubClientDelete,
    hubClientGet,
    hubClientPostTestItem,
    hubClientPut,
    randomChannelName,
    randomString,
    waitForCondition,
} = require('../lib/helpers');

const ssh = new NodeSSH();
const port = getCallBackPort();
const channelName = randomChannelName();
const webhookName = randomChannelName();
const callbackDomain = getCallBackDomain();
const callbackPath = `/${randomString(5)}`;
const callbackUrl = `${callbackDomain}:${port}${callbackPath}`;
const channelUrl = getChannelUrl();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
const testContext = {
    [channelName]: {
        callbackItemHistory: [],
        callbackServer: null,
        firstItem: '',
        handledKill: false,
        ready: false,
    },
};
const pendingIfNotReady = () => {
    if (!testContext[channelName].ready) {
        return pending('test configuration failed in before block');
    }
};
const testName = 'stability of webhook delivery';

const startServer = async (port, callback, path = '/', secure, file) => {
    const app = express();

    app.use(bodyParser.json());

    app.post(path, async (request, response) => {
        const { handledKill } = testContext[channelName];
        const node = fromObjectPath(['headers', 'hub-node'], request) || '';
        console.log('headers \n', getProp('headers', request));
        console.log('NODE', node);
        const nodeName = node.replace('http://', '').replace(':8080', '');
        console.log('nodeName', nodeName);
        if (!handledKill) {
            // kill the node before returning a 200
            try {
                const {
                    HOME,
                    SSH_PASS,
                    SSH_USER,
                    USER,
                } = process.env;
                await ssh.connect({
                    host: nodeName,
                    username: SSH_USER || USER,
                    privateKey: `${HOME}/.ssh/id_rsa`,
                    passphrase: SSH_PASS,
                });
                const { stdout: stdout1, stderr: stderr1 } = await ssh.execCommand('docker inspect -f "{{.State.Pid}}" hub');
                if (stderr1) console.log('stderr1', stderr1);
                const dockerPID = stdout1;
                const { stdout: stdout2, stderr: stderr2 } = await ssh.execCommand(`grep -i ppid /proc/${dockerPID}/status`);
                if (stderr2) console.log('stderr2', stderr2);
                const parsePID = (str) => {
                    const matchArray = (str && str.match(/\d/g)) || [];
                    const numStr = matchArray.join('');
                    return parseInt(numStr, 10) || 0;
                };
                console.log('stdout2', stdout2);
                const PROCESS_ID = parsePID(stdout2);
                const { stdout: stdout3, stderr: stderr3 } = await ssh.execCommand(`kill -SIGINT ${PROCESS_ID}`);
                if (stdout3) console.log('stdout3', stdout3);
                if (stderr3) console.log('stderr3', stderr3);
                response.format({
                    'default': () => {
                        console.log(`this logger is here to demonstrate that the
                        connection 200 ok is sent immed. after the above code SIGINTs the
                        requesting server.        ^^^^^^^^^^^^`);
                        response.status(200).end();
                    },
                });
            } catch (ex) {
                response.status(400).end();
                console.log('error', ex);
            }
        } else {
            const arr = fromObjectPath(['body', 'uris'], request) || [];
            const str = arr[arr.length - 1] || '';
            if (callback) callback(str);
            response.status(200).end();
        }
    });
    const server = app.listen(port, () => console.log(`app listening at ${callbackUrl}`));
    return server;
};

describe(testName, () => {
    beforeAll(async () => {
        // check hub health
        const hubHealthCheck = await hubClientGet(`${getHubUrlBase()}/health`, headers);
        const healthy = fromObjectPath(['body', 'healthy'], hubHealthCheck);
        console.log('healthy', getProp('body', hubHealthCheck));
        console.log('healthy', healthy);
        // create a channel
        const response1 = await createChannel(channelName, null, `testing ${testName}`);
        const channelCreated = getProp('statusCode', response1) === 201;
        console.log('channelCreated', channelCreated);
        // tag all as ready to roll
        testContext[channelName].ready = [healthy, channelCreated]
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
        pendingIfNotReady();
        const { firstItem, callbackItemHistory } = testContext[channelName];
        const condition = () => (callbackItemHistory.length && callbackItemHistory[0] === firstItem);
        await waitForCondition(condition);
        expect(condition()).toBe(true);
    });

    afterAll(async () => {
        await closeServer(testContext[channelName].callbackServer);
        await hubClientDelete(channelResource);
        await deleteWebhook(webhookName);
    });
});
