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
    }

    CompanyView.prototype.events = {
        "change; input[name='curCompany']" : function(event) {
            var view = this;
            view.$el.trigger("DO_SEARCH");
        }
    };
    CompanyView.prototype.events = $.extend(CompanyView.prototype.events, app.sidesection.BaseSideAdvanced.prototype.events);

    brite.inherit(CompanyView,app.sidesection.BaseSideAdvanced);

    brite.registerView("Company", {emptyParent: true},function(){
      return new CompanyView();
    });
})(jQuery);
