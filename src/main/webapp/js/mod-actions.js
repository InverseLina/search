$(function(){
	$(document).on("APPLY_PRESS",function(event,extra){
        extra.action = 'applyToJob';
        parent.postMessage(JSON.stringify(extra), '*');
    });
       $(document).on("EMAIL_PRESS",function(event,extra){
        extra.action = 'email';
        parent.postMessage(JSON.stringify(extra), '*');
    });
    $(document).on("SHORTLIST_PRESS",function(event,extra){
        extra.action = 'createShortlist';
        parent.postMessage(JSON.stringify(extra), '*');
    });
});