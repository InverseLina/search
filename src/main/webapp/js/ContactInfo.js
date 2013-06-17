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
                return values;
            },
            updateSearchValues:function(data){
                var view = this;
                console.log(view);
                for (var k in data) {
                   view.$el.find("input[name='" + k + "']").val(data[k]);
                }
            },
            events: {
                "clear": function(){
                    var view = this;
                    view.$el.find("input").val("");
                },
                "change; input[name='curTitle']" : function(event) {
                  var view = this;
                  view.$el.trigger("DO_SEARCH");
                }
            },
            docEvents: {}
        });
})(jQuery);
