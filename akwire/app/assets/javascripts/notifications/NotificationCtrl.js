(function() {

  controllersModule.controller('ListNotificationsCtrl', [
    "$scope", '$log',  'NotificationService',
    function($scope, $log, NotificationService) {
      $log.debug("constructing ListNotificationController");
      $scope.policies = [];

      $scope.getAllNotifications = function() {
        $log.debug("getAllNotifications()");
        return NotificationService.getNotifications().then(function(data) {
          $log.debug("Promise returned " + data.length + " Policies");
          $scope.policies = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Notifications: " + error);
        });
      };

      $scope.getAllNotifications();
    }
  ]);

  controllersModule.controller('CreateNotificationCtrl', [
    '$scope', '$log', '$state', 'NotificationService',
    function($scope, $log, $state, NotificationService) {
      $log.debug("constructing CreateNotificationController");

      $scope.notification = {};

      $scope.createNotification = function() {
        $log.debug("createNotification()");
        this.notification.active = true;
        return NotificationService.createNotification($scope.notification).then(function(data) {
          $log.debug("Promise returned " + data + " Notification");
          $scope.notification = data;
          $state.go("configure.notification", {});
          return data;
        }, function(error) {
          $log.error("Unable to create Notification: " + error);
          return null;
        });
      };
    }
  ]);

}).call(this);
