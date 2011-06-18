<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
Design by Free CSS Templates
http://www.freecsstemplates.org
Released for free under a Creative Commons Attribution 2.5 License
-->
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title>IS4 Console -- Registration Confirmation </title>
<script type="text/javascript" src="../is4m-console/lib/flot/jquery.js"></script>
<script type="text/javascript" src="../is4m-console/lib/flot/jquery.flot.js"></script>
<script id="source" type="text/javascript" src="grapher.js"></script>
<script id="source" type="text/javascript" src="register_validation.js"></script>
<script type="text/javascript">
</script>
<meta name="keywords" content="" />
<meta name="description" content="" />
<link href="default.css" rel="stylesheet" type="text/css" />
<style type="text/css">
<!--
#header #sn {
	font-family: Verdana, Geneva, sans-serif;
}
-->
</style>
</head>
<body>
<div id="header">
	<div id="sn">
	  <strong>IS4 Console</strong><br  />
	  Integrated Sensor-Stream <br  />Storage System
  </div>
	<div id="menu">
		<ul>
			<li class="first"><a href="joinsetup.html">Register</a></li>
			<li><a href="#" accesskey="2" title="">Link2</a></li>
			<li><a href="#" accesskey="3" title="">Link3</a></li>
			<li><a href="#" accesskey="4" title="">Link4</a></li>
			<li><a href="index.html">Stream Plotter</a></li>
	  </ul>
  </div>
</div>
<div id="stream_choices">
	<center>
  </center>

  <form action="" method="get">
    <center>
    </center>
  </form>
</div>
<hr width="700" />
<center>
<div id="content"></div>
</center>
<div id="feature">
  <p>Registration Confirmed!</p>
Your can now start publishing your data to IS4. Your publisher id is <?php 
	$pubid=$_GET["PubID"];
	echo "<strong>".$pubid."</strong>" 
	?>.  An example of how your 
    publisher should publish it's data can be seen below:  <br /><br />
  <p>
    <textarea name="publish_example" id="publish_example" cols="80" rows="8"><?php 
	$pubid=$_GET["PubID"];
	echo "{\n\t\"name\":\"data_stream\",\n\t\"PubID\":".$pubid.",\n\t\"Data\":\n\t{\n\t\t\"val\":75\n\t}\n}";
	?>
    </textarea>
  </p>
Remember<strong> <em>&quot;name&quot;:&quot;data_stream&quot;</em></strong> and <strong><em>&quot;PubID&quot; </em></strong>must be included or the published data will not be recorded. Also notice that the &quot;Data&quot; field need only be a data object. </div>
<hr width="750" />
<div id="footer_">
	<center>
	<div id="local_subdiv">
	<table width="719" height="54" border="0" align="center">
	  <tr>
      <td width="20"><a href="http://local.cs.berkeley.edu"><img src="images/local-logo.png" width="100" height="45" alt="local" border="0"/></a></td>
	    <td width="596"> <a href="http://smote.cs.berkeley.edu:8000/tracenv/wiki/is4">IS4 Information</a> | app by <a href="http://www.eecs.berkeley.edu/~jortiz">jortiz</a><br/>
        Research presented are partially based upon work supported by the National Science Foundation under grants #0435454 and #0454432. Any opinions, findings, and conclusions or recommendations expressed in this material are those of the author(s) and do not necessarily reflect the views of the National Science Foundation. </td>
      </tr>
	  </table>
    </div>
    </center>
</div>
</body>
</html>
