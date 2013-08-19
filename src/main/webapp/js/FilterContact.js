(function ($) {
    function FilterContact(){
      this.constructor._super.constructor.call(this,"Contact");
    }

    brite.inherit(FilterContact,app.ThPopup);

    FilterContact.prototype.events = $.extend({
        "btap; input.save":function(){
            var view = this, ele;
            var data = {};
            data.firstName = view.$el.find(":input[name='FirstName']").val();
            data.lastName = view.$el.find(":input[name='LastName']").val();
            data.email = view.$el.find(":input[name='Email']").val();
            data.title = view.$el.find(":input[name='Title']").val();
            var displayName = $.trim(data.FirstName + " " +  data.LastName);
            var $eles = view.$el.find(".selectedItems .item[data-name='" + displayName + "']");
            var len = $eles.length;
            if (len == 0) {
               view.$el.find(".selectedItems span.add").before(render("filterPanel-selectedItem-add",
                {name: displayName }));
                $eles = view.$el.find(".selectedItems .item[data-name='" + displayName + "']");
                view.$el.trigger("ADD_FILTER", {type:view.type, name: displayName, value: data})
            }
            ele = $($eles[0]);
            ele.data("value", data);


        }
    }, FilterContact.prototype.events||{});

    brite.registerView("FilterContact", {emptyParent: false},function(){
      return new FilterContact();
    });
})(jQuery);
