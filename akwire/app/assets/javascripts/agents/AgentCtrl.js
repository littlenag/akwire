(function() {
  "use strict";

  controllersModule.controller('AgentCtrl', ['$scope', '$log', 'AgentService', function($scope, $log, AgentService) {

      $log.debug("constructing AgentCtrl");
      $scope.agents = [];

      AgentService.listAgents().then(function(data) {
        $log.debug("Promise returned " + data.length + " Agents");
        $scope.agents = data;
      }, function(error) {
        $log.error("Unable to get Agents: " + error);
      });
    }
  ]);

}).call(this);
