/**
 * View: TabView
 *
 * TabView
 *
 *
 */
(function ($) {
    brite.registerView("TabView", {emptyParent: false, parent: ".ContentView"},
        {
            create: function (data, config) {
                return render("TabView");
            },

            postDisplay: function (data) {
                //if have labels should show
                //open content view

            },
            events: {
                "btap; li": function(event){
                   var searchView = true, view = this;
                   var $li = $(event.currentTarget);
                   if($li.hasClass("favLabel") || $li.hasClass("search")){
                       if($li.hasClass("favLabel")){
                           searchView = false;
                       }
                       view.$el.find("li button").removeClass("active");
                       $li.find("button").addClass("active");
                       if(searchView){
                           $li.trigger("RESTORE_SEARCH_VIEW");
                       }else {
                           $li.trigger("CHANGE_TO_FAV_VIEW", {id: $li.attr("data-label-id")});
                       }
                   }

                }
            },
            docEvents: {}
        });
})(jQuery);
