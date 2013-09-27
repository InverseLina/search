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
                setTimeout(function () {
                    view.barLength = $e.find(".bar").outerWidth();
                    setValue.call(view, view.opts.value);
                }, 200);
            },
            events: {
                "btap; .bar" : function(e) {
                    var view = this;
                    var $bar = $(e.currentTarget);
                    var position = e.pageX - view.barOffset.left;
                    setValue.call(view, position);
                },
                //add drag event
                "bdragmove;.slider":function(e){
                    var view = this;
                    var position =   e.bextra.pageX - view.barOffset.left;

                    setValue.call(view,position);
                },
/*                "bdragstart; .slider":function(){
                    var view = this;
                    view.$el.find(".bar, .slider").css("cursor", "pointer");
                    view.$el.css("cursor", "pointer")
                },*/
/*                "bdragend; .slider":function(){
                    var view = this;
                    view.$el.find(".bar").css("cursor", "default");
                    view.$el.css("cursor", "default");
                }*/
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
            }

        });
    /**
     * set position by value
     */
    function setPosition(){
        var view = this;
        var $e = this.$el;

        var value = view.opts.value;
        if(isNaN(value)){
            value = view.opts.min;
        }

        if (value > view.opts.max) {
            value = view.opts.max;
        }

        if (value < view.opts.min) {
            value = view.opts.min;
        }
        view.opts.value = value;

        $e.find(".slider").html(value==0?"-":value);
        var position = 0;
        if(view.opts.max != view.opts.min){
            position = (view.opts.value - view.opts.min) / (view.opts.max - view.opts.min) * view.barLength;
        }else{
            position = view.barLength;
        }
        view.position = position;
        var $valve = $e.find(".slider");
        $valve.css("left",(position - $valve.width()/2)+"px");
        view.$el.trigger("SLIDER_VALUE_CHANGE");
    }

    /**
     * set value by position
     */
    function setValue(pos){
        var view = this;
        pos = pos||0;
        console.log(pos);
        if(pos<=0){
           view.$el.find(".slider").addClass("zero");
           view.$el.addClass("zero");
        }else{
            view.$el.find(".slider").removeClass("zero");
            view.$el.removeClass("zero");
        }
        var value = pos/view.barLength * (view.opts.max - view.opts.min) + view.opts.min;
        value = Math.round(value);
        if(value > view.opts.max){
            value = view.opts.max;
        }
        if(value < view.opts.min){
            value = view.opts.min;
        }

        view.opts.value = value;
        setPosition.call(view);
    }

})(jQuery);
