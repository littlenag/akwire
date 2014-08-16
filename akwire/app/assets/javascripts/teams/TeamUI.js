(function() {

    angular.module('akwire.ui.teams', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('admin.team', {
              url: "/team",
              abstract: true,
              templateUrl: "/assets/partials/teams/base.html",
            })
            .state('admin.team.list', {
              url: "",
              templateUrl: "/assets/partials/teams/list.html",
            })
            .state('admin.team.create', {
              url: "/create",
              templateUrl: "/assets/partials/teams/create.html",
            })
            .state('admin.team.edit', {
              url: "/edit/:teamId",
              templateUrl: "/assets/partials/teams/edit.html",
            });
        }
    ]);

}).call(this);