(function() {

  controllersModule.controller('ListRuleCtrl', [
    '$scope', '$log', 'RuleService', function($scope, $log, RuleService) {
      $log.debug("constructing ListRuleController");
      $scope.userRules = [];
      $scope.teamRules = [];

      $scope.getUserRules = function() {
        $log.debug("getAllRules()");
        return RuleService.getUserRules().then(function(data) {
          $log.debug("Promise returned rules", data);
          $scope.userRules = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Rules: " + error);
        });
      };

      $scope.getTeamRules = function() {
        $log.debug("getAllRules()");
        return RuleService.getTeamRules().then(function(data) {
          $log.debug("Promise returned rules", data);
          $scope.teamRules = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Rules: " + error);
        });
      };

      $scope.getUserRules();
      $scope.getTeamRules();
    }
  ]);

  controllersModule.controller('EditRuleCtrl', [
    '$scope', '$log', '$state', 'RuleService', function($scope, $log, $state, RuleService) {

      $log.debug("constructing EditRuleController");
      $scope.rule = null;
      $scope.ruleId = $state.params.ruleId;
      $scope.ruleScope = $state.params.ruleScope;

      RuleService.getRule($scope.ruleId).then(function(data) {
        $log.debug("Promise returned Rule(" + $scope.ruleId + ")");
        $scope.rule = data;
        return data;
      }, function(error) {
        return $log.error("Unable to get Rule(" + $scope.ruleId + ": " + error);
      });

      $scope.updateRule = function() {
        $log.debug("updateRule()");
        $scope.rule.active = true;
        return RuleService.updateRule($scope.rule).then(function(data) {
          $log.debug("Promise returned " + data + " Rule");
          $scope.rule = data;
          return $state.go("admin.rules.list", {});
        }, function(error) {
          return $log.error("Unable to update Rule: " + error);
        });
      };
    }
  ]);

  controllersModule.controller('CreateRuleCtrl', [
    '$scope', '$log', '$state', 'RuleService', function($scope, $log, $state, RuleService) {

      $log.debug("constructing CreateRuleController");
      $scope.rule = {};

      $scope.createRule = function(entityType) {
        $log.debug("createRule()");
        $scope.rule.active = true;

        return RuleService.createSimpleThresholdRule($scope.rule, entityType).then(function(data) {
          $log.debug("Promise returned Rule", data);
          $scope.rule = data;
          return $state.go("configure.rules.list", {});
        }, function(error) {
          return $log.error("Unable to create Rule: " + error);
        });
      };
    }
  ]);

}).call(this);
