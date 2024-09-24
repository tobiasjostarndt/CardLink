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
    }
};

module.exports = CardLink;