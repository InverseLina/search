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


                $(document).on("btap." + view.cid, function(event){
                    var width = view.$el.width();
                    var height = view.$el.height();
                    var pos = view.$el.offset();
                    if(event.pageX > pos.left && event.pageY < pos.left + width
                        && event.pageY > pos.top && event.pageY < pos.top + height){
                        view.$el.off("mouseleave");
                    }else{
                        view.$el.bRemove();
                        $(document).off("btap." + view.cid);
                    }
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