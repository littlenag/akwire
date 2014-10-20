(function() {

  servicesModule.service('RoleService', [
    '$log', '$http', '$q', function($log, $http, $q) {
      var service = {};

      $log.debug("constructing RoleService");

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.getRoles = function() {
        var deferred,
          _this = this;
        $log.debug("getRoles()");
        deferred = $q.defer();
        $http.get("/roles").success(function(data, status, headers) {
          $log.info("Successfully listed Roles - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to list Roles - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.getRole = function(roleId) {
        var deferred,
          _this = this;
        $log.debug("getRole('" + roleId + "')");
        deferred = $q.defer();
        $http.get("/roles/" + roleId).success(function(data, status, headers) {
          $log.info("Successfully retrieved Role - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to retrieve Role - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.createRole = function(role) {
        var deferred,
          _this = this;
        $log.debug("createRole " + (angular.toJson(role, true)));
        deferred = $q.defer();
        $http.post('/roles', role).success(function(data, status, headers) {
          $log.info("Successfully created Role - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to create role - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.updateRole = function(role) {
        var deferred,
          _this = this;
        $log.debug("updateRole " + (angular.toJson(role, true)));
        deferred = $q.defer();
        $http.post("/roles/" + role._id, role).success(function(data, status, headers) {
          $log.info("Successfully updated Role - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to update Role - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      return service;

      }
  ]);

}).call(this);
