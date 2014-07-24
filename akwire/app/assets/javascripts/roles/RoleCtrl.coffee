
controllersModule.controller 'ListRoleCtrl', [ '$log', 'RoleService', ($log, RoleService) ->
  new class ListRoleCtrl
    constructor: () ->
        $log.debug "constructing ListRoleController"
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

controllersModule.controller 'EditRoleCtrl', [ '$log', '$location', '$routeParams', 'RoleService', ($log, $location, $routeParams, RoleService) ->
  new class EditRoleCtrl
    constructor: () ->
        $log.debug "constructing EditRoleController"
        @role = null
        @roleId = $routeParams.roleId
        RoleService.getRole(@roleId)
        .then(
            (data) =>
                $log.debug "Promise returned Role(#{@roleId})"
                @role = data
            ,
            (error) =>
                $log.error("Unable to get Role(#{@roleId}: #{error}")
        )

    updateRole: () ->
        $log.debug "updateRole()"
        @role.active = true
        RoleService.updateRole(@role)
        .then(
            (data) =>
                $log.debug "Promise returned #{data} Role"
                @role = data
                $location.path("/admin/roles/#{@roleId}")
            ,
            (error) =>
                $log.error("Unable to update Role: #{error}")
        )
]

controllersModule.controller 'CreateRoleCtrl', [ '$log', '$location', 'RoleService', ($log, $location, RoleService) ->
  new class CreateRoleCtrl

    constructor: () ->
        $log.debug "constructing CreateRoleController"
        @role = {}

    createRole: () ->
        $log.debug "createRole()"
        @role.active = true
        @RoleService.createRole(@role)
        .then(
            (data) =>
                $log.debug "Promise returned #{data} Role"
                @role = data
                $location.path("/admin")
            ,
            (error) =>
                $log.error "Unable to create Role: #{error}"
        )
]
