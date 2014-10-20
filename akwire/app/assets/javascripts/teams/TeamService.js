(function() {

  servicesModule.service('TeamService', [
    '$log', '$http', '$q', function($log, $http, $q) {
      var service = {};

      $log.debug("constructing TeamService");

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.getTeams = function() {
        $log.debug("getTeams()");
        var deferred = $q.defer();
        $http.get("/teams").success(function(data, status, headers) {
          $log.info("Successfully listed Teams - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to list Teams - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.getTeam = function(teamId) {
        $log.debug("getTeam('" + teamId + "')");
        var deferred = $q.defer();
        $http.get("/teams/" + teamId).success(function(data, status, headers) {
          $log.info("Successfully retrieved Team - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to retrieve Team - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.createTeam = function(team) {
        $log.debug("createTeam " + (angular.toJson(team, true)));
        var deferred = $q.defer();
        $http.post('/teams', team).success(function(data, status, headers) {
          $log.info("Successfully created Team - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to create team - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.updateTeam = function(team) {
        $log.debug("updateTeam " + (angular.toJson(team, true)));
        var deferred = $q.defer();
        $http.post("/teams/" + team._id, team).success(function(data, status, headers) {
          $log.info("Successfully updated Team - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to update Team - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };
    }
  ]);

}).call(this);
