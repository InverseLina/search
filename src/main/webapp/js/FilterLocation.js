(function ($) {
    function FilterLocation(){
      this.constructor._super.constructor.call(this,"location");
    }

    brite.inherit(FilterLocation,app.ThPopup);

    FilterLocation.prototype.afterPostDisplay = function(){
        var view = this;
        //set max length

        app.getJsonData("/config/getByName/local_distance").done(function(result){
            var label = "Radius(miles)"
            if(result && result.value == "k"){
                label = "Radius(km)";
            }
            view.$el.find(".labelText").html(label);
        })
    }

    brite.registerView("FilterLocation", {emptyParent: false},function(){
      return new FilterLocation();
    });
})(jQuery);
