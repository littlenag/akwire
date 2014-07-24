
controllersModule.controller 'RoleCtrl', [ '$log', 'RoleService', ($log, RoleService) ->
  new class RoleCtrl
    constructor: () ->
        $log.debug "constructing RoleController"
        @roles = []
        @getAllRoles()

    getAllRoles: () ->
        $log.debug "getAllRoles()"

        RoleService.getRoles()
        .then(
            (data) =>
                $log.debug "Promise returned #{data.length} Roles"
                @roles = data
            ,
            (error) =>
                $log.error("Unable to get Roles: #{error}")
        )
]
