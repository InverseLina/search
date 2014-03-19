(function($) {
    var perfSearchDao = app.PerfDaoHandler;

    brite.registerView("PerfView", {
        parent : ".admincontainer",
        emptyParent : true
    }, {
        // --------- View Interface Implement--------- //
        create : function(data) {
           var dfd = $.Deferred();
           app.getJsonData("/perf").done(function(result){
        	   dfd.resolve(render("PerfView",{data:result}));
           });
           return dfd.promise();
        }
        // --------- /View Interface Implement--------- //

    });
})(jQuery); 