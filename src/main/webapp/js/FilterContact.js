(function ($) {
    function FilterContact(){
      this.constructor._super.constructor.call(this,"Contact");
    }

    brite.inherit(FilterContact,app.ThPopup);

    FilterContact.prototype.afterPostDisplay = function(){
        var view = this;
        view.$el.find("input:first").focus();
    }

    FilterContact.prototype.events = $.extend({
        "btap; input.save":function(){
            addItem.call(this);
        },
        "btap;.cancel":function(event){
        	var view = this;
        	var $span =$(event.target);
        	view.$el.find(".save").parent().addClass("hide");
        	view.$el.find(".selectedItems .add").removeClass("hide");
        	view.$el.find(":text").val("");
        },
        "btap;.selectedItems .item":function(event){
        	var view = this;
        	var $span = $(event.target);
        	var value = $("[data-column='contact']").find("[data-name='"+$span.attr("data-name")+"']").data("value");
        	view.$el.find(":input[name='FirstName']").val(value.firstName);
            view.$el.find(":input[name='LastName']").val(value.lastName);
            view.$el.find(":input[name='Email']").val(value.email);
            view.$el.find(":input[name='Title']").val(value.title);
        },
        "keyup; input[type='text']": function(event){
            var view = this;
            if(event.keyCode == 13){
                addItem.call(view);
            }else if(event.keyCode == 27){
                view.close();
            }
        }
    }, FilterContact.prototype.events||{});

    brite.registerView("FilterContact", {emptyParent: false},function(){
      return new FilterContact();
    });

    function addItem(){
        var view = this, ele;
        var data = {};
        data.firstName = view.$el.find(":input[name='FirstName']").val();
        data.lastName = view.$el.find(":input[name='LastName']").val();
        data.email = view.$el.find(":input[name='Email']").val();
        data.title = view.$el.find(":input[name='Title']").val();
        var displayName = app.getContactDisplayName(data);
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
//        view.$el.find(".save").parent().addClass("hide");
        view.$el.find(".selectedItems .add").removeClass("hide");
        view.$el.find(":text").val("");
        view.$el.find("input:first").focus();
    }
})(jQuery);
