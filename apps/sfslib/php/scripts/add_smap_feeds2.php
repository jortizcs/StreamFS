<?php
include_once "sfslib.php";

//check if the feed is already in /is4/buildings/CoryHall/inventory/
$sfs_host = "is4server.com";
$sfs_port = 8080;
$sfsConn = new SFSConnection();
$sfsConn->setStreamFSInfo($sfs_host, $sfs_port);


//read the smap feeds and register those have are not 
//already in the cory hall inventory
$smap_feeds = "smap_feeds.txt";
$feeds_path = "/is4/feeds/";
$handle = fopen($smap_feeds, "r");
$count=1;
if($handle){
	$line ="";
	while(($line=fgets($handle)) !== false){
		$line = substr($line, 0, strlen($line)-1);
		if($line[0] !== '#'){
			$smap_feed_url = strtok($line, " ");
			$smap_feed_name = strtok(" ");
			$devs = get($smap_feed_url."/data/");
			
			if(!empty($devs)){
				$full_smapurl = $smap_feed_url."/data/*/*/*/reading/";
				if(!$sfsConn->exists($feeds_path.$smap_feed_name) == true){
					$type = "device";
					$ret1 = $sfsConn->mkrsrc($feeds_path, $smap_feed_name, $type);
					$ret2 = $sfsConn->mksmappub($feeds_path.$smap_feed_name, $full_smapurl);
					echo $count.".  ";
					echo $ret1.": adding feed: ".$feeds_path.$smap_feed_name."; ";
					echo $ret2." with smapurl: ".$full_smapurl."\n";
				}
			} 
		}
		$count+=1;
	}
}
fclose($handle);

echo "\n\nTotal: ".$count."\n\n";

?>
