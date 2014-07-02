
class RoleCtrl

    constructor: (@$log, @RoleService) ->
        @$log.debug "constructing RoleController"
        @roles = []
        @getAllRoles()

    getAllRoles: () ->
        @$log.debug "getAllRoles()"

        @RoleService.listRoles()
        .then(
            (data) =>
                @$log.debug "Promise returned #{data.length} Roles"
                @roles = data
            ,
            (error) =>
                @$log.error "Unable to get Roles: #{error}"
            )


controllersModule.controller('RoleCtrl', RoleCtrl)