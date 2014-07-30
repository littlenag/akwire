(function() {
    'use strict';

    console.log("Akwire Client starting. Starting AngularJS.")

    var dependencies = [
//        'ngRoute',
        'ui.bootstrap',
        'ui.router',
//        'akwire.routeConfig'
        'akwire.filters',
        'akwire.services',
        'akwire.controllers',
        'akwire.directives',
        'akwire.common',
    ];

    var app = angular.module('akwire', dependencies);

    app.config(function($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise("/agents");

        $stateProvider
            .state('incidents', {
              url: "/incidents",
              templateUrl: "/assets/partials/incidents/list.html"
            })
            .state('agents', {
              url: "/agents",
              templateUrl: "/assets/partials/agents/list.html"
            })
            .state('admin', {
              url: "/admin",
              templateUrl: "/assets/partials/admin/base.html",
            })
            .state('admin.role', {
              url: "/role",
              templateUrl: "/assets/partials/roles/base.html",
            })
            .state('admin.role.edit', {
              url: "/:roleId",
              templateUrl: "/assets/partials/roles/edit.html",
            })
            .state('admin.role.create', {
              url: "/create",
              templateUrl: "/assets/partials/roles/create.html",
            });
    });

    this.commonModule = angular.module('akwire.common', []);
    this.controllersModule = angular.module('akwire.controllers', []);
    this.servicesModule = angular.module('akwire.services', []);
    this.modelsModule = angular.module('akwire.models', []);
    this.directivesModule = angular.module('akwire.directives', []);
    this.filtersModule = angular.module('akwire.filters', []);

}).call(this);
