
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
                var $e =  $(render("SavedSearches"));
                app.SavedSearchesDaoHandler.list({offset:0, limit:6}).done(function (result) {
                    showDetail(result, $e);
                    dfd.resolve($e);
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

                            app.SavedSearchesDaoHandler.save(name, data).done(function () {
                                updateDetail.call(view);
                            })

                        }

                    }
                },
                "btap; li[data-id]":function(event){
                     var view = this;
                     var name = $(event.currentTarget).closest("li").attr("data-name");
                    view.$el.find("input").val(name);
                    var value = $(event.currentTarget).closest("li").attr("data-value");
                    value = JSON.parse(value);
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
                    var flag = $btn.attr("data-show");
                    var $ul = $btn.closest("ul");
                    var offset = view.$el.find("li[data-id]").length;
                    var $btns = $("li.btns", $ul);

                    // show more items
                    if (flag == "more") {
                        app.SavedSearchesDaoHandler.list({offset: offset, limit: 21}).done(function(result){
                            showDetail(result, view.$el, 20)
                        });
                        // show less items
                    } else {
                        var hideNum = (offset-5) % 20;
                        if(hideNum <= 0){
                            hideNum = 20;
                        }
                        var itemNum = offset
                        var num = 0;
                        var $hideLi = $("li[data-id]:gt(" + (itemNum - hideNum -1)   +")", $ul).remove();

/*                        $hideLi.hide(1000, function() {
                            $(this).remove();
                        });*/

                        $btns.find("span[data-show='more']").show();

                        if(offset <= 25){
                            $btns.find("span[data-show='less']").hide();
                        }



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
                result.push({name: item.cname, value: val, itemNum: item.getItemNum()});
            }

        });
        var contentValue = mainView.contentView.getSearchValues();
        if(!/^\s*$/g.test(contentValue.search)){
            result.push({name: "contentView", value:contentValue })
        }
        return result;
    }

    function updateDetail(){
        var view = this;
        var dataLen = view.$el.find("li[data-id]").length;
        var offset = 5;
        if(dataLen > 5){
            offset = parseInt((dataLen -5)/20) * 20;
        }
        if((dataLen - 5) % 20 > 0){
            offset+=20;
        }
        view.$el.find("li[data-id]").remove();
        app.SavedSearchesDaoHandler.list({offset: 0, limit: offset+1}).done(function(result){
            showDetail(result, view.$el, offset)
        });
    }

    function restoreSearchValue(values) {
        var view = this;
        var mainView = view.$el.bView("MainView");
        clean.call(view);

        $.each(values, function(idx, item){
              if(item.name == "contentView"){
                  mainView.contentView.$el.find(".search-query").val(item.value.search);
              }else{
                  var sideSection = mainView.sideNav.$el.find(".SideSection[data-subComponent='" + item.name + "']").bView();
                  if(sideSection.subComponent){
                	  sideSection.subComponent.values=item.value;
                      sideSection.subComponent.$el.trigger("restoreSearchList", {itemNum: item.itemNum, value: item.value});
                  }else{
                	  sideSection.values=item.value;
                  }
                      sideSection.updateSearchValues(item.value, item.itemNum);

              }
        });
        setTimeout(function(){
        	 view.$el.trigger("DO_SEARCH");
        }, 100);
       
    }

    function clean(){
        var view = this;
        var mainView = view.$el.bView("MainView");
        mainView.contentView.$el.find(".search-query").val("");
        var ss = mainView.$el.bFindComponents("SideSection");
        $.each(ss, function(idx, item){
            item.clearSearchValues();
        });
    }


    function showDetail(result, $e, limit) {
        var html;
        limit = limit||5;
        if (result.length > limit) {
            html = render("SavedSearches-detail", {data: result.slice(0, limit), display: "show"});
        } else {
            html = render("SavedSearches-detail", {data: result, display: "hide"});
        }
        var $btns = $e.find(".btns");
        $btns.before(html);
        var $more = $btns.find("span[data-show='more']");
        var $less = $btns.find("span[data-show='less']");
        if (result.length > limit) {
            $more.show();
        }else{
            $more.hide();
        }
        if ($e.find("li[data-id]").length > 5) {
            $less.show();
        }else{
            $less.hide();
        }
    }

})(jQuery);
