(function ($) {
    function FilterEmployer(){
      this.constructor._super.constructor.call(this,"Employer");
    }

    brite.inherit(FilterEmployer,app.ThPopup);

    brite.registerView("FilterEmployer", {emptyParent: false},function(){
      return new FilterEmployer();
    });
})(jQuery);
