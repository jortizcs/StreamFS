-- MySQL dump 10.13  Distrib 5.1.41, for debian-linux-gnu (i486)
--
-- Host: localhost    Database: jstree
-- ------------------------------------------------------
-- Server version	5.1.41-3ubuntu12.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `tree`
--

DROP TABLE IF EXISTS `tree`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tree` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) unsigned NOT NULL,
  `position` bigint(20) unsigned NOT NULL,
  `left` bigint(20) unsigned NOT NULL,
  `right` bigint(20) unsigned NOT NULL,
  `level` bigint(20) unsigned NOT NULL,
  `title` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=238 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tree`
--

LOCK TABLES `tree` WRITE;
/*!40000 ALTER TABLE `tree` DISABLE KEYS */;
INSERT INTO `tree` VALUES (1,0,25,1,136,0,'ROOT',''),(2,1,0,2,135,1,'Main','drive'),(171,2,0,3,6,2,'SB1-LX','folder'),(172,2,1,7,18,2,'SB2-3PW/BP','folder'),(173,2,2,19,22,2,'SB3-r173','folder'),(174,2,3,23,34,2,'SB4-MCL','folder'),(175,2,4,35,44,2,'SB5-4PE','folder'),(176,2,5,45,50,2,'SB6-WPR','folder'),(177,2,6,51,60,2,'SB7-GPW','folder'),(178,2,7,61,72,2,'SB8-BG3','folder'),(179,2,8,73,96,2,'SB9-EPR','folder'),(180,2,9,97,124,2,'SB10-5DPA','folder'),(181,2,10,125,126,2,'SB11-Park','folder'),(182,2,11,127,132,2,'SB13-BG2','folder'),(183,2,12,133,134,2,'SB14-5DBP','folder'),(184,172,0,8,9,3,'Basement_SF_EF','folder'),(185,172,1,10,17,3,'3PW-408','folder'),(186,185,0,11,12,4,'AHU-1','folder'),(187,185,1,13,14,4,'AHU-2','folder'),(188,185,2,15,16,4,'4th_SW-Receptical_Panel','folder'),(189,173,0,20,21,3,'1st_floor_east_labs_480-208','folder'),(190,174,0,24,33,3,'MCL','folder'),(191,190,0,25,26,4,'MCL_East_Chiller_54','folder'),(192,190,1,27,28,4,'MCL_West_Chiller_55','folder'),(193,190,2,29,30,4,'MCL_Hood_fan_73','folder'),(194,190,3,31,32,4,'MCL_Supply_Fan','folder'),(195,175,0,36,43,3,'4PE-432','folder'),(196,195,0,37,38,4,'4LE1','folder'),(197,195,1,39,40,4,'4LN','folder'),(198,195,2,41,42,4,'Bus_Duct','folder'),(199,176,0,46,47,3,'1PW-206','folder'),(200,176,1,48,49,3,'2PW-306','folder'),(201,177,0,52,59,3,'GPW-106','folder'),(202,201,0,53,54,4,'AC108','folder'),(203,201,1,55,56,4,'AC109','folder'),(204,201,2,57,58,4,'panel1-cm','folder'),(205,178,0,62,71,3,'5DPC','folder'),(206,205,0,63,64,4,'AH3-DOP','folder'),(207,205,1,65,66,4,'WP_47','folder'),(209,205,2,67,68,4,'DOP-ltx','folder'),(210,205,3,69,70,4,'WP_46','folder'),(211,179,0,74,77,3,'GPE-158','folder'),(212,179,1,78,79,3,'2PE-358','folder'),(213,179,2,80,81,3,'Splice-458','folder'),(214,179,3,82,83,3,'P432C','folder'),(215,179,4,84,95,3,'P5PA','folder'),(216,211,0,75,76,4,'Comp-PDU','folder'),(217,215,0,85,86,4,'AC48','folder'),(218,215,1,87,88,4,'WP_49','folder'),(219,215,2,89,90,4,'WP_53','folder'),(220,215,3,91,92,4,'WP_76','folder'),(221,215,4,93,94,4,'EF','folder'),(222,180,0,98,101,3,'4LB','folder'),(223,222,0,99,100,4,'New node','folder'),(224,180,1,102,103,3,'5LA','folder'),(225,180,2,104,105,3,'5LB','folder'),(226,180,3,106,107,3,'5LC','folder'),(227,180,4,108,109,3,'5LD','folder'),(228,180,5,110,111,3,'5LE','folder'),(229,180,6,112,113,3,'5LF','folder'),(230,180,7,114,115,3,'5LG','folder'),(231,180,8,116,117,3,'5HA','folder'),(232,180,9,118,119,3,'5HB','folder'),(233,180,10,120,121,3,'5HC','folder'),(234,180,11,122,123,3,'JB','folder'),(235,182,0,128,131,3,'P97','folder'),(236,235,0,129,130,4,'AC199','folder'),(237,171,0,4,5,3,'devices','folder');
/*!40000 ALTER TABLE `tree` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-07-22 14:58:28
