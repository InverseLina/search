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
			 }
		 }
	 }
	});
})(jQuery);