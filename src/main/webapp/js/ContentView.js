(function($) {

  brite.registerView("ContentView", {
    parent : "#contentview-ctn"
  }, {

    // --------- View Interface Implement--------- //
    create : function() {
      return render("ContentView");
    },

    postDisplay : function() {
      var view = this;
      
      brite.display("SearchDataGrid").done(function(searchDataGrid){
      	view.dataGridView = searchDataGrid;
      });
    }
    // --------- /View Interface Implement--------- //


  });



})(jQuery); 