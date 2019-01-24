var localtunnel = require('localtunnel');
var config = require('config');

var opts = (config.has("url") ? {subdomain: config.get("url")} : {});
var port = (config.has("port") ? config.get("port") : 3000);

exports.createTunnel = function () {
    var tunnel = localtunnel(port, opts, function(err, tunnel) {
        if (err) {
            console.error(err);
            process.exit(1);
        }

        console.log("Obtained URL: "+tunnel.url);
    });

    tunnel.on('close', function() {
        console.error("tunnel has been closed");
        process.exit(1);
    });

    tunnel.on('error', function(err) {
        console.error("An error on the tunnel occured "+err);
        process.exit(1);
    });

};