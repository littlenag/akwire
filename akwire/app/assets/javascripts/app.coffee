
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
        $routeProvider
            .when('/', {
                templateUrl: '/assets/partials/view-agents.html'
            })
            .when('/agents', {
                templateUrl: '/assets/partials/view-agents.html'
            })
            .when('/incidents', {
                templateUrl: '/assets/partials/view-incidents.html'
            })
            .when('/alert-rules', {
                templateUrl: '/assets/partials/view-rules.html'
            })
            .when('/admin', {
                templateUrl: '/assets/partials/view-admin.html'
            })
            .when('/wiki', {
                templateUrl: '/assets/partials/view-wiki.html'
            })
            .when('/users/create', {
                templateUrl: '/assets/partials/create.html'
            })
            .otherwise({redirectTo: '/'})

@commonModule = angular.module('akwire.common', [])
@controllersModule = angular.module('akwire.controllers', [])
@servicesModule = angular.module('akwire.services', [])
@modelsModule = angular.module('akwire.models', [])
@directivesModule = angular.module('akwire.directives', [])
@filtersModule = angular.module('akwire.filters', [])