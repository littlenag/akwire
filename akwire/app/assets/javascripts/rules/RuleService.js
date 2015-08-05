(function() {

  servicesModule.service('RuleService', [
    '$log', '$http', '$q', '$resource', 'Session', function($log, $http, $q, $resource, Session) {
      var service = {};

      $log.debug("constructing RuleService");

      service.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      };

      function makeRuleUrl(entityType) {
        if (entityType === "user") {
          return "/rules/user-" + Session.userId;
        } else if (entityType === "team") {
          return "/rules/team-" + Session.currentTeamId;
        } else {
          throw "Bad entity type" + entityType;
        }
      }

      service.getUserRules = function() {
        $log.debug("getRules()");
        var deferred = $q.defer();
        $http.get(makeRuleUrl("user")).success(function(data, status, headers) {
          $log.info("Successfully listed Rules", data);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to list Rules - status ", status, Session.userId);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.getTeamRules = function() {
        $log.debug("getRules()");
        var deferred = $q.defer();
        $http.get(makeRuleUrl("team")).success(function(data, status, headers) {
          $log.info("Successfully listed Rules", data);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to list Rules - status ", status, Session.currentTeamId);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.getUserRule = function(ruleId) {
        $log.debug("getRule('" + ruleId + "')");
        var deferred = $q.defer();
        $http.get(makeRuleUrl("user")).success(function(data, status, headers) {
          $log.info("Successfully retrieved Rule - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to retrieve Rule - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.getTeamRule = function(ruleId) {
        $log.debug("getRule('" + ruleId + "')");
        var deferred = $q.defer();
        $http.get(makeRuleUrl("team")).success(function(data, status, headers) {
          $log.info("Successfully retrieved Rule - status " + status);
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          $log.error("Failed to retrieve Rule - status " + status);
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.createSimpleThresholdRule = function(rule, entityType) {

        function pad(n, width, z) {
          z = z || '0';
          n = n + '';
          return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
        }

        var st = {
          id: pad(0, 24),
          owner: {
            id: Session.userId,
            scope: entityType.toUpperCase()
          },
          name : rule.name,
          builderClass : "resources.rules.SimpleThreshold",

          params : {
            threshold : rule.threshold,
            op : rule.op
          },
          streamExpr : {
            instance : rule.stream.instance,
            host : rule.stream.host,
            observer : rule.stream.observer,
            key : rule.stream.key
          },
          active : true,
          impact : "IL_5"
        };

        $log.debug("ruleservice::createSimpleThresholdRule", st);

        var deferred = $q.defer();

        $http.post(makeRuleUrl(entityType), st).success(function(data, status, headers) {
          return deferred.resolve(data);
        }).error(function(data, status, headers) {
          return deferred.reject(data);
        });
        return deferred.promise;
      };

      service.updateRule = function(rule) {
        $log.debug("updateRule " + (angular.toJson(rule, true)));
        var deferred = $q.defer();
        $http.post(makeRuleUrl("user") + "/" + rule._id, rule).success(function(data, status, headers) {
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
