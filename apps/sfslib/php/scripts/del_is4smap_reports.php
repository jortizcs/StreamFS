<?php
include_once "curl_ops.php";

$prefix = "http://is4server.com";
$smapfile= "smap_feeds.txt";
$h = fopen($smapfile, "r");
$line = fgets($h);
while(!empty($line) ==1){
	$t_smapurl = strtok($line," ");
	if($t_smapurl[0] !== '#'){
		$reply = get($t_smapurl."/reporting/reports/");
		if(!empty($reply) == 1){
			$reply = "{\"reports\":".$reply."}";
			//echo $t_smapurl."/reporting/reports/:";
			//echo $reply."\n";

			$reply = json_decode($reply,true);
			$reports = $reply["reports"];
			$numreports = count($reports);

			for($i=0; $i<$numreports; $i++){
				$reply = get($t_smapurl."/reporting/reports/".$reports[$i]);
				if(!empty($reply) ==1){
					$reply = json_decode($reply,true);
					$target = $reply["ReportDeliveryLocation"];
					if(strlen($target)>strlen(prefix) && 
						strcmp(substr($target, 0, strlen($prefix)), $prefix)==0)
					{
						echo "Deleting: ".$t_smapurl."/reporting/reports/".$reports[$i]."\n";
						print_r($reply);
						delete($t_smapurl."/reporting/reports/".$reports[$i]);

					}
				}
			}

		}
	}
	$line = fgets($h);
}
fclose($h);
?>
