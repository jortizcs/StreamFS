<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
Design by Free CSS Templates
http://www.freecsstemplates.org
Released for free under a Creative Commons Attribution 2.5 License
-->
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title>IS4 Console - Cory Hall Load Tree</title>
<!--<script type="text/javascript" src="../is4console/flot/jquery.js"></script>
<script type="text/javascript" src="../is4console/flot/jquery.flot.js"></script>
<script id="source" type="text/javascript" src="../is4console/grapher.js"></script>
<script id="source" type="text/javascript" src="../is4console/register_validation.js"></script>-->
<script type="text/javascript" src="../jquery_lib/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/external/jquery.bgiframe-2.1.1.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.mouse.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.draggable.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.resizable.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.ui.dialog.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.effects.core.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.effects.blind.js"></script>
<script type="text/javascript" src="../jquery_lib/development-bundle/ui/jquery.effects.explode.js"></script>
<link type="text/css" href="../jquery_lib/development-bundle/demos/demos.css" rel="stylesheet" />
<meta name="keywords" content="" />
<meta name="description" content="" />
<link href="../is4console/default.css" rel="stylesheet" type="text/css" />
<link type="text/css" href="../jquery_lib/development-bundle/themes/base/jquery.ui.all.css" rel="stylesheet" />
<style type="text/css">
<!--
#header #sn {
	font-family: Verdana, Geneva, sans-serif;
}
-->
</style>

<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<title>IS4 Load Tree Viewer</title>
	<!--<script type="text/javascript" src="lib/jsTree.v.1.0rc/_lib/jquery.js"></script>-->
	<script type="text/javascript" src="lib/jsTree.v.1.0rc/_lib/jquery.cookie.js"></script>
	<script type="text/javascript" src="lib/jsTree.v.1.0rc/_lib/jquery.hotkeys.js"></script>
	<script type="text/javascript" src="lib/jsTree.v.1.0rc/jquery.jstree.min.js"></script>
    <script type="text/javascript" src="lib/flot/jquery.flot.js"></script>
    <script type="text/javascript" src="grapher_mconsole.js"></script>

	<style type="text/css">
	html, body { margin:0; padding:0; }
	body, td, th, pre, code, select, option, input, textarea { font-family:verdana,arial,sans-serif; font-size:10px; }
	.demo, .demo input, .jstree-dnd-helper, #vakata-contextmenu { font-size:10px; font-family:Verdana; }
	#container {
		width:700px;
		overflow:hidden;
		position:relative;
		margin-top: 10px;
		margin-right: auto;
		margin-bottom: 10px;
		margin-left: auto;
	}
	#loadtree { width:auto; height:400px; overflow:auto; border:1px solid gray; }

	#menu2 { height:30px; overflow:auto; }
	#text { margin-top:1px; }

	#alog { font-size:9px !important; margin:5px; border:1px solid silver; }
	</style>

</head>
<br /><br />

<!--<body>-->
<div id="header">
	<div id="sn">
	  <strong>IS4 Console</strong><br  />
	  Integrated Sensor-Stream <br  />Storage System
  </div>
<div id="menu">
		<ul>
			<li class="first"><a href="../is4console/joinsetup.html">Register </a></li>
			<li><a href="http://jortiz81.homelinux.com/is4apps/is4m-console/" accesskey="2" title="">Cory LT</a></li>
			<li><a href="#" accesskey="3" title="">Link3</a></li>
			<li><a href="#" accesskey="4" title="">Link4</a></li>
			<li><a href="../is4console/stream_plotter.html">Stream Plotter</a></li>
	  </ul>
  </div>
	<p>&nbsp;</p>
	<p>&nbsp;</p>
	<div id="lt_title">
	  <h1>Cory Hall Load Tree    </h1>
	</div>
</div>
<hr width="700" />
<center>

<div id="feature">
<center>
<table width="708" height="56">
<!--<td width="280">-->
<p>&nbsp;</p>
<div id="menu2">
    <p>
      <input type="button" id="add_folder" value="add element" style="display:block; float:left;"/>
      <!--<input type="button" id="add_default" value="add properties" style="display:block; float:left;"/>-->
      <input type="button" id="rename" value="rename" style="display:block; float:left;"/>
      <input type="button" id="remove" value="remove" style="display:block; float:left;"/>
      <!--<input type="button" id="cut" value="cut" style="display:block; float:left;"/>
      <input type="button" id="copy" value="copy" style="display:block; float:left;"/>
      <input type="button" id="paste" value="paste" style="display:block; float:left;"/>-->
      <input type="button" id="info" value="info" style="display:block; float:left;"/>
	  <input type="button" id="export" value="export" style="display:block; float:left;"/>
      <input type="button" id="sync" value="sync" style="display:block; float:left;"/>
       <input type="button" id="stream_view" value="view stream" style="display:block; float:left;"/>
      <input type="button" id="clear_search" value="clear" style="display:block; float:right;"/>
      <input type="button" id="search" value="search" style="display:block; float:right;"/>
      <input type="text" id="text" value="" style="display:block; float:right;" />
    </p>
