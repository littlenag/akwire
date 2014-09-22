(function() {

  servicesModule.service('RuleService', [
    '$log', '$http', '$q', '$resource', 'Session', function($log, $http, $q, $resource, Session) {
      var service = {};

      $log.debug("constructing RuleService");

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      service.getRules = function() {
        $log.debug("getRules()");
        var deferred = $q.defer();
        $http.get("/teams/" + Session.teamId).success(function(data, status, headers) {
          $log.info("Successfully listed Rules - status " + status);
          return deferred.resolve(data.rules);
        }).error(function(data, status, headers) {
          $log.error("Failed to list Rules - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.getRule = function(ruleId) {
        $log.debug("getRule('" + ruleId + "')");
        var deferred = $q.defer();
        $http.get("/rules/" + ruleId).success(function(data, status, headers) {
          $log.info("Successfully retrieved Rule - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to retrieve Rule - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.createRule = function(rule) {
        $log.debug("createRule " + (angular.toJson(rule, true)));
        var deferred = $q.defer();

        rule.team = Session.teamId;

        $http.post("/teams/" + Session.teamId + '/rules', rule).success(function(data, status, headers) {
          $log.info("Successfully created Rule - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to create rule - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.updateRule = function(rule) {
        $log.debug("updateRule " + (angular.toJson(rule, true)));
        var deferred = $q.defer();
        $http.post("/rules/" + rule._id, rule).success(function(data, status, headers) {
          $log.info("Successfully updated Rule - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to update Rule - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
