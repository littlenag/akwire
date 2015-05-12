(function() {

  controllersModule.controller('ListNotifacationsCtrl', [
    "$scope", '$log',  'NotifacationService',
    function($scope, $log, NotifacationService) {
      $log.debug("constructing ListNotifacationController");
      $scope.notifacations = [];

      $scope.getAllNotifacations = function() {
        $log.debug("getAllNotifacations()");
        return NotifacationService.getNotifacations().then(function(data) {
          $log.debug("Promise returned " + data.length + " Notifacations");
          $scope.notifacations = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Notifacations: " + error);
        });
      };

      $scope.getAllNotifacations();
    }
  ]);

  controllersModule.controller('CreateNotifacationCtrl', [
    '$scope', '$log', '$state', 'NotifacationService',
    function($scope, $log, $state, NotifacationService) {
      $log.debug("constructing CreateNotifacationController");

      $scope.notifacation = {};

      $scope.createNotifacation = function() {
        $log.debug("createNotifacation()");
        this.notifacation.active = true;
        return NotifacationService.createNotifacation($scope.notifacation).then(function(data) {
          $log.debug("Promise returned " + data + " Notifacation");
          $scope.notifacation = data;
          $state.go("admin.notifacation.list", {});
          return data;
        }, function(error) {
          $log.error("Unable to create Notifacation: " + error);
          return null;
        });
      };
    }
  ]);

}).call(this);
