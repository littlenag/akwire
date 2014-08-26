(function() {

  servicesModule.service('RuleService', [
    '$log', '$http', '$q', function($log, $http, $q) {
      var RuleService;
      return new (RuleService = (function() {

        RuleService.headers = {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        };

        RuleService.defaultConfig = {
          headers: RuleService.headers
        };

        function RuleService() {
          $log.debug("constructing RuleService");
        }

        RuleService.prototype.getRules = function() {
          var deferred,
            _this = this;
          $log.debug("getRules()");
          deferred = $q.defer();
          $http.get("/rules").success(function(data, status, headers) {
            $log.info("Successfully listed Rules - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to list Rules - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        RuleService.prototype.getRule = function(ruleId) {
          var deferred,
            _this = this;
          $log.debug("getRule('" + ruleId + "')");
          deferred = $q.defer();
          $http.get("/rules/" + ruleId).success(function(data, status, headers) {
            $log.info("Successfully retrieved Rule - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to retrieve Rule - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        RuleService.prototype.createRule = function(rule) {
          var deferred,
            _this = this;
          $log.debug("createRule " + (angular.toJson(rule, true)));
          deferred = $q.defer();
          $http.post('/rules', rule).success(function(data, status, headers) {
            $log.info("Successfully created Rule - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to create rule - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        RuleService.prototype.updateRule = function(rule) {
          var deferred,
            _this = this;
          $log.debug("updateRule " + (angular.toJson(rule, true)));
          deferred = $q.defer();
          $http.post("/rules/" + rule._id, rule).success(function(data, status, headers) {
            $log.info("Successfully updated Rule - status " + status);
            return deferred.resolve(data);
          }).error(function(data, status, headers) {
            $log.error("Failed to update Rule - status " + status);
            return deferred.reject(data);
          });
          return deferred.promise;
        };

        return RuleService;

      })());
    }
  ]);

}).call(this);
