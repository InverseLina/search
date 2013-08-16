(function ($) {
    function FilterContact(){
      this.constructor._super.constructor.call(this,"Contact");
    }

    brite.inherit(FilterContact,app.ThPopup);

    FilterContact.prototype.events = $.extend({

    }, FilterContact.prototype.events||{});

    brite.registerView("FilterContact", {emptyParent: false},function(){
      return new FilterContact();
    });
})(jQuery);
