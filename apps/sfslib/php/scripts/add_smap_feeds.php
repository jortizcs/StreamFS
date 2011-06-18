<?php
include_once "sfslib.php";

//check if the feed is already in /is4/buildings/CoryHall/inventory/
$sfs_host = "is4server.com";
$sfs_port = 8080;
//$s_path = "/is4/buildings/CoryHall/inventory/";
$sfs_instance_url = "http://".$sfs_host.":".$sfs_port.$s_path;	//is4server.com:8080/is4/buildings/CoryHall/inventory/";
$sfsConn = new SFSConnection();
$sfsConn->setStreamFSInfo($sfs_host, $sfs_port);

//starred smap url to inventory uri mapping
$star_smapurl_to_sfsurl = array();

if($sfsConn->exists($s_path)){
	$reg_devs = get($sfs_instance_url);
	$reg_devs_array = json_decode($reg_devs);
	//print_r($reg_devs_array);

	$children = $reg_devs_array->{"children"};
	//echo $children[0]."\n";
	$c_size = count($children);
	//echo "c_size=".$c_size."\n";
	for($i=0; $i<$c_size; $i++){
		$thisurl = $sfs_instance_url.$children[$i]."/";
		//echo "getting this url: ".$thisurl."\n";
		$thisget = get($thisurl);
		$thisget_array = json_decode($thisget);
		$stream_childs = $thisget_array->{"children"};
		//print_r($stream_childs);
		$num_streams = count($stream_childs);
		if($num_streams>0){
			$first_stream_name = $stream_childs[0];
			$stream_url = $thisurl.$first_stream_name."/";
			//echo "getting: ".$stream_url."\n";
			$stream_info = get($stream_url);
			$stream_info_array = json_decode($stream_info);
			$assoc_smap_url = $stream_info_array->{"smap_url"};
			//echo $assoc_smap_url."\n";

			//parse out the last 3 directories and append */*/reading
			$dirs = array();
			$t_dir = strtok($assoc_smap_url, "/");
			while(!empty($t_dir)){
				array_push($dirs, $t_dir);
				$t_dir = strtok("/");
			}
			$new_ = "";
			for($k=0; $k<count($dirs)-4; $k++){
				$new_.=$dirs[$k]."/";
			}
			$new_.="*/*/*/reading/";
			//echo $new_."\t".$thisurl."\n";
			$new_=str_replace("http:/", "http://", $new_);
			$star_smapurl_to_sfsurl[$new_]=$thisurl;
		}
	}
	print_r($star_smapurl_to_sfsurl);
} else {
	echo "Could not connect to ".$sfs_instance_url."\n";
}


//read the smap feeds and register those have are not 
//already in the cory hall inventory
$smap_feeds = "smap_feeds.txt";
$feeds_path = "/is4/feeds/";
$handle = fopen($smap_feeds, "r");
if($handle){
	$line ="";
	while(($line=fgets($handle)) !== false){
		$line = substr($line, 0, strlen($line)-1);
		if($line[0] !== '#'){
			$smap_feed_url = strtok($line, " ");
			$smap_feed_name = strtok(" ");
			//echo "feed:".$smap_feed_url.", name:".$smap_feed_name."\n";
			$devs = get($smap_feed_url."/data/");
			//echo $devs."\n";
			$t_key =$smap_feed_url."/data/*/*/*/reading/";
			//echo "looking up: ".$t_key."\n";
			
			
			//$lookup = $star_smapurl_to_sfsurl[$t_key];

			//echo "val:".$lookup."\n";
			//echo "val:".$lookup_bool."\n";
			//$lookup_bool = array_key_exists($t_key, $star_smapurl_to_sfsurl);
			//print_r($star_smapurl_to_sfsurl);
			
			if(!empty($devs)){// && empty($lookup)){
				$full_smapurl = $smap_feed_url."/data/*/*/*/reading/";
				if(!$sfsConn->exists($feeds_path.$smap_feed_name)){
					$type = "device";
					$ret1 = $sfsConn->mkrsrc($feeds_path, $smap_feed_name, $type);
					$ret2 = $sfsConn->mksmappub($feeds_path.$smap_feed_name, $full_smapurl);
					echo $ret1.": adding feed: ".$feeds_path.$smap_feed_name."; ";
					echo $ret2." with smapurl: ".$full_smapurl."\n";
				}
			} /*elseif(!empty($lookup)){
				$prefix = "http://".$sfs_host.":".$sfs_port;
				$t_lookup = substr($lookup, strlen($prefix));
				echo "adding a symlink: /feeds/".$smap_feed_name."->".$t_lookup."\n";
				//$sfsConn->mksymlink($feeds_path, $t_lookup, $smap_feed_name);
			}*/
		}
	}
}
fclose($handle);

?>