</div>
    <!-- the tree container (notice NOT an UL node) -->

<!--</td>-->
</table>
</center>
<script type="text/javascript">
var last_selected_nodeid;
var selected_pubid="";
var last_nodeid_info;
var thisnode_uri;
var newnode_selected = true;
var server_url = "server2.php?operation=";

/*
 * Returns a new XMLHttpRequest object, or false if this browser
 * doesn't support it
 */
function newXMLHttpRequest() {
	var xmlreq = false;
	if (window.XMLHttpRequest) {
	// Create XMLHttpRequest object in non-Microsoft browsers
	xmlreq = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		// Create XMLHttpRequest via MS ActiveX
		try {
		  // Try to create XMLHttpRequest in later versions of Internet Explorer
		  xmlreq = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (e1) {
		  // Failed to create required ActiveXObject
		  try {
			// Try version supported by older versions of Internet Explorer
			xmlreq = new ActiveXObject("Microsoft.XMLHTTP");
		  } catch (e2) {
			// Unable to create an XMLHttpRequest with ActiveX
		  }
		}
	}
	return xmlreq;
}
</script>
<script type="text/javascript">
	//var input = $("input:file");
    // increase the default animation speed to exaggerate the effect
    $.fx.speeds._default = 500;
    $(function() {
        $('#dialog').dialog({
            autoOpen: false,
            show: 'blind',
            hide: 'explode',
			height: 415,
			width: 780
        });
        
		$('#live_stream_dialog').dialog({
            autoOpen: false,
            show: 'blind',
            hide: 'explode',
			height: 415,
			width: 950
        });
		
		$('#stream_view').click(function() {
			if(selected_pubid.length>0){
				$('#live_stream_dialog').dialog({title: "Live Stream Viewer"});
				$('#live_stream_dialog').dialog('open');
				subscribe(selected_pubid);
			} else {
				alert("Can only view stream for publisher resource");
			}
		});
		
		$('#live_stream_dialog').dialog({
			close: function(event, ui){
				$.post("proxy_mconsole.php?command=subremove&subid=" + subscriber_id);
				subscriber_id =0;
			}
		});
		
        $('#info').click(function() {
			//var dlog = document.getElementById("dialog");
			
			/*//add new input type element to the dialog form			
			var input_ = document.createElement("input");
			input_.setAttribute("type","submit");
			input_.setAttribute("value", "new");
			var formelt = document.getElementById("dialog_form");
			formelt.appendChild(input_);*/
			
			//dlog.firstChild.nodeValue = "text goes here";
			var dialog_text_obj = document.getElementById("dialog_text");
			if(dialog_text_obj.value !== null && typeof(dialog_text_obj.value) !== "undefined" && newnode_selected){
				last_nodeid_info = dialog_text_obj.value;
			} else if(last_nodeid_info !== null  && typeof(last_nodeid_info) !== "undefined") {
				dialog_text_obj.value = last_nodeid_info;
			}
			
			newnode_selected = false;
			
			//set the selected node id as a hidden input field
			var dialog_obj = document.getElementById("dialog_form");
			var hidden_input = document.createElement("input");
			hidden_input.setAttribute("type", "hidden");
			hidden_input.setAttribute("id", "hidden_nodeid_field");
			hidden_input.setAttribute("name", "selected_nodeid");
			hidden_input.setAttribute("value",last_selected_nodeid);
			dialog_obj.appendChild(hidden_input);
			//////////////////////////////////////////////////////////
			
			//check for image and add it
			var thisnode_img_fn = thisnode_uri.replace("http://is4server.com:8080/is4/Cory/lt/", "");
			thisnode_img_fn = thisnode_img_fn.replace(/\//g, "_");
			thisnode_img_fn = thisnode_img_fn.substr(0, thisnode_img_fn.length-1);
			var dialog_img_obj = document.getElementById("dialog_image_form");
			var image_element = document.createElement("img");
			//alert("images/temp_images/" + thisnode_img_fn + ".jpg");
			var center_element = document.createElement("center");
			var href_element = document.createElement("a");
			href_element.setAttribute("id", "img_link");
			href_element.setAttribute("href", "images/temp_images/" + thisnode_img_fn + ".jpg");
			image_element.setAttribute("src", "images/temp_images/" + thisnode_img_fn + ".jpg");
			image_element.setAttribute("width", 275);
			image_element.setAttribute("height", 325);
			image_element.setAttribute("alt", "element image");
			href_element.appendChild(image_element);
			center_element.appendChild(href_element);
			//var last_child = dialog_img_obj.lastChild;
			//dialog_img_obj.removeChild(last_child);
			var childOne = document.getElementById("dialog_image_choice");
			var childTwo = document.getElementById("dialog_image_choice_submit");
			var childThree = document.getElementById("img_link");
			if(typeof(childOne) !== "undefined" && childOne !== null)
				dialog_img_obj.removeChild(childOne);
			if(typeof(childTwo) !=="undefined" && childTwo !== null)
				dialog_img_obj.removeChild(childTwo);
			if(typeof(childThree) !=="undefined" && childThree != null)
				dialog_img_obj.removeChild(childThree);
			
			dialog_img_obj.appendChild(href_element);
			dialog_img_obj.appendChild(childOne);
			dialog_img_obj.appendChild(childTwo);
			/////////////////////
			
			if(last_selected_nodeid !== undefined) {
				$('#dialog').dialog({title: "Properties"});
				$('#dialog').dialog('open');
			}
			else {
				alert('No node selected');
			}
            return false;
        });
		
		$('#import').click(function() {
			//alert("import");
			var input = $("import:file")
			});
			
		$('#export').click(function() {
			var xmlhttp = newXMLHttpRequest();
			xmlhttp.onreadystatechange=function()
				{
					//alert("status: " + xmlhttp.status + "\nready: " + xmlhttp.readyState);
					if(xmlhttp.status == 200 && xmlhttp.readyState==4){
						window.open(xmlhttp.responseText, "Export");
					}
				}	
			
		
			xmlhttp.open("GET",server_url + "genContextGraph" ,true);
			xmlhttp.send(null);
			});
			
		$('#sync').click(function() {
			var xmlhttp = newXMLHttpRequest();
			xmlhttp.onreadystatechange=function()
				{
					//alert("status: " + xmlhttp.status + "\nready: " + xmlhttp.readyState);
					if(xmlhttp.status == 200 && xmlhttp.readyState==4){
						alert("IS4 Sync Done");
					}
				}	
			
		
			xmlhttp.open("GET",server_url + "is4Sync" ,true);
			xmlhttp.send(null);
			});
    });
</script>
<script>
function dialog_cancel_hdlr(){
	var dtext = document.getElementById('dialog_text');
	dtext.value = '';
	$('#dialog').dialog('close');
	//var img = document.getElementById("img_id");
}

function dialog_submit_hdlr(){
	var date = new Date();
	var timestamp = date.getUTCFullYear() + "-" + date.getUTCMonth() + "-" + date.getUTCDay() + "T"
						+ date.getUTCHours() + ":" + date.getUTCMinutes() + ":" + date.getUTCSeconds() + "-" 
						+ date.getTimezoneOffset();
	var is4event_win = document.getElementById("is4_output");
	is4event_win.value += timestamp + ": PUT" + " http://is4server.com:8080/is4/Cory/lt/" + thisnode_uri + "\n";
}

function dialog_image_hdlr(){
	var image_dlog_obj = document.getElementById("dialog_image_form");
	var hidden_input = document.createElement("input");
	hidden_input.setAttribute("type", "hidden");
	hidden_input.setAttribute("id", "image_node_id");
	hidden_input.setAttribute("name", "node_image_fn");
	var node_image_fn = thisnode_uri.replace("http://is4server.com:8080/is4/Cory/lt/", "");
	var node_image_fn = node_image_fn.replace(/\//g, "_");
	node_image_fn = node_image_fn.substr(0, node_image_fn.length-1);
	//alert(node_image_fn);
	hidden_input.setAttribute("value", node_image_fn);
	image_dlog_obj.appendChild(hidden_input);
}
</script>

<?php
	//print_r($_REQUEST);
	if(!empty($_REQUEST["prop_input"])){

		$props_json = array();
		$props_json["type"]="properties";
		$props_json["id"] = $_REQUEST["selected_nodeid"];
		$props_json["data"] = $_REQUEST["prop_input"];
		$props_json["prefix"] = "http://is4server.com:8080/is4/Cory/lt";
		
		$res = "http://jortiz81.homelinux.com/is4apps/is4m-console/server2.php?operation=is4Put";

		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_POST,1);
		curl_setopt($curl_handle, CURLOPT_POSTFIELDS, json_encode($props_json));
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$reply = curl_exec($curl_handle);
		curl_close($curl_handle);
		
		//echo $reply;
		
	} elseif(!empty($_FILES["image"]["name"])){
		$image_fn = $_FILES["image"]["name"];
		$image_size = $_FILES["image"]["size"];
		$image_temp_fn = $_FILES["image"]["tmp_name"];
		/*print_r($_FILES);
		echo "<br />";
		print_r($_REQUEST["node_image_fn"]);
		echo "<br />";
		
		echo "NAME: ".$_FILES["image"]["name"];*/
		
		//save it to images/temp_images
		$local_img_fn = $_REQUEST["node_image_fn"];
		$fn_tokens = explode(".", $image_fn);
		//print_r($fn_tokens);
		if(count($fn_tokens) == 2 && 
			(in_array("jpg", $fn_tokens))){// || in_array("gif", $fn_tokens) || in_array("png", $fn_tokens))){
			$type = $fn_tokens[1];
			
			$image_fh = fopen($image_temp_fn, "r");
			$image_content = fread($image_fh, $image_size);
			fclose($image_fh);
			
			$fh = fopen("images/temp_images/".$local_img_fn.".".$type, "w");
			fwrite($fh, $image_content, $image_size);
			fclose($fh);
			
			echo "<script>";
			echo "$(function(){alert(\"Image saved!\");})";
			echo "</script>";
			
		} else {
			echo "<script>";
			echo "$(function(){alert(\"Unknown file type\");})";
			echo "</script>";
		}
	}
?>

<script type="text/javascript">
function handleResponse(){
	alert("ok");
}

function addDevice(){	
	var deviceName = document.getElementById("newDevName").value;
	var smapurl =document.getElementById("smap_loc_select").value;
	var reportResource = document.getElementById("smapResource").value;
	var pubalias = document.getElementById("pubalias").value;
	if(deviceName !== "" && smapurl !== "" && reportResource !== "" && pubalias !== "" && reportResource.indexOf("*")<0 ){
		$.post("server2.php?operation=addOrUpdateDevice",
			{
				"devName":deviceName,
				"smap_url":smapurl,
				"smap_resource":reportResource,
				"alias":pubalias,
				"parent_id":last_selected_nodeid
			},
			function(data){
				if(data.status == "success"){
					//alert("Great success!");
					$('#loadtree').jstree('refresh',-1);
					$('#dialog').dialog('close');
				}
			},
			"json"
			);
	} else if(deviceName !== "" && smapurl !== "" && reportResource !== "" && pubalias !== "" && reportResource.indexOf("*")>0){
		$.post("server2.php?operation=addOrUpdateDeviceBulk",
			{
				"devName":deviceName,
				"smap_url":smapurl,
				"smap_resource":reportResource,
				"parent_id":last_selected_nodeid
			},
			function(data){
				if(data.status == "success"){
					//alert("Great success!");
					$('#loadtree').jstree('refresh',-1);
					$('#dialog').dialog('close');
				}
			},
			"json"
			);
	} else {
		alert("Device Name, Resource, and Alias must be Specified");
	}
}
</script>

<div id="live_stream_dialog" title>
<center>
<div id="placeholder" style='width: 900px; height: 300px;'></div>
</center>
</div>

<div id="dialog" title="">
    <p></p>
    <table id="outer_table">
    <td>
    
    <table id="inner_table" border="1">
    <tr>
    <td>
    <strong>Device Name:</strong>
    <input id="newDevName" name="Device Name" type="text" value="" />
    <select id="smap_loc_select"> 
        <option value="http://local.cs.berkeley.edu:8001" selected>RADLab Veris Panel Meter</option> 
        <option value="http://jackalope.cs.berkeley.edu:8011">LBL ACme deployment</option> 
        <option value="http://buzzing.cs.berkeley.edu:8080">RADLab ACme deployment</option> 
        <option value="http://jackalope.cs.berkeley.edu:8021">Cory ACme deployment</option> 
        <option value="http://local.cs.berkeley.edu:8002">CA ISO Grid Demand</option> 
        <option value="http://local.cs.berkeley.edu:8003">HeatX Chilled Water Meter</option> 
        <option value="http://local.cs.berkeley.edu:8009">Berkeley Weather</option> 
        <option value="http://local.cs.berkeley.edu:8010/mdc1-ims">Berkeley Sun Blackbox</option> 
        <option value="http://local.cs.berkeley.edu:8011">Berkeley/Cory Hall Virtual Meter</option> 
        <option value="http://local.cs.berkeley.edu:8015/main_xfmr">PQube: Main Transformer Meter</option> 
        <option value="http://local.cs.berkeley.edu:8015/4mcl">PQube: Circuit 4, MCL</option> 
        <option value="http://local.cs.berkeley.edu:8016/cory-5a7">ION 6200: Cory 5A7</option> 
        <option value="http://local.cs.berkeley.edu:8016/cory-5b7">ION 6200: Cory 5B7</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-1/elt-A">Dent: basement-1/elt-A: Circuit Breaker 1</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-1/elt-B">Dent: basement-1/elt-B: Circuit Breaker 2</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-1/elt-C">Dent: basement-1/elt-C: Circuit Breaker 3</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-1/elt-E">Dent: basement-1/elt-E: Circuit 5</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-1/elt-F">Dent: basement-1/elt-F: Circuit 6</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-2/elt-A">Dent: basement-2/elt-A: Circuit 7</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-2/elt-B">Dent: basement-2/elt-B: Circuit 8</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-2/elt-D">Dent: basement-2/elt-D: Circuit 9</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-2/elt-E">Dent: basement-2/elt-E: Circuit 10</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-2/elt-F">Dent: basement-2/elt-F: Circuit 11</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-3/elt-C">Dent: basement-3/elt-C: Circuit 14</option> 
        <option value="http://local.cs.berkeley.edu:8005/basement-3/elt-D">Dent: basement-3/elt-D: Circuit 13, BG-2</option> 
        <option value="http://local.cs.berkeley.edu:8006/5PA/elt-A">Dent: 5PA/elt-A: 5PA Feed: 240V Panel 5PA</option> 
        <option value="http://local.cs.berkeley.edu:8006/5PA/elt-B">Dent: 5PA/elt-B: Circuits 1,3,5: AC48, Chilled water</option> 
        <option value="http://local.cs.berkeley.edu:8006/5PA/elt-C">Dent: 5PA/elt-C: Circuits 2,4,6: WP76 Process cooling loop</option> 
        <option value="http://local.cs.berkeley.edu:8006/5PA/elt-D">Dent: 5PA/elt-E: Circuits 8,10,12: WP53</option> 
        <option value="http://local.cs.berkeley.edu:8006/5PA/elt-E">Dent: 5PA/elt-E: Circuits 14,16,18: WP49</option> 
        <option value="http://local.cs.berkeley.edu:8006/5PA/elt-F">Dent: 5PA/elt-F: Circuits 9,35,36: 5th fl. utilities</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA1/elt-A">Dent: 5DPA1/elt-A: Circuits 1,3,5: 30kva Trans to PNLS 4LA,4LE</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA1/elt-B">Dent: 5DPA1/elt-B: Circuits 7,9,11: 120/208 Trans.</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA1/elt-C">Dent: 5DPA1/elt-C: Circuits 13,15,17: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA1/elt-D">Dent: 5DPA1/elt-D: Circuits 19,21,23: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA1/elt-E">Dent: 5DPA1/elt-E: Circuits 25,27,29: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA1/elt-F">Dent: 5DPA1/elt-F: Circuits 31,33,35: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA2/elt-A">Dent: 5DPA2/elt-A: Circuits 2,4,6: 30KVA Trans to PNLS 4LA,4LE</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA2/elt-B">Dent: 5DPA2/elt-B: Circuits 8,10,12: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA2/elt-C">Dent: 5DPA2/elt-C: Circuits 14,16,18: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA2/elt-D">Dent: 5DPA2/elt-D: Circuits 20,22,24: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA2/elt-E">Dent: 5DPA2/elt-E: Circuits 26,28,30: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPA2/elt-F">Dent: 5DPA2/elt-F: Circuits 32,34,36: 120/208 Trans</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPB/elt-A">Dent: 5DPB/elt-A: Circuits 2,4,6: AC#90 (Train Unit)</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPB/elt-B">Dent: 5DPB/elt-B: Circuits 1,3,5: East Passenger Elevator</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPB/elt-C">Dent: 5DPB/elt-C: Circuits 7,9,11: Cooling Tower Pumps (two speed)</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPB/elt-D">Dent: 5DPB/elt-D: Circuits 13,15,17: West Passenger Elevator</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPB/elt-F">Dent: 5DPB/elt-F: Circuits 14,16,18: AC#91, Chiller</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPC/elt-A">Dent: 5DPC/elt-A: Circuits 1,3,5: AH-3, DOP Center ventilation</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPC/elt-B">Dent: 5DPC/elt-B: Circuits 13,15,17: WP#47</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPC/elt-C">Dent: 5DPC/elt-C: Circuits 19,21,23: EP #46</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPC/elt-D">Dent: 5DPC/elt-D: Circuits 2,4,6: SF-3, Aux Supply Fan DOP</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPC/elt-E">Dent: 5DPC/elt-E: Circuits 8,10,12: RF-3 Aux AH-3</option> 
        <option value="http://local.cs.berkeley.edu:8006/5DPC/elt-F">Dent: 5DPC/elt-F: Circuits 14,16,16: Trans #1</option> 
        <option value="http://local.cs.berkeley.edu:8006/MCL/elt-A">Dent: MCL/elt-A: Circuit #4 Switch Gear</option> 
        <option value="http://local.cs.berkeley.edu:8006/MCL/elt-B">Dent: MCL/elt-B: HF#83</option> 
        <option value="http://local.cs.berkeley.edu:8006/MCL/elt-C">Dent: MCL/elt-C: HF #84, Microlab</option> 
        <option value="http://local.cs.berkeley.edu:8006/MCL/elt-D">Dent: MCL/elt-D: CHWS #54, East</option> 
        <option value="http://local.cs.berkeley.edu:8006/MCL/elt-E">Dent: MCL/elt-E: MCL-19, SF-1023, Services Microlab</option> 
        <option value="http://local.cs.berkeley.edu:8006/MCL/elt-F">Dent: MCL/elt-F: CHWS #55, West</option> 
        <option value="http://local.cs.berkeley.edu:8006/358/elt-A">Dent: 358/elt-A: GPE Feet and db 1,3,5: panel feed & tool feed</option> 
        <option value="http://local.cs.berkeley.edu:8006/358/elt-B">Dent: 358/elt-B: pnl DPE, cb 37,39,41: PDU in mach. room</option> 
        <option value="http://local.cs.berkeley.edu:8006/358/elt-C">Dent: 358/elt-C: PNL 2PE Feed, cb 1,3,5: 3fl east plug load, etc</option> 
        <option value="http://local.cs.berkeley.edu:8006/358/elt-D">Dent: 358/elt-D: Splice box: 5PA, transformer, rm 545V</option> 
        <option value="http://local.cs.berkeley.edu:8006/358/elt-E">Dent: 358/elt-E: PNL 2PE, cb 25,27,29: xfmr in rm 355, plug loads</option> 
        <option value="http://local.cs.berkeley.edu:8006/358/elt-F">Dent: 358/elt-F: PNL 2PE, cb 19,21,23: xfmr in rm 545G</option> 
     </select><br />
     <strong>Resource:</strong>
     <input id="smapResource" name="smapResource" type="text" size="60" /><br />
     <strong>Alias:</strong>
    <input id="pubalias" name="publisher alias" type="text" value="" /><br />
    <input name="addDeviceButton" type="button" onClick="addDevice()" value="Add Device"/><br /> <br />
    </td>
    </tr>
    <tr>
    <td>
    <strong>Devices:</strong><select id="devices_list" name="devices"></select><br />
    <strong>sMAP Sources:</strong><select id="server_list" name="smap_servers"></select><br />
    <strong>Resources:</strong><select id="uri_list" name="smap_uris"></select><br />
    </td>
    </tr>
    
    <tr>
    <td>
    <br /><br />
    <strong>Properties:</strong>
    <form id="dialog_form" method="POST" action="">
	<textarea name="prop_input" rows="10" cols="40" id="dialog_text"></textarea><br />
    <input type="submit" name="dialog_submit" value="submit" id="dialog_submit"/ onclick="dialog_submit_hdlr()" >
    <!--<input type="hidden" name="node_id" value=last_selected_nodeid id="nodeid"/>-->
    <input type="button" value="cancel" id="dialog_cancel" onclick="dialog_cancel_hdlr()"/>
  </form>
    </td>
    </tr>
    
    </table> <!-- close inner table -->
    
    </td>
    
    <td>
    <form id="dialog_image_form" enctype="multipart/form-data" method="post">
    <!-- Image display-->
    <!--<img src="images/Cory Power pics 2009/1st fl. hallways cory/cory 1st floor  2009 063.jpg" id="associate_img" width=625 height=425 alt="panel" /><br />-->
    <input type="file" name="image" id="dialog_image_choice"/>
    <input type="submit" name="submit_image" value="submit" id="dialog_image_choice_submit" onclick="dialog_image_hdlr()"/>
    </form>
    </td>
    </table><!-- close outer table -->
</div>


<div id="container">
  	
<div id="loadtree" class="demo"></div>
<!-- JavaScript neccessary for the tree -->
<script type="text/javascript">
$(function () {
	// Settings up the tree - using $(selector).jstree(options);
	// All those configuration options are documented in the _docs folder
	$("#loadtree")
		.jstree({ 
			// the list of plugins to include
			"plugins" : [ "themes", "json_data", "ui", "crrm", "cookies", "dnd", "search", "types", "hotkeys", "contextmenu" ],
			// Plugin configuration

			// I usually configure the plugin that handles the data first - in this case JSON as it is most common
			"json_data" : { 
				// I chose an ajax enabled tree - again - as this is most common, and maybe a bit more complex
				// All the options are the same as jQuery's except for `data` which CAN (not should) be a function
				"ajax" : {
					// the URL to fetch the data
					"url" : "server2.php",
					// this function is executed in the instance's scope (this refers to the tree instance)
					// the parameter is the node being loaded (may be -1, 0, or undefined when loading the root nodes)
					"data" : function (n) { 
						// the result is fed to the AJAX request `data` option
						return { 
							"operation" : "get_children", 
							"id" : n.attr ? n.attr("id").replace("node_","") : 1 
						}; 
					}
				}
			},
			// Configuring the search plugin
			"search" : {
				// As this has been a common question - async search
				// Same as above - the `ajax` config option is actually jQuery's object (only `data` can be a function)
				"ajax" : {
					"url" : "server2.php",
					// You get the search string as a parameter
					"data" : function (str) {
						return { 
							"operation" : "search", 
							"search_str" : str 
						}; 
					}
				}
			},
			// Using types - most of the time this is an overkill
			// Still meny people use them - here is how
			"types" : {
				// I set both options to -2, as I do not need depth and children count checking
				// Those two checks may slow jstree a lot, so use only when needed
				"max_depth" : -2,
				"max_children" : -2,
				// I want only `drive` nodes to be root nodes 
				// This will prevent moving or creating any other type as a root node
				"valid_children" : [ "drive" ],
				"types" : {
					// The default type
					"default" : {
						// I want this type to have no children (so only leaf nodes)
						// In my case - those are files
						"valid_children" : "none",
						// If we specify an icon for the default type it WILL OVERRIDE the theme icons
						"icon" : {
							"image" : "lib/jsTree.v.1.0rc/_demo/file.png"
						}
					},
					// The `folder` type
					"folder" : {
						// can have files and other folders inside of it, but NOT `drive` nodes
						"valid_children" : [ "default", "folder" ],
						"icon" : {
							"image" : "lib/jsTree.v.1.0rc/_demo/folder.png"
						}
					},
					// The `drive` nodes 
					"drive" : {
						// can have files and folders inside, but NOT other `drive` nodes
						"valid_children" : [ "default", "folder" ],
						"icon" : {
							"image" : "lib/jsTree.v.1.0rc/_demo/root.png"
						},
						// those options prevent the functions with the same name to be used on the `drive` type nodes
						// internally the `before` event is used
						"start_drag" : false,
						"move_node" : false,
						"delete_node" : true,
						"remove" : false
					}
				}
			},
			// For UI & core - the nodes to initially select and open will be overwritten by the cookie plugin

			// the UI plugin - it handles selecting/deselecting/hovering nodes
			"ui" : {
				// this makes the node with ID node_4 selected onload
				"initially_select" : [ "node_4" ]
			},
			// the core plugin - not many options here
			"core" : { 
				// just open those two nodes up
				// as this is an AJAX enabled tree, both will be downloaded from the server
				"initially_open" : [ "node_2" , "node_3" ] 
			}
		})
		.bind("create.jstree", function (e, data) {
			$.post(
				"./server2.php", 
				{ 
					"operation" : "create_node", 
					"id" : data.rslt.parent.attr("id").replace("node_",""), 
					"position" : data.rslt.position,
					"title" : data.rslt.name,
					"type" : data.rslt.obj.attr("rel")
				}, 
				function (r) {
					if(r.status) {
						$(data.rslt.obj).attr("id", "node_" + r.id);
					}
					else {
						$.jstree.rollback(data.rlbk);
					}
				}
			);
		})
		.bind("remove.jstree", function (e, data) {
			var nodeid = data.rslt.obj.attr("id").replace("node_","");
			//get the full path for this node
			$.post("./server2.php", {"operation" : "get_path", "id" : nodeid}, 
				function (r) {
					if(r.status==1) {
						//delete resource from IS4
						$.get("./proxy_mconsole.php?command=removeResource&path=" + r.path);
						//remove node from ui
						$.post(
							"./server2.php", 
							{ 
								"operation" : "remove_node", 
								"id" : nodeid
							}, 
							function (r) {
								if(!r.status) {
									$.jstree.rollback(data.rlbk);
								}
							}
						);
					}
				}
			);
			
			
		})
		.bind("rename.jstree", function (e, data) {
			$.post(
				"./server2.php", 
				{ 
					"operation" : "rename_node", 
					"id" : data.rslt.obj.attr("id").replace("node_",""),
					"title" : data.rslt.new_name,
				}, 
				function (r) {
					if(!r.status) {
						$.jstree.rollback(data.rlbk);
					}
				}
			);
		})
		.bind("move_node.jstree", function (e, data) {
			$.post(
				"./server2.php", 
				{ 
					"operation" : "move_node", 
					"id" : data.rslt.o.attr("id").replace("node_",""), 
					"ref" : data.rslt.np.attr("id").replace("node_",""), 
					"position" : data.rslt.cp,
					"title" : data.rslt.name,
					"copy" : data.rslt.cy ? 1 : 0
				}, 
				function (r) {
					if(!r.status) {
						$.jstree.rollback(data.rlbk);
					}
					else {
						$(data.rslt.oc).attr("id", "node_" + r.id);
						if(data.rslt.cy && oc.children("UL").length) {
							data.inst.refresh(data.rslt.oc);
						}
					}
					$("#analyze").click();
				}
			);
		})
		.bind("select_node.jstree", function (e, data) { //click.jstree also works to capture click event
				var nodeid = data.rslt.obj.attr("id").replace("node_","");
				last_selected_nodeid = nodeid;
				newnode_selected = true;
				$.post(
					"./server2.php", 
					{ 
						"operation" : "is4Get", 
						"id" : nodeid,
						"prefix": "http://is4server.com:8080/is4/Cory/lt"
					}, 
					function (r) {
						var is4server = "";
						if(r.path !== undefined){
							is4server = r.path.replace("ROOT", "http://is4server.com:8080/is4/Cory/lt");
						}
						var date = new Date();
						var timestamp = date.getUTCFullYear() + "-" + date.getUTCMonth() + "-" + date.getUTCDay() + "T"
											+ date.getUTCHours() + ":" + date.getUTCMinutes() + ":" + date.getUTCSeconds() + "-" 
											+ date.getTimezoneOffset();
						var is4event_win = document.getElementById("is4_output");
						if(r.status){
							is4event_win.value += timestamp + ": GET" + " " + r.url + "\n";
							thisnode_uri = r.url;
							if(typeof(r.properties) !=="undefined" && typeof(r.properties.data) !=="undefined"){
								var dialog_text_obj = document.getElementById("dialog_text");
								dialog_text_obj.value = r.properties.data;
								
								//populate the selected_pubid variable if the selected node is a publisher
								if(typeof(r.properties.PubId) !== "undefined"){
									selected_pubid = r.properties.PubId;
								} else {
									selected_pubid ="";
								}
							}
						} else {
							is4event_win.value += timestamp + ": GET " + r.fetchUrl + ", FETCH ERROR\n";
						}
						is4event_win.scrollTop = is4event_win.scrollHeight;
					}
				);
		})
		;
});
</script>
<script type="text/javascript">
$(function () { 
	$("#menu2 input").click(function () {
		switch(this.id) {
			case "add_default":
			case "add_folder":
				$("#loadtree").jstree("create", null, "last", { "attr" : { "rel" : this.id.toString().replace("add_", "") } });
				break;
			case "search":
				$("#loadtree").jstree("search", document.getElementById("text").value);
				break;
			case "text": break;
			default:
				$("#loadtree").jstree(this.id);
				break;
		}
	});
});
</script>

<div style="position:absolute; right:20px; top:10px; width:220px; border:1px solid silver; min-height:160px;">
	
    <input type="button" style='display:block; width:170px; height:24px; margin:5px auto;' value="reconstruct" onclick="$.get('./server2.php?reconstruct', function () { $('#loadtree').jstree('refresh',-1); });" />
    
	<input type="button" style='display:block; width:170px; height:24px; margin:5px auto;' id="analyze" value="analyze" onclick="$('#alog').load('./server2.php?analyze');" />
	
    <input type="button" style='display:block; width:170px; height:24px; margin:5px auto;' value="refresh" onclick="$('#loadtree').jstree('refresh',-1);" />
	<div id='alog'></div>
</div>


<div id="is4log">
	<center>
	  <textarea name="is4_output" id="is4_output" cols="136" rows="8"></textarea>
	</center>
  <h3 align="right">IS4 Event Window</h3>
</div>

</div>

<div id="menu3">
        <form action="" method="POST" enctype="multipart/form-data">
          <input type="file" id="file" name="file" style="display:block; float:left;"/>
          <input type="submit" id="submit" value="import file" style="display:block; float:left;" />
        </form>
</div>

<?php
	//print_r($_FILES);
	if(!empty($_FILES["file"]["tmp_name"])){
		$fh = fopen($_FILES["file"]["tmp_name"], r);
		$postData = fread($fh, filesize($_FILES["file"]["tmp_name"]));
		$pd = json_decode($postData, true);
		//var_dump($pd);
		//$pd["postdata"] = $pd;
		fclose($fh);
		$res = "http://jortiz81.homelinux.com/is4apps/is4m-console/server2.php?operation=importContextGraph";
		//echo $postData."<br />POSTING to ".$res;
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_POST,1);
		curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $postData);
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$reply = curl_exec($curl_handle);
		/*if($reply==false)
			echo "reply was false<br />Error: ".curl_error($curl_handle);*/
		curl_close($curl_handle);
		//echo "<br />REPLY:";
		//var_dump($reply);
		echo $reply;
	}
?>
<br />

<hr width="700" />
<div id="footer_">
	<center>
	<div id="local_subdiv">
	<table width="719" height="54" border="0" align="center">
	  <tr>
      <td width="20"><a href="http://local.cs.berkeley.edu"><img src="../is4console/images/local-logo.png" width="100" height="45" alt="local" border="0"/></a></td>
	    <td width="596"> <a href="http://smote.cs.berkeley.edu:8000/tracenv/wiki/is4">IS4 Information</a> | app by <a href="http://www.eecs.berkeley.edu/~jortiz">jortiz</a><br/>
        Research presented are partially based upon work supported by the National Science Foundation under grants #0435454 and #0454432. Any opinions, findings, and conclusions or recommendations expressed in this material are those of the author(s) and do not necessarily reflect the views of the National Science Foundation. </td>
      </tr>
	  </table>
    </div>
    </center>
</div>
</body>
</html>
