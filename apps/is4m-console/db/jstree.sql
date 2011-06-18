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
) ENGINE=MyISAM AUTO_INCREMENT=183 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tree`
--

LOCK TABLES `tree` WRITE;
/*!40000 ALTER TABLE `tree` DISABLE KEYS */;
INSERT INTO `tree` VALUES (1,0,26,1,124,0,'ROOT',''),(2,1,0,2,123,1,'Campus','drive'),(5,2,0,3,8,2,'main','folder'),(14,2,1,9,20,2,'branch','folder'),(42,2,2,21,118,2,'xform-west','folder'),(43,2,3,119,122,2,'xform-east','folder'),(44,42,1,26,115,3,'panel-msb0','folder'),(49,44,0,27,32,4,'main','folder'),(50,44,1,33,96,4,'branch','folder'),(56,50,0,34,37,5,'CB-01','folder'),(57,44,2,97,112,4,'xform-01','folder'),(59,50,1,38,41,5,'CB-02','folder'),(60,50,2,42,45,5,'CB-03','folder'),(61,50,3,46,49,5,'CB-04','folder'),(62,50,4,50,53,5,'CB-05','folder'),(63,50,5,54,57,5,'CB-06','folder'),(64,50,6,58,61,5,'CB-07','folder'),(65,50,7,62,65,5,'CB-08','folder'),(66,50,8,66,69,5,'CB-09','folder'),(67,50,9,70,73,5,'CB-10','folder'),(68,50,10,74,77,5,'CB-11','folder'),(69,50,11,78,81,5,'CB-12','folder'),(70,50,12,82,85,5,'CB-13','folder'),(71,50,13,86,89,5,'CB-14','folder'),(72,50,14,90,93,5,'CB-15','folder'),(82,66,0,67,68,6,'properties','folder'),(85,65,0,63,64,6,'properties','folder'),(87,64,0,59,60,6,'properties','folder'),(89,63,0,55,56,6,'properties','folder'),(91,62,0,51,52,6,'properties','folder'),(93,61,0,47,48,6,'properties','folder'),(95,60,0,43,44,6,'properties','folder'),(97,59,0,39,40,6,'properties','folder'),(99,56,0,35,36,6,'properties','folder'),(101,67,0,71,72,6,'properties','folder'),(103,68,0,75,76,6,'properties','folder'),(104,69,0,79,80,6,'properties','folder'),(105,70,0,83,84,6,'properties','folder'),(106,71,0,87,88,6,'properties','folder'),(107,72,0,91,92,6,'properties','folder'),(116,57,1,102,105,5,'Lighting-01','folder'),(117,57,2,106,109,5,'13_panels','folder'),(119,14,0,10,13,3,'CB-01','folder'),(120,14,1,14,17,3,'CB-02','folder'),(122,119,0,11,12,4,'properties','folder'),(123,120,0,15,16,4,'properties','folder'),(130,44,3,113,114,4,'properties','folder'),(132,42,2,116,117,3,'properties','folder'),(133,43,0,120,121,3,'properties','folder'),(134,5,0,4,5,3,'properties','folder'),(136,57,3,110,111,5,'properties','folder'),(137,49,0,28,29,5,'properties','folder'),(138,116,0,103,104,6,'properties','folder'),(139,117,0,107,108,6,'properties','folder'),(140,14,2,18,19,3,'properties','folder'),(141,50,15,94,95,5,'properties','folder'),(143,42,0,22,25,3,'main','folder'),(144,143,0,23,24,4,'devices','folder'),(145,49,1,30,31,5,'devices','folder'),(146,57,0,98,101,5,'main','folder'),(147,146,0,99,100,6,'devices','folder'),(170,5,1,6,7,3,'devices','folder');
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

-- Dump completed on 2010-07-22 12:04:30
