<?php
include_once "sfslib.php";
//$sfshost = "is4server.com";
//$sfsport = 8080;

$sfshost = "jortiz81.homelinux.com";
$sfsport = 8081;

$root = "http://".$sfshost.":".$sfsport;
$mels_root = "/is4/taxonomies/mels/";
$tax_file = "mels_taxonomy.txt";
$h = fopen($tax_file,"r");
$line = fgets($h);

$sfsConn = new SFSConnection();
$sfsConn->setStreamFSInfo($sfshost, $sfsport);
$rtype = "default";

while(!empty($line) ==1){
	$line = substr($line,0,strlen($line)-1);
	$cat = strtok($line,"\t");
	$subcat = strtok("\t");
	$type = strtok("\t");
	$props = strtok("\t");

	$dat = get($root.$mels_root);
	//echo $dat."\n\n";
	$dat = json_decode($dat,true);
	if(array_search($cat, $dat) == false){
		echo "Adding: ".$mels_root.$cat."/\n";
		$sfsConn->mkrsrc($mels_root, $cat, $rtype);
	}

	$dat2 =get($root.$mels_root.$cat."/");
	$dat2 = json_decode($dat2,true);
	if(array_search($subcat, $dat) == false){
		echo "Adding: ".$mels_root.$cat."/".$subcat."/\n";
		$sfsConn->mkrsrc($mels_root.$cat."/",$subcat,$rtype);
	}

	$dat3 =get($root.$mels_root.$cat."/".$subcat."/");
	$dat3 = json_decode($dat3,true);
	if(array_search($type, $dat) == false){
		echo "Adding: ".$mels_root.$cat."/".$subcat."/".$type."/\n";
		$sfsConn->mkrsrc($mels_root.$cat."/".$subcat."/",$type,$rtype);
	}

	if($props){
		$properties = array();
		$properties["description"]=$props;
		echo "Updating properties for: ".$mels_root.$cat."/".$subcat."/".$type."/\n";
		$sfsConn->updateProps($mels_root.$cat."/".$subcat."/".$type."/", $properties);
	}

	$line = fgets($h);
}
fclose($h);
?>
