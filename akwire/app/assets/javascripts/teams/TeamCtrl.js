(function() {

  controllersModule.controller('ListTeamCtrl', [
    '$log', 'TeamService', function($log, TeamService) {
      var ListTeamCtrl;
      return new (ListTeamCtrl = (function() {

        function ListTeamCtrl() {
          $log.debug("constructing ListTeamController");
          this.teams = [];
          this.getAllTeams();
        }

        ListTeamCtrl.prototype.getAllTeams = function() {
          var _this = this;
          $log.debug("getAllTeams()");
          return TeamService.getTeams().then(function(data) {
            $log.debug("Promise returned " + data.length + " Teams");
            return _this.teams = data;
          }, function(error) {
            return $log.error("Unable to get Teams: " + error);
          });
        };

        return ListTeamCtrl;

      })());
    }
  ]);

  controllersModule.controller('EditTeamCtrl', [
    '$scope', '$log', '$state', 'TeamService', function($scope, $log, $state, TeamService) {

      $log.debug("constructing EditTeamController");
      $scope.team = null;
      $scope.teamId = $state.params.teamId;

      TeamService.getTeam($scope.teamId).then(function(data) {
        $log.debug("Promise returned Team(" + $scope.teamId + ")");
        return $scope.team = data;
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
    '$log', '$location', 'TeamService', function($log, $location, TeamService) {
      var CreateTeamCtrl;
      return new (CreateTeamCtrl = (function() {

        function CreateTeamCtrl() {
          $log.debug("constructing CreateTeamController");
          this.team = {};
        }

        CreateTeamCtrl.prototype.createTeam = function() {
          var _this = this;
          $log.debug("createTeam()");
          this.team.active = true;
          return TeamService.createTeam(this.team).then(function(data) {
            $log.debug("Promise returned " + data + " Team");
            _this.team = data;
            return $location.path("/admin/team");
          }, function(error) {
            return $log.error("Unable to create Team: " + error);
          });
        };

        return CreateTeamCtrl;

      })());
    }
  ]);

}).call(this);
