var eddystoneBeacon = require('eddystone-beacon');

var namespaceId = '0eaea79793961d290fa4';
var instanceId = '3dfe4b5e89b9';

var options = {
    name: 'DoorPi', // set device name when advertising (Linux only)
    txPowerLevel: -22, // override TX Power Level, default value is -21,
    tlmCount: 2, // 2 TLM frames
    tlmPeriod: 10 // every 10 advertisements
};

exports.startBeacon = function () {
    eddystoneBeacon.advertiseUid(namespaceId, instanceId, [options]);
};
