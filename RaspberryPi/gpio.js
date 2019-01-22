var fs = require('fs');

const syspath = '/sys/class/gpio/gpio18';
const rpiStatus = require('rpi-status');

var mockstatus = 1; //Using Mockstatus if not on a RaspberryPi

function write(value, callback) {
    console.log("Writing " + value + " to GPIO");

    const path = syspath + '/value';

    value = !!value ? '1' : '0';
    if(rpiStatus.getSerialNumber() != "") {
        fs.writeFile(path, value, 'utf8', callback);
    }else{
        mockstatus = value;
        if(callback) callback(null, null);
    }
}

function read(callback) {
    console.log("Reading GPIO");

    const path = syspath + '/value';
    if(rpiStatus.getSerialNumber() != ""){
        fs.readFile(path, callback);
    }else{
        if(callback) callback(null, mockstatus);
    }
}


function noOp() {}

module.exports = {
    write: write,
    read: read
};