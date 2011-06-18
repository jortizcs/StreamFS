<?php
//$is4server = "http://smote.cs.berkeley.edu:8080/is4";
//$is4server = "http://jortiz81.homelinux.com:8081/is4";
$is4server = "http://184.106.204.181:8080/is4";
$command = $_GET["command"];
$resource = $_GET["resource"];
$subid = $_GET["subid"];
$pubid = $_GET["pubid"];
$firstSub = $_GET["firsttimesub"];

//print "command= ".$command."<br><br>";

function getResponse($res, $post, $post_data){
	if (!$post){
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$res);
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$buffer = curl_exec($curl_handle);
		curl_close($curl_handle);
		print $buffer;
	} elseif($post && !empty($post_data)){
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
		print $buffer;
	}
}

if(strcasecmp($command, "getData")==0 && !empty($resource)){
	//print $resource;
	getResponse($resource,false,"");
} elseif(strcasecmp($command,"getAllPubs")==0){
	//print $is4server."/pub/all";
	getResponse($is4server."/pub/all", false, "");
} elseif(strcasecmp($command,"getMyPubs")==0 && !empty($subid)){
	$request="{\"name\":\"my_stream_list\",\"SubId\":".$subid."}";
	//print $request;
	getResponse($is4server."/sub/mypublist",true,$request);
} elseif(strcasecmp($command,"subscribe")==0 && !empty($pubid)){
	if(!empty($firstSub)){
		//automatically subscribe with proxy
		$request = "{\"streams\":[\"".$pubid."\"],\"enableProxy\":true}";
		//print $request;
		getResponse($is4server."/sub",true,$request);
	} elseif(empty($firstSub) && !empty($subid)) {
		$s1 = "{\"name\":\"sub_control\",\"SubId\":".$subid;
		$s2 = $s1.",\"StreamSubIds\":[".$pubid;
		$request = $s2."],\"StreamCancelIds\":[]}";
		//print $request;
		getResponse($is4server."/sub/control",true,$request);
	}
} elseif(strcasecmp($command,"unsubscribe")==0 && !empty($pubid) && !empty($subid)) {
		$s1 = "{\"name\":\"sub_control\",\"SubId\":".$subid;
		$s2 = $s1.",\"StreamSubIds\":[],";
		$request = $s2."\"StreamCancelIds\":[".$pubid."]}";
		//print $request;
		getResponse($is4server."/sub/control",true,$request);
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