<?php
include_once "sfslib.php";

/*$sfshost = "jortiz81.homelinux.com";
$sfsport = 8081;
$qrc_path = "/is4/buildings/jorgeApt/qrc";
*/

/*
$sfshost = "is4server.com";
$sfsport = 8080;
$qrc_path = "/is4/buildings/SDH/qrc";
*/

$sfsConnection = new SFSConnection();
$fpath = "sdh_qrcs.txt";

if(file_exists($fpath)){
	$frsrc = fopen($fpath, 'r');
	$line = "";
	$sfsConnection->setStreamFSInfo($sfshost, $sfsport);
	while(($line=fgets($frsrc)) != FALSE){
		$line = substr($line, 0, strlen($line)-1);
		$type = "default";
		$res=$sfsConnection->mkrsrc($qrc_path, $line, $type);
		echo "mkrsrc(".$line."): ".$res."\n";
	}
	fclose($frsrc);
}

?>
