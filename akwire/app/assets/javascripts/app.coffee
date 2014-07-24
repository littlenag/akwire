
dependencies = [
    'ngRoute',
    'ui.bootstrap',
    'akwire.filters',
    'akwire.services',
    'akwire.controllers',
    'akwire.directives',
    'akwire.common',
    'akwire.routeConfig'
]

app = angular.module('akwire', dependencies)

angular.module('akwire.routeConfig', ['ngRoute'])
    .config ($routeProvider) ->
        # create, list/base, edit, detail
        $routeProvider
            .when('/', {
                templateUrl: '/assets/partials/agents/list.html'
            })
            .when('/agents', {
                templateUrl: '/assets/partials/agents/list.html'
            })
            .when('/incidents', {
                templateUrl: '/assets/partials/incidents/list.html'
            })
            .when('/alert-rules', {
                templateUrl: '/assets/partials/rules/list.html'
            })
            .when('/admin', {
                templateUrl: '/assets/partials/admin/base.html'
            })
            .when('/admin/roles', {
                templateUrl: '/assets/partials/roles/list.html'
            })
            .when('/admin/roles/create', {
                templateUrl: '/assets/partials/roles/create.html'
            })
            .when('/admin/roles/:roleId', {
                templateUrl: '/assets/partials/roles/edit.html'
            })
            .when('/admin/users', {
                templateUrl: '/assets/partials/users/list.html'
            })
            .when('/admin/users/create', {
                templateUrl: '/assets/partials/users/create.html'
            })
            .when('/wiki', {
                templateUrl: '/assets/partials/wiki/base.html'
            })
            .otherwise({redirectTo: '/'})

@commonModule = angular.module('akwire.common', [])
@controllersModule = angular.module('akwire.controllers', [])
@servicesModule = angular.module('akwire.services', [])
@modelsModule = angular.module('akwire.models', [])
@directivesModule = angular.module('akwire.directives', [])
@filtersModule = angular.module('akwire.filters', [])
