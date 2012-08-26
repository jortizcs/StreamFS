-- MySQL dump 10.13  Distrib 5.1.63, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: sfs_state
-- ------------------------------------------------------
-- Server version	5.1.63-0ubuntu0.11.04.1

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
-- Table structure for table `bulk_reports`
--

DROP TABLE IF EXISTS `bulk_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bulk_reports` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `smap_server` varchar(255) NOT NULL,
  `smap_port` int(10) NOT NULL,
  `smap_uri` varchar(255) NOT NULL,
  `smap_report_id` varchar(15) NOT NULL,
  `muxReportEx` longblob,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `smap_report_id` (`smap_report_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `devices`
--

DROP TABLE IF EXISTS `devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) NOT NULL,
  `pubtable_id` varchar(24) DEFAULT NULL,
  `rrid` varchar(24) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rrid` (`rrid`),
  KEY `pubtable_id` (`pubtable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_privileges`
--

DROP TABLE IF EXISTS `group_privileges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group_privileges` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `groupname` varchar(50) NOT NULL,
  `gid` varchar(36) NOT NULL,
  `sid` varchar(36) NOT NULL,
  `path` varchar(260) DEFAULT NULL,
  `command` enum('GET','PUT','POST','DELETE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `groupname` (`groupname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishers`
--

DROP TABLE IF EXISTS `publishers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishers` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `pubid` varchar(36) NOT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `last_recv_time` int(10) NOT NULL DEFAULT '0' COMMENT 'Timestamp when last data item was received for this publisher.',
  `smap_server` varchar(255) DEFAULT NULL,
  `smap_port` int(10) DEFAULT NULL,
  `smap_uri` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `report_uri` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `smap_report_id` varchar(15) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pubid` (`pubid`),
  UNIQUE KEY `report_uri` (`report_uri`),
  KEY `smap_server` (`smap_server`),
  KEY `alias` (`alias`),
  KEY `smap_uri` (`smap_uri`)
) ENGINE=InnoDB AUTO_INCREMENT=694 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rest_resources`
--

DROP TABLE IF EXISTS `rest_resources`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rest_resources` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `path` varchar(700) NOT NULL,
  `properties` longblob,
  `last_props_update_time` int(10) NOT NULL DEFAULT '0' COMMENT 'The last time this properties field was updated.',
  `last_model_update_time` int(10) NOT NULL DEFAULT '0',
  `type` enum('default','generic_publisher','publisher','devices','device','subscription','symlink','model','process','process_code') NOT NULL DEFAULT 'default',
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `path` (`path`),
  KEY `type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=1920 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subscriptions`
--

DROP TABLE IF EXISTS `subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subscriptions` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `subid` varchar(36) NOT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `dest_url` varchar(255) DEFAULT NULL,
  `dest_uri` varchar(255) DEFAULT NULL,
  `src_pubid` varchar(36) NOT NULL,
  `wildcardPath` varchar(255) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `procsvr_name` varchar(255) NOT NULL DEFAULT '',
  `procsvr_host` varchar(255) NOT NULL DEFAULT '',
  `procsvr_port` int(5) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`id`),
  KEY `subid` (`subid`),
  KEY `src_pubid` (`src_pubid`)
) ENGINE=MyISAM AUTO_INCREMENT=276 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `symlinks`
--

DROP TABLE IF EXISTS `symlinks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `symlinks` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `symlink_uri` varchar(255) NOT NULL,
  `links_to` varchar(255) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `symlink_uri` (`symlink_uri`)
) ENGINE=MyISAM AUTO_INCREMENT=289 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_privileges`
--

DROP TABLE IF EXISTS `user_privileges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_privileges` (
  `id` int(10) NOT NULL,
  `username` varchar(50) NOT NULL,
  `sid` varchar(36) NOT NULL,
  `path` varchar(260) NOT NULL,
  `command` enum('GET','PUT','POST','DELETE') NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-08-22  3:37:30
