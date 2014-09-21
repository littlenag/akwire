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
              templateUrl: "/assets/partials/home.html"
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


    app.constant('AUTH_EVENTS', {
      loginSuccess: 'auth-login-success',
      loginFailed: 'auth-login-failed',
      logoutSuccess: 'auth-logout-success',
      sessionTimeout: 'auth-session-timeout',
      notAuthenticated: 'auth-not-authenticated',
      notAuthorized: 'auth-not-authorized'
    })

    app.constant('USER_ROLES', {
      all: '*',
      super_admin: 'super_admin',
      team_admin: 'team_admin',
      team_member: 'team_member'
    })

    controllersModule.controller('AdminCtrl', ['$scope', '$log', function($scope, $log) {
      $log.debug("constructing AdminCtrl");
    }]);

    controllersModule.controller('ConfigureCtrl', ['$scope', '$log', function($scope, $log) {
      $log.debug("constructing ConfigureCtrl");
    }]);

    controllersModule.controller('LoginController', ['$scope', '$rootScope', 'AUTH_EVENTS', 'AuthService', '$http', '$state', '$log',
    function($scope, $rootScope, AUTH_EVENTS, AuthService, $http, $state, $log) {

      $log.debug("constructing LoginController");

      $scope.credentials = {
        username: "",
        password: ""
      }

      // If already authenticated just move to the home state
      if (AuthService.isAuthenticated()) {
          $state.go("home", {});
      }

      $scope.login = function (credentials) {
        AuthService.login(credentials).then(function (user) {
          $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
          $scope.setCurrentUser(user);
          $state.go("home", {});
        }, function () {
          $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
        });
      };

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

    controllersModule.controller('ApplicationController', function ($scope, $log,
                                                                    USER_ROLES,
                                                                    AuthService) {
      $scope.currentUser = null;
      $scope.userRoles = USER_ROLES;
      $scope.isAuthorized = AuthService.isAuthorized;

      $scope.setCurrentUser = function (user) {
        $log.info("Current User: " + angular.toJson(user));
        $scope.currentUser = user;
      };
    })

    ///////////////////////////////////////////////////////////////////////////

    servicesModule.service('Session', function () {
      this.create = function (userId, userEmail, userName, teamId, teamName, teams) {
        //this.sessionId = sessionId;
        //this.userRole = userRole;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.teamId = teamId;            // these are just the current team, the user might change their selection
        this.teamName = teamName;
        this.teams = teams;
      };
      this.destroy = function () {
        this.userId = null;
        this.userEmail = null;
        this.userName = null;
        this.teamId = null;
        this.teamName = null;
        this.teams = null;
      };
      return this;
    })

    servicesModule.service('AuthService', function ($http, $log, Session) {
      var authService = {};

      authService.login = function (credentials) {
        $log.info("Validating credentials: " + angular.toJson(credentials));

        return $http
          .post("/handleAuth/userpass", credentials)
          .then(function(res) {

            $log.info("Successfully Authenticated: " + angular.toJson({user: res.data}));

            Session.create(res.data.id, res.data.mail, res.data.name, res.data.memberOfTeams[0].id, res.data.memberOfTeams[0].name, res.data.memberOfTeams)

            return res.data;

        }, function(err) {
          $log.error("Failed to authenticate: " + err);
          return err;
        });
      };

      authService.isAuthenticated = function () {
        return !!Session.userId;
      };

      authService.isAuthorized = function (authorizedRoles) {
        if (!angular.isArray(authorizedRoles)) {
          authorizedRoles = [authorizedRoles];
        }
        return (authService.isAuthenticated() &&
          authorizedRoles.indexOf(Session.userRole) !== -1);
      };

      return authService;
    })


}).call(this);
