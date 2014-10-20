(function() {

  controllersModule.controller('UserCtrl', [
    '$scope', '$log', 'UserService', function($scope, $log, UserService) {

      $log.debug("constructing UserController");
      $scope.users = [];

      $scope.getAllUsers = function() {
        $log.debug("getAllUsers()");
        return UserService.listUsers().then(function(data) {
          $log.debug("Promise returned " + data.length + " Users");
          $scope.users = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Users: " + error);
        });
      };

      $scope.getAllUsers();
  }]);

}).call(this);
