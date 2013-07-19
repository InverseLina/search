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
                app.SavedSearchesDaoHandler.list().done(function(result){
                    var html = render("SavedSearches", {data: result});
                    console.log(html);
                    dfd.resolve(html);
                });
                return  dfd.promise();
            },

            postDisplay: function (data) {

            },
            events: {
                "btap; .btn": function(){
                    var view = this;
                    var name = view.$el.find("input").val();
                    if(!/^\s*$/g.test(name)){
                        name = $.trim(name);
                        var value = buildSearchParameter();
                        var exits = view.$el.find("li[data-name='" + name + "']");
                        var data = JSON.stringify(value);

                        if(exits.length==1) {
                          var id = $(exits[0]).attr("data-id");
                            app.SavedSearchesDaoHandler.update(id, data).done(function(result){
                                console.log(result);
                            })
                        }else{
                            app.SavedSearchesDaoHandler.save(name, data).done(function(result){
                                console.log(result);
                            })
                        }

                    }
                },

                "btap; li .clear": function(event){
                    var id = $(event.currentTarget).closest("li").attr("data-id");
                    app.SavedSearchesDaoHandler.delete(id).done(function(result){
                        console.log(result);
                    });
                }
            },
            docEvents: {}
        });

    function buildSearchParameter(){
        var mainView = $(".MainView").bView();
        var sideNav = mainView.sideNav;
        var $e = sideNav.$el;
        var result = [], val;
        $.each($e.bFindComponents("SideSection"), function(idx,item){
            val =  item.getSearchValues();
            if(val && !$.isEmptyObject(val)){
                result.push({name: item.cname, value: val});
            }

        });
        result.push({name: "contentView", value: mainView.contentView.getSearchValues()})
        return result;
    }
})(jQuery);
