
class CreateRoleCtrl

    constructor: (@$log, @$location, @RoleService) ->
        @$log.debug "constructing CreateRoleController"
        @role = {}

    createRole: () ->
        @$log.debug "createRole()"
        @role.active = true
        @RoleService.createRole(@role)
        .then(
            (data) =>
                @$log.debug "Promise returned #{data} Role"
                @role = data
                @$location.path("/admin")
            ,
            (error) =>
                @$log.error "Unable to create Role: #{error}"
            )

controllersModule.controller('CreateRoleCtrl', CreateRoleCtrl)
