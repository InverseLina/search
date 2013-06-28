/**
 * View: SelectColumns
 *
 * SelectColumns
 *
 *
 */
(function ($) {
    brite.registerView("SelectColumns", {emptyParent: false,parent:".search-result"},
        {
            create: function (data, config) {
                return render("SelectColumns", {columns:app.preference.displayColumns});
            },

            postDisplay: function (data) {
                var view = this;
                var columns = app.preference.columns();

                $.each(columns, function(idx, item){
                     view.$el.find("input[value='" + item + "']").attr("checked", true);
                });


                view.$el.on("mouseleave", function(){
                    view.$el.bRemove();
                })

            },
            events: {
                "click; input[type='checkbox']":function(){
                    var view = this;
                    var columns = [];
                    view.$el.find("input[type='checkbox']:checked").each(function(){
                        columns.push($(this).val());
                    });
                    if(columns.length > 0 ) {
                        view.$el.trigger("DO_SET_COLUMNS", {columns:columns});
                    }

                }

            }
        });
})(jQuery);
