<?php
include_once ("../sfslib/php/sfslib.php");

/*$sfs_host = $_POST["sfs_host"];
$sfs_port = $_POST["sfs_port"];*/
$sfs_host = "jortiz81.homelinux.com";
$sfs_port = 8081;
$method=$_REQUEST["method"];

function get_tree(){
	global $sfs_host, $sfs_port;
	#echo "http://".$sfs_host.":".$sfs_port."/admin/listrsrcs/<br>";
	$reply = get("http://".$sfs_host.":".$sfs_port."/admin/listrsrcs/");
	if(!empty($reply) && $reply !=false){
		$reply_json = json_decode($reply, true);
		$keys = array_keys($reply_json);
		$pathToObj = array();

		for($i=0; $i<count($keys); $i++){
			$tkey = trim($keys[$i]);
			$toks = explode("/", $tkey);
			$nodeObj = array();
			if(count($toks)-2>0){
				$nodeObj["data"]=$toks[ count($toks)-2];
				$nodeObj["state"]="closed";
			} else {
				$nodeObj["data"]="/";
				$nodeObj["state"]="open";
			}
			$nodeObj["metadata"]=array("id" => $tkey, "type" => $reply_json[$tkey]["type"]);
			$nodeObj["children"]=array();

			#print_r($nodeObj);
			#echo $nodeObj["metadata"]["id"];
			#echo "<br><br>";
			$pathToObj[$tkey]=$nodeObj;
		}

		for($i=0; $i<count($keys); $i++){
			$tkey = trim($keys[$i]);
			$tnode = &$pathToObj[$tkey];
			$toks = explode("/",$tkey);
			$level = count($toks)-2;
			$parent="/";
			if($level>0){
				for($j=1; $j<count($toks)-2; $j++)
					$parent = $parent.$toks[$j]."/";
				$parentObj = &$pathToObj[$parent];
				if(!empty($parentObj))
					array_push($parentObj["children"], &$tnode);
			}
		}
		return json_encode($pathToObj["/"]);
		
	}
	return false;
}

if(!empty($sfs_host) && !empty($sfs_port) && !empty($method)){
	$sfsconn = new SFSConnection($sfs_host, $sfs_port);

	if(strcmp($method, "get_all_resources")==0){
		echo get_tree();
	} elseif(strcmp($method, "get_path")==0){
		$path = $_REQUEST["path"];
		echo get("http://".$sfs_host.":".$sfs_port.$path);
	}
}
?>
