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
                var defaultOpen = app.preference.defaultSectionOpen;
                this.status = app.preference.get(data.component + ".status", defaultOpen[data.component]);
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
                        if(!$.isEmptyObject(view.values)){
                            view.subComponent.updateSearchValues(view.values);
                        }
                      }
                    });
                    view.$el.find(".open").removeClass("icon-chevron-right").addClass("icon-chevron-down");
                }else {
                    view.$el.find(".open").removeClass("icon-chevron-down").addClass("icon-chevron-right");
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
            	
                "btap; .not-open .clear":function(event){
                    event.stopPropagation();
                    var view = $(event.currentTarget).bView();
                    if(view.status == "open"){
                        view.subComponent.$el.trigger("clear");
                    }else{
                        //check sub component is create or not.
                        if(view.subComponent){
                            view.subComponent.$el.trigger("clear");
                        }
                        view.$el.find(".not-open").remove();
                    }
                    //remove cookie
                    app.preference.store(view.cname + ".values", null);
                    //clear all the text input value
                    view.$el.find(".control-group").removeClass("has-value").find(":text").val("");
                    view.$el.find(":checkbox:not([data-name])").prop("checked",false);
                    view.$el.trigger("DO_SEARCH");
                },
                
                "btap; .section-banner":function(event){
                    var view = $(event.currentTarget).bView();
                    if(view.status=="open"){
                        //close
                        view.status == close;
                        var values = view.subComponent.getSearchValues();

                        view.$el.find("." + view.cname).hide();
                        view.status = "close";
                        app.preference.store(view.cname + ".status", "close");
                        if(!$.isEmptyObject(values)){
                            app.preference.store(view.cname + ".values", JSON.stringify(values));
                            view.$el.find(".content .not-open").remove();
                            view.$el.find(".content").append(render("SideSection-close", {value: formatDisplay(values)}))
                        }
                        view.$el.find(".open").removeClass("icon-chevron-down").addClass("icon-chevron-right");
                    }else{
                        //open
                        view.values = JSON.parse(app.preference.get(view.cname + ".values", ""));
                        view.status = "open";
                        app.preference.store(view.cname + ".status", "open");
                        if( view.$el.find("div."+view.cname).size()>0){
                      		view.$el.find("div."+view.cname).show();
                      		view.$el.find("span.not-open").hide();
  	                      view.subComponent.refreshSelections();
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
                        view.$el.find(".open").removeClass("icon-chevron-right").addClass("icon-chevron-down");
                    }
                },
                "store":function(){
                    //force store value
                    var view = this;
                    var values = view.subComponent.getSearchValues();
                    if(!$.isEmptyObject(values)){
                        app.preference.store(view.cname + ".values", JSON.stringify(values));
                    }else {
                        app.preference.store(view.cname + ".values", null);
                    }
                }
            },
            parentEvents: {
                MainView : {
                    "SEARCH_RESULT_CHANGE": function(event, result){
                        var view = this;
                        if (view.status == "open") {
                            var values = view.subComponent.getSearchValues();
                            if (!$.isEmptyObject(values)) {
                                app.preference.store(view.cname + ".values", JSON.stringify(values));
                            }
                        }
                    }
                }
            }
        });

    function formatDisplay(obj){
        var val;
        var result = "";
        for(var k in obj){
            val = obj[k];
            if(k=="companyNames" || k=="skillNames" || k == "educationNames"){
                if(val.length && val.length > 0){
                  var value = "";
                  var pairs = val.split(",");
                  for(var i = 0; i < pairs.length; i++){
                    if(i!=0){
                      value += ",";
                    }
                    var values = pairs[i].split("|");
                    if(values[1] == ""  && values[2] == ""){
                      value = value + values[0];
                    }else{
                      value = value + values[0] + "("+values[1]+"~"+values[2]+")";
                    }
                  }
                  
                  result += " " + value;
                }else{
                  result += " " + val;
                }
                
            }else{
                result = result +" " + k + ":" + obj[k];
            }
        }
        return result;
    }
})(jQuery);
