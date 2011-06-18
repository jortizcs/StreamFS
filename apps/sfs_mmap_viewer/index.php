<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
Design by Free CSS Templates
http://www.freecsstemplates.org
Released for free under a Creative Commons Attribution 2.5 License
-->
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title>StreamFS Console - SFS Metdata Tree Viewer</title>
<script type="text/javascript" src="../jquery_lib/jquery-1.6.1.min.js"></script>
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


<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>SFS Tree Viewer</title>
<script type="text/javascript" src="lib/jsTree.v.1.0rc/_lib/jquery.cookie.js"></script>
<script type="text/javascript" src="lib/jsTree.v.1.0rc/_lib/jquery.hotkeys.js"></script>
<script type="text/javascript" src="lib/jsTree.v.1.0rc/jquery.jstree.min.js"></script>
<script type="text/javascript" src="lib/flot/jquery.flot.js"></script>
<style type="text/css">
	html, body { margin:0; padding:0; }
	body, td, th, pre, code, select, option, input, textarea { font-family:verdana,arial,sans-serif; font-size:10px; }
	.demo, .demo input, .jstree-dnd-helper, #vakata-contextmenu { font-size:10px; font-family:Verdana; }
</style>

</head>
<body>
<br>
<div id="header">
<div id="sn">
<strong>StreamFS Console</strong><br  />
Data Management for <br  />Physical Data
</div>

<div id="menu">
<ul>
<li class="first"><a href="#">Link1</a></li>
<li><a href="#" accesskey="2" title="">Link2</a></li>
<li><a href="#" accesskey="3" title="">Link3</a></li>
<li><a href="#" accesskey="4" title="">Link4</a></li>
</ul>
</div>

<p>&nbsp;</p>
<p>&nbsp;</p>
<div id="ViewTitle">
<h1>Metadata Map Tree View</h1>
</div>
</div>
<hr width="700" />





<div id="feature">
<table width="708">
<p>&nbsp;</p>
<div id="menu2">
    <p>
	<input type="button" id="add_folder" value="add element" style="display:block; float:left;"/>
	<input type="button" id="rename" value="rename" style="display:block; float:left;"/>
	<input type="button" id="clear_search" value="clear" style="display:block; float:right;"/>
	<input type="button" id="search" value="search" style="display:block; float:right;"/>
	<input type="text" id="text" value="" style="display:block; float:right;" />
    </p>
</div>
</table>

<table>
<tr>
<td>
<div id="viewer" class="viewer_window" align="left"></div>
</td>
<td>
<div id="properties" class="props_window" align="center"></div>
</td>
<td>
	<table>
	<tr><td>
		<!--<div id="graph" style="width:600px;height:300px;"></div>-->
		<textarea id="output_id" rows="20" cols="100"></textarea>
	</tr></td>
	<tr><td>
		<!--<div id="graph" style="width:600px;height:300px;"></div>-->
		
	</tr></td>
	</table>
</td>
</tr>
</table>

<script type="text/javascript">
function graphit() {
    var d1 = [];
    for (var i = 0; i < 14; i += 0.5)
        d1.push([i, Math.sin(i)]);
 
    var d2 = [[0, 3], [4, 8], [8, 5], [9, 13]];
 
    // a null signifies separate line segments
    var d3 = [[0, 12], [7, 12], null, [7, 2.5], [12, 2.5]];
    $.plot($("#graph"), [ d1, d2, d3 ]);
}
</script>
<script type="text/javascript">
$(function () { 
	$("#menu2 input").bind('click', 
		function () {
			switch(this.id) {
				/*case "add_default":
				case "add_folder":
					$("#viewer").jstree("create", null, "last", { "attr" : { "rel" : this.id.toString().replace("add_", "") } });
					break;*/
				case "search":
					$("#viewer").jstree("search", document.getElementById("text").value);
					break;
				case "text": break;
				default:
					$("#viewer").jstree(this.id);
				break;
		}
	});
});
</script>

