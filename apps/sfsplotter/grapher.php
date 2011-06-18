<html>
<head>
<script language="javascript" type="text/javascript" src="lib/jquery.min.js"></script>
<script language="javascript" type="text/javascript" src="lib/flot/jquery.flot.js"></script>
<style type="text/css">
#loading { display:none; }
#stats { display:none; }
#mselector { display:none; }
#dl_data { display:none; }
</style>
<script type="text/javascript" language="Javascript">
function setStats(numpoints){
	//query string
	var queryHidden = document.getElementById("queryHidden");
	var queryText = document.getElementById("queryText");
	queryText.value = queryHidden.value;

	//number of points returns in query	
	var pointsText = document.getElementById("ptsText");
	pointsText.innerHTML = numpoints;

	//calculate time for query to return	
	var qstartTime = document.getElementById("qstartTime");
	var qtimeText = document.getElementById("qtimeText");
	var now = (new Date()).getTime() / 1000;
	var totalTime = now - qstartTime.value;
	qtimeText.innerHTML = totalTime + " seconds";

	showStats();
}

function addStream(feedname, channame){
	var mselector = document.getElementById("streamlist");
	var newOption = document.createElement('option');
	var feedsel = document.getElementById(feedname);
	var chansel = document.getElementById(channame);
	//var idx = mselector.options.length + 1;
	newOption.text = /*idx + ".  " +*/ feedsel.options[feedsel.selectedIndex].text+ "/"+chansel.options[chansel.selectedIndex].text;
	mselector.add(newOption);
	showMSelector();
}

function removeStream(){
	var mselector = document.getElementById("streamlist");
	mselector.remove(mselector.selectedIndex);
	if(mselector.options.length==0){
		hideMSelector();
	}
}

function getChannels(data,textStatus){
	if(textStatus=="success" && data.length>0){
		var dataJson = eval('(' + data + ')');
		var clist = document.getElementById("chanselector");
		var oldOptionsCnt = clist.length;
		for(i=0; i<oldOptionsCnt; i++){
			clist.remove(0);
		}
		for(i=0; i<dataJson.children.length; i++){
			var newOption=document.createElement('option');
			newOption.text = dataJson.children[i];
			newOption.value=i;
			clist.add(newOption);
		}
	} else {
		if(data.length==0){
			alert("Empty response");
		} else {
			alert(textStatus);
		}
	}	
	hideLoading();
}

function OnChange(Url, name)
{
	var dropdown = document.getElementById(name);
	var myindex  = dropdown.selectedIndex;
	var SelValue = dropdown.options[myindex].text;
	var cUrl = Url + SelValue;
	showLoading();
	var input = {url:cUrl};
	var x = $.post("droplistlib.php", input, getChannels);
	return true;
}

function getReadings(feed, chan, tback, Url){
	deldata();
	var dropdown = document.getElementById(feed);
	var myindex = dropdown.selectedIndex;
	var d1text = dropdown.options[myindex].text;

	var channel = document.getElementById(chan);
	var chanindex = channel.selectedIndex;
	var chanText = channel.options[chanindex].text;

	var timebackInput = document.getElementById(tback);
	var interval = timebackInput.value;

	if(interval>86400 || interval<0){
		interval=600;
	}
	var cUrl = Url + d1text + "/" + chanText + "?query=true&ts_timestamp=gt:now-"+interval;

	//set the hidden values for stats display
	var hiddenQuery = document.getElementById("queryHidden");
	hiddenQuery.value = cUrl;
	var qstartTime = document.getElementById("qstartTime");
	qstartTime.value = (new Date()).getTime() / 1000;

	hideStats();
	showLoading();
	var fchan = d1text + "." + chanText;
	var input = {url:cUrl, "feedchan":fchan};
	var x = $.post("droplistlib.php", input, handleQuery);
	return true;
}

function handleQuery(data, textStatus){
	if(textStatus=="success"){
		var dataJson = eval('('+data+')');
		//alert("plotting: " + dataJson.ts_query_results.results.length + " points");
		plotPoints(dataJson.ts_query_results.results);

		//set stats
		setStats(dataJson.ts_query_results.results.length);
	} else {
		alert(textStatus);
	}
	hideLoading();
}

var res = {};
var feedchans=[];
var fchanidx=0;
var total_streams=0;
var streams_fetched=0;
var alldata = [];
var total_points = 0;
function handlemdata(d_, status_){
	if(status_=="success"){
		var dps = [];
		var dataJson = eval('('+d_+')');
		total_points += dataJson.ts_query_results.results.length;
		for(var k=0; k<dataJson.ts_query_results.results.length; k++){
			var ts = (dataJson.ts_query_results.results[k].timestamp-28800)*1000;
			var r = dataJson.ts_query_results.results[k].Reading;
			dps.push([ts,r]);
		}
		//var lval = feedchans[streams_fetched];
		var thisPath = dataJson.path;
		var j=0; var found=false;
		while (j<feedchans.length && !found){
			if(thisPath.indexOf(feedchans[j])>0){
				var lval = feedchans[j];
				res = {label:lval, "data":dps};
				alldata.push(res);
				found = true;
			}
			j+=1;
		}
		//print if all streams have been fetched
		streams_fetched +=1;
		if(streams_fetched == total_streams){
			plotAllStreams(alldata);
			//set stats
			setStats(total_points);
			hideLoading();
			showStats();
			res={};
			feedchans=[];
			fchanidx=0;
			total_streams=0;
			streams_fetched=0;
			alldata=[];
			total_points=0;
		}

	}
	return true;
}
function getMultiReadings(feed, chan, tback, Url){

	deldata();
	var mstreamlist = document.getElementById("streamlist");
	total_streams = mstreamlist.options.length;
	var timebackInput = document.getElementById(tback);
	var interval = timebackInput.value;
	if(total_streams>0 && typeof interval != "undefined"){
		
		if(interval>86400 || interval<0){
			interval=600;
		}

		hideStats();
		showLoading();

		for(var i=0; i<mstreamlist.options.length; i++){
			feedchans.push(mstreamlist.options[i].text); 
			var cUrl = Url + feedchans[fchanidx] + "?query=true&ts_timestamp=gt:now-"+interval;
			fchanidx+=1;

			if(i==0){
				//set the hidden values for stats display
				var hiddenQuery = document.getElementById("queryHidden");
				hiddenQuery.value = cUrl;
				var qstartTime = document.getElementById("qstartTime");
				qstartTime.value = (new Date()).getTime() / 1000;
			}

			var input = {url:cUrl, "feedchan":feedchans[fchanidx-1]};
			var x = $.post("droplistlib.php", input, handlemdata);
		}
		
		return true;
	} else if (typeof interval == "undefined"){
		alert("Time value must be postive");
	} else {
		return getReadings(feed, chan, tback, Url);
	}
}

