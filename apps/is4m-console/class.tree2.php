<?php
class _tree_struct {
	// Structure table and fields
	protected $table	= "";
	protected $fields	= array(
			"id"		=> false,
			"parent_id"	=> false,
			"position"	=> false,
			"left"		=> false,
			"right"		=> false,
			"level"		=> false
		);

	// Constructor
	function __construct($table = "tree", $fields = array()) {
		$this->table = $table;
		if(!count($fields)) {
			foreach($this->fields as $k => &$v) { $v = $k; }
		}
		else {
			foreach($fields as $key => $field) {
				switch($key) {
					case "id":
					case "parent_id":
					case "position":
					case "left":
					case "right":
					case "level":
						$this->fields[$key] = $field;
						break;
				}
			}
		}
		// Database
		$this->db = new _database;
	}
	
	

	function _get_node($id) {
		$this->db->query("SELECT `".implode("` , `", $this->fields)."` FROM `".$this->table."` WHERE `".$this->fields["id"]."` = ".(int) $id);
		$this->db->nextr();
		return $this->db->nf() === 0 ? false : $this->db->get_row("assoc");
	}
	
	
	function _get_children($id, $recursive = false) {
		$children = array();
		if($recursive) {
			$node = $this->_get_node($id);
			$this->db->query("SELECT `".implode("` , `", $this->fields)."` FROM `".$this->table."` WHERE `".$this->fields["left"]."` >= ".(int) $node[$this->fields["left"]]." AND `".$this->fields["right"]."` <= ".(int) $node[$this->fields["right"]]." ORDER BY `".$this->fields["left"]."` ASC");
		}
		else {
			$this->db->query("SELECT `".implode("` , `", $this->fields)."` FROM `".$this->table."` WHERE `".$this->fields["parent_id"]."` = ".(int) $id." ORDER BY `".$this->fields["position"]."` ASC");
		}
		while($this->db->nextr()) $children[$this->db->f($this->fields["id"])] = $this->db->get_row("assoc");
		return $children;
	}
	
	
	function _get_path($id) {
		$node = $this->_get_node($id);
		$path = array();
		if($node === false) return false;
		$this->db->query("SELECT `".implode("` , `", $this->fields)."` FROM `".$this->table."` WHERE `".$this->fields["left"]."` <= ".(int) $node[$this->fields["left"]]." AND `".$this->fields["right"]."` >= ".(int) $node[$this->fields["right"]]);
		while($this->db->nextr()) $path[$this->db->f($this->fields["id"])] = $this->db->get_row("assoc");
		return $path;
	}
	
	/**
	 * Follow the path from the currently selected node to the ROOT node.
	 */
	function _get_path2($id){
		$node = $this->_get_node($id);
		$path = array();
		if($node === false) return false;
		$parentid=-1;
		$this->db->query("SELECT id, parent_id, title from ".$this->table." WHERE id=".$node[$this->fields["id"]]);
		while($parentid!=0 && $this->db->nextr()){
			$thisrow = $this->db->get_row("assoc");
			$parentid = $thisrow["parent_id"];
			array_push($path, $thisrow["title"]);
			$node = $this->_get_node($parentid);
			$this->db->query("SELECT id, parent_id, title from ".$this->table." WHERE id=".$node[$this->fields["id"]]);
		}
		$path = array_reverse($path);
		return $path;
	}
	
	function _remove($id) {
		$data = $this->_get_node($id);
		$lft = (int)$data[$this->fields["left"]];
		$rgt = (int)$data[$this->fields["right"]];
		$dif = $rgt - $lft + 1;

		// deleting node and its children
		$this->db->query("" . 
			"DELETE FROM `".$this->table."` " . 
			"WHERE `".$this->fields["left"]."` >= ".$lft." AND `".$this->fields["right"]."` <= ".$rgt
		);
		// shift left indexes of nodes right of the node
		$this->db->query("".
			"UPDATE `".$this->table."` " . 
				"SET `".$this->fields["left"]."` = `".$this->fields["left"]."` - ".$dif." " . 
			"WHERE `".$this->fields["left"]."` > ".$rgt
		);
		// shift right indexes of nodes right of the node and the node's parents
		$this->db->query("" . 
			"UPDATE `".$this->table."` " . 
				"SET `".$this->fields["right"]."` = `".$this->fields["right"]."` - ".$dif." " . 
			"WHERE `".$this->fields["right"]."` > ".$lft
		);

		$pid = (int)$data[$this->fields["parent_id"]];
		$pos = (int)$data[$this->fields["position"]];

		// Update position of siblings below the deleted node
		$this->db->query("" . 
			"UPDATE `".$this->table."` " . 
				"SET `".$this->fields["position"]."` = `".$this->fields["position"]."` - 1 " . 
			"WHERE `".$this->fields["parent_id"]."` = ".$pid." AND `".$this->fields["position"]."` > ".$pos
		);
		return true;
	}
	
	
	
