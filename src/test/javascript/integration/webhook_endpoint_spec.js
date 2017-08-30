require('../integration_config');

var request = require('request');
var moment = require('moment');

var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    heartbeat: true,
    batch: "SECOND"
};
var endItem;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - add first item
 * 2 - create a webhook on that channel - with heartbeat
 * 3 - start a server at the endpoint
 * 5 - after endtime - validate that the webhook goes away
 *
 */
describe(testName, function () {

    var callbackServer;
    var callbackItems = [];

    utils.createChannel(channelName, false, testName);
    utils.addItem(channelResource, 201); // add first item for to build startItem endItem urls

    it('configures webhook', function (done) {
        //
        utils.httpGet(hubUrlBase + '/internal/time/millis', "", false)
            .then(function (response) {
                var millis = parseInt(response.body);
                var nowish = moment(millis);
                var nowPlus10 = moment(millis + 5000);
                var f = "/YYYY/MM/DD/hh/mm/ss/SSS";
                endItem = channelResource + nowPlus10.format(f) + "/123456"
                webhookConfig.startItem = channelResource + nowish.format(f) + "/123456";
                webhookConfig.endItem = endItem;
                console.log(webhookConfig);
                done();
            });

    });

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    it('verifies endItem', function (done) {
        var url = utils.getWebhookUrl() + '/' + webhookName;
        utils.httpGet(url, "", false)
            .then(function (response) {
                expect(response.body.endItem).toBe(endItem);

            })
            .catch(function () {

            })
            .finally(done);
    });

    it('starts a callback server', function (done) {
        callbackServer = utils.startHttpServer(port, function (string) {
            callbackItems.push(string);
        }, done);
    });

    utils.itSleeps(10 * 1000);

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);

    });

    it('verifies callback is gone', function (done) {
        var url = utils.getWebhookUrl() + '/' + webhookName;
        utils.httpGet(url, "", false)
            .then(function (response) {
                expect(response.statusCode).toBe(404);
                done();
            });
    });


});
