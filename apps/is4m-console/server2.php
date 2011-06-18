<?php
	//server2.php
	require_once("config2.php");
	$jstree = new json_tree();
	
	//$jstree->_create_default();
	//die();
	
	if(isset($_GET["reconstruct"])) {
		$jstree->_reconstruct();
		die();
	}
	if(isset($_GET["analyze"])) {
		echo $jstree->_analyze();
		die();
	}
	
	/*if($_REQUEST["operation"] && strpos("_", $_REQUEST["operation"]) !== 0 && 
			strcmp($_REQUEST["operation"], "is4Put")==0 && method_exists($jstree, $_REQUEST["operation"])) {
				$r = $jstree->{$_REQUEST["operation"]}($_REQUEST);
				echo $r."<br />";
				print_r($_REQUEST);
				echo "<br />";
				$tdat = file_get_contents("php://input");
				print_r($tdat);
				die();
			}*/
	
	/*if($_REQUEST["operation"] && strpos("_", $_REQUEST["operation"]) !== 0 && 
			strcmp($_REQUEST["operation"], "importContextGraph")==0 && method_exists($jstree, $_REQUEST["operation"])) {
				//echo "okasdas:".$_REQUEST["operation"]."::";
				//print_r($_REQUEST);
				$data2 = file_get_contents("php://input");
				echo $data2;
				echo "<br />".gettype($data2);
				//$pdata = $_REQUEST["pdata"];
				//echo gettype($pdata);
				/*echo "<br />POST_DATA:";
				print_r($_POST);*/
				/*$r = $jstree->{$_REQUEST["operation"]}($_REQUEST);
				echo $r;
				die();
			}*/
	
	if($_REQUEST["operation"] && strpos("_", $_REQUEST["operation"]) !== 0 && method_exists($jstree, $_REQUEST["operation"])) {
		header("HTTP/1.0 200 OK");
		header('Content-type: text/json; charset=utf-8');
		header("Cache-Control: no-cache, must-revalidate");
		header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
		header("Pragma: no-cache");
		//echo $_REQUEST["operation"];
		echo $jstree->{$_REQUEST["operation"]}($_REQUEST);
		die();
	}
	header("HTTP/1.0 404 Not Found");
	
?>
