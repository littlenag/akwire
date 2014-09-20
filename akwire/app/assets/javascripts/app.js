(function() {
    "use strict";

    console.log("Akwire Client starting. Starting AngularJS.")

    var dependencies = [
        'ui.bootstrap',
        'ui.router',
        'akwire.ui.teams',
        'akwire.ui.rules',
        'akwire.ui.detectors',
        'akwire.ui.incidents',
        'akwire.filters',
        'akwire.services',
        'akwire.controllers',
        'akwire.directives',
        'akwire.common',
    ];

    var app = angular.module('akwire', dependencies);

    this.commonModule = angular.module('akwire.common', []);
    this.uiModule = angular.module('akwire.ui', ['ui.router', 'ngAnimate']);
    this.controllersModule = angular.module('akwire.controllers', []);
    this.servicesModule = angular.module('akwire.services', ['ngResource', 'ngCookies']);
    this.modelsModule = angular.module('akwire.models', []);
    this.directivesModule = angular.module('akwire.directives', []);
    this.filtersModule = angular.module('akwire.filters', []);

    app.config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise("/");

        $stateProvider

            .state('login', {
              url: "/",
              templateUrl: "/assets/partials/login/login.html"
            })

            .state('home', {
              url: "/home",
              views: {
                '': {
                  templateUrl: "/assets/partials/home.html"
                },
                'activeteam@': {
                  templateUrl: "/assets/partials/system-menu.html"
                }
              }
            })

            .state('agents', {
              url: "/agents",
              templateUrl: "/assets/partials/agents/list.html"
            })
            .state('admin', {
              url: "/admin",
              templateUrl: "/assets/partials/admin/base.html",
            })
            .state('configure', {
              url: "/configure",
              templateUrl: "/assets/partials/configure/base.html",
            })

            .state('admin.role', {
              url: "/role",
              abstract: true,
              templateUrl: "/assets/partials/roles/base.html",
            })
            .state('admin.role.list', {
              url: "",
              templateUrl: "/assets/partials/roles/list.html",
            })
            .state('admin.role.create', {
              url: "/create",
              templateUrl: "/assets/partials/roles/create.html",
            })
            .state('admin.role.edit', {
              url: "/edit/:roleId",
              templateUrl: "/assets/partials/roles/edit.html",
            })

            .state('admin.user', {
              url: "/user",
              templateUrl: "/assets/partials/users/list.html",
            })
            .state('admin.user.edit', {
              url: "/:userId",
              templateUrl: "/assets/partials/users/edit.html",
            })
            .state('admin.user.create', {
              url: "/create",
              templateUrl: "/assets/partials/users/create.html",
            });
    }]);

    app.run(
      [ '$rootScope', '$state', '$stateParams',
      function ($rootScope, $state, $stateParams) {

        // It's very handy to add references to $state and $stateParams to the $rootScope
        // so that you can access them from any scope within your applications.For example,
        // <li ui-sref-active="active }"> will set the <li> // to active whenever
        // 'contacts.list' or one of its decendents is active.
        $rootScope.$state = $state;
        $rootScope.$stateParams = $stateParams;
      }]);

    controllersModule.controller('AdminCtrl', ['$scope', '$log', function($scope, $log) {
      $log.debug("constructing AdminCtrl");
    }]);

    controllersModule.controller('ConfigureCtrl', ['$scope', '$log', function($scope, $log) {
      $log.debug("constructing ConfigureCtrl");
    }]);

    controllersModule.controller('LoginController', ['$scope', '$http', '$state', '$log', 'UserSessionService', function($scope, $http, $state, $log, UserSessionService) {
      $log.debug("constructing LoginController");
      if (! $scope.form) {
        $scope.form = {}
      }

      $scope.login = function() {
        $log.info("Posting credentials: " + angular.toJson($scope.form));
        $http.post("/auth/authenticate/userpass", $scope.form).success(function(data, status, headers) {

          $log.info("Successfully Authenticated: " + angular.toJson({status: status}));

          UserSessionService.initForUser($scope.form.username)

          $state.go("home", {});

        }).error(function(data, status, headers) {
          $log.error("Failed to authenticate: " + status);
          $scope.form.errors = response
        });
      }

      $scope.logout = function() {
        $http.get("/auth/logout").success(function(data, status, headers) {
          $log.info("Successfully Logged Out" + status);
          $state.go("login", {});
        }).error(function(data, status, headers) {
          $log.error("Failed to log out " + status);
          $state.go("login", {});
        });
      }
    }]);

    controllersModule.controller('MenuCtrl', ['$scope', '$log', 'UserSessionService', function($scope, $log, UserSessionService) {
      $log.debug("constructing MenuCtrl");
      $scope.userInfo = UserSessionService.getUserInfo();
      $log.debug("MenuCtrl userInfo: "+angular.toJson($scope.userInfo));
    }]);

    servicesModule.service('UserSessionService', [
      '$log', '$http', '$q', '$resource', '$cookieStore', function($log, $http, $q, $resource, $cookieStore) {

        $log.debug("constructing UserSessionService");

        var service = {};

        service.headers = {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        };

        service.userInfo = {
          email: "",
          id: "",
          name: "",
          teamId: "",      // these are just the current team, the user might change their "view"
          teamName: ""
        }

        service.getUserInfo = function() {
          if (service.userInfo.email === "") {
            var lastEmailUsed = $cookieStore.get("akwire.email");

            var Users = $resource("/users/by-email/:email");

            var user = Users.get({email:lastEmailUsed},
              function() {
                $log.debug("User record returned")
                service.userInfo.email = lastEmailUsed;
                service.userInfo.id = user.id;
                service.userInfo.name = user.name;
                service.userInfo.teamId = user.memberOfTeams[0].id;
                service.userInfo.teamName = user.memberOfTeams[0].name;
              }, function() {
                $log.error("Failed to get User record");
              }
            );
          }

          return service.userInfo;
        }

        service.initForUser = function(email) {
          $log.debug("initForUser("+email+")");

          service.userInfo.email = email;

          var lastEmailUsed = $cookieStore.get("akwire.email");

          if (lastEmailUsed == null || ! (lastEmailUsed === service.userInfo.email)) {
            $cookieStore.put("akwire.email", service.userInfo.email);
          }

          service.getUserInfo();
        };

        return service;
      }
    ]);

}).call(this);
