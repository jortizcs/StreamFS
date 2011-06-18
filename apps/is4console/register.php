<?php

//object stream vars
$devname = $_GET['ostream_devname'];
$make = $_GET['ostream_make'];
$model = $_GET['ostream_model'];
$odesc = $_GET['ostream_description'];
$address=$_GET['ostream_address'];
$sensorlist=$_GET['ostream_sensorlist'];

//context stream vars
$cdesc=$_GET['cstream_description'];
$coords=$_GET['cstream_coords'];

//logic streamv ars
$schemaurl=$_GET['lstream_schemaurl'];

//$res="http://smote.cs.berkeley.edu:8080/is4/join";
$res="http://jortiz81.homelinux.com:8081/is4/join";
$ostream = array("name"=>"object_stream", 
				 "device_name"=>$devname, 
				 "model"=>$model, "desc"=>$odesc, 
				 "address"=>$address, 
				 "sensors"=>$sensorlist);
$cstream = array("name"=>"context_stream",
				 "context_desc"=>$cdesc);/*,
				 "coordinates"=>$coords,
				 "timestamp"=>1);*/

$refurl = array("\$ref"=>$schemaurl);
$lstream = array("name"=>"logic_stream",
				 "dataschema"=>$refurl);/*,
				 "timestamp"=>1);*/


/*print_r($ostream);echo "\n";
print_r($cstream);echo "\n";
print_r($lstream);echo "\n";
*/

$join_request = array("object_stream"=>$ostream, "context_stream"=>$cstream, "logic_stream"=>$lstream);
$join_request_json = json_encode($join_request);
//echo "\n".$join_request_json."\n";

$curl_handle=curl_init();
curl_setopt($curl_handle,CURLOPT_URL,$res);
curl_setopt($curl_handle,CURLOPT_POST,1);
curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $join_request_json);
curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,0);
curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
$buffer = curl_exec($curl_handle);
curl_close($curl_handle);
//print "Buffer : ".$buffer."<br>";
print $buffer;


?>
