<?php
// Database config & class
$db_config = array(
	"servername"=> "localhost",
	"username"	=> "root",
	"password"	=> "root",
	"database"	=> "jstree"
);
if(extension_loaded("mysqli")) require_once("lib/jsTree.v.1.0rc/_demo/_inc/class._database_i.php"); 
else require_once("lib/jsTree.v.1.0rc/_demo/_inc/class._database.php"); 

// Tree class
require_once("class.tree2.php"); 
?>