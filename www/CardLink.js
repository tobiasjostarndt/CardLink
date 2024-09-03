var exec = require ('cordova/exec');

var PLUGIN_NAME = 'CardLink';

var CardLink = {
    cardlink: function (arg0, cb) {
        exec(cb, null, PLUGIN_NAME, 'cardlink', [arg0]);
    }
};

module.exports = CardLink;