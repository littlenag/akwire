(function() {
  "use strict";

  servicesModule.service('NotificationService', ['$log', '$http', '$q',
    function($log, $http, $q) {

      $log.debug("constructing NotificationService");

      var service = {};

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.saveNotification = function(notification) {
        $log.debug("createNotification " + (angular.toJson(notification, true)));

        var deferred = $q.defer();

        $http.post('/notification/create', notification).
          success(function(data, status, headers) {
            $log.info("Successfully created Notification - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to create notification - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.getNotification = function(notificationId) {
        $log.debug("getNotification('" + notificationId + "')");

        var deferred = $q.defer();

        $http.get("/notification", {params : {notificationId: notificationId}}).
          success(function(data, status, headers) {
            $log.info("Successfully retrieved Notification - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to retrieve Notification - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      // ack, suppress (single, any from host, any from rule), resolve, archive

      service.ackNotification = function(notification) {
        $log.debug("ACK Notification " + (angular.toJson(notification, true)));

        var deferred = $q.defer();

        $http.post("/notification", notification).
          success(function(data, status, headers) {
            $log.info("Successfully updated Notification - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to update Notification - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
