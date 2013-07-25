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

    brite.inherit(CompanyView,app.sidesection.BaseSideAdvanced);

    CompanyView.prototype.events = {
        "change; input[name='curCompany']" : function(event) {
            var view = this;
            view.$el.trigger("DO_SEARCH");
        }
    };
    CompanyView.prototype.events = $.extend(CompanyView.prototype.events, app.sidesection.BaseSideAdvanced.prototype.events);

/*    CompanyView.prototype.validate =function(values){
        var vals = values.split("|");
        var v1=false, errors = [];
        if(vals[1].length > 0 ){
            if(!/\d+/g.test(vals[1])){
                errors.push(vals[0] + " min value require be number");
            }else{
                v1 = true;
            }
        }
        if(vals[2].length > 0 ){
            if(!/\d+/g.test(vals[2])){
                errors.push(vals[0] +  " max value require be number");
            }
            if(v1){
                if(parseInt(vals[2]) - parseInt(vals[1])<=0){
                    errors.push(vals[0] + " max value must big than min value");
                }
            }
        }

        return errors;
    }*/

    brite.registerView("Company", {emptyParent: true},function(){
      return new CompanyView();
    });
})(jQuery);
