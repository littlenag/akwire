(function() {

  controllersModule.controller('CreateUserCtrl', [
    '$scope', '$log', 'UserService', function($scope, $log, UserService) {
      $log.debug("constructing CreateUserController");

      $scope.user = {};

      $scope.createUser = function() {
        $log.debug("createUser()");
        $scope.user.active = true;
        return UserService.createUser($scope.user).then(function(data) {
          $log.debug("Promise returned " + data + " User");
          $scope.user = data;
          return $location.path("/");
        }, function(error) {
          return $log.error("Unable to create User: " + error);
        });
      };
    }]
  );

}).call(this);
