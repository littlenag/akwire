(function() {

  controllersModule.controller('ListTeamCtrl', [
    '$scope', '$log', 'TeamService', function($scope, $log, TeamService) {
      $log.debug("constructing ListTeamController");

      $scope.teams = [];

      $scope.getAllTeams = function() {
        $log.debug("getAllTeams()");
        return TeamService.getTeams().then(function(data) {
          $log.debug("Promise returned " + data.length + " Teams");
          $scope.teams = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Teams: " + error);
        });
      };

      $scope.getAllTeams();
    }
  ]);

  controllersModule.controller('EditTeamCtrl', [
    '$scope', '$log', '$state', 'TeamService', function($scope, $log, $state, TeamService) {

      $log.debug("constructing EditTeamController");
      $scope.team = null;
      $scope.teamId = $state.params.teamId;

      TeamService.getTeam($scope.teamId).then(function(data) {
        $log.debug("Promise returned Team(" + $scope.teamId + ")");
        $scope.team = data;
        return data;
      }, function(error) {
        return $log.error("Unable to get Team(" + $scope.teamId + ": " + error);
      });

      $scope.updateTeam = function() {
        $log.debug("updateTeam()");
        $scope.team.active = true;
        return TeamService.updateTeam($scope.team).then(function(data) {
          $log.debug("Promise returned " + data + " Team");
          $scope.team = data;
          return $state.go("admin.team.list", {});
        }, function(error) {
          return $log.error("Unable to update Team: " + error);
        });
      };
    }
  ]);

  controllersModule.controller('CreateTeamCtrl', [
    '$scope', '$log', '$state', 'TeamService', function($scope, $log, $state, TeamService) {

      $log.debug("constructing CreateTeamController");

      $scope.team = {};
      $scope.createTeam = function() {

          $log.debug("createTeam()");
          $scope.team.active = true;

          return TeamService.createTeam($scope.team).then(function(data) {
            $log.debug("Promise returned " + data + " Team");
            $scope.team = data;
            return $state.go("admin.team.list", {});
          }, function(error) {
            return $log.error("Unable to create Team: " + error);
          });
      };

    }
  ]);

}).call(this);
