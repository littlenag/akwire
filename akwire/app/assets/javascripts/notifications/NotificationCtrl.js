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
          $log.debug("Promise returned " + data + " Policy");
          $scope.notification = data;
          $state.go("configure.notification", {});
          return data;
        }, function(error) {
          $log.error("Unable to create Policy: " + error);
          return null;
        });
      };
    }
  ]);

  controllersModule.controller('UserPolicyCtrl', [
    "$scope", '$log',  'NotificationService',
    function($scope, $log, NotificationService) {
      $log.debug("constructing UserPolicyCtrl");

      $scope.editorLoaded = function(_editor) {
        $log.debug("editor loaded");

        // Populate with User's Personal policy

        // Editor part
        var _session = _editor.getSession();
        var _renderer = _editor.renderer;

        // Set read-only until the policy is loaded
        _editor.setReadOnly(true);
        _session.setUndoManager(new ace.UndoManager());
        _renderer.setShowGutter(true);

        // Fetch the policy from the server


        // Events
        _editor.on("changeSession", function() {
          // Not sure when this is called
          $log.debug("editor session change");
        });

        _session.on("change", function() {
          // Called whenever the content of the editor changes.
        });
      };
    }
  ]);

}).call(this);
