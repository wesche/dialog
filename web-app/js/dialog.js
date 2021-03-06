/*
* Grails Dialog plug-in
* Copyright 2011 Open-T B.V., and individual contributors as indicated
* by the @author tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Affero General Public License
* version 3 published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.

* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see http://www.gnu.org/licenses
*/

/**
 * Capitalize a string
 */
String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

/**
 * Removed leading and trailing spaces
 * @param value
 * @returns
 */
dialog.trim = function trim(value) {
	  value = value.replace(/^\s+/,'');
	  value = value.replace(/\s+$/,'');
	  return value;
}

/**
 * Pack params in an url-friendly String
 * @param params
 * @returns {String}
 */
dialog.obj2ParamStr = function obj2ParamStr(params) {
	var paramStr="";
	 if (params) {
		 var sep = "?";
		 for (key in params) {
			 paramStr=paramStr+sep+key+"="+params[key];
			 sep="&";
		 }		 
	 }
	 return paramStr;
}

/**
 * Modal JQuery UI confirmation dialog
 */
dialog.confirm = function confirm(message,title,url) {
	var htmlMessage='<div id="dialog-confirm" title="'+title+'"><p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>'+message+'</p></div>';
	
	var confirmDialog=$(htmlMessage).dialog({
		resizable: true,
		width:600,
		//height:140,
		modal: true,
		buttons: {
			"OK": function() {								
				$( this ).dialog( "close" );
			},
			Cancel: function() {
				$( this ).dialog( "close" );
			}
		},
        close: function(event, ui) {      
            confirmDialog.remove();
            }
        });			
	
}

/**
 * Show dialog
 * @param id
 * @param controllerName
 * @param options
 * @param urlParams
 * @returns {Boolean}
 */
