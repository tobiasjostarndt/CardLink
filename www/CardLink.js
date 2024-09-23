var exec = require ('cordova/exec');

var PLUGIN_NAME = 'CardLink';

var CardLink = {
    establishWSS: function (arg0, cb) {
        exec(cb, null, PLUGIN_NAME, 'establishWSS', [arg0]);
    },
    isConnected: function(cb){
        exec(cb, null, PLUGIN_NAME, 'isConnected', []);
    }
};

module.exports = CardLink;