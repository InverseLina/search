(function($){
       
    Handlebars.registerHelper('listNum', function(start,end,currentPage, options) {
	  currentPage = parseInt(currentPage);
	  var fn = options.fn, inverse = options.inverse;
	  var ret = "", data;
	  if (options.data) {
	    data = Handlebars.createFrame(options.data);
	  }
	  var nums=new Array();
	  for(var i=start;i<=end;i++){
		 if(currentPage==i){
			nums.push({num:i,css:"currentPage"});
		 } else{
			nums.push({num:i});
		 }
	  }
	  if(nums && nums.length > 0) {
	    for(var i=0, j=nums.length; i<j; i++) {
	      if (data) { data.index = i; }
	      ret = ret + fn(nums[i], { data: data });
	    }
	  } else {
	    ret = inverse(this);
	  }
	  return ret;
    });

    Handlebars.registerHelper('fromTo', function(start,end,pageNo, options) {
	  var fn = options.fn, inverse = options.inverse;
	  var ret = "", data;
	  if (options.data) {
	    data = Handlebars.createFrame(options.data);

	  }
	  var nums=new Array();
	  for(var i=start;i<=end;i++){
		 nums.push({num:i, pageNo:pageNo});
	  }
	  if(nums && nums.length > 0) {
	    for(var i=0, j=nums.length; i<j; i++) {
	      if (data) { data.index = i; }
	      ret = ret + fn(nums[i], { data: data });
	    }
	  } else {
	    ret = inverse(this);
	  }
	  return ret;
    });
	
    Handlebars.registerHelper('subString', function(src,start,num,ellipsis) {
    	ellipsis =ellipsis||false;
    	if(num=="end"){
    		return new Handlebars.SafeString(src.substring(start,src.length-start));
    	}else{
			if(num+start<src.length&&ellipsis){
				return new Handlebars.SafeString(src.substring(start,num)+"...");
			}else{
				return new Handlebars.SafeString(src.substring(start,num));
			}
    	}
    });

    Handlebars.registerHelper('comments', function(postId,template,options) {
        var html;
        app.Api.getComments(postId).done(function(result){
            html = Handlebars.templates[template](result);
            console.log(html);
        });

        return html;
    });

})(jQuery);