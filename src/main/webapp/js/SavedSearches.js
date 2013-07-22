/**
 * View: SavedSearches
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("SavedSearches", {emptyParent: true},
        {
            create: function (data, config) {
                var dfd = $.Deferred();
                var item, data = [];
                app.SavedSearchesDaoHandler.list({offset:0, limit:6}).done(function (result) {
                    console.log(result)
                    var html;
                    if(result.length > 5) {
                       html = render("SavedSearches", {data: result.slice(0,5), display:"show"});
                    }else{
                        html = render("SavedSearches", {data: result, display:"hide"});
                    }
                    dfd.resolve(html);
                });
                return  dfd.promise();
            },

            postDisplay: function (data) {

            },
            events: {
                "btap; .btn": function () {
                    var view = this;
                    var name = view.$el.find("input").val();
                    if (!/^\s*$/g.test(name)) {
                        name = $.trim(name);
                        var value = buildSearchParameter();
                        if (value.length > 0) {
                            var exits = view.$el.find("li[data-name='" + name + "']");
                            var data = JSON.stringify(value);

                            if (exits.length == 1) {
                                var id = $(exits[0]).attr("data-id");
                                app.SavedSearchesDaoHandler.update(id, data).done(function () {
                                    updateDetail.call(view);
                                })
                            } else {
                                app.SavedSearchesDaoHandler.save(name, data).done(function () {
                                    updateDetail.call(view);
                                })
                            }
                        }

                    }
                },
                "btap; li[data-id]":function(event){
                     var view = this;
                     var name = $(event.currentTarget).closest("li").attr("data-name");
                    view.$el.find("input").val(name);
                    var value = $(event.currentTarget).closest("li").attr("data-value");
                    value = JSON.parse(value);
                    console.log(value);
                    restoreSearchValue.call(view, value);
                },
                "btap; li .clear": function (event) {
                    var view = this;
                    var id = $(event.currentTarget).closest("li").attr("data-id");
                    app.SavedSearchesDaoHandler.delete(id).done(function (result) {
                        updateDetail.call(view);
                    });
                    event.stopPropagation();
                },
                "btap; .btns span": function(event) {
                    var view = this;
                    var $btn = $(event.currentTarget);
                    var $li = $btn.parent("li");
                    var flag = $btn.attr("data-show");
                    var $ul = $btn.closest("ul");
                    var offset = $btn.attr("data-offset")
                    var $btns = $("li.btns", $ul);

                    // show more items
                    if (flag == "more") {
                        $btn.hide();
                        app.SavedSearchesDaoHandler.list({offset: offset, limit: 21}).done(function(result){
                            var data;
                            if(result.length>21){
                                data = {display:"show", offset:offset+20, data: result.slice(0,20), toHide:20}
                            }else{
                                data = {display:"hide", offset:offset+result.length, data: result, toHide: result.length }
                            }
                            view.$el.find("ul").append(render("SavedSearches-more", data));
                        });
                        // show less items
                    } else {
                        var hideNum = $btn.attr("data-hide");
                        var itemNum = view.$el.find("li[data-id]").length;
                        console.log(hideNum)
                        console.log(itemNum)
                        var num = 0;
                        var $hideLi = $("li:gt(" + (itemNum - hideNum )   +")", $ul);
                        $($btns.get($btns.length-2)).find("[data-show='more']").show();
                        $hideLi.hide(1000, function() {
                            $(this).remove();
                        });



                    }

                },
            },
            docEvents: {}
        });

    function buildSearchParameter() {
        var mainView = $(".MainView").bView();
        var sideNav = mainView.sideNav;
        var $e = sideNav.$el;
        var result = [], val;
        $.each($e.bFindComponents("SideSection"), function (idx, item) {
            val = item.getSearchValues();
            if (val && !$.isEmptyObject(val)) {
                result.push({name: item.cname, value: val});
            }

        });
        var contentValue = mainView.contentView.getSearchValues();
        console.log(contentValue);
        if(!/^\s*$/g.test(contentValue.search)){
            result.push({name: "contentView", value:contentValue })
        }

        return result;
    }

    function updateDetail(){
        console.log("xxxxxxxxxxxxxx")
        var view = this;
        app.SavedSearchesDaoHandler.list().done(function (result) {
            var html;
            if(result.length > 5) {
                html = render("SavedSearches-detail", {data: result.slice(0,5), display:"show"});
            }else{
                html = render("SavedSearches-detail", {data: result, display:"hide"});
            }
            console.log(html);
            view.$el.find("ul").html(html);
        });
    }

    function restoreSearchValue(values) {
        var view = this;
        var mainView = view.$el.bView("MainView");
        $.each(values, function(idx, item){
              if(item.name == "contentView"){
                  mainView.contentView.$el.find(".search-query").val(item.value.search);
              }else{
                  mainView.sideNav.$el.bFindFirstComponent(item.name)[0].updateSearchValues(item.value);
              }
        });
    }

})(jQuery);
