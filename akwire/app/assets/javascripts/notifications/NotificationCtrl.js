(function() {

  controllersModule.controller('ListNotificationsCtrl', [
    "$scope", '$log',  'NotificationPolicyService',
    function($scope, $log, NotificationPolicyService) {
      $log.debug("constructing ListNotificationController");
      $scope.policies = [];

      $scope.getAllNotifications = function() {
        $log.debug("getAllNotifications()");
        return NotificationPolicyService.getNotifications().then(function(data) {
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
    '$scope', '$log', '$state', 'NotificationPolicyService',
    function($scope, $log, $state, NotificationPolicyService) {
      $log.debug("constructing CreateNotificationController");

      $scope.notification = {};

      $scope.createNotification = function() {
        $log.debug("createNotification()");
        this.notification.active = true;
        return NotificationPolicyService.createNotification($scope.notification).then(function(data) {
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

  controllersModule.controller('NotificationPolicyCtrl', [
    "$scope", '$log',  'NotificationPolicyService', 'Session',
    function($scope, $log, NotificationPolicyService, Session) {
      $log.debug("constructing NotificationPolicyCtrl");

      // Policy object we've loaded from the server
      $scope.activePolicy = null;

      $scope._session = null;
      $scope._renderer = null;

      $scope.savePolicy = function() {
        $log.debug("saving policy to server");

        // Save the user policy
        NotificationPolicyService.saveUserPolicy($scope.activePolicy, Session.userId).then(function(data) {
          $log.debug("Promise returned Policy data", data);
        }, function(error) {
          $log.error("Unable to save Policy: ", error);
        });
      };

      $scope.editorLoaded = function(_editor) {
        $log.debug("editor loaded");

        // Populate with User's Personal policy

        // Editor part
        var _session = _editor.getSession();
        var _renderer = _editor.renderer;

        $scope._session = _session;
        $scope._renderer = _renderer;

        // Set read-only until the policy is loaded
        _editor.setReadOnly(true);
        _session.setUndoManager(new ace.UndoManager());
        _renderer.setShowGutter(true);

        // Fetch the policy from the server
        NotificationPolicyService.getDefaultUserPolicy(Session.userId).then(function(data) {
          $log.debug("Promise returned Policy data", data);
          _editor.setReadOnly(false);
          _editor.setValue(data.policySource, -1);
          $scope.activePolicy = data;
        }, function(error) {
          $log.error("Unable to get Policy: ", error);
          // Probably want to open a modal here, or may be retry
          $scope.activePolicy = null;
          $scope._session = null;
          $scope._renderer = null;
        });

        // Events
        _editor.on("changeSession", function() {
          // Not sure when this is called
          $log.debug("editor session change");
        });

        _session.on("change", function() {
          // Called whenever the content of the editor changes.
          if ($scope.activePolicy !== null) {
            $scope.activePolicy.policySource = _editor.getValue();
          }
        });
      };
    }
  ]);

}).call(this);
