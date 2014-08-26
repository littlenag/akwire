(function() {

    angular.module('akwire.ui.rules', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('admin.rule', {
              url: "/rule",
              abstract: true,
              templateUrl: "/assets/partials/rules/base.html",
            })
            .state('admin.rule.list', {
              url: "",
              templateUrl: "/assets/partials/rules/list.html",
            })
            .state('admin.rule.create', {
              url: "/create",
              templateUrl: "/assets/partials/rules/create.html",
            })
            .state('admin.rule.edit', {
              url: "/edit/:ruleId",
              templateUrl: "/assets/partials/rules/edit.html",
            });
        }
    ]);

}).call(this);