function plotAllStreams(alldata){
	var options = {
		series: {lines:{show:true},points:{show:true}},
			xaxis:{mode:"time", timeformat:"%m/%d %H:%M:%S %p"}
			};
	$.plot($("#placeholder"), alldata, options);
}

function plotPoints(results){
	var d1 = [];
	for (var i = 0; i<results.length; i++){
		var ts = (results[i].timestamp-28800) * 1000;
		var point = results[i].Reading;
		d1.push([ts,point]);
	}
	var options = {xaxis:{mode:"time", timeformat:"%m/%d %H:%M:%S %p"}};

	$.plot($("#placeholder"), [d1], options);
}

function gendata(){
	var input={func:"tarit"};
	$.post("savedata.php", input);
	showDataDL();
}

function deldata(){
	hideDataDL();
	var input={func:"delall"};
	$.post("savedata.php", input);
}

/*function plotMStream(){
	var data = [ { label: "Foo", data: [ [10, 1], [17, -14], [30, 5] ] },
			{ label: "Bar", data: [ [11, 13], [19, 11], [30, -7] ] } ];
	$.plot("#placeholder", data);
}*/

function showLoading() {
	  $("#loading").show();
}
function hideLoading() {
	  $("#loading").hide();
}
function showStats() {
	  $("#stats").show();
}
function hideStats() {
	  $("#stats").hide();
}
function showMSelector() {
	  $("#mselector").show();
}
function hideMSelector() {
	  $("#mselector").hide();
}
function showDataDL() {
	  $("#dl_data").show();
}
function hideDataDL() {
	  $("#dl_data").hide();
}
</script>
</head>
<div id="loading">
<p><img src="ajax-loader.gif" /> Please Wait</p>
</div>


<?php
include_once "droplistlib.php";
$configFile = "config.json";
$host = "localhost";
$port = 8080;
$feedsroot = "/feeds/";
if(file_exists($configFile)){
	$configJson = json_decode(file_get_contents($configFile), true);
	$host = $configJson["host"];
	$port = $configJson["port"];
	$feedsroot = $configJson["feeds_root_path"];
}
$droplist = new SFSDroplistGen();
$droplist->init($host, $port);
$sfs_feeds_url="http://".$host.":".$port.$feedsroot;
$droplist->genFeedsDropList("selector1", "OnChange(\"$sfs_feeds_url\",\"selector1\")");

######################################## ##########
#   initialize with the first feed's channels #####
###################################################
$feeds_resp = json_decode(get($sfs_feeds_url), true);
$feeds = $feeds_resp["children"];
$firstfeed = $feeds[0];
$name = "chanselector";
$droplist->genChanDropList($name, $firstfeed);
?>

CurrentTime - <input type="text" id="timeback" name="timeback" size="5"/> seconds
<!--<input type="button" name="shorttext" value="Submit" onClick="getReadings('selector1','chanselector','timeback','http://is4server.com:8080/is4/feeds/')">-->
<input type="button" name="shorttext" value="Submit" onClick="getMultiReadings('selector1','chanselector','timeback','http://is4server.com:8080/is4/feeds/')">
<!--<input type="button" name="shorttext" value="Submit" onClick="plotMStream()">-->
<input type="button" name="add_stream" value="Add Stream" onClick="addStream('selector1','chanselector')">

<div id="mselector">
<input type="button" name="remove_stream" value="Remove Stream" onClick="removeStream()">
<select multiple id="streamlist" name="streamlist" size="4"></select>
</div>

<div id="placeholder" style="width:1200px;height:600px"></div>

<div id="stats">
	<input type="hidden" id="queryHidden" value=""/>
	<input type="hidden" id="qstartTime" value=""/>
	SFS Query: <input type="text" id="queryText" name="querystring" size="100"/>
	
	<table border="1">
	<tr>
	<td><b>Stat</b></td>
	<td><b>Value</b></td>
	</tr>
	<tr>
	<td>Num points</td>
	<td id="ptsText"></td>
	</tr>
	<tr>
	<td>Query time</td>
	<td id="qtimeText"></td>
	</tr>
	</table>

	<input type="button" name="gendata" value="Generate data files" onClick="gendata()">
</div>

<div id="dl_data">
	<a href="data/data.tgz">Download Data</a>
</div>

</html>
