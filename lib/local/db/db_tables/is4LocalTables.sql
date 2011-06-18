-- phpMyAdmin SQL Dump
-- version 3.3.2deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Dec 15, 2010 at 09:10 PM
-- Server version: 5.1.41
-- PHP Version: 5.3.2-1ubuntu4.2

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `jortiz`
--

-- --------------------------------------------------------

--
-- Table structure for table `bulk_reports`
--

CREATE TABLE IF NOT EXISTS `bulk_reports` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `smap_server` varchar(255) NOT NULL,
  `smap_port` int(10) NOT NULL,
  `smap_uri` varchar(255) NOT NULL,
  `smap_report_id` varchar(15) NOT NULL,
  `muxReportEx` longblob,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `smap_report_id` (`smap_report_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=390 ;

-- --------------------------------------------------------

--
-- Table structure for table `contextGraphs`
--

CREATE TABLE IF NOT EXISTS `contextGraphs` (
  `id` varchar(8) NOT NULL,
  `graph` longblob NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `contextNode`
--

CREATE TABLE IF NOT EXISTS `contextNode` (
  `cid` varchar(25) NOT NULL,
  `type` varchar(50) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `body` blob NOT NULL,
  PRIMARY KEY (`cid`),
  KEY `timestamp` (`timestamp`),
  KEY `type` (`type`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) NOT NULL,
  `pubtable_id` varchar(24) DEFAULT NULL,
  `rrid` varchar(24) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rrid` (`rrid`),
  KEY `pubtable_id` (`pubtable_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=13710 ;

-- --------------------------------------------------------

--
-- Table structure for table `publishers`
--

CREATE TABLE IF NOT EXISTS `publishers` (
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
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=13317 ;

-- --------------------------------------------------------

--
-- Table structure for table `rest_resources`
--

CREATE TABLE IF NOT EXISTS `rest_resources` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `path` varchar(700) NOT NULL,
  `properties` longblob,
  `last_props_update_time` int(10) NOT NULL DEFAULT '0' COMMENT 'The last time this properties field was updated.',
  `type` enum('default','generic_publisher','publisher','building','nsroot','panel','load','suite','room','floor','area','hvac_wet','hvac_dry','ct_src','ct_ret','pump_src','pump_ret','chiller_src','chiller_ret','ahu_sup','ahu_ex','fan_sup','fan_ex','heater','ac','devices','device','subscription','symlink') NOT NULL DEFAULT 'default',
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `path` (`path`),
  KEY `type` (`type`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=15184 ;

-- --------------------------------------------------------

--
-- Table structure for table `subscribers`
--

CREATE TABLE IF NOT EXISTS `subscribers` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `subid` varchar(36) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `uri` varchar(255) NOT NULL,
  `dest_url` varchar(255) NOT NULL,
  `proxy` tinyint(1) NOT NULL,
  `src_pubid` varchar(36) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `subid` (`subid`),
  KEY `src_pubid` (`src_pubid`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `symlinks`
--

CREATE TABLE IF NOT EXISTS `symlinks` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `symlink_uri` varchar(255) NOT NULL,
  `links_to` varchar(255) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `symlink_uri` (`symlink_uri`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=139 ;
