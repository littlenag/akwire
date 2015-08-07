(function() {

  controllersModule.controller('ListIncidentsCtrl', [
    "$scope", '$log', '$state', 'IncidentService',
    function($scope, $log, $state, IncidentService) {
      $log.debug("constructing ListIncidentController");
      $scope.incidents = [];

      $scope.getAllIncidents = function() {
        $log.debug("getAllIncidents()");
        return IncidentService.getIncidents().then(function(data) {
          $log.debug("Promise returned Incidents", data);
          $scope.incidents = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Incidents: " + error);
        });
      };

      $scope.archiveIncident = function(id) {
        $log.debug("archiveIncident()", id);
        return IncidentService.archiveIncident(id).then(function(data) {
          $log.debug("Incident archived", id);
          $state.go("incidents.list", {});
        }, function(error) {
          return $log.error("Unable to get Incidents: " + error);
        });
      };

      $scope.getAllIncidents();
    }
  ]);

  controllersModule.controller('CreateIncidentCtrl', [
    '$scope', '$log', '$state', 'IncidentService',
    function($scope, $log, $state, IncidentService) {
      $log.debug("constructing CreateIncidentController");

      $scope.incident = {};

      $scope.createIncident = function() {
        $log.debug("createIncident()");
        this.incident.active = true;
        return IncidentService.createIncident($scope.incident).then(function(data) {
          $log.debug("Promise returned " + data + " Incident");
          $scope.incident = data;
          $state.go("admin.incident.list", {});
          return data;
        }, function(error) {
          $log.error("Unable to create Incident: " + error);
          return null;
        });
      };
    }
  ]);

}).call(this);
