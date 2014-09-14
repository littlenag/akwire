(function() {

    angular.module('akwire.ui.rules', ['ui.router']).config([

      '$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {

        //$log.debug("setting ui routes: configure.rule.*");

        $stateProvider
            .state('configure.rules', {
              url: "/rules",
              abstract: true,
              templateUrl: "/assets/partials/rules/base.html",
            })
            .state('configure.rules.list', {
              url: "",
              templateUrl: "/assets/partials/rules/list.html",
            })
            .state('configure.rules.create', {
              url: "/create",
              templateUrl: "/assets/partials/rules/create.html",
            })
            .state('configure.rules.edit', {
              url: "/edit/:ruleId",
              templateUrl: "/assets/partials/rules/edit.html",
            });
        }
    ]);

}).call(this);