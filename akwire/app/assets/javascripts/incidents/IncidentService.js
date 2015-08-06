(function() {
  "use strict";

  servicesModule.service('IncidentService', ['$log', '$http', '$q',
    function($log, $http, $q) {

      $log.debug("constructing IncidentService");

      var service = {};

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.createIncident = function(incident) {
        $log.debug("createIncident " + (angular.toJson(incident, true)));

        var deferred = $q.defer();

        $http.post('/incident/create', incident).
          success(function(data, status, headers) {
            $log.info("Successfully created Incident - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to create incident - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.getIncidents = function() {
        $log.debug("getIncidents()");

        var deferred = $q.defer();

        $http.get("/incidents").
          success(function(data, status, headers) {
            $log.info("Successfully listed Incidents - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to list Incidents - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.getIncident = function(incidentId) {
        $log.debug("getIncident('" + incidentId + "')");

        var deferred = $q.defer();

        $http.get("/incident/" + incidentId).
          success(function(data, status, headers) {
            $log.info("Successfully retrieved Incident - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to retrieve Incident - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      // ack, suppress (single, any from host, any from rule), resolve, archive

      service.ackIncident = function(incident) {
        $log.debug("ACK Incident " + (angular.toJson(incident, true)));

        var deferred = $q.defer();

        $http.post("/incident", incident).
          success(function(data, status, headers) {
            $log.info("Successfully updated Incident - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to update Incident - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
