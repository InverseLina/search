(function ($) {
    function FilterLocation(){
      this.constructor._super.constructor.call(this,"Location");
    }

    brite.inherit(FilterLocation,app.ThPopup);

    brite.registerView("FilterLocation", {emptyParent: false},function(){
      return new FilterLocation();
    });
})(jQuery);
