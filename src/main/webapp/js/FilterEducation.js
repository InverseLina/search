(function ($) {
    function FilterEducation(){
      this.constructor._super.constructor.call(this,"education");
    }

    brite.inherit(FilterEducation,app.ThPopup);

    brite.registerView("FilterEducation", {emptyParent: false},function(){
      return new FilterEducation();
    });
})(jQuery);
