(function() {
  "use strict";

  console.log("inside " + servicesModule);

  servicesModule.service('AgentService', ['$log', '$http', '$q',
    function($log, $http, $q) {
      $log.debug("constructing AgentService");

      var service = {};

      service.headers = {'Accept': 'application/json', 'Content-Type': 'application/json'};
      service.defaultConfig = { headers: service.headers };

      service.listAgents = function() {
        $log.debug("listAgents()");
        var deferred = $q.defer();

        $http.get("/agents").success(function (data, status, headers) {
                $log.info("Successfully listed Agents - status #{status}");
                deferred.resolve(data);
        }).error(function (data, status, headers) {
                $log.error("Failed to list Agents - status #{status}");
                deferred.reject(data);
        });

        return deferred.promise;
      };

      return service;
    }
  ]);

}).call(this);
