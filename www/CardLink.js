var exec = require ('cordova/exec');

var PLUGIN_NAME = 'CardLink';

var CardLink = {
    establishWSS: function (arg0, cb) {
        exec(cb, null, PLUGIN_NAME, 'establishWSS', [arg0]);
    },
    isConnectedWSS: function(cb){
        exec(cb, null, PLUGIN_NAME, 'isConnectedWSS', []);
    },
    sendRequestSMSCodeMessage: function(arg0, cb){
        exec(cb, null, PLUGIN_NAME, 'sendRequestSMSCodeMessage', [arg0]);
    },
    verifyCode: function(arg0, cb){
        exec(cb, null, PLUGIN_NAME, 'verifyCode', [arg0]);
    },
    isSMSCodeCorrect: function(cb){
        exec(cb, null, PLUGIN_NAME, 'isSMSCodeCorrect', []);
    },
    setCanNumber: function(arg0, cb){
        exec(cb, null, PLUGIN_NAME, 'setCanNumber', [arg0]);
    },
    startReadCard: function(cb){
        exec(cb, null, PLUGIN_NAME, 'startReadCard', []);
    },
    isCardScanned: function(cb){
        exec(cb, null, PLUGIN_NAME, 'isCardScanned', []);
    }
};

module.exports = CardLink;