dialog.formDialog = function formDialog(id,controllerName, options ,urlParams) {
	var urlId=id+dialog.obj2ParamStr(urlParams);
	
	var dialogName = (options != null && options["dialogname"] != null) ? options["dialogname"] : "dialog";
	var submitName = (options != null && options["submitname"] != null) ? options["submitname"] : "submit"+dialogName;
	
	
	var refreshTableKey = (options != null && options["refresh"] != null) ? options["refresh"] : "NO_REFRESH";
	
	// if true, form submit will be used instead of AJAX
	var submitForm = (options != null && options["submitform"] != null) ? options["submitform"] : false;
	
	// if true, form will not be submitted at all
	var noSubmit = (options != null && options["nosubmit"] != null) ? options["nosubmit"] : false;
	
	var domainClass = (options != null && options["domainclass"] != null) ? options["domainclass"] : controllerName.capitalize();
	
	 theUrl=dialog.baseUrl+'/'+controllerName+'/'+dialogName+'/'+urlId	 	
	 
	 var dialogHTML = $.ajax({
		  url: theUrl,
		  async: false,
		  cache: false
		 }).error(function(event, jqXHR, ajaxSettings, thrownError) { 
				window.location.reload();
			}).responseText;
	 
	 var formelements=$(dialogHTML).find('form')
	 if (formelements.length==0) {
		 window.location.reload()
	 } else {
	 
	 var theWidth=$(dialogHTML).css("width").replace("px","");
	 var theHeight=$(dialogHTML).css("height").replace("px","");
	 if (theHeight=="0") theHeight="auto";
	 
	 var theDialog=$(dialogHTML).dialog({ 
		 modal:true,
		 width:theWidth,
		 height:theHeight,		 
		 buttons: 
			 [ { text: window.dialog.messages.ok, click: function(e,ui) {
	        	$(this).trigger("dialog-submit",{event:e,ui:ui,'this':this,id:id,controllerName:controllerName});

		 		if (submitForm) {
		 			theDialog.find("form").attr("action",dialog.baseUrl+"/"+controllerName+"/"+submitName+"/"+urlId);
		        	theDialog.find("form").submit();
		        	$( this ).dialog( "close" );
		 		} else {
		 		if (!noSubmit) {
				 	var formData=theDialog.find("form").serialize();
				 	$.post(dialog.baseUrl+"/"+controllerName+"/"+submitName+"/"+urlId,formData, function(data) 
				 		{
				 		var jsonResponse = data.result;
				 		
				 		$(".dialog-events").trigger("dialog-refresh",{dc:domainClass,id:id,jsonResponse:jsonResponse})
				 		$(".dialog-events").trigger("dialog-message",{message:jsonResponse.message})
				 					 		
				 		if(jsonResponse.success){
					 		theDialog.dialog("close");
					 		if (jsonResponse.nextDialog) {
					 			dialog.formDialog(jsonResponse.nextDialog.id,jsonResponse.nextDialog.controllerName,jsonResponse.nextDialog.options,jsonResponse.nextDialog.urlParams)
					 		}
					 	} else  {
					 		for (key in jsonResponse.errorFields) {
					 			var errorField=jsonResponse.errorFields[key]
					 			$("#"+errorField).parent().addClass("errors")				 			
					 		}
					 		theDialog.find("div.errors").html(jsonResponse.message)
					 		theDialog.find("div.errors").show();				 		
					 	
				 		}
				 	});
		 		
		 		} else {
		 			$( this ).dialog( "close" );
		 		}
		 		}
        	}
			 }
        , { text: window.dialog.messages.cancel, click:  function() {
	        		$( this ).dialog( "close" );
	        	}
        }
       	],
        open: function(event, ui) {
        	// This will trigger all modules that want to receive open events; the second parameter is the params object that will be received by the event handler
        	$(this).trigger("dialog-open",{event:event,ui:ui,'this':this,id:id,controllerName:controllerName});
        	
    		 $(this).keyup(function(e) {
    		    if (e.keyCode == 13 && e.target.nodeName!="TEXTAREA") {
    		    	$(this).parents('.ui-dialog').first().find('.ui-button').first().click();
           		 return false;
    		    }
    		});
    	        	
         	// Initialize date picker input elements
    		$.datepicker.setDefaults( $.datepicker.regional[ dialog.language ] );
       		$(this).find(".dialogtabs").tabs();
       		$(this).find(".altselect").altselect();       	
       		
       		// get z-index of dialog so we can put cluetips above it
       		var parentZIndex=parseInt($(this.parentNode).css('z-index'));
       		
       		$(this).find(".help").tooltip({container:'body',placement:'left'});

       		$(this).find("input[type!='hidden'],select,textarea").filter(":first").focus();

       		
         },
        close: function(event, ui) {
        	$(this).trigger("dialog-close",{event:event,ui:ui,'this':this})       	
            theDialog.dialog("destroy").remove();
         }
       });
	 
	 }
	 return false;
}

/**
 * Show a delete dialog
 * @param id
 * @param controllerName
 * @param options
 * @param urlParams
 */
dialog.deleteDialog = function deleteDialog(id,controllerName, options ,urlParams) {
	var urlId=id+dialog.obj2ParamStr(urlParams);
	var controllerTitle=controllerName.charAt(0).toUpperCase() + controllerName.slice(1); 
	 var dialogHTML = '<div class="delete-dialog" title="'+dialog.messages.confirmdeleteTitle+'"><form><div class="errors" style="display:none;"></div><div>'+dialog.messages.confirmdelete+' '+controllerTitle+' '+id+' ?</div></form></div>'

	 var domainClass = (options != null && options["domainclass"] != null) ? options["domainclass"] : controllerName.capitalize();

	 var theDialog=$(dialogHTML).dialog({ 
		 modal:true,
		 width:400,
		 height:200,
		 buttons: { 
		 	"Delete": function(e) {
			 	var formData=theDialog.find("form").serialize();
			 	$.post(dialog.baseUrl+"/"+controllerName+"/delete/"+urlId,formData, function(data) 
			 		{
			 		var result=data.result			 		

			 		
			 		$(".dialog-events").trigger("dialog-refresh",{dc:domainClass,id:id,jsonResponse:result})
			 		$(".dialog-events").trigger("dialog-message",{message:result.message})
			 		
			 		if(result.success){
				 		theDialog.dialog("close");
				 	} else  {				 		
				 		theDialog.find("div.errors").html(result.message)
				 		theDialog.find("div.errors").show();				 		
				 	}			 		
			 	});			 	
       	},
      	Cancel: function() {
	        		$( this ).dialog( "close" );
	        	}
      	},
       close: function(event, ui) {      
              theDialog.dialog("destroy").remove();
            }
        });
}


