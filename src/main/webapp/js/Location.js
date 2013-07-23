/**
 * View: Location
 *
 * Location
 *
 *
 */
(function ($) {
    brite.registerView("Location", {emptyParent: true},
        {
            create: function (data, config) {
                return render("Location");
            },

            postDisplay: function (data) {
            	var view = this;
            	 $.ajax({
           			url:"/config/get/local_distance",
           			type:"Get",
           			dataType:'json'
         	  	  }).done(function(data){});
            },
            getSearchValues:function(){
                var values = {};
                var view = this;
                view.$el.find(":checkbox:checked").each(function(idx, item){
                	var $item = $(item);
                	values[$item.attr("name")] = true;
                });
                
                view.$el.find("input[type='text']").each(function(idx, item){
                    var $item = $(item);
                    var val = $item.val();
                    if(!/^\s*$/.test(val)){
                    	if($item.hasClass("radius")&&view.$el.find("span.unit").html()=="kilometers"){
                    		val = parseFloat(val)*1000;
                    	}
                        values[$item.attr("name")] = val;
                    }
                });
                return values;
            },
            updateSearchValues:function(data){
                var view = this;
                for (var k in data) {
                   view.$el.find("input[name='" + k + "']").val(data[k]);
                   view.$el.find("input[name='" + k + "']").closest(".control-group").removeClass("has-value").addClass("has-value");
                   if(k=="radiusFlag"){
                	   view.$el.find("input[name='" + k + "']").prop("checked",true);
                   } 
                }
            },
            clearValues:function(){
                var view = this;
                view.$el.find("input[type='text']").val("");
                view.$el.find("input[type='checkbox']").prop("checked", false);
            },
            events: {
                "btap; .clear": function(event){
                    var view = this;
                    var $group =$(event.currentTarget).closest(".control-group");
                    $group.removeClass("has-value");
                    $group.find("input").val("");
                    view.$el.trigger("DO_SEARCH");
                    if(view.$el.find(".has-value").length==0){
                    	view.$el.find(":checkbox").prop("checked",false);
                    }else{
                    	view.$el.find(":checkbox").prop("checked",true);
                    }
                    event.stopPropagation();
                },
                "keyup; input[type='text']":function(event){
                	var view = this;
                    event.stopPropagation();
                    var $target = $(event.currentTarget);
                    var val = $target.val();

                    if(!/^\s*$/.test(val)){
                        $target.closest(".control-group").addClass("has-value");
                    }else{
                        $target.closest(".control-group").removeClass("has-value");
                    }
                    if($target.hasClass("radius")){
                    	if(/^\s*$/.test(val)){
                    		view.$el.find(":checkbox").prop("checked",false);
                    	}else{
                    		view.$el.find(":checkbox").prop("checked",true);
                    	}
                    }
                    
                },
                "change; :checkbox":function(event){
                	var view = this;
                	if($(event.target).prop("checked")){
                		if(view.$el.find(":input.radius").val()==""){
                			view.$el.find(":input.radius").val(50);
                		}
                	}else if(!$(event.target).prop("checked")){
                		view.$el.find(":input.radius").val("");
                	}
                    view.$el.trigger("DO_SEARCH");
                }
            },
            docEvents: {}
        });
})(jQuery);
