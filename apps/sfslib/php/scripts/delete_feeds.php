<?php
include_once "curl_ops.php";

$sfshost="is4server.com";
$sfsport=8080;
$sfsurl = "http://".$sfshost.":".$sfsport;

$feeds = get($sfsurl."/is4/feeds");
if(strlen($feeds)>0){
	$feeds = json_decode($feeds,true);
	$feeds = $feeds["children"];
	for($i=0; $i<count($feeds); $i++){
		echo $i.".\tDeleting: ".$sfsurl."/is4/feeds/".$feeds[$i]."/*"."\n";
		delete($sfsurl."/is4/feeds/".$feeds[$i]."/*");
	}
}
?>
