(function() {

    angular.module('akwire.ui.notifications', ['ui.router']).config( [ '$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('configure.notifications', {
              url: "/notification-policy",
              templateUrl: "/assets/javascripts/notifications/base.html"
            })
            .state('configure.notifications.user', {
              url: "/personal",
              templateUrl: "/assets/javascripts/notifications/user.html"
            })
            .state('configure.notifications.team', {
                url: "/edit",
                templateUrl: "/assets/javascripts/notifications/team.html"
            })
            .state('configure.notifications.service', {
              url: "/create",
              templateUrl: "/assets/javascripts/notifications/service.html"
            });
        }
    ]);

}).call(this);