	function _create($parent, $position) {
		return $this->_move(0, $parent, $position);
	}
	
	
	
	
	function _move($id, $ref_id, $position = 0, $is_copy = false) {
		$sql		= array();						// Queries executed at the end
		$node		= $this->_get_node($id);		// Node data
		$nchildren	= $this->_get_children($id);	// Node children
		$ref_node	= $this->_get_node($ref_id);	// Ref node data
		$rchildren	= $this->_get_children($ref_id);// Ref node children

		$ndif = 2;
		$node_ids = array(-1);
		if($node !== false) {
			$node_ids = array_keys($this->_get_children($id, true));
			// TODO: should be !$is_copy && , but if copied to self - screws some right indexes
			if(in_array($ref_id, $node_ids)) return false;
			$ndif = $node[$this->fields["right"]] - $node[$this->fields["left"]] + 1;
		}
		if($position >= count($rchildren)) {
			$position = count($rchildren);
		}

		// Not creating or copying - old parent is cleaned
		if($node !== false && $is_copy == false) {
			$sql[] = "" . 
				"UPDATE `".$this->table."` " . 
					"SET `".$this->fields["position"]."` = `".$this->fields["position"]."` - 1 " . 
				"WHERE " . 
					"`".$this->fields["parent_id"]."` = ".$node[$this->fields["parent_id"]]." AND " . 
					"`".$this->fields["position"]."` > ".$node[$this->fields["position"]];
			$sql[] = "" . 
				"UPDATE `".$this->table."` " . 
					"SET `".$this->fields["left"]."` = `".$this->fields["left"]."` - ".$ndif." " . 
				"WHERE `".$this->fields["left"]."` > ".$node[$this->fields["right"]];
			$sql[] = "" . 
				"UPDATE `".$this->table."` " . 
					"SET `".$this->fields["right"]."` = `".$this->fields["right"]."` - ".$ndif." " . 
				"WHERE " . 
					"`".$this->fields["right"]."` > ".$node[$this->fields["left"]]." AND " . 
					"`".$this->fields["id"]."` NOT IN (".implode(",", $node_ids).") ";
		}
		// Preparing new parent
		$sql[] = "" . 
			"UPDATE `".$this->table."` " . 
				"SET `".$this->fields["position"]."` = `".$this->fields["position"]."` + 1 " . 
			"WHERE " . 
				"`".$this->fields["parent_id"]."` = ".$ref_id." AND " . 
				"`".$this->fields["position"]."` >= ".$position." " . 
				( $is_copy ? "" : " AND `".$this->fields["id"]."` NOT IN (".implode(",", $node_ids).") ");

		$ref_ind = $ref_id === 0 ? (int)$rchildren[count($rchildren) - 1][$this->fields["right"]] + 1 : (int)$ref_node[$this->fields["right"]];
		$ref_ind = max($ref_ind, 1);

		$self = ($node !== false && !$is_copy && (int)$node[$this->fields["parent_id"]] == $ref_id && $position > $node[$this->fields["position"]]) ? 1 : 0;
		foreach($rchildren as $k => $v) {
			if($v[$this->fields["position"]] - $self == $position) {
				$ref_ind = (int)$v[$this->fields["left"]];
				break;
			}
		}
		if($node !== false && !$is_copy && $node[$this->fields["left"]] < $ref_ind) {
			$ref_ind -= $ndif;
		}

		$sql[] = "" . 
			"UPDATE `".$this->table."` " . 
				"SET `".$this->fields["left"]."` = `".$this->fields["left"]."` + ".$ndif." " . 
			"WHERE " . 
				"`".$this->fields["left"]."` >= ".$ref_ind." " . 
				( $is_copy ? "" : " AND `".$this->fields["id"]."` NOT IN (".implode(",", $node_ids).") ");
		$sql[] = "" . 
			"UPDATE `".$this->table."` " . 
				"SET `".$this->fields["right"]."` = `".$this->fields["right"]."` + ".$ndif." " . 
			"WHERE " . 
				"`".$this->fields["right"]."` >= ".$ref_ind." " . 
				( $is_copy ? "" : " AND `".$this->fields["id"]."` NOT IN (".implode(",", $node_ids).") ");

		$ldif = $ref_id == 0 ? 0 : $ref_node[$this->fields["level"]] + 1;
		$idif = $ref_ind;
		if($node !== false) {
			$ldif = $node[$this->fields["level"]] - ($ref_node[$this->fields["level"]] + 1);
			$idif = $node[$this->fields["left"]] - $ref_ind;
			if($is_copy) {
				$sql[] = "" . 
					"INSERT INTO `".$this->table."` (" . 
						"`".$this->fields["parent_id"]."`, " . 
						"`".$this->fields["position"]."`, " . 
						"`".$this->fields["left"]."`, " . 
						"`".$this->fields["right"]."`, " . 
						"`".$this->fields["level"]."`" . 
					") " . 
						"SELECT " . 
							"".$ref_id.", " . 
							"`".$this->fields["position"]."`, " . 
							"`".$this->fields["left"]."` - (".($idif + ($node[$this->fields["left"]] >= $ref_ind ? $ndif : 0))."), " . 
							"`".$this->fields["right"]."` - (".($idif + ($node[$this->fields["left"]] >= $ref_ind ? $ndif : 0))."), " . 
							"`".$this->fields["level"]."` - (".$ldif.") " . 
						"FROM `".$this->table."` " . 
						"WHERE " . 
							"`".$this->fields["id"]."` IN (".implode(",", $node_ids).") " . 
						"ORDER BY `".$this->fields["level"]."` ASC";
			}
			else {
				$sql[] = "" . 
					"UPDATE `".$this->table."` SET " . 
						"`".$this->fields["parent_id"]."` = ".$ref_id.", " . 
						"`".$this->fields["position"]."` = ".$position." " . 
					"WHERE " . 
						"`".$this->fields["id"]."` = ".$id;
				$sql[] = "" . 
					"UPDATE `".$this->table."` SET " . 
						"`".$this->fields["left"]."` = `".$this->fields["left"]."` - (".$idif."), " . 
						"`".$this->fields["right"]."` = `".$this->fields["right"]."` - (".$idif."), " . 
						"`".$this->fields["level"]."` = `".$this->fields["level"]."` - (".$ldif.") " . 
					"WHERE " . 
						"`".$this->fields["id"]."` IN (".implode(",", $node_ids).") ";
			}
		}
		else {
			$sql[] = "" . 
				"INSERT INTO `".$this->table."` (" . 
					"`".$this->fields["parent_id"]."`, " . 
					"`".$this->fields["position"]."`, " . 
					"`".$this->fields["left"]."`, " . 
					"`".$this->fields["right"]."`, " . 
					"`".$this->fields["level"]."` " . 
					") " . 
				"VALUES (" . 
					$ref_id.", " . 
					$position.", " . 
					$idif.", " . 
					($idif + 1).", " . 
					$ldif. 
				")";
		}
		
		foreach($sql as $q) {
			$this->db->query($q); 
		}
		$ind = $this->db->insert_id();
		if($is_copy) $this->_fix_copy($ind, $position);
		return $node === false || $is_copy ? $ind : true;
	}
	
	
	
	
	
	
	
