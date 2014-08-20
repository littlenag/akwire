(function() {

  controllersModule.controller('ListIncidentsCtrl', [
    "$scope", '$log',  'IncidentService',
    function($scope, $log, IncidentService) {
      $log.debug("constructing ListIncidentController");
      $scope.incidents = [];

      $scope.getAllIncidents = function() {
        $log.debug("getAllIncidents()");
        return IncidentService.getIncidents().then(function(data) {
          $log.debug("Promise returned " + data.length + " Incidents");
          return $scope.incidents = data;
        }, function(error) {
          return $log.error("Unable to get Incidents: " + error);
        });
      };

      $scope.getAllIncidents()
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
