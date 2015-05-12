(function() {

    angular.module('akwire.ui.notifications', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('notifications', {
              url: "/notifications",
              abstract: true,
              templateUrl: "/assets/javascripts/notifications/base.html"
            })
            .state('notifications.list', {
              url: "",
              templateUrl: "/assets/javascripts/notifications/list.html"
            })
            .state('notifications.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/notifications/create.html"
            });
        }
    ]);

}).call(this);