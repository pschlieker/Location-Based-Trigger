var express = require('express');
var router = express.Router();

var gpio = require('../gpio');

//Main Page containing Status
router.get('/', function(req, res, next) {
  gpio.read(function(err,data) {
    if (err) {
      console.log(err);
      res.send("error");
    }

    res.render('index', { status: (String(data).trim() == "1" ? "off" : "on") });
  });
});

router.get('/trigger', function(req, res, next) {
  var timeout = (isNaN(req.query.timeout) ? 6000 : parseInt(req.query.timeout));

  gpio.write(0, function(err){
    if(!err){
      res.render('action', { info: 'Trigger activated for '+timeout+'ms!'});
      console.log("Trigger activated!");
      setTimeout(function () {
        gpio.write(1);
        console.log("Trigger deactivated!");
      }, timeout);
    }else{
      console.log(err);
      res.send("error");
    }
  });
});

router.get('/on', function(req, res, next) {
  gpio.write(0, function(err){
    if(!err){
      res.render('action', { info: 'Switching On!'});
      console.log("Switching on!");
    }else{
      console.log(err);
      res.send("error");
    }
  });
});

router.get('/off', function(req, res, next) {
  gpio.write(1, function(err){
    if(!err){
      res.render('action', { info: 'Switching off!'});
      console.log("Switching off!");
    }else{
      console.log(err);
      res.send("error");
    }
  });
});

module.exports = router;
