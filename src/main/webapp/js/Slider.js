/**
 * View: Slider
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("Slider", {loadTmpl:false, emptyParent: true},
        {
            create: function (data, config) {
                data = data || {};


                // options, property for component
                 this.opts = $.extend({}, {min:0,max:20,value:0}, data);
                return render("Slider");
            },

            postDisplay: function (data) {
                var view = this;
                var $e = this.$el;


                var width = $e.parent().outerHeight();
                var height = $e.parent().outerWidth();
                view.barOffset = $e.find(".bar").offset();
                if(width >0 && height > 0){
                    $e.find(".bar").css({"border-width": "{0}px {1}px 0 0".format(width, height)});
                }
                //get the length from bar height
//                setTimeout(function () {
                    view.barLength = $e.find(".bar").outerWidth();
//                    setPosition.call(view, view.opts.value);
//                }, 200);
            },
            events: {
                "btap; .bar" : function(e) {
                    e.stopPropagation();
                    e.preventDefault();
                    var view = this;
                    var $bar = $(e.currentTarget);
                    var position = e.pageX - view.barOffset.left;
                    setPosition.call(view, position);
                },
                //add drag event
                "bdragmove;.slider":function(e){
                    e.stopPropagation();
                    e.preventDefault();
                    var view = this;
                    var position =   e.bextra.pageX - view.barOffset.left;

                    setPosition.call(view,position, true);
                },
/*                "bdragstart; .slider":function(){
                    var view = this;
                    view.$el.find(".bar, .slider").css("cursor", "pointer");
                    view.$el.css("cursor", "pointer")
                },*/
                "bdragend; .slider":function(e){
                    e.stopPropagation();
                    e.preventDefault();
                    var view = this;
                    var position =   e.bextra.pageX - view.barOffset.left;

                    setPosition.call(view,position);
                }
            },
            docEvents: {},
            /**
             * get value
             */
            getValue : function(){
                var view = this;
                return view.opts.value;
            },
            reset: function(){
                var view = this;
                setValue.call(view, 0);
            },
            inc: function(){
                var view = this;
                setValue.call(view, (view.opts.value||0) + 1);
            },
            dec: function(){
                var view = this;
                setValue.call(view, (view.opts.value||0) - 1);
            }

        });
    /**
     * set position by value
     */
    function setPosition(pos, force){
        var view = this;
        var $e = this.$el;
        force = force||false;

        pos = pos||0;
        if(pos<=0){
            view.$el.find(".slider").addClass("zero");
            view.$el.addClass("zero");
            pos = 0;
        }else{
            view.$el.find(".slider").removeClass("zero");
            view.$el.removeClass("zero");
            if(pos > view.barLength){
                pos = view.barLength;
            }
        }
        var value = Math.round(pos/view.barLength * (view.opts.max - view.opts.min) + view.opts.min);
        if(isNaN(value)){
            value = view.opts.min;
        }

        view.opts.value = value;

        $e.find(".slider").html(value==0?"-":value);

        var $valve = $e.find(".slider");
        $valve.css("left",(pos - $valve.width()/2)+"px");
        if(view.position !== pos && !force){
            view.position = pos;
            view.$el.trigger("SLIDER_VALUE_CHANGE");
        }
    }

    /**
     * set value
     */
    function setValue(value) {
        var view = this;
        var position = 0;
        value = value || 0;
        value = Math.round(value);
        if(value > view.opts.max){
            value = view.opts.max;
        }

        if (value < view.opts.min) {
            value = view.opts.min;
        }

        if (view.opts.max != view.opts.min) {
            position = Math.round((value - view.opts.min) / (view.opts.max - view.opts.min) * view.barLength);
        } else {
            position = view.barLength;
        }
        setPosition.call(view, position);
    }

})(jQuery);
