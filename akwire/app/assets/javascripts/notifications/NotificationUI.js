(function() {

  angular.module('akwire.ui.notifications', ['ui.router']).config( [ '$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {

    // Three different types of policies: 'user', 'team', 'service'.
    $stateProvider
      .state('configure.notifications', {
        url: "/notification-policy/:policy",
        templateUrl: "/assets/javascripts/notifications/base.html"
      });
  }
]);

}).call(this);