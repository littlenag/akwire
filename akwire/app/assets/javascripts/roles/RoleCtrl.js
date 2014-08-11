(function() {

  controllersModule.controller('ListRoleCtrl', [
    '$log', 'RoleService', function($log, RoleService) {
      var ListRoleCtrl;
      return new (ListRoleCtrl = (function() {

        function ListRoleCtrl() {
          $log.debug("constructing ListRoleController");
          this.roles = [];
          this.getAllRoles();
        }

        ListRoleCtrl.prototype.getAllRoles = function() {
          var _this = this;
          $log.debug("getAllRoles()");
          return RoleService.getRoles().then(function(data) {
            $log.debug("Promise returned " + data.length + " Roles");
            return _this.roles = data;
          }, function(error) {
            return $log.error("Unable to get Roles: " + error);
          });
        };

        return ListRoleCtrl;

      })());
    }
  ]);

  controllersModule.controller('EditRoleCtrl', [
    '$scope', '$log', '$state', 'RoleService', function($scope, $log, $state, RoleService) {

      $log.debug("constructing EditRoleController");
      $scope.role = null;
      $scope.roleId = $state.params.roleId;

      RoleService.getRole($scope.roleId).then(function(data) {
        $log.debug("Promise returned Role(" + $scope.roleId + ")");
        return $scope.role = data;
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
    '$log', '$location', 'RoleService', function($log, $location, RoleService) {
      var CreateRoleCtrl;
      return new (CreateRoleCtrl = (function() {

        function CreateRoleCtrl() {
          $log.debug("constructing CreateRoleController");
          this.role = {};
        }

        CreateRoleCtrl.prototype.createRole = function() {
          var _this = this;
          $log.debug("createRole()");
          this.role.active = true;
          return RoleService.createRole(this.role).then(function(data) {
            $log.debug("Promise returned " + data + " Role");
            _this.role = data;
            return $location.path("/admin/role");
          }, function(error) {
            return $log.error("Unable to create Role: " + error);
          });
        };

        return CreateRoleCtrl;

      })());
    }
  ]);

}).call(this);
