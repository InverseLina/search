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
                view.$el.attr("data-subComponent", view.cname);
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
                        view.$el.find(".content").html(render("SideSection-close",
                            {value: formatDisplay(view.values), realValue:JSON.stringify(view.values)}))
                    }
                }
            },
            
            getSearchValues: function(){
                var view = this;
                if(view.status == "open"){
                    if(view.subComponent.getSearchValues && $.isFunction(view.subComponent.getSearchValues)){
                        return view.subComponent.getSearchValues();
                    }else{
                        return {};
                    }

                }else{
                	if(view.subComponent){
                        return view.subComponent.values;
                    }else{
                        var sv = view.values;
                        return sv;
                    }
//                    return JSON.parse(app.preference.get(view.cname + ".values", ""));
                }
            },
            updateSearchValues: function(values, itemNum){
                if(!itemNum){
                    itemNum = 0;
                }
                var view = this;
                if(view.status == "open"){
                    if(view.subComponent){
                        view.subComponent.updateSearchValues(values);
                    }
                    
                }else{
                    if(view.subComponent){
                        view.subComponent.updateSearchValues(values);
                    }
                    if(!$.isEmptyObject(values)){
                        view.$el.find(".content .not-open").remove();
                        view.$el.find(".content").append(render("SideSection-close",
                            {realValue:JSON.stringify(values), value: formatDisplay(values)}))
                    }

                    view.$el.find(".content .not-open").attr("data-itemNum", itemNum);
                }
            },
            clearSearchValues: function(){
                var view = this;
                view.values={};
                if(view.status == "open"){
                    if(view.subComponent && view.subComponent.clearValues){
                        view.subComponent.clearValues();
                    }
                }else{
                    if(view.subComponent && view.subComponent.clearValues){
                        view.subComponent.clearValues();
                    }
                    view.$el.find(".content .not-open").remove();
                }
            },
            getItemNum: function(){
                var view = this;
                if(view.status == "open"){
                    if(view.subComponent && view.subComponent.itemNum){
                        return view.subComponent.itemNum;
                    }else{
                        return app.defaultMenuSize;
                    }
                }else{
                    var num = view.$el.find(".content .not-open").attr("data-itemNum");
                    if(!num){
                        num = app.defaultMenuSize;
                    }
                    return num;
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
                    if(view.subComponent){
                        view.subComponent.values={};
                    }
                    view.values={};
                    view.$el.trigger("DO_SEARCH");
                },
                
                "btap; .section-banner":function(event){
                    var view = $(event.currentTarget).bView();
                    if(view.status=="open"){
                        //close
                        view.status = "close";
                       // app.preference.store(view.cname + ".status", "close");
                        view.$el.find("." + view.cname).hide();
                        view.$el.find(".open").removeClass("icon-chevron-down").addClass("icon-chevron-right");

                        if(view.subComponent.getSearchValues){
                            var values = view.subComponent.getSearchValues();
                            view.clearSearchValues();

                            if(!$.isEmptyObject(values)){
                                view.$el.find(".content .not-open").remove();
                                view.$el.find(".content").append(render("SideSection-close",
                                    {value: formatDisplay(values), realValue:JSON.stringify(values), itemNum: view.subComponent.itemNum||app.defaultMenuSize}))
                                view.$el.find(".content .not-open").attr("data-itemNum", view.subComponent.itemNum||app.defaultMenuSize);
                            }
                        }

                    }else{
                        //open
                        var itemNum = view.$el.find("span.not-open").attr("data-itemNum");
                        if(!itemNum){
                            itemNum = 0;
                        }

                        view.values = view.$el.find("span.not-open").attr("data-value");
                        if(view.values){
                            view.values = JSON.parse(view.values);
                        }else{
                            view.values={};
                        }
                        view.status = "open";
                        //app.preference.store(view.cname + ".status", "open");
                        if( view.$el.find("div."+view.cname).size()>0){
                      		view.$el.find("div."+view.cname).show();
                      		view.$el.find("span.not-open").hide();
                            if(view.subComponent.refreshSelections){
  	                            view.subComponent.refreshSelections();
                            }
                            if(!$.isEmptyObject(view.values)){
                                view.subComponent.updateSearchValues(view.values, itemNum);
                            }
                      	}else{
  	                        brite.display(view.cname , view.$el.find(".content")).done(function(component){
  	                            if(component.$el.bView(view.name) == view){
  	                                view.subComponent = component;
  	                                if(!$.isEmptyObject(view.values)){
  	                                    view.subComponent.updateSearchValues(view.values, itemNum);
  	                                }
  	                            }
  	                        });
                      	}
                        view.$el.find(".open").removeClass("icon-chevron-right").addClass("icon-chevron-down");
                    }
                }
            }
        });

    function formatDisplay(obj){
        var val;
        var result = "";
        var t = 0;
        for(var k in obj){
            val = obj[k];
            if(k=="companyNames" || k=="skillNames" || k == "educationNames"){
                if(val.length && val.length > 0){
                  var value = "";
                  var pairs = val.split(",");
                  for(var i = 0; i < pairs.length; i++){
                    if(i!=0){
                      value += ", ";
                    }
                    var values = pairs[i].replace(/#/g,"").split("|");
                    if((!values[1] || values[1] == "")  && (!values[2] || values[2] == "")){
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
                if(t != 0){
                  result = result + ", ";
                }
                result = result + k + ":" + obj[k];
            }
            t++;
        }
        return result;
    }
})(jQuery);
