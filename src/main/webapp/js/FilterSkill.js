(function ($) {
    function FilterSkill(){
      this.constructor._super.constructor.call(this,"skill");
    }

    brite.inherit(FilterSkill,app.ThPopup);

    brite.registerView("FilterSkill", {emptyParent: false},function(){
      return new FilterSkill();
    });
})(jQuery);
