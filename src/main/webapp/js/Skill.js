/**
 * View: Skill
 *
 * Skill
 *
 *
 */
(function ($) {
    function SkillView(){
      this.constructor._super.constructor.call(this,"skill","skills");
    };
    
    brite.inherit(SkillView,app.sidesection.BaseSideAdvanced);
    
    
    brite.registerView("Skill", {emptyParent: true},function(){
      return new SkillView();
    });
})(jQuery);
