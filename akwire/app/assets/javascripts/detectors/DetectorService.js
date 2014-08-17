(function() {

  servicesModule.service('DetectorService', [
    '$log', '$http', '$q', function($log, $http, $q) {
      var DetectorService;
      return new (DetectorService = (function() {

        DetectorService.headers = {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        };

        DetectorService.defaultConfig = {
          headers: DetectorService.headers
        };

        function DetectorService() {
          $log.debug("constructing DetectorService");
        }

        DetectorService.prototype.getDetectors = function() {
          var deferred,
            _this = this;
          $log.debug("getDetectors()");
          deferred = $q.defer();
          $http.get("/teams").success(function(data, status, headers) {
            $log.info("Successfully listed Detectors - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to list Detectors - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        DetectorService.prototype.getDetector = function(teamId) {
          var deferred,
            _this = this;
          $log.debug("getDetector('" + teamId + "')");
          deferred = $q.defer();
          $http.get("/teams/" + teamId).success(function(data, status, headers) {
            $log.info("Successfully retrieved Detector - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to retrieve Detector - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        DetectorService.prototype.createDetector = function(team) {
          var deferred,
            _this = this;
          $log.debug("createDetector " + (angular.toJson(team, true)));
          deferred = $q.defer();
          $http.post('/teams', team).success(function(data, status, headers) {
            $log.info("Successfully created Detector - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to create team - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        DetectorService.prototype.updateDetector = function(team) {
          var deferred,
            _this = this;
          $log.debug("updateDetector " + (angular.toJson(team, true)));
          deferred = $q.defer();
          $http.post("/teams/" + team._id, team).success(function(data, status, headers) {
            $log.info("Successfully updated Detector - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to update Detector - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        return DetectorService;

      })());
    }
  ]);

}).call(this);
