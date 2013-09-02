<script type="text/javascript">
  $(function(){
  [#if user??]
      brite.display("MainView");

      var $searchInput = $("#search-input");
      var $searchResult = $("#search-result");
      var $searchInfo = $("#search-info");

      $searchInput.on("keypress",function(event){
          if (event.which === 13){
              var search = $searchInput.val();
              console.log("will search: " + search);
              $.ajax({
                  url: "search",
                  type: "GET",
                  data: {
                      q_search: search
                  },
                  dataType: "json"
              }).always(function(data){
                          console.log("new result: " + data.result.result.length);
                          var result = data.result;
                          var html = render("search-items",{items:result.result});
                          $searchResult.html(html);
                          var htmlTxt = "Result size: " + result.count + " | Duration: " + result.duration + "ms";
                          htmlTxt += " (c: " + result.countDuration + "ms," + " s: " + result.selectDuration + "ms)";
                          $searchInfo.html(htmlTxt);
                      });

          }
      });
  [#else]
      window.location.href = "sf1";
  [/#if]

    
  });
</script>