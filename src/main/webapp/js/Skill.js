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

    SkillView.prototype.validate =function(values){
        var vals = values.split("|");
        var v1=false, errors = [];
        if(vals[1].length > 0 ){
            if(!/\d+/g.test(vals[1])){
                errors.push(vals[0] + " min rate require be number");
            }else{
                v1 = true;
            }
        }
        if(vals[2].length > 0 ){
            if(!/\d+/g.test(vals[2])){
                errors.push(vals[0] + " max rate require be number");
            }
            if(v1){
                if(parseInt(vals[2]) - parseInt(vals[1])<=0){
                    errors.push(vals[0] + " max rate must big than min rate");
                }
            }
        }

        return errors;
    }



    brite.registerView("Skill", {emptyParent: true},function(){
      return new SkillView();
    });
})(jQuery);
