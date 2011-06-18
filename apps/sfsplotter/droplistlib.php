<?php
include_once("../sfslib/php/sfslib.php");
include_once("savedata.php");

class SFSDroplistGen {

	private $host="";
	private $port=-1;
	private $sfsconnObj;

	public function init($sfshost, $sfsport){
		global $host,$port;
	        $host= $sfshost;
		$port = $sfsport;
		$sfsconnObj = new SFSConnection();
		$sfsconnObj->setStreamFSInfo($sfshost, $sfsport);
	}

	public function genFeedsDropList($selectName, $onchangeHandler){
		global $host,$port;
		$sfs_feeds_url="http://".$host.":".$port."/is4/feeds/";
		$feedcnt=0;
		$feeds_resp = json_decode(get($sfs_feeds_url), true);
		$feeds = $feeds_resp["children"];

		$selectStr = "<select";
		if(strlen($selectName)>0){
			$selectStr = $selectStr." id=\"".$selectName."\" name=\"".$selectName."\"";
		}
		if(strlen($onchangeHandler)>0){
			$selectStr=$selectStr." onchange=".$onchangeHandler;
		}
		$selectStr=$selectStr.">";
		echo $selectStr;
		foreach($feeds as $feedname){
			echo "<option value=\"".$feedcnt."\">".$feedname."</option>";
			$feedcnt+=1;
		}
		echo "</select>";
	}

	public function genChanDropList($name, $feed){
		global $host,$port;
		$furl="http://".$host.":".$port."/is4/feeds/".$feed;
		$feeds_resp = json_decode(get($furl), true);
		$feeds = $feeds_resp["children"];
		$selectStr = "<select";
		if(strlen($name)>0){
			$selectStr = $selectStr." id=\"".$name."\" name=\"".$name."\"";
		}
		$selectStr=$selectStr.">";
		echo $selectStr;
		foreach($feeds as $feedname){
			echo "<option value=\"".$feedcnt."\">".$feedname."</option>";
			$feedcnt+=1;
		}
		echo "</select>";
	}
}

#usage
#$droplist = new SFSDroplistGen();
#$droplist->init("is4server.com", 8080);
#$droplist->genFeedsDropList();
#$droplist->genChanDropList("ION_6200_Cory");

#file_get_contents('php://input')
#print_r($_POST);
$res = get($_POST["url"]);
echo $res;
######################
###save the resutls###
######################
$savedata = new SaveData();
if(strpos("/", $_POST["feedchan"])===false){
	$name = str_replace("/", ".", $_POST["feedchan"]);
	$savedata->saveQueryResults($name, $res, " ");
} else {
	$savedata->saveQueryResults($_POST["feedchan"], $res, " ");
}
?>