	function _fix_copy($id, $position) {
		$node = $this->_get_node($id);
		$children = $this->_get_children($id, true);

		$map = array();
		for($i = $node[$this->fields["left"]] + 1; $i < $node[$this->fields["right"]]; $i++) {
			$map[$i] = $id;
		}
		foreach($children as $cid => $child) {
			if((int)$cid == (int)$id) {
				$this->db->query("UPDATE `".$this->table."` SET `".$this->fields["position"]."` = ".$position." WHERE `".$this->fields["id"]."` = ".$cid);
				continue;
			}
			$this->db->query("UPDATE `".$this->table."` SET `".$this->fields["parent_id"]."` = ".$map[(int)$child[$this->fields["left"]]]." WHERE `".$this->fields["id"]."` = ".$cid);
			for($i = $child[$this->fields["left"]] + 1; $i < $child[$this->fields["right"]]; $i++) {
				$map[$i] = $cid;
			}
		}
	}




	function _reconstruct() {
		$this->db->query("" . 
			"CREATE TEMPORARY TABLE `temp_tree` (" . 
				"`".$this->fields["id"]."` INTEGER NOT NULL, " . 
				"`".$this->fields["parent_id"]."` INTEGER NOT NULL, " . 
				"`". $this->fields["position"]."` INTEGER NOT NULL" . 
			") type=HEAP"
		);
		$this->db->query("" . 
			"INSERT INTO `temp_tree` " . 
				"SELECT " . 
					"`".$this->fields["id"]."`, " . 
					"`".$this->fields["parent_id"]."`, " . 
					"`".$this->fields["position"]."` " . 
				"FROM `".$this->table."`"
		);

		$this->db->query("" . 
			"CREATE TEMPORARY TABLE `temp_stack` (" . 
				"`".$this->fields["id"]."` INTEGER NOT NULL, " . 
				"`".$this->fields["left"]."` INTEGER, " . 
				"`".$this->fields["right"]."` INTEGER, " . 
				"`".$this->fields["level"]."` INTEGER, " . 
				"`stack_top` INTEGER NOT NULL, " . 
				"`".$this->fields["parent_id"]."` INTEGER, " . 
				"`".$this->fields["position"]."` INTEGER " . 
			") type=HEAP"
		);
		$counter = 2;
		$this->db->query("SELECT COUNT(*) FROM temp_tree");
		$this->db->nextr();
		$maxcounter = (int) $this->db->f(0) * 2;
		$currenttop = 1;
		$this->db->query("" . 
			"INSERT INTO `temp_stack` " . 
				"SELECT " . 
					"`".$this->fields["id"]."`, " . 
					"1, " . 
					"NULL, " . 
					"0, " . 
					"1, " . 
					"`".$this->fields["parent_id"]."`, " . 
					"`".$this->fields["position"]."` " . 
				"FROM `temp_tree` " . 
				"WHERE `".$this->fields["parent_id"]."` = 0"
		);
		$this->db->query("DELETE FROM `temp_tree` WHERE `".$this->fields["parent_id"]."` = 0");

		while ($counter <= $maxcounter) {
			$this->db->query("" . 
				"SELECT " . 
					"`temp_tree`.`".$this->fields["id"]."` AS tempmin, " . 
					"`temp_tree`.`".$this->fields["parent_id"]."` AS pid, " . 
					"`temp_tree`.`".$this->fields["position"]."` AS lid " . 
				"FROM `temp_stack`, `temp_tree` " . 
				"WHERE " . 
					"`temp_stack`.`".$this->fields["id"]."` = `temp_tree`.`".$this->fields["parent_id"]."` AND " . 
					"`temp_stack`.`stack_top` = ".$currenttop." " . 
				"ORDER BY `temp_tree`.`".$this->fields["position"]."` ASC LIMIT 1"
			);

			if ($this->db->nextr()) {
				$tmp = $this->db->f("tempmin");

				$q = "INSERT INTO temp_stack (stack_top, `".$this->fields["id"]."`, `".$this->fields["left"]."`, `".$this->fields["right"]."`, `".$this->fields["level"]."`, `".$this->fields["parent_id"]."`, `".$this->fields["position"]."`) VALUES(".($currenttop + 1).", ".$tmp.", ".$counter.", NULL, ".$currenttop.", ".$this->db->f("pid").", ".$this->db->f("lid").")";
				$this->db->query($q);
				$this->db->query("DELETE FROM `temp_tree` WHERE `".$this->fields["id"]."` = ".$tmp);
				$counter++;
				$currenttop++;
			}
			else {
				$this->db->query("" . 
					"UPDATE temp_stack SET " . 
						"`".$this->fields["right"]."` = ".$counter.", " . 
						"`stack_top` = -`stack_top` " . 
					"WHERE `stack_top` = ".$currenttop
				);
				$counter++;
				$currenttop--;
			}
		}

		$temp_fields = $this->fields;
		unset($temp_fields["parent_id"]);
		unset($temp_fields["position"]);
		unset($temp_fields["left"]);
		unset($temp_fields["right"]);
		unset($temp_fields["level"]);
		if(count($temp_fields) > 1) {
			$this->db->query("" . 
				"CREATE TEMPORARY TABLE `temp_tree2` " . 
					"SELECT `".implode("`, `", $temp_fields)."` FROM `".$this->table."` "
			);
		}
		$this->db->query("TRUNCATE TABLE `".$this->table."`");
		$this->db->query("" . 
			"INSERT INTO ".$this->table." (" . 
					"`".$this->fields["id"]."`, " . 
					"`".$this->fields["parent_id"]."`, " . 
					"`".$this->fields["position"]."`, " . 
					"`".$this->fields["left"]."`, " . 
					"`".$this->fields["right"]."`, " . 
					"`".$this->fields["level"]."` " . 
				") " . 
				"SELECT " . 
					"`".$this->fields["id"]."`, " . 
					"`".$this->fields["parent_id"]."`, " . 
					"`".$this->fields["position"]."`, " . 
					"`".$this->fields["left"]."`, " . 
					"`".$this->fields["right"]."`, " . 
					"`".$this->fields["level"]."` " . 
				"FROM temp_stack " . 
				"ORDER BY `".$this->fields["id"]."`"
		);
		if(count($temp_fields) > 1) {
			$sql = "" . 
				"UPDATE `".$this->table."` v, `temp_tree2` SET v.`".$this->fields["id"]."` = v.`".$this->fields["id"]."` ";
			foreach($temp_fields as $k => $v) {
				if($k == "id") continue;
				$sql .= ", v.`".$v."` = `temp_tree2`.`".$v."` ";
			}
			$sql .= " WHERE v.`".$this->fields["id"]."` = `temp_tree2`.`".$this->fields["id"]."` ";
			$this->db->query($sql);
		}
	}







