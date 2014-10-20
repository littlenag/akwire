(function() {

  controllersModule.controller('ListRuleCtrl', [
    '$scope', '$log', 'RuleService', function($scope, $log, RuleService) {
      $log.debug("constructing ListRuleController");
      $scope.rules = [];

      $scope.getAllRules = function() {
        $log.debug("getAllRules()");
        return RuleService.getRules().then(function(data) {
          $log.debug("Promise returned " + data.length + " Rules");
          $scope.rules = data;
          return data;
        }, function(error) {
          return $log.error("Unable to get Rules: " + error);
        });
      };

      $scope.getAllRules();
    }
  ]);

  controllersModule.controller('EditRuleCtrl', [
    '$scope', '$log', '$state', 'RuleService', function($scope, $log, $state, RuleService) {

      $log.debug("constructing EditRuleController");
      $scope.rule = null;
      $scope.ruleId = $state.params.ruleId;

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

      $scope.createRule = function() {
        $log.debug("createRule()");
        $scope.rule.active = true;

        return RuleService.createRule($scope.rule).then(function(data) {
          $log.debug("Promise returned " + data + " Rule");
          $scope.rule = data;
          return $state.go("admin.rules.list", {});
        }, function(error) {
          return $log.error("Unable to create Rule: " + error);
        });
      };
    }
  ]);

}).call(this);