/**
 * Show status message event handler
 * @param event
 * @param eventData
 */
dialog.statusMessage = function statusMessage(event,eventData) {
	if (eventData.message) {
        var alertType="success";
        if (eventData.alertType!==null) {
            alertType=eventData.alertType;
        }
		var msg='<div id="alertmessage" class="alert alert-'+alertType+' fade"><button type="button" class="close" data-dismiss="alert">×</button><div>'+eventData.message+'</div></div>';
		$("#statusmessage").html(msg);
		$("#alertmessage").addClass("in");
		// TODO This is more annoying than helpful. Maybe make this configurable.
		//setTimeout( function() {$("#alertmessage").alert("close")}, 2000 );
	}
};

/**
 * Delete file dialog
 * @param id
 * @param controllerName
 * @param filename
 * @param options
 */
dialog.deleteFile = function deleteFile(id,controllerName, filename,options) {
		 
	 var dialogHTML = '<div class="delete-dialog" title="Confirm delete"><form><div class="errors" style="display:none;"></div><div>Are you sure you want to delete '+filename+' from '+controllerName+' '+id+' ?</div></form></div>'	 
	 var domainClass = (options != null && options["domainclass"] != null) ? options["domainclass"] : controllerName.capitalize();

	 var theDialog=$(dialogHTML).dialog({ 
		 modal:false,
		 width:400,
		 height:200,
         zIndex:10000,
		 buttons: { 
		 	"Delete": function(e) {
			 	var formData=theDialog.find("form").serialize();
			 	$.post(dialog.baseUrl+"/"+controllerName+"/deletefile/"+id+"?filename="+filename,formData, function(data) 
			 		{
			 		var result=data.result			 		

			 		
			 		$(".dialog-events").trigger("dialog-refresh",{dc:domainClass,id:id})
			 		$(".dialog-events").trigger("dialog-message",{message:result.message})
			 		
			 		if(result.success){
				 		theDialog.dialog("close");
				 	} else  {				 		
				 		theDialog.find("div.errors").html(result.message)
				 		theDialog.find("div.errors").show();				 		
				 	}			 		
			 	});			 	
       	},
      	Cancel: function() {
	        		$( this ).dialog( "close" );
	        	}
      	},
       close: function(event, ui) {      
              theDialog.dialog("destroy").remove();
            }
        });
}

$(function() {
	
	jQuery.fn.center = function () {
	    this.css("position","absolute");
	    this.css("top", ( $(window).height() - this.height() ) / 2+$(window).scrollTop() + "px");
	    this.css("left", ( $(window).width() - this.width() ) / 2+$(window).scrollLeft() + "px");
	    return this;
	}

	jQuery.fn.hcenter = function () {
	    this.css("position","absolute");
	    this.css("left", ( $(window).width() - this.width() ) / 2+$(window).scrollLeft() + "px");
	    return this;
	}

	
	// Initialize date picker input elements
 	$(".datepicker").datepicker({ dateFormat: "yyyy-MM-dd'T'HH:mm:ss" , changeMonth: true, changeYear:true});
  	
  	$("a.confirm").live("click",function(){
  	return confirm('Are you sure?')
  	});
  	
	$("#statusmessage").bind("dialog-message",dialog.statusMessage);	
	$("#statusmessage").addClass("dialog-events");
	
});


