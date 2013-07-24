/**
 * View: ContactInfo
 *
 * ContactInfo
 *
 *
 */
(function ($) {
    brite.registerView("ContactInfo", {emptyParent: true},
        {
            create: function (data, config) {
                return render("ContactInfo");
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
                if(view.$el.find("input:checked").length > 0){
                    values.curTitle = true;
                }
                view.values = values;
                return values;
            },
            clearValues:function(){
              view = this;
              view.$el.find("input[type='text']").val("");
              view.$el.find("input[type='checkbox']").prop("checked", false);
                view.$el.find(".control-group").removeClass("has-value")
            },
            updateSearchValues:function(data){
                console.log(data)
                var view = this;
                for (var k in data) {
                    if(k =="curTitle") {
                        view.$el.find("input[name='" + k + "']").prop("checked", data[k])
                    }else{
                        view.$el.find("input[name='" + k + "']").val(data[k]);
                        view.$el.find("input[name='" + k + "']").closest(".control-group").removeClass("has-value").addClass("has-value");
                    }


                }
            },
            events: {
                "btap; .clear": function(event){
                    var view = this;
                    var $group =$(event.currentTarget).closest(".control-group");
                    $group.removeClass("has-value");
                    $group.find("input").val("");
                    view.$el.trigger("DO_SEARCH");
                    view.values={};
                    event.stopPropagation();
                },
                "change; input[name='curTitle']" : function(event) {
                  var view = this;
                  view.$el.trigger("DO_SEARCH");
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
