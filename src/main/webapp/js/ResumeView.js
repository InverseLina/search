/**
 * View: ResumeView
 *
 *
 *
 *
 */
(function ($) {

  brite.registerView("ResumeView", {
    emptyParent : false,
    parent : "body"
  }, {
    create : function(data, config) {
      $("#resumeModal").bRemove();
      data = data || {};

      return render("ResumeView", {name : data.name});
    },

    postDisplay : function(data) {
      showView.call(this,data);
      
    },
    events : {
      "btap; .btn-primary, .close" : function() {
        var view = this;
        view.$el.bRemove();
      },

    },
    docEvents : {}
  }); 
  
  
  function showView(data){
    var view = this;
    var $e = view.$el;
    var $body = $e.find(".modal-body");
    if (org.apex_resume_url && org.apex_resume_url.length && org.apex_resume_url.length > 0) {
      var url = org.apex_resume_url + data.sfid;
      var $content = $(render("ResumeView-iframe",{url:url}));
      $body.append($content);
    } else {
      app.getJsonData("getResume", {cid : data.id}).done(function(result) {
        if (result.length > 0) {
          var $content = $(render("ResumeView-content", {resume : result[0]["ts2__text_resume__c"]}));
        } else {
          var $content = $(render("ResumeView-content", {resume : "not resume"}));
        }
        $body.append($content);
          
      });
    }
  }

})(jQuery);
