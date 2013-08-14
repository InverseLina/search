(function ($) {
    function FilterContact(){
      this.constructor._super.constructor.call(this,"Contact");
    }

    brite.inherit(FilterContact,app.ThPopup);

    brite.registerView("FilterContact", {emptyParent: false},function(){
      return new FilterContact();
    });
})(jQuery);
