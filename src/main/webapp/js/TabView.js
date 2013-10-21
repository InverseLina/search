/**
 * View: TabView
 *
 * TabView
 *
 *
 */
(function ($) {
    var dao = app.LabelDaoHandler;
    brite.registerView("TabView", {emptyParent: false, parent: ".ContentView"},
        {
            // --------- Implement Interface--------- //
            create: function (data, config) {
                return render("TabView");
            },

            postDisplay: function (data) {
                var view = this;
                dao.list().done(function (labels) {
                    if ((labels || []).length > 0) {

                    labels =  $.grep(labels, function (label, idx) {
                            if (label.name == "Favorites") {
                                view.$el.find("li.favLabel").attr("data-label-id", label.id);
                                return false;
                            } else {
                                return true;
                            }
                        })
                        if (labels.length > 0) {
                            var html = render("TabView-insert-labels", {labels: labels });
                            view.$el.find(".addFav").before(html);
                        }
                    } else {
                        dao.save("Favorites").done(function (id) {
                            view.$el.find("li.favLabel").attr("data-label-id", id);
                        });
                    }
                })

            },
            // --------- /Implement Interface--------- //
            getSelectLabel: function(){
               var view = this;
               var $i = view.$el.find("li i.select");
               var $li = $i.closest("li");
               if($i.length > 0 ){
                   return {id: $li.attr("data-label-id"), name: $.trim($li.text())};
               }else {
                   return null;
               }
            },
            events: {
                "btap; li": function(event){
                    event.stopPropagation();
                    event.preventDefault();
                   var searchView = true, view = this;
                   var $li = $(event.currentTarget);
                   if($li.hasClass("favLabel") || $li.hasClass("search")){
                       if($li.hasClass("favLabel")){
                           searchView = false;
                       }
                       view.$el.find("li").removeClass("active");
                       $li.addClass("active");
                       if(searchView){
                           $li.trigger("RESTORE_SEARCH_VIEW");
                       }else {
                           $li.trigger("CHANGE_TO_FAV_VIEW", {id: $li.attr("data-label-id")});
                       }
                   }

                },
                PATH_INFO_CHANGE: function(event, extra){

                },
                "btap; li i": function(event){
                    var view = this;
                    event.stopPropagation();
                    event.preventDefault();
                    view.$el.find("li i").removeClass("select");
                    $(event.currentTarget).addClass("select");
                    view.$el.trigger("CHANGE_SELECT_LABEL");

                },
                "keyup; input": function(event){
                    var view = this;
                    var $input = $(event.currentTarget);
                    if(event.keyCode == 13){
                        var name = $.trim($input.val());
                        if(name.length > 0) {
                            dao.save(name).done(function(id){
                               if( $.isNumeric(id)){
                                    var html = render("TabView-new-label", {id:id, name:name});
                                    $input.closest("li").before(html);
                                    $input.val("").focus();
                                }

                            })
                        }
                    }
                }
            },
            docEvents: {}
        });
})(jQuery);