<script type="text/javascript">
var fileGets = new Array();
var thisData=null;
var last_selected_nodeId = "";
function getResponse(data){
	var output_win = document.getElementById("output_id");
	output_win.value = data;
	if(fileGets[last_selected_nodeId] == null)
		fileGets[last_selected_nodeId]=data;
}
function resourceList(data){
	var dataStr = "{ \"data\":[" +  data + ']}';
	thisData = eval('(' + dataStr + ')');
	loadTree();
}
//graphit();
var viewerObj = $("#viewer");
var reqInput = new Object();
reqInput.method="get_all_resources";
jQuery.post("sfs_marshaller.php",reqInput,resourceList);
function loadTree() {
	viewerObj.jstree({ 
		"json_data" : thisData,
		"unique":{
			"error_callback": function(n, p, f){
				alert("duplicate file names not allowed");
				$.jstree.rollback(data.rlbk);
				}
		},
		"plugins" : [ "themes", "json_data", "ui", "hotkeys", "unique","crrm", "cookies", "dnd", "search", "types", "contextmenu"  ]
	});

	viewerObj.bind("select_node.jstree", 
		function (e, data) { 
			var nodeId = jQuery.data(data.rslt.obj[0], "jstree").id;
			//alert(nodeId);
			var date = new Date();
			var timestamp = date.getUTCFullYear() + "-" + date.getUTCMonth() + "-" + date.getUTCDay() + "T"
						+ date.getUTCHours() + ":" + date.getUTCMinutes() + ":" + date.getUTCSeconds() + "-" 
						+ date.getTimezoneOffset();
			var is4event_win = document.getElementById("sfs_reqlog_id");
			
			last_selected_nodeId = nodeId;
			if(fileGets[nodeId] == null || nodeId == "/time/"){
				is4event_win.value += timestamp + ": GET " + nodeId + "\n";

				reqInput = new Object();
				reqInput.method = "get_path";
				reqInput.path=nodeId;
				jQuery.post("sfs_marshaller.php", reqInput, getResponse);
			} else {
				is4event_win.value += timestamp + ": GET (Cache) " + nodeId + "\n";
				getResponse(fileGets[nodeId]);
			}
			is4event_win.scrollTop = is4event_win.scrollHeight;
		}
	);

	viewerObj.bind("remove.jstree", 
		function(e, data) {
			alert("process this when remove node is called");
			$.jstree.rollback(data.rlbk);
		}
	);

	viewerObj.bind("rename.jstree", 
		function(e, data){
			alert("action to take when renaming a node");
			$.jstree.rollback(data.rlbk);
		}
	);

	viewerObj.bind("create.jstree",
		function(e, data) {
			//alert("process this when create node is called: " + this.id);
			var reqInput = new Object();
			reqInput.method = "create";
			reqInput.path = last_selected_nodeId;
			var thisdata = data.rslt.name;
			if(thisdata.indexOf(" ")>0 || thisdata.indexOf("/")>0){
				alert("No spaces!");
				$.jstree.rollback(data.rlbk);
			} else {
				alert("you're good to go");
				$.jstree.rollback(data.rlbk);
			}
		}
	);
}
</script>
</div>




<div id="sfs_reqlog">
	<center>
		<textarea name="sfs_reqlog" id="sfs_reqlog_id" cols="144" rows="8"></textarea>
  		<h3 align="center">SFS Request Log</h3>
		<!--<textarea name="sfs_resplog" id="sfs_resplog" rows="8" cols="144" ></textarea>
		<h3 align="center">SFS Response Log</h3>-->
	</center>
</div>








<hr width="700" />
<div id="footer_">
	<center>
	<div id="local_subdiv">
	<table width="719" height="54" border="0" align="center">
	  <tr>
      <td width="20"><a href="http://local.cs.berkeley.edu"><img src="../is4console/images/local-logo.png" width="100" height="45" alt="local" border="0"/></a></td>
	    <td width="596"> <a href="http://smote.cs.berkeley.edu:8000/tracenv/wiki/is4">StreamFS Information</a> | app by <a href="http://www.eecs.berkeley.edu/~jortiz">jortiz</a><br/>
        Research presented are partially based upon work supported by the National Science Foundation under grants #0435454 and #0454432. Any opinions, findings, and conclusions or recommendations expressed in this material are those of the author(s) and do not necessarily reflect the views of the National Science Foundation. </td>
      </tr>
	  </table>
    </div>
    </center>
</div>
</body>
</html>