	function _analyze() {
		$report = array();

		$this->db->query("" . 
			"SELECT " . 
				"`".$this->fields["left"]."` FROM `".$this->table."` s " . 
			"WHERE " . 
				"`".$this->fields["parent_id"]."` = 0 "
		);
		$this->db->nextr();
		if($this->db->nf() == 0) {
			$report[] = "[FAIL]\tNo root node.";
		}
		else {
			$report[] = ($this->db->nf() > 1) ? "[FAIL]\tMore than one root node." : "[OK]\tJust one root node.";
		}
		$report[] = ($this->db->f(0) != 1) ? "[FAIL]\tRoot node's left index is not 1." : "[OK]\tRoot node's left index is 1.";

		$this->db->query("" . 
			"SELECT " . 
				"COUNT(*) FROM `".$this->table."` s " . 
			"WHERE " . 
				"`".$this->fields["parent_id"]."` != 0 AND " . 
				"(SELECT COUNT(*) FROM `".$this->table."` WHERE `".$this->fields["id"]."` = s.`".$this->fields["parent_id"]."`) = 0 ");
		$this->db->nextr();
		$report[] = ($this->db->f(0) > 0) ? "[FAIL]\tMissing parents." : "[OK]\tNo missing parents.";

		$this->db->query("SELECT MAX(`".$this->fields["right"]."`) FROM `".$this->table."`");
		$this->db->nextr();
		$n = $this->db->f(0);
		$this->db->query("SELECT COUNT(*) FROM `".$this->table."`");
		$this->db->nextr();
		$c = $this->db->f(0);
		$report[] = ($n/2 != $c) ? "[FAIL]\tRight index does not match node count." : "[OK]\tRight index matches count.";

		$this->db->query("" . 
			"SELECT COUNT(`".$this->fields["id"]."`) FROM `".$this->table."` s " . 
			"WHERE " . 
				"(SELECT COUNT(*) FROM `".$this->table."` WHERE " . 
					"`".$this->fields["right"]."` < s.`".$this->fields["right"]."` AND " . 
					"`".$this->fields["left"]."` > s.`".$this->fields["left"]."` AND " . 
					"`".$this->fields["level"]."` = s.`".$this->fields["level"]."` + 1" . 
				") != " .
				"(SELECT COUNT(*) FROM `".$this->table."` WHERE " . 
					"`".$this->fields["parent_id"]."` = s.`".$this->fields["id"]."`" . 
				") "
			);
		$this->db->nextr();
		$report[] = ($this->db->f(0) > 0) ? "[FAIL]\tAdjacency and nested set do not match." : "[OK]\tNS and AJ match";

		return implode("<br />",$report);
	}








	function _dump($output = false) {
		$nodes = array();
		$this->db->query("SELECT * FROM ".$this->table." ORDER BY `".$this->fields["left"]."`");
		while($this->db->nextr()) $nodes[] = $this->db->get_row("assoc");
		if($output) {
			echo "<pre>";
			foreach($nodes as $node) {
				echo str_repeat("&#160;",(int)$node[$this->fields["level"]] * 2);
				echo $node[$this->fields["id"]]." (".$node[$this->fields["left"]].",".$node[$this->fields["right"]].",".$node[$this->fields["level"]].",".$node[$this->fields["parent_id"]].",".$node[$this->fields["position"]].")<br />";
			}
			echo str_repeat("-",40);
			echo "</pre>";
		}
		return $nodes;
	}
	
