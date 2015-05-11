(function() {

    angular.module('akwire.ui.detectors', ['ui.router']).config(
    [ '$stateProvider', '$urlRouterProvider',
      function($stateProvider, $urlRouterProvider) {

        $stateProvider
            .state('admin.detector', {
              url: "/detectors",
              abstract: true,
              templateUrl: "/assets/javascripts/detectors/base.html",
            })
            .state('admin.detector.list', {
              url: "",
              templateUrl: "/assets/javascripts/detectors/list.html",
            })
            .state('admin.detector.create', {
              url: "/create",
              templateUrl: "/assets/javascripts/detectors/create.html",
            })
            .state('admin.detector.edit', {
              url: "/edit/:detectorId",
              templateUrl: "/assets/javascripts/detectors/edit.html",
            });
        }
    ]);

}).call(this);