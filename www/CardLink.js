var exec = require ('cordova/exec');

var PLUGIN_NAME = 'CardLink';

var CardLink = {
    establishWSS: function (arg0, cb) {
        exec(cb, null, PLUGIN_NAME, 'establishWSS', [arg0]);
    }
};

module.exports = CardLink;