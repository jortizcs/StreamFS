<?php
include_once "sfslib.php";

$sfshost = "jortiz81.homelinux.com";
$sfsport = 8081;
$t_path = "/is4/buildings/soda/spaces/";



//$sfshost = "is4server.com";
//$sfsport = 8080;
//$t_path = "/is4/buildings/SDH/spaces/";

$sfsConnection = new SFSConnection();
//$fpath = "sdh_rooms_origins.txt";
$fpath = "soda_rooms_origins.txt";

if(file_exists($fpath)){
	$frsrc = fopen($fpath, 'r');
	$line = "";
	$sfsConnection->setStreamFSInfo($sfshost, $sfsport);
	$type = "default";
	while(($line=fgets($frsrc)) != FALSE){
		$line = substr($line, 0, strlen($line)-1);
		$room = strtok($line, ' ');
		$floor=-1; $x=-100; $y=-100;
		$count = 1;
		while($count != 4){
			$count += 1;
			if($count==2) {
				$floor = strtok(' ');
			} elseif($count==3){
				$x = strtok(' ');
			} elseif($count==4){
				$y = strtok(' ');
			}
		}

		//construct the name for the floor resource
		$floorname = "floor";
		if($floor>=0 && $floor<10){
			$floorname.="0".$floor;
		} elseif($floor>=10) {
			$floorname.=$floor;
		}
		echo "checking ".$t_path.$floorname."\n";

		//check if that resource already exists
		if($floor>=0 && !$sfsConnection->exists($t_path.$floorname)){
			echo $t_path.$floorname." does not exist; creating it\n";
			$sfsConnection->mkrsrc($t_path, $floorname, $type);
		}

		if(!empty($room) && $x>0 && $y>0 && $floor>=0 ) {

			if(!$sfsConnection->exists($t_path.$floorname."/room".$room)){
				echo "Creating room resource: ".$t_path.$floorname."/room".$room;

				//create the room under the floor resource	
				$res=$sfsConnection->mkrsrc($t_path.$floorname, "room".$room, $type);
				echo $res."\n";
			}

			$props = array();
			$props["f"]=$floor;
			$props["x"]=$x;
			$props["y"]=$y;
			$res=$sfsConnection->updateProps($t_path.$floorname."/room".$room, $props);
			echo "room:".$room.", floor:".$floor.", (".$x.",".$y.")\n";
			echo $res."\n";
		}
	}
	fclose($frsrc);
}

?>
