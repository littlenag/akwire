(function() {

  controllersModule.controller('ListDetectorCtrl', [
    '$log', 'DetectorService', function($log, DetectorService) {
      var ListDetectorCtrl;
      return new (ListDetectorCtrl = (function() {

        function ListDetectorCtrl() {
          $log.debug("constructing ListDetectorController");
          this.monEngines = [];
          this.getAllDetectors();
        }

        ListDetectorCtrl.prototype.getAllDetectors = function() {
          var _this = this;
          $log.debug("getAllDetectors()");
          return DetectorService.getDetectors().then(function(data) {
            $log.debug("Promise returned " + data.length + " Detectors");
            return _this.monEngines = data;
          }, function(error) {
            return $log.error("Unable to get Detectors: " + error);
          });
        };

        return ListDetectorCtrl;

      })());
    }
  ]);

  controllersModule.controller('EditDetectorCtrl', [
    '$scope', '$log', '$state', 'DetectorService', function($scope, $log, $state, DetectorService) {

      $log.debug("constructing EditDetectorController");
      $scope.monEngine = null;
      $scope.monEngineId = $state.params.monEngineId;

      DetectorService.getDetector($scope.monEngineId).then(function(data) {
        $log.debug("Promise returned Detector(" + $scope.monEngineId + ")");
        return $scope.monEngine = data;
      }, function(error) {
        return $log.error("Unable to get Detector(" + $scope.monEngineId + ": " + error);
      });

      $scope.updateDetector = function() {
        $log.debug("updateDetector()");
        $scope.monEngine.active = true;
        return DetectorService.updateDetector($scope.monEngine).then(function(data) {
          $log.debug("Promise returned " + data + " Detector");
          $scope.monEngine = data;
          return $state.go("admin.monEngine.list", {});
        }, function(error) {
          return $log.error("Unable to update Detector: " + error);
        });
      };
    }
  ]);

  controllersModule.controller('CreateDetectorCtrl', [
    '$log', '$location', 'DetectorService', function($log, $location, DetectorService) {
      var CreateDetectorCtrl;
      return new (CreateDetectorCtrl = (function() {

        function CreateDetectorCtrl() {
          $log.debug("constructing CreateDetectorController");
          this.monEngine = {};
        }

        CreateDetectorCtrl.prototype.createDetector = function() {
          var _this = this;
          $log.debug("createDetector()");
          this.monEngine.active = true;
          return DetectorService.createDetector(this.monEngine).then(function(data) {
            $log.debug("Promise returned " + data + " Detector");
            _this.monEngine = data;
            return $location.path("/admin/monEngine");
          }, function(error) {
            return $log.error("Unable to create Detector: " + error);
          });
        };

        return CreateDetectorCtrl;

      })());
    }
  ]);

}).call(this);
