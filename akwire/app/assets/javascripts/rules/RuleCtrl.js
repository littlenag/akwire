(function() {

  controllersModule.controller('ListRuleCtrl', [
    '$log', 'RuleService', function($log, RuleService) {
      var ListRuleCtrl;
      return new (ListRuleCtrl = (function() {

        function ListRuleCtrl() {
          $log.debug("constructing ListRuleController");
          this.rules = [];
          this.getAllRules();
        }

        ListRuleCtrl.prototype.getAllRules = function() {
          var _this = this;
          $log.debug("getAllRules()");
          return RuleService.getRules().then(function(data) {
            $log.debug("Promise returned " + data.length + " Rules");
            return _this.rules = data;
          }, function(error) {
            return $log.error("Unable to get Rules: " + error);
          });
        };

        return ListRuleCtrl;

      })());
    }
  ]);

  controllersModule.controller('EditRuleCtrl', [
    '$scope', '$log', '$state', 'RuleService', function($scope, $log, $state, RuleService) {

      $log.debug("constructing EditRuleController");
      $scope.rule = null;
      $scope.ruleId = $state.params.ruleId;

      RuleService.getRule($scope.ruleId).then(function(data) {
        $log.debug("Promise returned Rule(" + $scope.ruleId + ")");
        return $scope.rule = data;
      }, function(error) {
        return $log.error("Unable to get Rule(" + $scope.ruleId + ": " + error);
      });

      $scope.updateRule = function() {
        $log.debug("updateRule()");
        $scope.rule.active = true;
        return RuleService.updateRule($scope.rule).then(function(data) {
          $log.debug("Promise returned " + data + " Rule");
          $scope.rule = data;
          return $state.go("admin.rule.list", {});
        }, function(error) {
          return $log.error("Unable to update Rule: " + error);
        });
      };
    }
  ]);

  controllersModule.controller('CreateRuleCtrl', [
    '$log', '$location', 'RuleService', function($log, $location, RuleService) {
      var CreateRuleCtrl;
      return new (CreateRuleCtrl = (function() {

        function CreateRuleCtrl() {
          $log.debug("constructing CreateRuleController");
          this.rule = {};
        }

        CreateRuleCtrl.prototype.createRule = function() {
          var _this = this;
          $log.debug("createRule()");
          this.rule.active = true;
          return RuleService.createRule(this.rule).then(function(data) {
            $log.debug("Promise returned " + data + " Rule");
            _this.rule = data;
            return $location.path("/admin/rule");
          }, function(error) {
            return $log.error("Unable to create Rule: " + error);
          });
        };

        return CreateRuleCtrl;

      })());
    }
  ]);

}).call(this);
