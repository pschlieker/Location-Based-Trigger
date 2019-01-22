var localtunnel = require('localtunnel');

exports.createTunnel = function () {
    var tunnel = localtunnel(3000,function(err, tunnel) {
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