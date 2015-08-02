(function() {
  "use strict";

  servicesModule.service('NotificationPolicyService', ['$log', '$http', '$q',
    function($log, $http, $q) {

      $log.debug("constructing NotificationPolicyService");

      var service = {};

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.saveUserPolicy = function(policy) {
        $log.debug("saveUserPolicy ", policy);

        var deferred = $q.defer();

        $http.put('/policies/user', policy).
          success(function(data, status, headers) {
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      service.getUserPolicy = function(policyId) {
        $log.debug("getUserPolicy('" + policyId + "')");

        var deferred = $q.defer();

        $http.get("/notification", {params : {notificationId: policyId}}).
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

      service.getDefaultUserPolicy = function() {
        $log.debug("getDefaultUserPolicy()");

        var deferred = $q.defer();

        $http.get("/policies/user/default").
          success(function(data, status, headers) {
            $log.info("Successfully retrieved (default) Policy - status " + status);
            return deferred.resolve(data);
          }).
          error(function(data, status, headers) {
            $log.error("Failed to retrieve (default) Policy - status " + status);
            return deferred.reject(data);
          });

        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
