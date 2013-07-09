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

            },
            getSearchValues:function(){
                 var values = {};
                var view = this;
                view.$el.find("input[type='text']").each(function(idx, item){
                    var $item = $(item);
                    var val = $item.val();
                    if(!/^\s*$/.test(val)){
                        values[$item.attr("name")] = val;
                    }
                });
                view.$el.find(":checkbox:checked").each(function(idx, item){
                	var $item = $(item);
                	values[$item.attr("name")] = true;
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
            events: {
                "btap; .clear": function(event){
                    var view = this;
                    var $group =$(event.currentTarget).closest(".control-group");
                    $group.removeClass("has-value");
                    $group.find("input").val("");
                    view.$el.bView("SideSection").$el.trigger("store");
                    view.$el.trigger("DO_SEARCH");
                    event.stopPropagation();
                },
                "keyup; input[type='text']":function(event){
                    event.stopPropagation();
                    var $target = $(event.currentTarget);
                    var val = $target.val();

                    if(!/^\s*$/.test(val)){
                        $target.closest(".control-group").addClass("has-value");
                    }else{
                        $target.closest(".control-group").removeClass("has-value");
                    }
                }
            },
            docEvents: {}
        });
})(jQuery);
