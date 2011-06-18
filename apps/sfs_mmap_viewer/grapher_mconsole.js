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
        lines: { show: true, fill:true },
        points: { show: true },
        xaxis: { /*tickDecimals: 0, 
				tickSize: 5000,*/
				mode:"time",
				timeformat: "%H:%M:%S"
				},
		fill:true
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
	fetchCalled = false;
	if (subscriber_id.length>0){
		var proxyUrl = local_proxy_url;
		var data_ = "command=getData2&subid=" +subscriber_id + "&resource=http://" + proxyurl + "/buffer.php";
		try{
			var xmlhttp = newXMLHttpRequest();
			
			xmlhttp.onreadystatechange=function()
			{
				if(xmlhttp.status == 200 && xmlhttp.readyState==4 && xmlhttp.responseText.length>2){
					var data_val=0;
					var jsonreq = eval('(' + xmlhttp.responseText + ')');
					var ni = document.getElementById('is4_output');
					var i=0;
					for(i=0; i<jsonreq.BufferedData.length; i++){
						timestamp = jsonreq.BufferedData[i].timestamp;
						data_pubid = jsonreq.BufferedData[i].pubid;
						data_eval = eval('(' + jsonreq.BufferedData[i].data + ')');
						//data_val = data_eval.RateInstantaneous;
						data_val = data_eval.Reading;
						ni.value += "TimeStamp="+timestamp + " Publisher=" + data_pubid + " val:" + data_val + "\n";
			
						// add new data point to stream object data array
						var ts_unixtime = new Date(timestamp);
						//alert("(" + ts_unixtime.getTime() + ", " + data_val + ")");
						var new_datapoint = [ts_unixtime.getTime(), data_val];
						var found=false;
						for(var j=0; j<streamsArray.length; j++){
							if(streamsArray[j].streamid==data_pubid){
								found=true;
								while(streamsArray[j].data.length>=5){
									streamsArray[j].data.shift();
								}
								streamsArray[j].data.push(new_datapoint);
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
						//var jsonentry = {label:streamsArray[idx].streamid, data:streamsArray[idx].data};
						var jsonentry = {label:"Current (Amps)", data:streamsArray[idx].data};
						alldata.push(jsonentry);					
					}
					
					drawGraph(alldata);
				}
				if(xmlhttp.status == 200 && xmlhttp.readyState==4 && !fetchCalled){
					setTimeout("fetchData()", 10000);
					fetchCalled = true;
				}
			} 

			xmlhttp.open("POST",proxyUrl,true);
			xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
			xmlhttp.send(data_);
		} catch(err){
			if(!fetchCalled){
				setTimeout("fetchData()", 10000);
				fetchCalled = true;
			}
		}
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

function subscribe(pubid){
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
					//proxyport = jsonresp.proxyPort;
					var ni = document.getElementById('is4_output');
					ni.value += "SubID: " + subscriber_id + "\nProxyUrl: http://" + proxyurl + "\n";// + ":" + proxyport + "\n";
					if(!fetchCalled)
						fetchData();
				}
				
				getMyPubIds();
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
function StreamObject(pubid) {
	this.streamid = pubid;
	this.data = new Array();
}

var MyStreamList = new Array();
var local_proxy_url="proxy_mconsole.php";

var subscriber_id=0;
var proxyurl="";
var proxyprt=-1;
var fetchCalled = false;