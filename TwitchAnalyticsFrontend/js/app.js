var app = angular.module('bde-hadoop',
	['Routes',
	'Services',
	'WelcomeControllers',
	'mgcrea.ngStrap',
	'Interceptors',
	'Filters',
	'nvd3',
	'Directives']);

	app.config(['$httpProvider', function($httpProvider) {
	        $httpProvider.defaults.useXDomain = true;
	        delete $httpProvider.defaults.headers.common['X-Requested-With'];
	    }
	]);
