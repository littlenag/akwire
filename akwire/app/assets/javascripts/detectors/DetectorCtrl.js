(function() {

  controllersModule.controller('ListDetectorCtrl', [
    "$scope", '$log',  'DetectorService',
    function($scope, $log, DetectorService) {
      $log.debug("constructing ListDetectorController");
      $scope.detectors = [];

      $scope.getAllDetectors = function() {
        $log.debug("getAllDetectors()");
        return DetectorService.getDetectors().then(function(data) {
          $log.debug("Promise returned " + data.length + " Detectors");
          return $scope.detectors = data;
        }, function(error) {
          return $log.error("Unable to get Detectors: " + error);
        });
      };

      $scope.getAllDetectors()
    }
  ]);

  controllersModule.controller('EditDetectorCtrl', [
    '$scope', '$log', '$state', 'DetectorService',
    function($scope, $log, $state, DetectorService) {

      $log.debug("constructing EditDetectorController");
      $scope.detector = null;
      $scope.detectorId = $state.params.detectorId;

      DetectorService.getDetector($scope.detectorId).then(function(data) {
        $log.debug("Promise returned Detector(" + $scope.detectorId + ")");
        return $scope.detector = data;
      }, function(error) {
        return $log.error("Unable to get Detector(" + $scope.detectorId + ": " + error);
      });

      $scope.updateDetector = function() {
        $log.debug("updateDetector()");
        $scope.detector.active = true;
        return DetectorService.updateDetector($scope.detector).then(function(data) {
          $log.debug("Promise returned " + data + " Detector");
          $scope.detector = data;
          return $state.go("admin.detector.list", {});
        }, function(error) {
          return $log.error("Unable to update Detector: " + error);
        });
      };
    }
  ]);

  controllersModule.controller('CreateDetectorCtrl', [
    '$scope', '$log', '$state', 'DetectorService',
    function($scope, $log, $state, DetectorService) {
      $log.debug("constructing CreateDetectorController");

      $scope.detector = {};

      $scope.createDetector = function() {
        $log.debug("createDetector()");
        this.detector.active = true;
        return DetectorService.createDetector($scope.detector).then(function(data) {
          $log.debug("Promise returned " + data + " Detector");
          $scope.detector = data;
          $state.go("admin.detector.list", {});
          return data;
        }, function(error) {
          $log.error("Unable to create Detector: " + error);
          return null;
        });
      };
    }
  ]);

}).call(this);
