
class AgentCtrl

    constructor: (@$log, @AgentService) ->
        @$log.debug "constructing AgentController"
        @agents = []
        @getAllAgents()

    getAllAgents: () ->
        @$log.debug "getAllAgents()"

        @AgentService.listAgents()
        .then(
            (data) =>
                @$log.debug "Promise returned #{data.length} Agents"
                @agents = data
            ,
            (error) =>
                @$log.error "Unable to get Agents: #{error}"
            )


controllersModule.controller('AgentCtrl', AgentCtrl)