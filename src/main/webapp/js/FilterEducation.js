(function ($) {
    function FilterEducation(){
      this.constructor._super.constructor.call(this,"Contact");
    }

    brite.inherit(FilterEducation,app.ThPopup);

    brite.registerView("FilterEducation", {emptyParent: false},function(){
      return new FilterEducation();
    });
})(jQuery);
