
/**
 * View: SavedSearches
 *
 *
 *
 *
 */
(function ($) {
    var dao = app.SavedSearchesDaoHandler;
    brite.registerView("SavedSearches", {parent:".saveSearchesContainer", emptyParent: true},
        {
            create: function (data, config) {
                var dfd = $.Deferred();
                var item, data = [];
                var $e =  $(render("SavedSearches"));
/*                app.SavedSearchesDaoHandler.list({offset:0, limit:6}).done(function (result) {
                    showDetail(result, $e);
                    dfd.resolve($e);
                });*/
                dfd.resolve($e);
                return  dfd.promise();
            },

            postDisplay: function (data) {

            },
            events: {
                "SEARCH_RESULT_CHANGE":function(){
                    var view = this;
                    checkAndChangeBtnState.call(view, true);
                },
                "keyup; input":function(){
                   var view = this;
                    checkAndChangeBtnState.call(view);
                },
                "btap; .btn":function(event){
                    event.stopPropagation();
                    var query, view = this;
                    var $btn = $(event.currentTarget);
                    var searchName = $.trim(view.$el.find("input").val());
                    if($btn.hasClass("update")){
                        searchName = searchName.substring(0, searchName.length - 2);
                        view.$el.find("input").val(searchName);
                        $btn.removeClass("update");
                        $btn.text("Save");
                    }
                    enableBtn(view, false);
                    view.$el.find("input").focus();
                    var content = {};
                    query = getSearchQuery(view);
                    if(query!==""){
                        content.query = query;
                    }
                    if(hasSearchFilter()){
                        content.filters = app.ParamsControl.getFilterParams();
                    }
                    dao.save(searchName, JSON.stringify(content));
                },
                "btap; .drawdown": function(event){
                    var view = this;
                    event.stopPropagation();
                    event.preventDefault();
                    dao.list().done(function(result){
                        var html = render("SavedSearches-list", {result: result});
                        view.$el.find(".search-list").empty().html(html).show();
                    });
                },
                "btap; .createNew":function(event){
                    event.stopPropagation();
                    event.preventDefault();
                    var view = this;
                    var $input = view.$el.find("input");
                    $input.val("My search 1").select().focus();
                    $input.trigger("SEARCH_QUERY_CHANGE");
                    view.$el.find(".search-list").hide();
                },
                "btap; li .remove i": function(event){
                    event.stopPropagation();
                    event.preventDefault();
                    var $li = $(event.currentTarget).closest("li");
                    var id = $li.attr("data-objId");
                    if(id) {
                        dao.delete(id).done(function(){
                           $li.remove();
                        });
                    }
                },
                "btap; li[data-objId]":function(event){
                    var view = this;
                    var $li = $(event.currentTarget);
                    var id = $li.attr("data-objId");
                    var $input = view.$el.find("input");
                    dao.get(id).done(function(result){
                        if(result){
                            $input.val(result.name).focus().select();
                            enableBtn(view, false);
                            view.$el.find(".search-list").hide();
                            //todo should restore params

                        }
                    });
                }
            },
            docEvents: {
                "SEARCH_QUERY_CHANGE ADD_FILTER REMOVE_FILTER": function(){
                    var view = this;
                    checkAndChangeBtnState.call(view);
                }
            }
        });

    function enableBtn(view, status) {
        var $btn = view.$el.find(".btn");
        if (status) {
            $btn.removeClass("disabled");
        } else {
            if (!$btn.hasClass("disabled")) {
                $btn.addClass("disabled");
            }
        }
    }

    function getSearchQuery(view){
        var contentView = view.$el.bView(("ContentView"));
        return $.trim(contentView.$el.find(".search-form .search-input").val());
    }

    function hasSearchFilter(){
        var filters = app.ParamsControl.getFilterParams();
        var hasFilter = false;
        for (var key in filters) {
            if(filters[key].length > 0) {
                hasFilter = true;
                break;
            }
        }
        return hasFilter;
    }

    function checkAndChangeBtnState(justSearch){
         justSearch = justSearch||false;
         var view = this;
        var $btn = view.$el.find(".btn");

        var query = getSearchQuery(view);
        var searchName = $.trim(view.$el.find("input").val());

        var hasName = searchName.length > 0;
        var isUpdate = $btn.hasClass("update");
        if(isUpdate) {
            searchName = searchName.substring(0, searchName.length -2)
        }

        var hasFilter = hasSearchFilter();

        if(!hasName){
           enableBtn(view, false);
        }else{
            if(hasFilter || query!=""){
                dao.count(searchName).done(function(count){
                   if(count>0){
                      if(justSearch){
                          if(isUpdate){
                             enableBtn(view, true);
                          }else{
                             enableBtn(view, false);
                          }
                      }else{
                          enableBtn(view, true);
                          if(!isUpdate) {
                              $btn.addClass("update");
                              $btn.text("Update");
                              view.$el.find("input").val(searchName + " *");
                          }
                      }
                   }else{
                      if(isUpdate) {
                          $btn.removeClass("update");
                          $btn.text("Save");
                      }else{
                        enableBtn(view, true);
                      }
                   }
                })
            }else{
                enableBtn(view, false);
            }
        }
    }

})(jQuery);
