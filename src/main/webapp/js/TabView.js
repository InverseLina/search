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
            	data = data || {};
            	var $e = $(render("TabView"));
            	if(data.hide){
            		$e.hide();
            	}
                return $e;
            },

            postDisplay: function (data) {
                var view = this;
                dao.list().done(function (labels) {
                    if ((labels || []).length > 0) {

                    labels =  $.grep(labels, function (label, idx) {
                            if (label.name == "Favorites") {
                                view.$el.find("li.favLabel").attr("data-label-id", label.id);
                                view.$el.find("li.favLabel a").attr("href", "#/list/" +label.id);
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
                            view.$el.find("li.Favorites").attr("data-label-id", id);
                            view.$el.find("li.Favorites a").attr("href", "#/list/" +id);
                        });
                    }
                    view.$el.trigger("PATH_INFO_CHANGE", view.pathInfo);
                    var offset = view.$el.offset();
                    var bodyWidth = $("body").width();
                    view.$el.css({"max-width":bodyWidth - 180 - offset.left})
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
                   return {name:"Favorites"};
               }
            },
            getLabelName: function(id){
               var view = this;
               var  $li = view.$el.find("li[data-label-id='" + id + "']");
                if($li.length >0){
                    return $.trim($li.text());
                }else{
                    return "Favorites";
                }
            },
            events: {
                "click; li.search": function(event){
                    var view = this;
                    event.stopPropagation();
                    event.preventDefault();
                    var $li = $(event.currentTarget);
                    changeView.call(view, $li);
                },
                PATH_INFO_CHANGE: function(event, extra){
                    var view = this;
                    var path = null;
                    if(extra && extra.paths && extra.paths.length == 3 && extra.paths[1] ==  "list"){
                         path =  extra.paths[2];
                        changeView.call(view,path);
                    }

                },
                "click; li i": function(event){
                    var view = this;
                    event.stopPropagation();
                    event.preventDefault();
                    view.$el.find("li i").removeClass("select");
                    $(event.currentTarget).addClass("select");
                    view.$el.trigger("CHANGE_SELECT_LABEL");

                },
                "keyup; .addText": function(event){
                    var view = this;
                    var $input = $(event.currentTarget);
                    if(event.keyCode == 13){
                        var name = $.trim($input.val());
                        if(name != "" && name.length > 0) {
                            dao.save(name).done(function(id){
                               if( $.isNumeric(id)){
                                    var html = render("TabView-new-label", {id:id, name:name});
                                    $input.closest("li").before(html);
                                    $input.val("").focus();
                                }

                            })
                        }
                    }else if(event.keyCode == 27){
                    		var view = this;
                            var html = render("TabView-add-list");
                            var $input = $(event.currentTarget);
                            $input.closest("div").html(html);
                	}
                    event.preventDefault() ;
                },
                "click; .saveTab":function(event){
                	var view = this;
                    var $input = $(event.currentTarget).closest("div").find(".addText");
                        var name = $.trim($input.val());
                        if(name != "" && name.length > 0) {
                            dao.save(name).done(function(id){
                               if( $.isNumeric(id)){
                                    var html = render("TabView-new-label", {id:id, name:name});
                                    $input.closest("li").before(html);
                                    $input.val("").focus();
                                }

                            })
                        }
                },
                "click; .cancelTab": function(event){
                    var view = this;
                    var html = render("TabView-add-list");
                    var $input = $(event.currentTarget);
                    $input.closest("div").html(html);
                    view.$el.find(":text").placeholder();
                },
                "click; .showSave": function(event){
                    var view = this;
                    var html = render("TabView-save-list");
                    var $input = $(event.currentTarget);
                    $input.closest("div").html(html).find(".addText").focus();
                },
                "click; .deleteList":function(event){
                    var view = this;
                    event.stopPropagation();
                    event.preventDefault();
                    var pathInfo = app.buildPathInfo();
                    if(pathInfo.labelAssigned){
                        dao.del(pathInfo.paths[2]).done(function(){
                            var $li = view.$el.find("li[data-label-id='" + pathInfo.paths[2] + "']");
                            if(!$li.hasClass("Favorites")){
                               $li.remove();
                            }else{
                                dao.save("Favorites").done(function (id) {
                                    $li.attr("data-label-id", id);
                                    $li.find("a").attr("href", "#/list/" +id);
                                });
                            }
                            view.$el.find("li.Favorites i").addClass("select");
                            view.$el.trigger("CHANGE_SELECT_LABEL");
                            changeView.call(view);
                        });
                    }
                }
            },
            docEvents: {
            },
            // --------- Windows Event--------- //
            winEvents: {
                hashchange: function(event){
                    var view = this;
                    var pathInfo = buildPathInfo();
                    view.$el.trigger("PATH_INFO_CHANGE", pathInfo);
                },
                load: function(){
                    var view = this;
                    var pathInfo = buildPathInfo();
                    view.pathInfo = pathInfo;
                }

            },
            // --------- /Windows Event--------- //
        });

    // --------- Utilities--------- //
    function buildPathInfo(){
        var pathInfo = {};
        var hash = window.location.hash;
        if (hash){
            hash = hash.substring(1);
            if (hash){
                var pathAndParam = hash.split("!");
                pathInfo.paths = pathAndParam[0].split("/");
                // TODO: need to add the params
            }
        }
        app.pathInfo = pathInfo;
        return pathInfo;
    }

    function changeView($li){
        var view = this;
        var searchView = true, view = this;
        if(!$li){
            $li = view.$el.find("li.search");
        }
        if($.isNumeric($li)){
            $li = view.$el.find("li[data-label-id='" + $li + "']");
        }
        view.$el.find(".deleteList").hide();
        if ($li.length > 0) {
            if ($li.hasClass("favLabel") || $li.hasClass("search")) {
                if ($li.hasClass("favLabel")) {
                    searchView = false;
                }
                view.$el.find("li").removeClass("active");
                $li.addClass("active");
                if (searchView) {
//                    $li.trigger("RESTORE_SEARCH_VIEW");
                    location.href = "#"
                    
                    //find default select
                } else {
                    view.$el.find("li i").removeClass("select");
                    $li.find("i").addClass("select");
                    var label =  {id: $li.attr("data-label-id"), name: $.trim($li.find("a").text())};
                    if(!$li.hasClass("Favorites")){
                    	view.$el.find(".deleteList").show();
                    }
                }
                view.$el.find(".addList").html(render("TabView-add-list"));
                view.$el.trigger("DO_SEARCH");
            }
        }
    }

/*    function doSearch(label){
        label = label || {};
        var view = this;

        view.$el.bView("ContentView").loading();
        var labelAssigned = !$.isEmptyObject(label);
        var searchParameter = app.ParamsControl.getParamsForSearch({label:label.name, labelAssigned: labelAssigned});
        searchParameter.pageIndex =  1;

        app.SearchDaoHandler.search(searchParameter).always(function (result) {
            result.labelAssigned = labelAssigned;
            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
        });
    }*/
    // --------- /Utilities--------- //
})(jQuery);
