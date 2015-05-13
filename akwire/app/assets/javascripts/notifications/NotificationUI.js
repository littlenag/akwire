(function() {

    angular.module('akwire.ui.notifications', ['ui.router']).config( [ '$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('configure.notifications', {
              url: "/notifications",
              abstract: true,
              templateUrl: "/assets/javascripts/notifications/base.html"
            })
            .state('configure.notifications.detail', {
              url: "",
              templateUrl: "/assets/javascripts/notifications/detail.html"
            })
            .state('configure.notifications.edit', {
                url: "/edit",
                templateUrl: "/assets/javascripts/notifications/edit.html"
            })
            .state('configure.notifications.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/notifications/create.html"
            });
        }
    ]);

}).call(this);