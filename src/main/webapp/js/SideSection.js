/**
 * View: SideSection
 *
 * SideSection
 *
 *
 */
(function ($) {
    brite.registerView("SideSection", {emptyParent: false, parent:".sec"},
        {
            create: function (data, config) {
                this.status = app.preference.get(data.component + ".status", "open");
                this.values = app.preference.get(data.component + ".values", "");
                this.values = JSON.parse(this.values);
                this.cname = data.component;
                return render("SideSection", {title: data.title});
            },

            postDisplay: function (data) {
                var view = this;
                if(view.status == "open"){
                    brite.display(data.component , view.$el.find(".content")).done(function(component){

                      if(component.$el.bView(view.name) == view){
                        view.subComponent = component;
                        console.log(view.subComponent);
                        if(!$.isEmptyObject(view.values)){
                            view.subComponent.updateSearchValues(view.values);
                        }
                      }
                    });
                    view.$el.find(".open").removeClass("icon-play").addClass("icon-eject")
                }else {
                    view.$el.find(".open").removeClass("icon-eject").addClass("icon-play");
                    if(!$.isEmptyObject(view.values)){
                        view.$el.find(".content").html(render("SideSection-close", {value: formatDisplay(view.values)}))
                    }

                }
            },
            getSearchValues: function(){
                var view = this;
                if(view.status == "open"){
                    return view.subComponent.getSearchValues();
                }else{
                    return JSON.parse(app.preference.get(view.cname + ".values", ""));
                }
            },
            events: {
                "btap; .clear":function(event){
                    var view = $(event.currentTarget).bView();
                    if(view.status == "open"){
                        view.subComponent.$el.trigger("clear");
                    }else{
                        view.$el.find(".content").empty();
                    }
                    //remove cookie
                    app.preference.store(view.cname + ".values", null);
                    view.$el.trigger("DO_SEARCH");
                },
                "btap; .open":function(event){
                    var view = $(event.currentTarget).bView();
                    if(view.status=="open"){
                        //close
                        view.status == close;
                        var values = view.subComponent.getSearchValues();
                        console.log(values);

                        view.$el.find(".open").removeClass("icon-eject").addClass("icon-play");
                        view.$el.find("." + view.cname).bRemove();
                        view.status = "close";
                        app.preference.store(view.cname + ".status", "close");
                        if(!$.isEmptyObject(values)){
                            app.preference.store(view.cname + ".values", JSON.stringify(values));
                            view.$el.find(".content").html(render("SideSection-close", {value: formatDisplay(values)}))
                        }
                    }else{
                        //open
                        view.values = JSON.parse(app.preference.get(view.cname + ".values", ""));
                        view.status = "open";
                        app.preference.store(view.cname + ".status", "open");
                        if( view.$el.find("div."+view.cname).length>0){
                    		view.$el.find("div."+view.cname).show();
                    		view.$el.find("span.not-open").remove();
                    	}else{
	                        brite.display(view.cname , view.$el.find(".content")).done(function(component){
	                            if(component.$el.bView(view.name) == view){
	                                view.subComponent = component;
	                                if(!$.isEmptyObject(view.values)){
	                                    view.subComponent.updateSearchValues(view.values);
	                                }
	                            }
	                        });
                    	}
                        view.$el.find(".open").removeClass("icon-play").addClass("icon-eject")
                    }
                }
            },
            docEvents: {}
        });
})(jQuery);
