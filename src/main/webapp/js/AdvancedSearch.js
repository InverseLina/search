(function($){
	var searchDao = app.SearchDaoHandler;
	
	brite.registerView("AdvancedSearch",{parent:".advanced",emptyParent:true},{
		create: function(data){
			return render("AdvancedSearch",data);
	 }, 
	 
	 postDisplay: function(data){
		 var view = this;
		 view.$el.find(".advancedItems li.all :checkbox").prop("checked",true).closest("li").addClass("selected");
		 view.$el.trigger("DO_SEARCH");
	 },
	 
	 events: {
		 "btap;.advancedItems label":function(event){
		   //left click
			 if(event.which==1){
				 var $li = $(event.target).closest("li");
				 var $ul = $li.parent("ul");
				 if($li.hasClass("all")){
					 $("li:gt(0)",$ul).removeClass("selected").find(":checkbox").prop("checked",false);
				 }else{
					 if($li.prev().hasClass("all")){
						 $("li:eq(0)",$ul).removeClass("selected").find(":checkbox").prop("checked",false);
						 $("li:gt(1)",$ul).removeClass("selected").find(":checkbox").prop("checked",false);
					 }else{
						 $("li:lt(2)",$ul).removeClass("selected").find(":checkbox").prop("checked",false);
					 }
				 }
				 
				 if($li.hasClass("selected")){
					 $li.removeClass("selected");
				 }else{
					 $li.addClass("selected");
				 }
				 
				 this.$el.trigger("DO_SEARCH");
			 }
		 },
		 "btap;.btns span":function(event){
			 var view = this;
			 var $btn = $(event.currentTarget);
			 var $li = $btn.parent("li");
			 var flag = $btn.attr("data-show");
			 var $ul = $btn.closest("ul.advancedItems");
			 var type = $ul.hasClass("company")?"company":($ul.hasClass("skill")?"skill":"education");
			 var dataName = (type=="company")?"companies":((type=="skill")?"skills":"educations");
			 // show more items
			 if(flag=="more"){
			   // get advanced menu data from server
				 searchDao.getAdvancedMenu({type:type,offset:app.preference.get(type,6),limit:20}).pipe(function(data){
					 $li.before(render("AdvancedSearch-"+type,data));
                     $li.closest("ul").find(".toShow").show(1000, function(){
                         $(this).removeClass("toShow");
                     })
           //save the offset
					 app.preference.store(type,(parseInt(app.preference.get(type,6))+data[dataName].length));
					 $btn.next().show();
					 if(data.length<20){
						 $btn.hide();
					 }
					 view.$el.trigger("DO_SEARCH");
				 });
			 // show less items
			 }else{
				 var itemNum = parseInt(app.preference.get(type,6));
				 var hideNum = 0;
				 if((itemNum-6)%20==0){
					 hideNum = 20;
				 }else{
					 hideNum = (itemNum-6)%20;
				 }
				 app.preference.store(type,(itemNum-hideNum));
				 var num = 0;
				 var $hideLi = $("li:not('.btns'):gt("+(itemNum-hideNum)+")",$ul);
				 $hideLi.hide(1000,function(){
					 $(this).remove();
					 num++;
					 if(num==$hideLi.length){
						 view.$el.trigger("DO_SEARCH");
					 }
				 });
				 $btn.prev().show();
				 if((itemNum-hideNum)<=6){
					 $btn.hide();
				 }
			 }
		 }
	 },
	 getSearchValues:function(){
	   var view = this;
	   var $e = view.$el;
	   var $companyContainer = $e.find("ul.company");
	   var $educationContainer = $e.find("ul.education");
	   var $skillContainer = $e.find("ul.skill");
	   var companyALL = $companyContainer.find("li[data-name='ALL']").hasClass("selected");
	   var companyStr = "";
	   // get companies filter
	   $companyContainer.find("li[data-name!='ALL']").each(function(i){
	     var $li = $(this);
	     var value = $(this).attr("data-name");
	     //get selected or all option is selected.
	     if($li.hasClass("selected") || companyALL){
  	     if(companyStr.length != 0){
  	       companyStr += ",";
  	     }
  	     companyStr += value;
	     }
	   });
	   
	   if(companyALL){
		   companyStr="Any Company";
	   }
	   
	   var educationALL = $educationContainer.find("li[data-name='ALL']").hasClass("selected");
	   var educationStr = "";
	   // get educations filter
	   $educationContainer.find("li[data-name!='ALL']").each(function(i){
	     var $li = $(this);
	     var value = $(this).attr("data-name");
	     //get selected or all option is selected.
	     if($li.hasClass("selected") || educationALL){
  	     if(educationStr.length != 0){
  	       educationStr += ",";
  	     }
  	     educationStr += value;
	     }
	   });
	   if(educationALL){
		   educationStr="Any Education";
	   }
	   

	   var skillALL = $skillContainer.find("li[data-name='ALL']").hasClass("selected");
	   var skillStr = "";
	   // get educations filter
	   $skillContainer.find("li[data-name!='ALL']").each(function(i){
	     var $li = $(this);
	     var value = $(this).attr("data-name");
	     //get selected or all option is selected.
	     if($li.hasClass("selected") || skillALL){
  	     if(skillStr.length != 0){
  	    	skillStr += ",";
  	     }
  	     skillStr += value;
	     }
	   });
	   if(skillALL){
		   skillStr="Any Skill";
	   }
	   return {companyNames:companyStr,educationNames:educationStr,skillNames:skillStr};
	 }
	});
})(jQuery);