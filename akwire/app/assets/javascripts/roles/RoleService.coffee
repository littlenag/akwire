
servicesModule.service 'RoleService', [ '$log', '$http', '$q', ($log, $http, $q) ->
  new class RoleService

    @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    @defaultConfig = { headers: @headers }

    constructor: ->
        $log.debug "constructing RoleService"

    getRoles: () ->
        $log.debug "getRoles()"
        deferred = $q.defer()

        $http.get("/roles")
        .success((data, status, headers) =>
                $log.info("Successfully listed Roles - status #{status}")
                deferred.resolve(data)
            )
        .error((data, status, headers) =>
                $log.error("Failed to list Roles - status #{status}")
                deferred.reject(data);
            )
        deferred.promise

    getRole: (roleId) ->
        $log.debug "getRole('#{roleId}')"
        deferred = $q.defer()

        $http.get("/roles/#{roleId}")
        .success((data, status, headers) =>
                $log.info("Successfully retrieved Role - status #{status}")
                deferred.resolve(data)
            )
        .error((data, status, headers) =>
                $log.error("Failed to retrieve Role - status #{status}")
                deferred.reject(data);
            )
        deferred.promise

    createRole: (role) ->
        $log.debug "createRole #{angular.toJson(role, true)}"
        deferred = $q.defer()

        $http.post('/roles', role)
        .success((data, status, headers) =>
                $log.info("Successfully created Role - status #{status}")
                deferred.resolve(data)
            )
        .error((data, status, headers) =>
                $log.error("Failed to create role - status #{status}")
                deferred.reject(data);
            )
        deferred.promise

    updateRole: (role) ->
        $log.debug "updateRole #{angular.toJson(role, true)}"
        deferred = $q.defer()

        $http.post('/roles/#{role.id}', role)
        .success((data, status, headers) =>
                $log.info("Successfully updated Role - status #{status}")
                deferred.resolve(data)
            )
        .error((data, status, headers) =>
                $log.error("Failed to update Role - status #{status}")
                deferred.reject(data);
            )
        deferred.promise

]