	function _have_child_with_title($parent_id, $title) {
		$this->db->query("SELECT `id` FROM ".$this->table." WHERE `".$this->fields["parent_id"]."`=".$parent_id." AND `".$this->fields["title"]."`=\"".$title."\"");
		if($this->db->nextr())
			return "{\"status\":1, \"id\":".$this->db->f("id")."}";
		return 0;
	}
	
	
	
	function _drop() {
		$this->db->query("TRUNCATE TABLE `".$this->table."`");
	}
}







class json_tree extends _tree_struct {
	
	 
	function __construct($table = "tree", $fields = array(), $add_fields = array("title" => "title", "type" => "type")) {
		parent::__construct($table, $fields);
		$this->fields = array_merge($this->fields, $add_fields);
		$this->add_fields = $add_fields;
	}





	function create_node($data) {
		$id = parent::_create((int)$data[$this->fields["id"]], (int)$data[$this->fields["position"]]);
		if($id) {
			$data["id"] = $id;
			$this->set_data($data);
			return  "{ \"status\" : 1, \"id\" : ".(int)$id." }";
		}
		return "{ \"status\" : 0 }";
	}
	
	
	
	
	
	function set_data($data) {
		if(count($this->add_fields) == 0) { return "{ \"status\" : 1 }"; }
		$s = "UPDATE `".$this->table."` SET `".$this->fields["id"]."` = `".$this->fields["id"]."` "; 
		foreach($this->add_fields as $k => $v) {
			if(isset($data[$k]))	$s .= ", `".$this->fields[$v]."` = \"".$this->db->escape($data[$k])."\" ";
			else					$s .= ", `".$this->fields[$v]."` = `".$this->fields[$v]."` ";
		}
		$s .= "WHERE `".$this->fields["id"]."` = ".(int)$data["id"];
		$this->db->query($s);
		return "{ \"status\" : 1 }";
	}
	
	
	
	
	
	function rename_node($data) { return $this->set_data($data); }
	
	
	

	function move_node($data) { 
		return "{ \"status\" : ".parent::_move((int)$data["id"], (int)$data["ref"], (int)$data["position"], (int)$data["copy"])." }";
	}
	
	
	function remove_node($data) {
		$id = parent::_remove((int)$data["id"]);
		return "{ \"status\" : 1 }";
	}
	
	function get_path($data){
		$path = parent::_get_path2((int) $data["id"]);
		$pathstr="";
		if(!empty($path)) {
			$pathstr = implode("/",$path);
			return "{ \"status\" : 1, \"path\":\"".$pathstr."\"}";
		}
		return "{ \"status\" : 0, \"path\":\"none\"}";//, \"data\":".$data["id"]."}";
	}
	
	function addOrUpdateDevice(){
		//construct add device request
		$is4req = array();
		$smapurls = array();
		$aliases = array();
		
		//make sure the uri starts with a "/"
		if(strcmp(substr($_POST["smap_resource"],0,1), "/")!=0){
			$_POST["smap_resource"] = "/".$_POST["smap_resource"];
		}
			
		array_push($smapurls, $_POST["smap_url"].$_POST["smap_resource"]);
		array_push($aliases, $_POST["alias"]);
		$is4req["deviceName"] = $_POST["devName"];
		$is4req["smap_urls"] = $smapurls;
		$is4req["aliases"] = $aliases;

		//send it to is4
		$parentid = $_POST["parent_id"];
		$cdata["id"]=$parentid;
		$nodeTile = $_POST["alias"];
		$path_json =json_decode($this->get_path($cdata), true);
		$str = $path_json["path"];
		$sub1 = "devices";
		$sub2 = "devices/";
		$endsWithDevices_bool = (substr( $str, strlen( $str ) - strlen( $sub1 ) ) === $sub1) | 
							(substr( $str, strlen( $str ) - strlen( $sub2 ) ) === $sub2);

		if($endsWithDevices_bool){
			$is4req_str = json_encode($is4req);
			$thispath_str = $this->get_path($cdata);
			$thispath_json = json_decode($thispath_str, true);
			$url = str_replace("ROOT","http://is4server.com:8080/is4/Cory/lt", $thispath_json["path"]);
			//echo "{\"one\":".$is4req_str.", \"url\":".$url."}";
			$d = array(); $d["id"] = $parentid;
			$isAlreadyChild_ = parent::_have_child_with_title($parentid, $_POST["devName"]);
			$isAlreadyChild_json = json_decode($isAlreadyChild_, true);
			if($isAlreadyChild_json["status"]==0){
				$this->is4GenericPut($is4req_str, $url);
			} else {
				$this->is4GenericPost($is4req_str, $url);
			}
			
			//create the node(s) locally
			if($isAlreadyChild_json["status"]==0){
				$createStat = $this->create_node(array(
									"id" => $parentid,
									"position" => 0,
									"title" => $_POST["devName"],
									"type" => "folder"
								));
				$cstat_json = json_decode($createStat, true); //"{ \"status\" : 1, \"id\" : ".(int)$id." }";
								
				if($cstat_json["status"]==1){
					$devnode_id = $cstat_json["id"];
					$this->create_node(array(
										"id" => $devnode_id,
										"position" => 0,
										"title" => $_POST["alias"],
										"type" => "default"
									));
				}
			} elseif ($isAlreadyChild_json["status"]==1 && $isAlreadyChild_json["id"]>0){
				$this->create_node(array(
								"id" => $isAlreadyChild_json["id"],
								"position" => 0,
								"title" => $_POST["alias"],
								"type" => "default"
							));
			}
			
			$response = array();
			$response["status"] = "success";
			echo json_encode($response);
		} else{
			echo "{\"status\":\"fail\", \"message\":\"The first call failed\"}";
		}
	}
	
