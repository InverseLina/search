/**
 * View: SelectColumns
 *
 * SelectColumns
 *
 *
 */
(function ($) {
    brite.registerView("SelectColumns", {emptyParent: false,parent:"body"},
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


                var bh = $("body").height();
                var bw = $("body").width();
                view.$el.show();
                var pos = {top: data.top + view.$el.height() > bh ? bh - view.$el.height() -30 : data.top - 30,
                           left: data.left + view.$el.width() > bw ? bw - view.$el.width() - 30 : data.left-30}
                view.$el.css(pos);

                view.$el.mouseleave(function(){
                    view.$el.bRemove();
                });

            },
            events: {
                "click; input[type='checkbox']":function(){
                    var view = this;
                    var columns = [];
                    view.$el.find("input[type='checkbox']:checked").each(function(){
                        columns.push($(this).val());
                    })
                    if(columns.length > 0 ) {
                        view.$el.trigger("DO_SET_COLUMNS", {columns:columns});
                    }

                }

            },
            docEvents: {}
        });
})(jQuery);
