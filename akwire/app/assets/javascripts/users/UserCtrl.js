(function() {
  var UserCtrl;

  UserCtrl = (function() {

    function UserCtrl($log, UserService) {
      this.$log = $log;
      this.UserService = UserService;
      this.$log.debug("constructing UserController");
      this.users = [];
      this.getAllUsers();
    }

    UserCtrl.prototype.getAllUsers = function() {
      var _this = this;
      this.$log.debug("getAllUsers()");
      return this.UserService.listUsers().then(function(data) {
        _this.$log.debug("Promise returned " + data.length + " Users");
        return _this.users = data;
      }, function(error) {
        return _this.$log.error("Unable to get Users: " + error);
      });
    };

    return UserCtrl;

  })();

  controllersModule.controller('UserCtrl', UserCtrl);

}).call(this);