	/**
	 *  Creates a publisher for each channel sensor or meter reading that this starred uri resolves to.
	 */
	function addOrUpdateDeviceBulk(){
		//construct add device request
		$is4req = array();
		$smapurls = array();
		//$aliases = array();
		
		if(preg_match('/\*/', $_POST["smap_resource"])){		
			//make sure the uri starts with a "/"
			if(strcmp(substr($_POST["smap_resource"],0,1), "/")!=0){
				$_POST["smap_resource"] = "/".$_POST["smap_resource"];
			}
				
			array_push($smapurls, $_POST["smap_url"].$_POST["smap_resource"]);
			//array_push($aliases, $_POST["alias"]);
			$is4req["deviceName"] = $_POST["devName"];
			$is4req["smap_urls"] = $smapurls;
			//$is4req["aliases"] = $aliases;
	
			//send it to is4
			$parentid = $_POST["parent_id"];
			$cdata["id"]=$parentid;
			//$nodeTile = $_POST["alias"];
			$path_json =json_decode($this->get_path($cdata), true);
			$str = $path_json["path"];
			$sub1 = "devices";
			$sub2 = "devices/";
			$endsWithDevices_bool = (substr( $str, strlen( $str ) - strlen( $sub1 ) ) === $sub1) | 
								(substr( $str, strlen( $str ) - strlen( $sub2 ) ) === $sub2);
	
			if($endsWithDevices_bool){
				$is4req_str = json_encode($is4req);
				$thispath_str = $this->get_path($cdata);
				$thispath_json = json_decode($thispath_str, true);
				$url = str_replace("ROOT","http://is4server.com:8080/is4/Cory/lt", $thispath_json["path"]);
				//echo "{\"one\":".$is4req_str.", \"url\":".$url."}";
				$d = array(); $d["id"] = $parentid;
				$isAlreadyChild_ = parent::_have_child_with_title($parentid, $_POST["devName"]);
				$isAlreadyChild_json = json_decode($isAlreadyChild_, true);

				if($isAlreadyChild_json["status"]==0){
					$this->is4GenericPut($is4req_str, $url);
				} else {
					$this->is4GenericPost($is4req_str, $url);
				}
				
				$device_url = $url."/".$_POST["devName"];
				//get all the newly created resources -- 
				//is4GenericPut/Post don't usually return anything due to a broken pipe issue
				$reply = $this->is4GenericGet($device_url);
				$replyArray = json_decode($reply,true);
				$childrenArray = $replyArray["children"];
				
				/*$allpubids = array();
				for($i=0; $i<count($childrenArray); $i++){
					$thisurl = $device_url."/".$childrenArray[$i];
					$reply = $this->is4GenericGet($thisurl);
					$pubReply_array = json_decode($reply, true);
					$pubid = $pubReply_array["pubid"];
					array_push($allpubids, $pubid);
				}*/


				//////////////////////////////////////////////////////////////////////////////////////////			
				//create the node(s) locally
				if($isAlreadyChild_json["status"]==0){
					$createStat = $this->create_node(array(
										"id" => $parentid,
										"position" => 0,
										"title" => $_POST["devName"],
										"type" => "folder"
									));
					$cstat_json = json_decode($createStat, true); //"{ \"status\" : 1, \"id\" : ".(int)$id." }";
					for($i=0; $i<count($childrenArray); $i++){
						if($cstat_json["status"]==1){
							$devnode_id = $cstat_json["id"];
							$this->create_node(array(
												"id" => $devnode_id,
												"position" => 0,
												"title" => $childrenArray[$i],
												"type" => "default"
											));
						}
					}
				} elseif ($isAlreadyChild_json["status"]==1 && $isAlreadyChild_json["id"]>0){
					$this->create_node(array(
									"id" => $isAlreadyChild_json["id"],
									"position" => 0,
									"title" => $_POST["alias"],
									"type" => "default"
								));
				}
				
				$response = array();
				$response["status"] = "success";
				$response["pubids"] = $allpubids;
				echo json_encode($response);
			} else{
				echo "{\"status\":\"fail\", \"message\":\"The first call failed\"}";
			}
		}
	}
	
