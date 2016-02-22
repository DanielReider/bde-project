var routes = angular.module('Routes',['ngRoute']);


routes.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
    when('/', {
      templateUrl: 'views/index.html',
      controller: 'WelcomeController'
    }).

    when('/prediction', {
      templateUrl: 'views/prediction.html',
      controller: 'PredictionController'
    }).

    when('/404', {
      templateUrl: 'views/errors/404.html'
    }).

      otherwise({
        redirectTo: '/404'
      });
  }]);
