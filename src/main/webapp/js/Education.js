/**
 * View: Education
 *
 * Education
 *
 *
 */
(function ($) {
    function EducationView(){
      this.constructor._super.constructor.call(this,"education","educations");
    };
    
    brite.inherit(EducationView,app.sidesection.BaseSideAdvanced);
    
    
    brite.registerView("Education", {emptyParent: true},function(){
      return new EducationView();
    });
})(jQuery);
