<?php

class SaveData {
	/*
	 * Take the results returned from a timeseries query and save the timestamp and
	 * reading.
	 *
	 * fc: <feed>.<channel> (i.e. Dent_basement-1_elt-C_Circuit_Breaker_03.B_sensor_true_power)
	 * query_results: results returned from StreamFS timeseries query on a stream
	 * delim: delimiter (i.e. space, comma, etc)
	 */
	public function saveQueryResults($fc, $query_results, $delim){
		if(!empty($fc)){
			$tFileHandle = fopen("data/".$fc.".dat", 'a+');
			if($tFileHandle != FALSE){
				$qrjson = json_decode($query_results, true);
				$dat = $qrjson["ts_query_results"]["results"];
				for($i=0; $i<count($dat); $i++){
					$tdat = $dat[$i];
					$ts = $tdat["timestamp"];
					$r = $tdat["Reading"];
					fwrite($tFileHandle, $ts.$delim.$r."\n");
				}
				fclose($tFileHandle);
			}
		}
	}

	public function tarit(){
		shell_exec("tar cvzf data/data.tgz data/*.dat; rm -f data/*.dat");
	}

	public function delall(){
		shell_exec("rm -f data/*");
	}
}


$sd = new SaveData();
if(strcmp($_POST["func"], "tarit")==0){
	$sd->tarit();
} elseif(strcmp($_POST["func"], "delall")==0){
	$sd->delall();
}

?>
