<?php

/**
 * Generic functions to get/put/post/delete on a given url.
 */

function get($url){
	$curl_handle=curl_init();
	curl_setopt($curl_handle,CURLOPT_URL,$url);
	curl_setopt($curl_handle,CURLOPT_HTTPGET,1);
	curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,15);
	curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
	$reply = curl_exec($curl_handle);
	curl_close($curl_handle);
	
	return $reply;
}

function put($putData_str, $url){
	$putDataFile = tmpfile();
	fwrite($putDataFile, $putData_str);
	fseek($putDataFile, 0);

	$curl_handle=curl_init();
	curl_setopt($curl_handle,CURLOPT_URL,$url);
	curl_setopt($curl_handle,CURLOPT_PUT,1);
	curl_setopt($curl_handle,CURLOPT_INFILE, $putDataFile);
	curl_setopt($curl_handle,CURLOPT_INFILESIZE, strlen($putData_str));
	curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,15);
	curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
	$reply = curl_exec($curl_handle);
	curl_close($curl_handle);
	
	return $reply;
}

function post($postData_str, $url){
	$curl_handle=curl_init();
	curl_setopt($curl_handle,CURLOPT_URL,$url);
	curl_setopt($curl_handle,CURLOPT_POST,1);
	curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $postData_str);
	curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
	curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
	$reply = curl_exec($curl_handle);
	curl_close($curl_handle);

	return $reply;
}

function delete($url){
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$url);
		curl_setopt($curl_handle, CURLOPT_CUSTOMREQUEST, "DELETE");
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$reply = curl_exec($curl_handle);
		curl_close($curl_handle);
		return $reply;
}
?>
