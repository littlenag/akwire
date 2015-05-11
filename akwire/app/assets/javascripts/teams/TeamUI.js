(function() {

    angular.module('akwire.ui.teams', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('admin.team', {
              url: "/team",
              abstract: true,
              templateUrl: "/assets/javascripts/teams/base.html",
            })
            .state('admin.team.list', {
              url: "",
              templateUrl: "/assets/javascripts/teams/list.html",
            })
            .state('admin.team.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/teams/create.html",
            })
            .state('admin.team.edit', {
              url: "/edit/:teamId",
              templateUrl: "/assets/javascripts/teams/edit.html",
            });
        }
    ]);

}).call(this);