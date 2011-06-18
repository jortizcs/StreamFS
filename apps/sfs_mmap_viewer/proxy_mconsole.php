<?php
include_once("class.tree2.php");
//$is4server = "http://smote.cs.berkeley.edu:8080/is4";
//$is4server = "http://jortiz81.homelinux.com:8081/is4";
//$is4server = "http://184.106.204.181:8080/is4";
$is4server = "http://is4server.com:8080/is4";
$command = $_REQUEST["command"];
$resource = $_REQUEST["resource"];
$subid = $_REQUEST["subid"];
$pubid = $_REQUEST["pubid"];
$firstSub = $_REQUEST["firsttimesub"];

//print "command= ".$command."<br><br>";

function getResponse($res, $method, $post_data){
	if (strcasecmp($method, "GET")==0){
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$buffer = curl_exec($curl_handle);
		curl_close($curl_handle);
		if(empty($buffer) || strlen($buffer)==0)
			$buffer= "{}";
		echo $buffer;
	} elseif(strcasecmp($method, "POST")==0 && !empty($post_data)){
		//print "<br>resource=".$res."<br>"."request=".$post_data."<br><br>";
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_POST,1);
		curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $post_data);
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$buffer = curl_exec($curl_handle);
		curl_close($curl_handle);
		//print "Buffer : ".$buffer."<br>";
		if(empty($buffer) || strlen($buffer)==0)
			$buffer= "{}";
		echo $buffer;
	} elseif(strcasecmp($method, "DELETE")==0){
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_CUSTOMREQUEST, "DELETE");
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		if(!empty($post_data)){
			curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $post_data);
		}
		$buffer = curl_exec($curl_handle);
		curl_close($curl_handle);
		echo $buffer;
	} elseif(strcasecmp($method, "PUT")==0 && !empty($post_data)){
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_CUSTOMREQUEST, "PUT");
		curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $post_data);
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$buffer = curl_exec($curl_handle);
		curl_close($curl_handle);
		//print "Buffer : ".$buffer."<br>";
		if(empty($buffer) || strlen($buffer)==0)
			$buffer= "{}";
		echo $buffer;
	}
}

if(strcasecmp($command, "getData")==0 && !empty($resource)){
	//print $resource;
	getResponse($resource,"GET","");
} elseif(strcasecmp($command,"getData2")==0){
	$resource .= "?operation=buffer_get&subid=".$subid;
	getResponse($resource, "GET", "");
} elseif(strcasecmp($command,"getAllPubs")==0){
	//print $is4server."/pub/all";
	getResponse($is4server."/pub/all", "GET", "");
} elseif(strcasecmp($command,"getMyPubs")==0 && !empty($subid)){
	$request="{\"name\":\"my_stream_list\",\"SubId\":".$subid."}";
	//print $request;
	getResponse($is4server."/sub/mypublist","POST",$request);
} elseif(strcasecmp($command,"subscribe")==0 && !empty($pubid)){
	if(!empty($firstSub)){
		//automatically subscribe with proxy
		$request = "{\"streams\":[\"".$pubid."\"],\"enableProxy\":true}";
		//print $request;
		getResponse($is4server."/sub","POST",$request);
	} elseif(empty($firstSub) && !empty($subid)) {
		$s1 = "{\"name\":\"sub_control\",\"SubId\":\"".$subid;
		$s2 = $s1."\",\"StreamSubIds\":[\"".$pubid;
		$request = $s2."\"],\"StreamCancelIds\":[]}";
		//echo $request;
		//print_r(json_decode($request, true));
		getResponse($is4server."/sub/control","POST",$request);
	}
} elseif(strcasecmp($command,"unsubscribe")==0 && !empty($pubid) && !empty($subid)) {
		$s1 = "{\"name\":\"sub_control\",\"SubId\":".$subid;
		$s2 = $s1.",\"StreamSubIds\":[],";
		$request = $s2."\"StreamCancelIds\":[".$pubid."]}";
		//print $request;
		getResponse($is4server."/sub/control","POST",$request);
} elseif(strcasecmp($command,"subremove")==0 && !empty($subid)) {
		$request = "{\"name\":\"sub_removal\",\"SubIds\":[\"".$subid."\"]}";
		//echo $request;
		getResponse($is4server."/unsub","POST",$request);
} elseif(strcasecmp($command, "removeResource")==0 && $_GET["path"]){
	//is4 uri to delete
	$url = $is4server."/Cory/lt".str_replace("ROOT","",$_GET["path"]);
	echo getResponse($url,"DELETE","");
}

/*if (empty($buffer))
{
	print "Sorry, example.com are a bunch of poopy-heads.<p>";
}
else
{
	print $buffer;
}*/
	
?>