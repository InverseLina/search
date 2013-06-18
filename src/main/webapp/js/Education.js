/**
 * View: Education
 *
 * Education
 *
 *
 */
(function ($) {
    var searchDao = app.SearchDaoHandler;
    function EducationView(){
      this.constructor._super.constructor.call(this,"education","educations");
    };
    
    brite.inherit(EducationView,app.sidesection.BaseSideAdvanced);
    
    
    brite.registerView("Education", {emptyParent: true},function(){
      return new EducationView();
    });
})(jQuery);
