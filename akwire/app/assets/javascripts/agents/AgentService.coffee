
class AgentService

    @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    @defaultConfig = { headers: @headers }

    constructor: (@$log, @$http, @$q) ->
        @$log.debug "constructing AgentService"

    listAgents: () ->
        @$log.debug "listAgents()"
        deferred = @$q.defer()

        @$http.get("/agents")
        .success((data, status, headers) =>
                @$log.info("Successfully listed Agents - status #{status}")
                deferred.resolve(data)
            )
        .error((data, status, headers) =>
                @$log.error("Failed to list Agents - status #{status}")
                deferred.reject(data);
            )
        deferred.promise

servicesModule.service('AgentService', AgentService)
