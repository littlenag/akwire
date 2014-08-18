(function() {
  "use strict";

  servicesModule.service('DetectorService', ['$log', '$http', '$q',
    function($log, $http, $q) {

      $log.debug("constructing DetectorService");

      var service = {};

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.createDetector = function(detector) {
        $log.debug("createDetector " + (angular.toJson(detector, true)));

        var deferred = $q.defer();

        $http.post('/detector/create', detector).
          success(function(data, status, headers) {
            $log.info("Successfully created Detector - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to create detector - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.getDetectors = function() {
        $log.debug("getDetectors()");

        var deferred = $q.defer();

        $http.get("/detector/list").
          success(function(data, status, headers) {
            $log.info("Successfully listed Detectors - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to list Detectors - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.getDetector = function(detectorId) {
        $log.debug("getDetector('" + detectorId + "')");

        var deferred = $q.defer();

        $http.get("/detector", {params : {detectorId: detectorId}}).
          success(function(data, status, headers) {
            $log.info("Successfully retrieved Detector - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to retrieve Detector - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.updateDetector = function(detector) {
        $log.debug("updateDetector " + (angular.toJson(detector, true)));

        var deferred = $q.defer();

        $http.post("/detector", detector).
          success(function(data, status, headers) {
            $log.info("Successfully updated Detector - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to update Detector - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.genIntegrationToken = function(detector) {
        $log.debug("genIntegrationToken " + (angular.toJson(detector, true)));

        var deferred = $q.defer();

        $http.post("/detector/create_token", {}, {params : {detectorId : detector._id}}).
          success(function(data, status, headers) {
            $log.info("Successfully created Token; Detector - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to create Token; Detector - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
