<?php
//global reference to is4 url
$is4_url = "http://184.106.204.181:8080";

//valid operations
$bufferPut = "buffer_put";
$bufferGet = "buffer_get";

//issues get request to is4, returns json reply as an array or false if the reply was empty
function is4Get($is4Uri){
	global $is4_url;
	$res = $is4_url.$is4Uri;

	$curl_handle=curl_init();
	curl_setopt($curl_handle,CURLOPT_URL,$res);
	curl_setopt($curl_handle,CURLOPT_HTTPGET,1);
	curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
	curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
	$reply = curl_exec($curl_handle);
	curl_close($curl_handle);

	if(strlen($reply)>0){
		return json_decode($reply, true);
	} else {
		return FALSE;
	}
}

function db_connect(){
	// Connecting, selecting database
	$link = mysql_connect('localhost', 'root', '410soda') or die('Could not connect: ' . mysql_error());
	mysql_select_db('is4Buffer') or die('Could not select database');
	return $link;
}

function db_disconnect($dblink){
	// Closing connection
	mysql_close($link);
}

function isArray($var){
	return (strcmp(gettype($var), "array")==0);
}

function isBoolean($var){
		return (strcmp(gettype($var), "boolean")==0);
}

function emptyBuffer($subid, $databuffer){
	$query_template = "DELETE from `is4_proxy_buffer` where timestamp IN (%s) and publisher_id IN (%s) and subscriber_id='%s';";

	$timestamps = array();
	$timestamps_strs = array();
	$pubids_strs = array();
	//print_r($databuffer);

	//populate timestamps
	array_push($timestamps, array_keys($databuffer));
	//print_r($timestamps);

	//populate pubids
	for($i=0; $i<count($timestamps[0]); $i++){
		$this_ts = $timestamps[0][$i];
		$datapair = $databuffer[$this_ts];
		array_push($pubids_strs, "'".$datapair[0]."'");
		array_push($timestamps_strs, "'".$this_ts."'");
	}

	$timestamps_str = implode(", ", $timestamps_strs);
	$pubids_str = implode(", ", $pubids_strs);
	
	$query =sprintf($query_template, $timestamps_str, $pubids_str, $subid);
	//echo "QUERY: ".$query."\n";
	
	$dblink = db_connect();
	mysql_query($query, $dblink) or die('Query failed: ' . mysql_error());
	db_disconnect($dblink);
}

function getBufferedData($subid){
	$query_template = "SELECT publisher_id, data_object, timestamp FROM `is4_proxy_buffer` WHERE subscriber_id = '%s'";
	$query = sprintf($query_template, $subid);
	$dblink = db_connect();
	$result = mysql_query($query, $dblink) or die('Query failed: ' . mysql_error());
	/*construct json reply
	
		{"timestamp":[{"pubid", "data"}, ...],
			...,
		 "timestamp":[{"pubid", "data"}, ...]
		}
	*/
	$dataBuffer = array();
	while ($line = mysql_fetch_array($result, MYSQL_ASSOC)) {

		$this_pubid = $line["publisher_id"];
		$this_dataobj = $line["data_object"];
		$this_ts = $line["timestamp"];
		
		//populate pair
		$data_pair = array();
		array_push($data_pair, $this_pubid);
		array_push($data_pair, $this_dataobj);
		
		//indexed by timestamp
		$dataBuffer[$this_ts] = $data_pair;
	}
	
	// Free resultset
	mysql_free_result($result);
	db_disconnect($dblink);

	//print_r($dataBuffer);
	
	if(count($dataBuffer)>0){
		//delete these values from the buffer for this subscriber
		emptyBuffer($subid, $dataBuffer);
		return $dataBuffer;
	} else {
		return FALSE;
	}
}

function appendToBuffer($subid, $pubid, $data){
	$query_template = "INSERT INTO `is4_proxy_buffer` (`subscriber_id`, `publisher_id`, `data_object`) VALUES ('%s', '%s', '%s');";
	$query = sprintf($query_template, $subid, $pubid, $data);
	$dblink = db_connect();
	mysql_query($query, $dblink) or die('Query failed: ' . mysql_error());
	db_disconnect($dblink);
}

//get the subscriber id (who is asking for this information)
//ge the list of publisher ids
$operation = $_GET["operation"];
$subid = $_GET["subid"];
$pubid = $_GET["pubid"];
$postData = file_get_contents("php://input");

//echo "operation= ".$operation."\nsubid=".$subid."\npudid=".$pubid."\nposted: ".$postData;

//HANDLE DRAIN BUFFER REQUEST 
//Data fetch process
if(!empty($operation) && !empty($subid) && strcmp($operation, $bufferGet)==0){
		//valid subid
		$bufferedData = getBufferedData($subid);
		//echo "\nBufferedData: \n";
		//print_r($bufferedData);
		while(!isArray($bufferedData)) {
			sleep(5);
			$bufferedData = getBufferedData($subid);
		}
		echo json_encode($bufferedData);
} 

//HANDLE BUFFER PUSH REQUEST
elseif (!empty($operation) && !empty($subid) && !empty($pubid) && strcmp($operation, $bufferPut)==0){
	$allsubs_resp = is4Get("/is4/sub/all");
	$allpubs_resp = is4Get("/is4/pub/all");
	$postData_json = json_decode($postData, true);
	if(isArray($allsubs_resp) && isArray($allpubs_resp)&& in_array($subid, $allsubs_resp["subscribers"]) && 
		in_array($pubid, $allpubs_resp["streams"]) && $postData_json != NULL) {
		appendToBuffer($subid, $pubid, $postData);
	}
}

//Data post process
//if operation=put_buffer
//pudid must be set
//take the raw data and store it
?>
