(function() {

  servicesModule.service('UserService', [
    '$log', '$http', '$q', function($log, $http, $q) {

      var service = {};

      $log.debug("constructing UserService");

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.listUsers = function() {
        $log.debug("listUsers()");
        var deferred = this.$q.defer();
        $http.get("/users").success(function(data, status, headers) {
          $log.info("Successfully listed Users - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to list Users - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      UserService.prototype.createUser = function(user) {
        $log.debug("createUser " + (angular.toJson(user, true)));
        var deferred = this.$q.defer();
        $http.post('/user', user).success(function(data, status, headers) {
          $log.info("Successfully created User - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to create user - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
