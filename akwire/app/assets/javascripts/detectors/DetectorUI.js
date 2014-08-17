(function() {

    angular.module('akwire.ui.detectors', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('admin.detector', {
              url: "/detectors",
              abstract: true,
              templateUrl: "/assets/partials/detectors/base.html",
            })
            .state('admin.detector.list', {
              url: "",
              templateUrl: "/assets/partials/detectors/list.html",
            })
            .state('admin.detector.create', {
              url: "/create",
              templateUrl: "/assets/partials/detectors/create.html",
            })
            .state('admin.detector.edit', {
              url: "/edit/:detectorId",
              templateUrl: "/assets/partials/detectors/edit.html",
            });
        }
    ]);

}).call(this);