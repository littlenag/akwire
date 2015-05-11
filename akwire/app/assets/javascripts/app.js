(function() {
    "use strict";

    console.log("Akwire Client starting. Starting AngularJS.");

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
              templateUrl: "/assets/javascripts/login/login.html"
            })

            .state('logout', {
              url: "/logout",
              template: "",
              controller: ['$scope', '$state', 'ApplicationController',
                function ( $scope, $state, ApplicationController) {
                  ApplicationController.logout();
                }
              ]
            })

            .state('home', {
              url: "/home",
              templateUrl: "/assets/javascripts/home.html"
            })

            .state('agents', {
              url: "/agents",
              templateUrl: "/assets/javascripts/agents/list.html"
            })
            .state('admin', {
              url: "/admin",
              templateUrl: "/assets/javascripts/admin/base.html",
            })
            .state('configure', {
              url: "/configure",
              templateUrl: "/assets/javascripts/configure/base.html",
            })

            .state('admin.role', {
              url: "/role",
              abstract: true,
              templateUrl: "/assets/javascripts/roles/base.html",
            })
            .state('admin.role.list', {
              url: "",
              templateUrl: "/assets/javascripts/roles/list.html",
            })
            .state('admin.role.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/roles/create.html",
            })
            .state('admin.role.edit', {
              url: "/edit/:roleId",
              templateUrl: "/assets/javascripts/roles/edit.html",
            })

            .state('admin.user', {
              url: "/user",
              templateUrl: "/assets/javascripts/users/list.html",
            })
            .state('admin.user.edit', {
              url: "/:userId",
              templateUrl: "/assets/javascripts/users/edit.html",
            })
            .state('admin.user.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/users/create.html",
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
    });

    app.constant('USER_ROLES', {
      all: '*',
      super_admin: 'super_admin',
      team_admin: 'team_admin',
      team_member: 'team_member'
    });

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
      };

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
    }]);

    controllersModule.controller('ApplicationController', function ($scope, $rootScope, $log, $state,
                                                                    USER_ROLES, AUTH_EVENTS,
                                                                    AuthService) {
      $log.debug("constructing ApplicationController");

      $scope.currentUser = null;
      $scope.currentTeamId = null;
      $scope.currentTeamName = null;
      $scope.userRoles = USER_ROLES;
      $scope.isAuthorized = AuthService.isAuthorized;

      $scope.setCurrentUser = function (user) {
        $log.info("Current User: " + angular.toJson(user));
        $scope.currentUser = user;
        $scope.currentTeamId = user.memberOfTeams[0].id;
        $scope.currentTeamName = user.memberOfTeams[0].name;
      };

      $scope.logout = function() {
        $scope.currentUser = null;
        $scope.currentTeamId = null;
        $scope.currentTeamName = null;
        AuthService.logout().then(function(x) {
          $rootScope.$broadcast(AUTH_EVENTS.logoutSuccess);
          $state.go("login", {});
        });
      };

//      AuthService.retryAuth().then(function (user) {
//        if (user !== null) {
//          $log.info("Re-authed with User: " + angular.toJson(user));
//          $scope.setCurrentUser(user);
//        }
//      });
    });

    ///////////////////////////////////////////////////////////////////////////

    app.factory('AuthInterceptor', function ($window, $q) {
      return {
          request: function(config) {
              config.headers = config.headers || {};
              if ($window.sessionStorage.getItem('token')) {
                  //config.headers.Authorization = 'Bearer ' + $window.sessionStorage.getItem('token');
                  config.headers['X-Auth-Token'] = $window.sessionStorage.getItem('token');
              }
              return config || $q.when(config);
          },
          response: function(response) {
              if (response.status === 401) {
                  // TODO: Redirect user to login page.
              }
              return response || $q.when(response);
          }
      };
    });

    // Register the previously created AuthInterceptor.
    app.config(function ($httpProvider) {
        $httpProvider.interceptors.push('AuthInterceptor');
    });

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
    });

    servicesModule.service('AuthService', function ($http, $log, $cookieStore, $location, $q, Session, $window) {
      var authService = {};

      authService.login = function (credentials) {
        $log.info("Validating credentials: " + angular.toJson(credentials));

        return $http
          .post("/auth/api/authenticate/userpass?builder=cookie", credentials) // token now returns a JSON token document
          .then(function(res) {

            $log.info("Successfully Authenticated User: " + credentials.username);

            $log.info("res: " + angular.toJson(res));

            $window.sessionStorage.setItem('token', res.data.token);

            // securesocial returns nothing useful here other than our cookie

            // return a promise that will contain our user's info
            return authService.getUserInfo(credentials.username);

          }, function(err) {
            $log.error("Failed to authenticate: " + err);
            return $q.reject("Failed to authenticate");
          })
          .then(function(res) {
            // Now we have the user, create the session, stash the info, return the object
            $log.info("Successfully Authenticated");
            return authService.initSession(res.data);
          });
      };

      authService.logout = function() {
        return $http.get("/auth/logout").then(function(res) {
          $log.info("Successfully Logged Out");
          Session.destroy();
          return res;
        });
      };

      authService.getUserInfo = function (username) {
        $log.info("Fetching user entity: " + username);
        return $http.get("/users/by-email/" + username);
      };

      authService.initSession = function (user) {
        $log.info("User Info: " + angular.toJson(user));
        Session.create(user.id, user.mail, user.name, user.memberOfTeams[0].id, user.memberOfTeams[0].name, user.memberOfTeams);
        $cookieStore.put("akwire.email", user.mail);
        return user;
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

      authService.retryAuth = function() {
        // You might still have a good token in your browser, try if so
        var lastEmailUsed = $cookieStore.get("akwire.email");

        var deferred = $q.defer();

        if (lastEmailUsed !== null) {
          // try to re-fetch the user object
          $log.info("Retying auth for: " + lastEmailUsed);

          authService.getUserInfo(lastEmailUsed).then(function(res) {
            $log.info("Successfully Authenticated");
            deferred.resolve(authService.initSession(res.data));
          }, function(err) {
            $log.error("Failed to authenticate: " + angular.toJson(err));
            // Force the user back to the login page
            $location.path("/");
            deferred.reject(err);
          });
        } else {
          deferred.reject(null);
        }

        return deferred.promise;
      };

      return authService;
    });


}).call(this);
