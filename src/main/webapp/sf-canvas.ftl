<script type="text/javascript">
  $(function(){
    
   brite.display("MainView",null,{uiFlags:app.uiFlags});
    
    var $searchInput = $("#search-input"); 
    var $searchResult = $("#search-result");
    var $searchInfo = $("#search-info");
    
    $searchInput.on("keypress",function(event){
      if (event.which === 13){
        var search = $searchInput.val();
        $.ajax({
          url: contextPath + "search",
          type: "GET",
          data: {
            q_search: search
          },
          dataType: "json"
        }).always(function(data){
          var result = data.result;
          var html = render("search-items",{items:result.result});
          $searchResult.html(html);
          var htmlTxt = "Result size: " + result.count + " | Duration: " + result.duration + "ms";
          htmlTxt += " (c: " + result.countDuration + "ms," + " s: " + result.selectDuration + "ms)";
          $searchInfo.html(htmlTxt);
        });
            
      };
    });
    
  });
</script>