	function is4GenericGet($url){
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$url);
		curl_setopt($curl_handle,CURLOPT_HTTPGET,1);
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,15);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$reply = curl_exec($curl_handle);
		curl_close($curl_handle);
		
		return $reply;
	}
	
	function is4Get($data){
		$getpath_res = $this->get_path($data);
		$getpath_result = json_decode($getpath_res);
		if($getpath_result->{'status'}){
			$res = str_replace("ROOT", $data["prefix"], $getpath_result->{'path'});
			$res.="/";
			$curl_handle=curl_init();
			curl_setopt($curl_handle,CURLOPT_URL,$res);
			curl_setopt($curl_handle,CURLOPT_HTTPGET,1);
			curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
			curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
			$reply = curl_exec($curl_handle);
			curl_close($curl_handle);
			
			$reply_array = json_decode($reply, true);

			if(!empty($reply) && strcmp($reply_array["status"], "success")==0){
				$reply_array["status"] =1;
				$reply_array["url"] = $res;
				return json_encode($reply_array);
			} else {
				$rep = array();
				$rep["status"]=0;
				$rep["fetchUrl"] = $res;
				$rep["is4reply"] = json_encode(json_decode($reply, true));
				$val = json_encode($rep);
				return $val;
			}
		}
		return "{ \"status\" : 0 }";
	}
	
	function is4GenericPut($putData_str, $url){
		$putDataFile = tmpfile();
		fwrite($putDataFile, $putData_str);
		fseek($putDataFile, 0);
		
		$curl_handle=curl_init();
		curl_setopt($curl_handle,CURLOPT_URL,$url);
		curl_setopt($curl_handle,CURLOPT_PUT,1);
		curl_setopt($curl_handle,CURLOPT_INFILE, $putDataFile);
		curl_setopt($curl_handle,CURLOPT_INFILESIZE, strlen($putData_str));
		curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
		curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
		$reply = curl_exec($curl_handle);
		curl_close($curl_handle);
		
		return $reply;
	}
	
	function is4GenericPost($postData_str, $url){
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
	
	function is4Put(){
		$data = file_get_contents("php://input");
		$data_json = json_decode($data, true);
		$getpath_res = $this->get_path($data_json);
		$getpath_res_json = json_decode($getpath_res, true);
		if($getpath_res_json["status"]){
			$res = str_replace("ROOT", $data_json["prefix"], $getpath_res_json["path"]);
			$res .= "/";
			$putData = array();
			$putData["type"]="properties";
			$putData["data"]=$data_json["data"];
			$putData_str = json_encode($putData);
			$putDataFile = tmpfile();
			fwrite($putDataFile, $putData_str);
			fseek($putDataFile, 0);
			
			$curl_handle=curl_init();
			curl_setopt($curl_handle,CURLOPT_URL,$res);
			curl_setopt($curl_handle,CURLOPT_PUT,1);
			curl_setopt($curl_handle,CURLOPT_INFILE, $putDataFile);
			curl_setopt($curl_handle,CURLOPT_INFILESIZE, strlen($putData_str));
			curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
			curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
			$reply = curl_exec($curl_handle);
			curl_close($curl_handle);
			//return $reply;

			//assume an empty reply is success (broken pipe error)
			$reply_json = json_decode($reply, true);
			fclose($putDataFile);
			if(empty($reply) || strcmp($reply_json["status"], "success")==0){
				$reply_json["status"]=1;
				return json_encode($reply_json);
			}
		}
		return "{ \"status\" : 0 }";
	}
	
	function is4Post(){
		$data = file_get_contents("php://input");
		$data_json = json_decode($data, true);
		$getpath_res = $this->get_path($data_json);
		if($getpath_res["status"]){
			$res = $data_json["prefix"]."/".$get_path["path"];
			$postData = $data_json["data"];
			$curl_handle=curl_init();
			curl_setopt($curl_handle,CURLOPT_URL,$res);
			curl_setopt($curl_handle,CURLOPT_POST,1);
			curl_setopt($curl_handle, CURLOPT_POSTFIELDS, $postData);
			curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
			curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
			$reply = curl_exec($curl_handle);
			curl_close($curl_handle);

			$reply_json = json_decode($reply, true);
			if(strcmp($reply.chr(0), "{")==0){
				$reply_json["status"]=1;
				return json_encode($reply_json);
			}
		}
		return "{ \"status\" : 0 }";
	}
	
	function is4Delete($data){
		$getpath_res = $this->get_path($data);
		if($getpath_res["status"]){
			$res = $data["prefix"]."/".$get_path["path"];
			$postData = $data["data"];
			
			$curl_handle=curl_init();
			curl_setopt($curl_handle,CURLOPT_URL,$res);
			curl_setopt($curl_handle, CURLOPT_CUSTOMREQUEST, "DELETE");
			curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
			curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
			$reply = curl_exec($curl_handle);
			curl_close($curl_handle);

			if(strcmp($reply.chr(0), "{")==0){
				$reply["status"]=1;
				return $reply;
			} 
		}
		return "{ \"status\" : 0 }";
	}
	
	function is4Sync(){
		$cgraph = $this->exportContextGraph();
		$response = array();
		if(!empty($cgraph)){
			$putData_str = $cgraph;
			$putDataFile = tmpfile();
			fwrite($putDataFile, $putData_str);
			fseek($putDataFile, 0);
			
			$url = "http://184.106.204.181:8080/is4/context/";
			
			$curl_handle=curl_init();
			curl_setopt($curl_handle,CURLOPT_URL,$url);
			curl_setopt($curl_handle,CURLOPT_PUT,1);
			curl_setopt($curl_handle,CURLOPT_INFILE, $putDataFile);
			curl_setopt($curl_handle,CURLOPT_INFILESIZE, strlen($putData_str));
			curl_setopt($curl_handle,CURLOPT_CONNECTTIMEOUT,5);
			curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER,1);
			$reply = curl_exec($curl_handle);
			curl_close($curl_handle);
			return $reply;
		}
	}
	
	
	function genContextGraph(){
		$nodes = array();
		$query_all = "SELECT id, parent_id, title FROM tree";
		$this->db->query($query_all);
		
		$context_graph = array();
		$graph_nodes = array();
		$graph_edges = array();
		
		while($this->db->nextr()){
			$thisnode = array();
			$thisrow = $this->db->get_row("assoc");
			$id = $thisrow["id"];
			$parentid = $thisrow["parent_id"];
			$title = $thisrow["title"];
			
			if(strcmp($title, "ROOT") !== 0){
			
			$parents = array();
				array_push($parents, $parentid);
				
				$thisnode["label"] = $title;
				$thisnode["name"] = $title."_".$id;
				$thisnode["type"] = "context_node";
				$thisnode["\$schema"] = "context_graph_schema.json";
				$thisnode["description"] = "Node in electric load tree";
				$thisnode["AssociatedDevices"] = array();
				$thisnode["parents"] = $parents;
				$thisnode["children"] = array();
				$thisnode["cnid"] = $id;
				
				array_push($graph_nodes, $thisnode);
			}
		}
		
		$context_graph["type"] = "context_graph";
		$context_graph["graph_nodes"] = $graph_nodes;
		$context_graph["graph_edges"] = $graph_edges;
		
		$cgraph_json = json_encode($context_graph);
		$cgraph_file = "exports/cgraph.json";
		$fh = fopen($cgraph_file, 'w');
		fwrite($fh, $cgraph_json);
		fclose($fh);
		return "http://jortiz81.homelinux.com/is4apps/is4m-console/".$cgraph_file;
	}
	
	function exportContextGraph(){
		$nodes = array();
		$query_all = "SELECT id, parent_id, title FROM tree";
		$this->db->query($query_all);
		
		$context_graph = array();
		$graph_nodes = array();
		$graph_edges = array();
		
		while($this->db->nextr()){
			$thisnode = array();
			$thisrow = $this->db->get_row("assoc");
			$id = $thisrow["id"];
			$parentid = $thisrow["parent_id"];
			$title = $thisrow["title"];
			
			if(strcmp($title, "ROOT") !== 0){
			
			$parents = array();
				array_push($parents, $parentid);
				
				$thisnode["label"] = $title;
				$thisnode["name"] = $title."_".$id;
				$thisnode["type"] = "context_node";
				$thisnode["\$schema"] = "context_graph_schema.json";
				$thisnode["description"] = "Node in electric load tree";
				$thisnode["AssociatedDevices"] = array();
				$thisnode["parents"] = $parents;
				$thisnode["children"] = array();
				$thisnode["cnid"] = $id;
				
				array_push($graph_nodes, $thisnode);
			}
		}
		
		$context_graph["type"] = "context_graph";
		$context_graph["graph_nodes"] = $graph_nodes;
		$context_graph["graph_edges"] = $graph_edges;
		
		$cgraph_json = json_encode($context_graph);
		return $cgraph_json;
	}
	
	function importContextGraph(){
		$indata = file_get_contents("php://input");
		$indata_json = json_decode($indata, true);
		$response = array();
		if(!empty($indata_json["type"]) && !empty($indata_json["context_nodes"]) && !empty($indata_json["context_edges"]) &&
				strcasecmp($indata_json["type"], "context_graph")!==0 && 
				strcasecmp(gettype($indata_json["context_nodes"]), "array") !==0 &&
				strcasecmp(gettype($indata_json["context_edges"]), "array") !==0){
			//purge the entire table and re-construct it with the posted data file
			$response["status"]=0;
		} else {
			$response["status"]=1;
		}
		return json_encode($response);
	}
	
	function get_children($data) {
		$tmp = $this->_get_children((int)$data["id"]);
		if((int)$data["id"] === 1 && count($tmp) === 0) {
			$this->_create_default();
			$tmp = $this->_get_children((int)$data["id"]);
		}
		$result = array();
		foreach($tmp as $k => $v) {
			$result[] = array(
				"attr" => array("id" => "node_".$k, "rel" => $v[$this->fields["type"]]),
				"data" => $v[$this->fields["title"]],
				"state" => ($v[$this->fields["right"]] - $v[$this->fields["left"]] > 1) ? "closed" : ""
			);
		}
		return json_encode($result);
	}
	
	
	
	function search($data) {
		$this->db->query("SELECT `".$this->fields["left"]."`, `".$this->fields["right"]."` FROM `".$this->table."` WHERE `".$this->fields["title"]."` LIKE '%".$this->db->escape($data["search_str"])."%'");
		if($this->db->nf() === 0) return "[]";
		$q = "SELECT DISTINCT `".$this->fields["id"]."` FROM `".$this->table."` WHERE 0 ";
		while($this->db->nextr()) {
			$q .= " OR (`".$this->fields["left"]."` < ".(int)$this->db->f(0)." AND `".$this->fields["right"]."` > ".(int)$this->db->f(1).") ";
		}
		$result = array();
		$this->db->query($q);
		while($this->db->nextr()) { $result[] = "#node_".$this->db->f(0); }
		return json_encode($result);
	}





	function _create_default() {
		$this->_drop();
		$this->create_node(array(
			"id" => 0,
			"position" => 0,
			"title" => "ROOT"
		));
		$this->create_node(array(
			"id" => 1,
			"position" => 0,
			"title" => "C:",
			"type" => "drive"
		));
		$this->create_node(array(
			"id" => 1,
			"position" => 1,
			"title" => "D:",
			"type" => "drive"
		));
		$this->create_node(array(
			"id" => 2,
			"position" => 0,
			"title" => "_demo",
			"type" => "folder"
		));
		$this->create_node(array(
			"id" => 2,
			"position" => 1,
			"title" => "_docs",
			"type" => "folder"
		));
		$this->create_node(array(
			"id" => 4,
			"position" => 0,
			"title" => "index.html",
			"type" => "default"
		));
		$this->create_node(array(
			"id" => 5,
			"position" => 1,
			"title" => "doc.html",
			"type" => "default"
		));
	}
}

?>