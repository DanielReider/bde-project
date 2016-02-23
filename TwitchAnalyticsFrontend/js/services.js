var services = angular.module('Services',['ngResource']);
var hbaseURL = "http://10.60.64.45:8070/"
var wildflyURL = "http://10.60.64.45:1234/TwitchAnalyticsBackend/"

services.factory('HBaseService',['$http',function($http){

	var doRequest = function(db, query) {
    return $http({
  		url: hbaseURL + db + "/" + query,
  		method: 'GET'
        });
      }

	return {
		queryHBase: function(db,query) {return doRequest(db,query);}
	};
}]);

services.factory('PredictionService',['$http',function($http){

	var doRequest = function(name, game, duration, daytime, fps, day) {
    return $http({
  		url: wildflyURL + "prediction",
  		method: 'GET',
			params: {
				name: name,
				game: game,
				duration: duration,
				daytime: daytime,
				fps: fps,
				day: day,
			}
        });
      }

	return {
		predict: function(name, game, duration, daytime, fps, day) {return doRequest(name, game, duration, daytime, fps, day);}
	};
}]);
