/**
 * View: Company
 *
 * Company
 *
 *
 */
(function ($) {
    var searchDao = app.SearchDaoHandler;
    function CompanyView(){
      this.constructor._super.constructor.call(this,"company","companies");
        
      this.events = {
        "change; input[name='curCompany']" : function(event) {
          var view = this;
          view.$el.trigger("DO_SEARCH");
        }
      }; 
    };
    
    brite.inherit(CompanyView,app.sidesection.BaseSideAdvanced);
    
    CompanyView.prototype.updateSearchValues = function(data){
      var view = this;
      view.constructor._super.updateSearchValues.call(view,data);
      if (data.curCompany) {
        view.$el.find("input[name='curCompany']").prop("checked", true);
      }
      if (data.searchCompany) {
        view.$el.find('input[type=text]').val(data.searchCompany);
      }

    };
    
    CompanyView.prototype.getSearchValues = function(){
      var view = this;
      var $e = view.$el;
      var result = view.constructor._super.getSearchValues.call(view);
  
      var searchCompany = view.$el.find('input[type=text]').val();
      if (!/^\s*$/.test(searchCompany)) {
        result.searchCompany = searchCompany;
      }
      var curCompany = view.$el.find("input[name='curCompany']").prop("checked");
      if (curCompany) {
        result.curCompany = curCompany;
      }
  
      return result;

    };
    
    
    brite.registerView("Company", {emptyParent: true},function(){
      return new CompanyView();
    });
})(jQuery);
