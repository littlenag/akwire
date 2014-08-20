(function() {

    angular.module('akwire.ui.incidents', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('incidents', {
              url: "/incidents",
              abstract: true,
              templateUrl: "/assets/partials/incidents/base.html",
            })
            .state('incidents.list', {
              url: "",
              templateUrl: "/assets/partials/incidents/list.html",
            })
            .state('incidents.create', {
              url: "/create",
              templateUrl: "/assets/partials/incidents/create.html",
            });
        }
    ]);

}).call(this);