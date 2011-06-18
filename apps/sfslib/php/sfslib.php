<?php
include_once "curl_ops.php";

class SFSConnection{
	/**
	 * StreamFS API.  Creates the specified json object for the associated request and sends to
	 * to the specified StreamFS host and port.
	 * 
	 * Assumption: only http is supported (https will be supported in coming versions.)
	 */
	private $host="";
	private $port=8080;

	public function setStreamFSInfo($sfshost, $sfsport){
		global $host,$port;
	        $host= $sfshost;
		$port = $sfsport;
		//echo "Host set to ".$host." and port set to ".$port."<br/>";
	}



	/*
	 * Make a resource of the specified type.
	 * Supported types: default, devices, device, genpub
	 */
	public function mkrsrc($path, $name, $type){
		$request = array();
		if(strcasecmp($type,"default")==0){
			$request["operation"]="create_resource";
			$request["resourceName"]=$name;
			$request["resourceType"]=$type;
		} elseif(strcasecmp($type,"devices")==0){
			$request["operation"]="create_resource";
			$request["resourceName"]="devices";
			$request["resourceType"]=$type;
		} elseif(strcasecmp($type, "device")==0){
			$request["operation"]="create_resource";
			$request["resourceName"]=$name;
			$request["deviceName"]=$name;
			$request["resourceType"]=$type;
		} elseif(strcasecmp($type, "genpub")==0) {
			$request["operation"]="create_generic_resource";
			$request["resourceName"]=$name;
		}

		global $host, $port;	
		$url = "http://".$host.":".$port.$path;
		//echo "URL: ".$url."; Formed request: ".json_encode($request)."<br />";
		if(count($request)>0){
			$reply = put(json_encode($request), $url);
			return $reply;
		}
		return 0;
	}


	public function mksymlink($path, $target, $linkname){
		$request = array();
		$request["operation"]="create_symlink";
		$request["name"]=$linkname;
		if($target[0]=='/'){
			$request["uri"]=$target;
		} else {
			$request["url"]=$target;
		}

		global $host, $port;	
		$url = "http://".$host.":".$port.$path;
		//echo "URL: ".$url."; Formed request: ".json_encode($request)."<br />";
		if(count($request)>0){
			$reply = put(json_encode($request), $url);
			return $reply;
		}
		return 0;
	}

	public function mksmappub($path, $smapurl){
		$request = array();
		$smapurls = array();
		array_push($smapurls, $smapurl);
		$request["operation"]="create_smap_publisher";
		$request["smap_urls"]=$smapurls;

		global $host, $port;	
		$url = "http://".$host.":".$port.$path;
		//echo "URL: ".$url."; Formed request: ".json_encode($request)."<br />";
		if(count($request)>0){
			$reply = put(json_encode($request), $url);
			return $reply;
		}
		return 0;
	}

	public function overwriteProps($path, $props){
		$request = array();
		$request["operation"]="overwrite_properties";

		//props should be an associative array ["tag"->"value"]
		$request["properties"]=$props;

		global $host, $port;	
		$url = "http://".$host.":".$port.$path;
		//echo "URL: ".$url."; Formed request: ".json_encode($request)."<br />";
		if(count($request)>0){
			$reply = post(json_encode($request), $url);
			return $reply;
		}
		return 0;
	}

	public function updateProps($path, $props){
		$request = array();
		$request["operation"]="update_properties";

		//props should be an associative array ["tag"->"value"]
		$request["properties"]=$props;

		global $host, $port;	
		$url = "http://".$host.":".$port.$path;
		//echo "URL: ".$url."; Formed request: ".json_encode($request)."<br />";
		if(count($request)>0){
			$reply = post(json_encode($request), $url);
			return $reply;
		}
		return 0;
	}

	public function exists($path){
		global $host, $port;	
		$url = "http://".$host.":".$port.$path;
		//echo "checking url: ".$url."\n";
		$res = get($url);
		if(empty($res)){
			return false;
		}
		return true;
	}

	public function tsQuery($path, $timestamp){
		global $host, $port;
		$url = "http://".$host.":".$port.$path."?query=true&ts_timestamp=".$timestamp;
		$res = get($url);
		if(empty($res)){
			return 0;
		}
		return $res;
	}

	public function tsRangeQuery($path, $tslowerbound, $includelb, $tsupperbound, $includeub){
		global $host, $port;
		$queryParams = "?query=true&";
		if($includelb == true) {
			$queryParams = $queryParams."ts_timestamp=gte:".$tslowerbound;
		} else {
			$queryParams = $queryParams."ts_timestamp=gt:".$tslowerbound;
		}

		if($includeub == true){
			$queryParams = $queryParams.",lte:".$tsupperbound;
		} else {
			$queryParams = $queryParams.",lt:".$tsupperbound;
		}

		$url = "http://".$host.":".$port.$path.$queryParams;
		$res = get($url);
		if(empty($res)){
			return 0;
		}
		return $res;
	}

	public function tsNowRangeQuery($path, $tslowerbound,$includelb, $includeub){
		global $host, $port;
		$queryParams = "?query=true&";
		if($includelb == true) {
			$queryParams = $queryParams."ts_timestamp=gte:".$tslowerbound;
		} else {
			$queryParams = $queryParams."ts_timestamp=gt:".$tslowerbound;
		}

		if($includeub == true){
			$queryParams = $queryParams.",lte:now";
		} else {
			$queryParams = $queryParams.",lt:now";
		}
		$url = "http://".$host.":".$port.$path.$queryParams;
		$res = get($url);
		if(empty($res)){
			return 0;
		}
		return $res;
	}
	
	public function getSFSTime(){
		global $host, $port;
		$url = "http://".$host.":".$port."/time/";
		$res = get($url);
		if(empty($res)){
			return 0;
		}
		return $res;
	}

}

?>
