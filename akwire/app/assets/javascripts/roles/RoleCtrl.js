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
    '$log', '$location', '$routeParams', 'RoleService', function($log, $location, $routeParams, RoleService) {
      var EditRoleCtrl;
      return new (EditRoleCtrl = (function() {

        function EditRoleCtrl() {
          var _this = this;
          $log.debug("constructing EditRoleController");
          this.role = null;
          this.roleId = $routeParams.roleId;
          RoleService.getRole(this.roleId).then(function(data) {
            $log.debug("Promise returned Role(" + _this.roleId + ")");
            return _this.role = data;
          }, function(error) {
            return $log.error("Unable to get Role(" + _this.roleId + ": " + error);
          });
        }

        EditRoleCtrl.prototype.updateRole = function() {
          var _this = this;
          $log.debug("updateRole()");
          this.role.active = true;
          return RoleService.updateRole(this.role).then(function(data) {
            $log.debug("Promise returned " + data + " Role");
            _this.role = data;
            return $location.path("/admin/roles/" + _this.roleId);
          }, function(error) {
            return $log.error("Unable to update Role: " + error);
          });
        };

        return EditRoleCtrl;

      })());
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
          return this.RoleService.createRole(this.role).then(function(data) {
            $log.debug("Promise returned " + data + " Role");
            _this.role = data;
            return $location.path("/admin");
          }, function(error) {
            return $log.error("Unable to create Role: " + error);
          });
        };

        return CreateRoleCtrl;

      })());
    }
  ]);

}).call(this);
