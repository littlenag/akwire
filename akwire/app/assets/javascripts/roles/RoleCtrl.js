(function() {

  controllersModule.controller('ListRoleCtrl', [
    '$scope', '$log', 'RoleService', function($scope, $log, RoleService) {

      $log.debug("constructing ListRoleController");
      $scope.roles = [];

      $scope.getAllRoles = function() {
        $log.debug("getAllRoles()");
        return RoleService.getRoles().then(function(data) {
          $log.debug("Promise returned " + data.length + " Roles");
          $scope.roles = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Roles: " + error);
        });
      };

      $scope.getAllRoles();
    }
  ]);

  controllersModule.controller('EditRoleCtrl', [
    '$scope', '$log', '$state', 'RoleService', function($scope, $log, $state, RoleService) {

      $log.debug("constructing EditRoleController");
      $scope.role = null;
      $scope.roleId = $state.params.roleId;

      RoleService.getRole($scope.roleId).then(function(data) {
        $log.debug("Promise returned Role(" + $scope.roleId + ")");
        $scope.role = data;
        return data;
      }, function(error) {
        return $log.error("Unable to get Role(" + $scope.roleId + ": " + error);
      });

      $scope.updateRole = function() {
        $log.debug("updateRole()");
        $scope.role.active = true;
        return RoleService.updateRole($scope.role).then(function(data) {
          $log.debug("Promise returned " + data + " Role");
          $scope.role = data;
          return $state.go("admin.role.list", {});
        }, function(error) {
          return $log.error("Unable to update Role: " + error);
        });
      };
    }
  ]);

  controllersModule.controller('CreateRoleCtrl', [
    '$scope', '$log', '$location', 'RoleService', function($scope, $log, $location, RoleService) {
      $log.debug("constructing CreateRoleController");
      $scope.role = {};

      $scope.createRole = function() {
        $log.debug("createRole()");
        $scope.role.active = true;
        return RoleService.createRole(this.role).then(function(data) {
          $log.debug("Promise returned " + data + " Role");
          $scope.role = data;
          return $location.path("/admin/role");
        }, function(error) {
          return $log.error("Unable to create Role: " + error);
        });
      };
    }
  ]);

}).call(this);
