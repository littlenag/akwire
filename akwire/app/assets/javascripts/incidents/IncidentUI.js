(function() {

    angular.module('akwire.ui.incidents', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('incidents', {
              url: "/incidents",
              abstract: true,
              templateUrl: "/assets/javascripts/incidents/base.html",
            })
            .state('incidents.list', {
              url: "",
              templateUrl: "/assets/javascripts/incidents/list.html",
            })
            .state('incidents.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/incidents/create.html",
            });
        }
    ]);

}).call(this);