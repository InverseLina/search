(function($){
	var searchDao = app.SearchDaoHandler;
	
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
		 },
		 "btap;.btns span":function(event){
			 var $btn = $(event.currentTarget);
			 var $li = $btn.parent("li");
			 var flag = $btn.attr("data-show");
			 var $ul = $btn.closest("ul.advancedItems");
			 var type = $ul.hasClass("company")?"company":"education";
			 var dataName = (type=="company")?"companies":"educations";
			 if(flag=="more"){
				 searchDao.getAdvancedMenu({type:type,offset:app.preference.get(type,5),limit:20}).pipe(function(data){
					 $li.before(render("AdvancedSearch-"+type,data));
                     $li.closest("ul").find(".toShow").show(1000, function(){
                         $(this).removeClass("toShow");
                     })
					 app.preference.store(type,(parseInt(app.preference.get(type,5))+data[dataName].length));
					 $btn.next().show();
					 if(data.length<20){
						 $btn.hide();
					 }
				 });
			 }else{
				 var itemNum = parseInt(app.preference.get(type,5));
				 var hideNum = 0;
				 if((itemNum-5)%20==0){
					 hideNum = 20;
				 }else{
					 hideNum = (itemNum-5)%20;
				 }
				 app.preference.store(type,(itemNum-hideNum));
				 $("li:not('.btns'):gt("+(itemNum-hideNum)+")",$ul).hide(1000,function(){
					 $(this).remove();
				 });
				 $btn.prev().show();
				 if((itemNum-hideNum)<=5){
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