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
        },
        "keyup;input[name='company']":function(event){
        	var $input = $(event.target);
        	var view = this;
        	if($input.val().length>0){
        		 view.$el.find("li[data-name='ALL']").removeClass("selected").find(":checkbox").prop("checked", false);
        	}else{
        		if (view.$el.find("li:not(.all).selected").length === 0) {
        			view.$el.find("li.all").addClass("selected").find(":checkbox").prop("checked", true);
        		}
        	}
        }
      };

      this.events = $.extend(this.events, app.sidesection.BaseSideAdvanced.prototype.events);
        console.log(this.events);
    }


    brite.inherit(CompanyView,app.sidesection.BaseSideAdvanced);

    
    CompanyView.prototype.updateSearchValues = function(data){
      var view = this;
      view.constructor._super.updateSearchValues.call(view,data);
      if (data.curCompany) {
        view.$el.find("input[name='curCompany']").prop("checked", true);
      }
      if (data.searchCompany) {
        view.$el.find('input[type=text]').val(data.searchCompany);
        view.$el.find("li.all").removeClass("selected").find(":checkbox").prop("checked", false);
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
