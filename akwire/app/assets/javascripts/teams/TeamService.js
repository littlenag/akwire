(function() {

  servicesModule.service('TeamService', [
    '$log', '$http', '$q', function($log, $http, $q) {
      var TeamService;
      return new (TeamService = (function() {

        TeamService.headers = {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        };

        TeamService.defaultConfig = {
          headers: TeamService.headers
        };

        function TeamService() {
          $log.debug("constructing TeamService");
        }

        TeamService.prototype.getTeams = function() {
          var deferred,
            _this = this;
          $log.debug("getTeams()");
          deferred = $q.defer();
          $http.get("/teams").success(function(data, status, headers) {
            $log.info("Successfully listed Teams - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to list Teams - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        TeamService.prototype.getTeam = function(teamId) {
          var deferred,
            _this = this;
          $log.debug("getTeam('" + teamId + "')");
          deferred = $q.defer();
          $http.get("/teams/" + teamId).success(function(data, status, headers) {
            $log.info("Successfully retrieved Team - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to retrieve Team - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        TeamService.prototype.createTeam = function(team) {
          var deferred,
            _this = this;
          $log.debug("createTeam " + (angular.toJson(team, true)));
          deferred = $q.defer();
          $http.post('/teams', team).success(function(data, status, headers) {
            $log.info("Successfully created Team - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to create team - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        TeamService.prototype.updateTeam = function(team) {
          var deferred,
            _this = this;
          $log.debug("updateTeam " + (angular.toJson(team, true)));
          deferred = $q.defer();
          $http.post("/teams/" + team._id, team).success(function(data, status, headers) {
            $log.info("Successfully updated Team - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to update Team - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        return TeamService;

      })());
    }
  ]);

}).call(this);
