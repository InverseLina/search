(function($){
	
	brite.registerView("AdvancedSearch",{parent:".advanced",emptyParent:true},{
		create: function(data){
			return render("AdvancedSearch",data);
	 }, 
	 
	 postDisplay: function(){},
	 
	 events: {
		 "btap;.advancedItems label":function(event){
			 if(event.which==1){
				 var $li = $(event.target).closest("li");
				 if($li.hasClass("selected")){
					 $li.removeClass("selected");
				 }else{
					 $li.addClass("selected");
				 }
				 
				 this.$el.trigger("DO_SEARCH");
			 }
		 }
	 },
	 getSearchValues:function(){
	   var view = this;
	   var $e = view.$el;
	   var $companyContainer = $e.find("ul.company");
	   var $educationContainer = $e.find("ul.education");
	   var companyALL = $companyContainer.find("li[data-name='ALL']").hasClass("selected");
	   var companyStr = "";
	   $companyContainer.find("li[data-name!='ALL']").each(function(i){
	     var $li = $(this);
	     var value = $(this).attr("data-name");
	     if($li.hasClass("selected") || companyALL){
  	     if(companyStr.length != 0){
  	       companyStr += ",";
  	     }
  	     companyStr += value;
	     }
	   });
	   
	   if(companyALL){
		   companyStr="All Company";
	   }
	   
	   var educationALL = $educationContainer.find("li[data-name='ALL']").hasClass("selected");
	   var educationStr = "";
	   $educationContainer.find("li[data-name!='ALL']").each(function(i){
	     var $li = $(this);
	     var value = $(this).attr("data-name");
	     if($li.hasClass("selected") || educationALL){
  	     if(educationStr.length != 0){
  	       educationStr += ",";
  	     }
  	     educationStr += value;
	     }
	   });
	   
	   if(educationALL){
		   educationStr="All Education";
	   }
	   return {companyNames:companyStr,educationNames:educationStr};
	 }
	});
})(jQuery);