<?php
include_once "sfslib.php";

$sfshost = "is4server.com";
$sfsport = 8080;


//$sfshost = "jortiz81.homelinux.com";
//$sfsport = 8081;
$root = "http://".$sfshost.":".$sfsport;

$sfs_building_path = "/is4/buildings/";
$sfs_SDH_path = $sfs_building_path."SDH/";
//$sfs_SDH_path = $sfs_building_path."jorgeApt/";
//$sfs_SDH_path = $sfs_building_path."soda/";

//get the buildings
$buildings_rinfo = get($root.$sfs_building_path);

//get the floors
/*$SDHfloor_path = $sfs_building_path."SDH/spaces/";
$SDHfloors_rinfo = get($root.$SDHfloor_path);

if(!empty($SDHfloors_rinfo) ==1){
	$SDHfloors_rinfo = json_decode($SDHfloors_rinfo, true);
}*/

//get the rooms
/*$room_paths = array();
$SDHfloors = $SDHfloors_rinfo["children"];

for($i=0; $i<count($SDHfloors); $i++){
	$t_rooms_rinfo = get($root.$SDHfloor_path.$SDHfloors[$i]."/");
	if(!empty($t_rooms_rinfo) == 1){
		$t_rooms_rinfo = json_decode($t_rooms_rinfo, true);
		$t_rooms = $t_rooms_rinfo["children"];

		//clean up the symlinks
		for($j=0; $j<count($t_rooms); $j++){
			$t_room = $t_rooms[$j];
			if(strpos($t_room,"->") !== false){
				$t_room = strtok($t_room," -> ");
			}

			//place in room_path array
			array_push($room_paths, $SDHfloor_path.$SDHfloors[$i]."/".$t_room);
		}
	}
}*/


//get the inventory
/*$i_path = $sfs_SDH_path."inventory/";
$t_inventory_rinfo = get($root.$i_path);
$t_inventory = array();

if(!empty($t_inventory_rinfo) ==1){
	$t_inventory_rinfo = json_decode($t_inventory_rinfo, true);
	$t_inventory = $t_inventory_rinfo["children"];
}*/

//get all the qrcs, filter out those without children
$qrcs_path = $sfs_SDH_path."qrc/*/*";
$qrcs_rinfo = get($root.$qrcs_path);
$qrcs=array();

$str= "/is4/buildings/SDH/qrc/RAP-58251b70-0f28-4664-9fa0-28f416f164f8/";

if(!empty($qrcs_rinfo) == 1){
	$qrcs_rinfo = json_decode($qrcs_rinfo, true);
	$keys = array_keys($qrcs_rinfo);
	$count=1;
	for($k=0; $k<count($keys); $k++){
		if(strlen($keys[$k])>strlen($str)){
			echo "[".$count."]:\t".$keys[$k]."\n";
			$count+=1;
		}
	}
}


?>
