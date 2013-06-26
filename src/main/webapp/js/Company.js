/**
 * View: Company
 *
 * Company
 *
 *
 */
(function ($) {
    function CompanyView(){
      this.constructor._super.constructor.call(this,"company","companies");
        
      this.events = {
        "change; input[name='curCompany']" : function(event) {
          var view = this;
          view.$el.trigger("DO_SEARCH");
        }
      };

      this.events = $.extend(this.events, app.sidesection.BaseSideAdvanced.prototype.events);
    }


    brite.inherit(CompanyView,app.sidesection.BaseSideAdvanced);

    brite.registerView("Company", {emptyParent: true},function(){
      return new CompanyView();
    });
})(jQuery);
