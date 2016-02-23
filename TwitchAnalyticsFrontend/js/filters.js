var module = angular.module('Filters', [])
module.filter('cut', function () {
        return function (value, wordwise, max, tail) {
            if (!value) return '';

            max = parseInt(max, 10);
            if (!max) return value;
            if (value.length <= max) return value;

            value = value.substr(0, max);
            if (wordwise) {
                var lastspace = value.lastIndexOf(' ');
                if (lastspace != -1) {
                    value = value.substr(0, lastspace);
                }
            }

            return value + (tail || ' …');
        };
    });

module.filter('orderObjectBy', function() {
  return function(items, field, reverse) {
    var filtered = [];
    angular.forEach(items, function(item) {
      filtered.push(item);
    });
    filtered.sort(function (a, b) {
      return (a[field].toLowerCase() > b[field].toLowerCase() ? 1 : -1);
    });
    if(reverse) filtered.reverse();
    return filtered;
  };
});

module.filter('hoursToTime', function () {
        return function(decimal){
    			var hours = Math.floor(decimal);
    			var min = Math.round((decimal % 1)*60);
    			if (hours < 10)
    				hours = "0"+hours;
    			if (min < 10)
    				min = "0"+min;
    			return hours+":"+min;
    		};
    });
