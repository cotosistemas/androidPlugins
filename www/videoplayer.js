cordova.define("cordova/plugin/VideoPlayer", function(require, exports, module) {      

var exec = require("cordova/exec");
var VideoPlayer = function () {};

	VideoPlayer.prototype.DEFAULT_OPTIONS: {
        volume: 1.0,
        scalingMode: 1
    };

    VideoPlayer.prototype.SCALING_MODE: {
        SCALE_TO_FIT: 1,
        SCALE_TO_FIT_WITH_CROPPING: 2
    };
	
	VideoPlayer.prototype.play = function(path, options, successCallback, errorCallback) {
				 options = this.merge(this.DEFAULT_OPTIONS, options);
        exec(successCallback, errorCallback, "VideoPlayer", "play", [path, options]);
	};

	VideoPlayer.prototype.close = function(notificationId) {
		exec(successCallback, errorCallback, "VideoPlayer", "close", []);
	};

	VideoPlayer.prototype.merge = function() {
		 var obj = {};
         Array.prototype.slice.call(arguments).forEach(function(source) {
            for (var prop in source) {
                obj[prop] = source[prop];
            }
        });
        return obj;
	};


	var VideoPlayer = new VideoPlayer();
	module.exports = VideoPlayer
});