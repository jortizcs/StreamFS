// JavaScript Document
/**
 *  These functions are specific to joinsetup.html -- the publisher join registration page.
 *  Any changes to the joinsetup.html page will likely affect the references made by the javascript
 *  and assciated register.php file.
 */
function handlesubmit(){
	
	//Object Stream Field Values
	var device_name = document.getElementById("ostream_devname");
	var make = document.getElementById("ostream_make");
	var model = document.getElementById("ostream_model");
	var odesc = document.getElementById("ostream_description");
	var address = document.getElementById("ostream_address");
	var sensorlist = document.getElementById("ostream_sensorlist");
	
	//Context Stream Field Values
	var cdesc = document.getElementById("cstream_description");
	
	//Logic Stream Field Values
	var schemaurl = document.getElementById("lstream_schemaurl");

	//registration Request
	var regReq = "register.php?ostream_devname=" + device_name.value + 
				"&ostream_make=" + make.value + "&ostream_model=" + model.value +
				"&ostream_description=" + odesc.value + "&ostream_address=" + address.value +
				"&ostream_sensorlist=" + sensorlist.value + "&cstream_description=" + cdesc.value +
				"&lstream_schemaurl=" + schemaurl.value;
	//alert(regReq);

	var v = validate_vals(device_name, make, odesc, address, sensorlist, cdesc, schemaurl);
	if(v==true) {	
		//window.location = "http://www.google.com/"
		var xmlhttp = newXMLHttpRequest();
		xmlhttp.onreadystatechange=function()
		{
			if(xmlhttp.status == 200 && xmlhttp.readyState==4){
				//alert(xmlhttp.responseText);
				var jsonresp = eval('(' + xmlhttp.responseText + ')');
				if(jsonresp.status == "success" && jsonresp.ident>0){
					window.location = "reg_confirmation?PubID="+jsonresp.ident;
				} else if (jsonresp.status=="fail"){
					var estrings = "";
					for(var i=0; i<jsonresp.errors.length; i++){
						estrings += i+1 + ". " + jsonresp.errors[i];
						
						//Newlines for formatting
						if(i>0 && i != jsonresp.errors.length-1)
							estrings += "\n";
					}
					alert(estrings);
				}
			}
			//setTimeout(getAllPubIds,5000);
		}
		xmlhttp.open("GET",regReq,true);
		xmlhttp.send(null);
	}
}

/**
 *  Validates the values in the necessary registration fields.  Places an red error message next to all fields with errors.
 */
function validate_vals(device_name, make, odesc, address, sensors, cdesc, schemaurl){
	var valid = true;
	
	//ostream validation
	var devname_row = document.getElementById("ostream_table").rows[0].cells;
	var devmake_row = document.getElementById("ostream_table").rows[1].cells;
	var devdesc_row = document.getElementById("ostream_table").rows[3].cells;
	var devaddr_row = document.getElementById("ostream_table").rows[4].cells;
	var devslist_row = document.getElementById("ostream_table").rows[5].cells;
	
	if(device_name.value.length==0){
		devname_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else{
		devname_row[2].innerHTML = "";
	}
	
	if(make.value.length==0){
		devmake_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else {
		devmake_row[2].innerHTML = "";
	}
	
	if(odesc.value.length==0){
		devdesc_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else {
		devdesc_row[2].innerHTML = "";
	}
	
	if(address.value.length==0){
		devaddr_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else {
		devaddr_row[2].innerHTML = "";
	}
	
	if(sensors.value.length==0){
		devslist_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else {
		devslist_row[2].innerHTML = "";
	}
	
	//cstream validation
	var cdesc_row = document.getElementById("cstream_table").rows[0].cells;
	
	if(cdesc.value.length==0){
		cdesc_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else {
		cdesc_row[2].innerHTML = "";
	}
	
	//lstream validation
	var schemaurl_row = document.getElementById("lstream_table").rows[0].cells;
	
	if(schemaurl.value.length==0){
		schemaurl_row[2].innerHTML = "<strong><font color=\"red\">Error: Empty</font></strong>";
		valid = false;
	} else {
		schemaurl_row[2].innerHTML = "";
	}
	
	return valid;
}