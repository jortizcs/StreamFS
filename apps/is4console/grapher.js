// JavaScript Document
Array.prototype.contains = function (element) 
{
	for (var i = 0; i < this.length; i++) 
	{
		if (this[i] == element) 
		{
			return true;
		}
	}
	return false;
};

function drawGraph(alldata) {
	var options = {
        lines: { show: true },
        points: { show: true },
        xaxis: { tickDecimals: 0, tickSize: 1 }
    };
	//$.plot($("#placeholder"), [d1/*,d2*/], options);
	$.plot($("#placeholder"), alldata, options);
};

/*
 * Returns a new XMLHttpRequest object, or false if this browser
 * doesn't support it
 */
function newXMLHttpRequest() {
	var xmlreq = false;
	if (window.XMLHttpRequest) {
		xmlreq = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		try {
		  xmlreq = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (e1) {
		  try {
			xmlreq = new ActiveXObject("Microsoft.XMLHTTP");
		  } catch (e2) {
			// Unable to create an XMLHttpRequest with ActiveX
		  }
		}
	}
	return xmlreq;
}

function fetchData(){
	var droplist = document.getElementById("mypubdroplist");
	//alert(subscriber_id.length>0);
	if (subscriber_id.length>0){// && droplist.options.length>0){
		var proxyUrl = local_proxy_url + "?command=getData&resource=http://" + proxyurl + ":" + proxyport + "/buffer";
		//var ni = document.getElementById('is4_output');
		//ni.value += "Fetch data: " + proxyUrl + "\n";
		var xmlhttp = newXMLHttpRequest();
		xmlhttp.onreadystatechange=function()
		{
			try{
				if(xmlhttp.status == 200 && xmlhttp.readyState==4){
					//alert(xmlhttp.responseText);
					var data_val=0;
					var jsonreq = eval('(' + xmlhttp.responseText + ')');
					var ni = document.getElementById('is4_output');
					var i=0;
					for(i=0; i<jsonreq.BufferedData.length; i++){
						timestamp = jsonreq.BufferedData[i].timestamp;
						//Utimestamp = jsonreq.BufferedData[i].ReadingTime;
						data_pubid = jsonreq.BufferedData[i].PubId;
						//data_val =   jsonreq.BufferedData[i].Data.val;
						data_val =   jsonreq.BufferedData[i].RateInstantaneous;
						ni.value += "TimeStamp="+timestamp + " Publisher=" + data_pubid + " val:" + data_val + "\n";
						//d1.push([pos, data_val]);
						//d2.push([pos,data_val-3]);
						/*pos +=1;
						if (pos>25){
							d1.shift();
							//d2.shift();
						}*/
	
						// add new data point to stream object data array
						var new_datapoint = [timestamp, data_val];
						var found=false;
						for(var j=0; j<streamsArray.length; j++){
							if(streamsArray[j].streamid==data_pubid){
								found=true;
								while(streamsArray[j].data.length>=5){
									streamsArray[j].data.shift();
								}
								//alert("what " + streamsArray[j].data.length);
								streamsArray[j].data.push(new_datapoint);
								//alert("FOR newStream.data.length="+streamsArray[j].data.length);
							}
						}
						
						//new subscription stream
						if(!found){
							var newStream = new StreamObject(data_pubid);
							newStream.data.push(new_datapoint);
							//alert("newStream.data.length="+newStream.data.length);
							streamsArray.push(newStream);
						}
					}
					ni.scrollTop = ni.scrollHeight;
					
					//construct all data
					var alldata=[];
					for(var idx=0; idx<streamsArray.length; idx++){
						var jsonentry = {label:streamsArray[idx].streamid, data:streamsArray[idx].data};
						alldata.push(jsonentry);					
					}
					
					drawGraph(alldata);
				}
			} catch (err){
			}
			fetchData();
		}
		xmlhttp.open("GET",proxyUrl,true);
		xmlhttp.send(null);
	}
}

function getAllPubIds(){
	var xmlhttp = newXMLHttpRequest();
	xmlhttp.onreadystatechange=function()
	{
		if(xmlhttp.status == 200 && xmlhttp.readyState==4){
			//alert(xmlhttp.responseText);
			var jsonresp = eval('(' + xmlhttp.responseText + ')');
			if(jsonresp.status == "success"){
				//alert("success");
				var droplist = document.getElementById("pubdroplist");
				for(var c=0; c<jsonresp.streams.length; c++){
					if(!MyStreamList.contains(jsonresp.streams[c])){
						droplist.options[c]=new Option(jsonresp.streams[c],jsonresp.streams[c]);
					}
				}
			}
			//setTimeout(getAllPubIds,5000);
		}
	}
	var proxyUrl = local_proxy_url + "?command=getallpubs";
	xmlhttp.open("GET",proxyUrl,true);
	xmlhttp.send(null);
}

function getMyPubIds(){
	var xmlhttp = newXMLHttpRequest();
	xmlhttp.onreadystatechange=function()
	{
		if(xmlhttp.status == 200 && xmlhttp.readyState==4){
			//alert(xmlhttp.responseText);
			var jsonresp = eval('(' + xmlhttp.responseText + ')');
			if(jsonresp.status == "success"){
				var droplist = document.getElementById("mypubdroplist");
				var i=0;
				for(i=0; i<jsonresp.PubList.length; i++){
					droplist.options[i]=new Option(jsonresp.PubList[i],jsonresp.PubList[i]);
				}
			}
			//setTimeout(getMyPubIds,5000);
		}
	}
	if (subscriber_id>0){
		var proxyUrl = local_proxy_url + "?command=getMyPubs&subid=" + subscriber_id;
		xmlhttp.open("GET",proxyUrl,true);
		xmlhttp.send(null);
	}
}

function subscribe(){
	var droplist = document.getElementById("pubdroplist");
	var pubid = droplist.options[droplist.selectedIndex].value;
	var xmlhttp = newXMLHttpRequest();
	if(pubid != null){
		var proxyUrl = local_proxy_url + "?command=subscribe&pubid=" + pubid;
		if(subscriber_id ==0)
			proxyUrl += "&firsttimesub=true"
		else
			proxyUrl += "&subid="+subscriber_id;
		xmlhttp.open("GET",proxyUrl,true);
		xmlhttp.send(null);
	}
	
	xmlhttp.onreadystatechange=function()
	{
		if(xmlhttp.status == 200 && xmlhttp.readyState==4){
			var jsonresp = eval('(' + xmlhttp.responseText + ')');
			if(jsonresp.status == "success"){
				//alert(xmlhttp.responseText);
				MyStreamList[MyStreamList.length]=pubid;
				if(subscriber_id==0) {
					subscriber_id = jsonresp.SubID;
					proxyurl = jsonresp.proxyUrl;
					proxyport = jsonresp.proxyPort;
					var ni = document.getElementById('is4_output');
					ni.value += "SubID: " + subscriber_id + "\nProxyUrl: http://" + proxyurl + ":" + proxyport + "\n";
					fetchData();
				}
				
				getMyPubIds();
				//remove the option from the dropdown
				var numoptions = droplist.options.length;
				for(var k=0; k<numoptions; k++){
					if(droplist.options[k].value==pubid){
						var thisoption = droplist.options[k];
						droplist.removeChild(thisoption);
					}
				}
			}
		}
	}	
}

function refreshLists(){
	getAllPubIds();
	getMyPubIds();
}


var d1 = [];
var d2 =[];
var pos=1;
var streamsArray = new Array(); // array of stream objects
//alert(streamsArray.constructor==Array);
function StreamObject(pubid) {
	this.streamid = pubid;
	this.data = new Array();
}

var MyStreamList = new Array();
//alert(MyStreamList.constructor==Array)
//var url="../../proxy.php?command=getData&resource=http://smote.cs.berkeley.edu:8080/is4/streamtest";\
//fetchData(url);
var local_proxy_url="proxy.php";

var subscriber_id=0;
var proxyurl="";
var proxyprt=-1;

//var myPubIdsArray = new Array();
//updatePubDropList();
