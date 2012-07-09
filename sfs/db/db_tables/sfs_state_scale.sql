-- phpMyAdmin SQL Dump
-- version 3.3.2deb1ubuntu1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jul 10, 2012 at 05:44 AM
-- Server version: 5.1.63
-- PHP Version: 5.3.2-1ubuntu4.15

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `sfs_state_scale`
--

-- --------------------------------------------------------

--
-- Table structure for table `groups`
--

CREATE TABLE IF NOT EXISTS `groups` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `groupname` varchar(25) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `groupname` (`groupname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `group_privileges`
--

CREATE TABLE IF NOT EXISTS `group_privileges` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `groupname` varchar(50) NOT NULL,
  `gid` varchar(36) NOT NULL,
  `sid` varchar(36) NOT NULL,
  `path` varchar(260) DEFAULT NULL,
  `command` enum('GET','PUT','POST','DELETE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `groupname` (`groupname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `paths`
--

CREATE TABLE IF NOT EXISTS `paths` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `oid` varchar(36) NOT NULL,
  `path` varchar(700) NOT NULL,
  `properties` longblob,
  `last_props_update_time` int(10) NOT NULL DEFAULT '0' COMMENT 'The last time this properties field was updated.',
  `last_recv_time` int(10) NOT NULL DEFAULT '0',
  `type` enum('default','generic_publisher','subscription','symlink','model','process','process_code') NOT NULL DEFAULT 'default',
  `links_to` varchar(255) DEFAULT NULL,
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `path` (`path`),
  KEY `type` (`type`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1263 ;

-- --------------------------------------------------------

--
-- Table structure for table `subscriptions`
--

CREATE TABLE IF NOT EXISTS `subscriptions` (
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
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=195 ;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `username` varchar(25) NOT NULL,
  `password` varchar(200) NOT NULL,
  `email` varchar(255) NOT NULL,
  `sid` int(200) DEFAULT NULL,
  `gid` int(10) DEFAULT NULL,
  `is_gid_primary` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `email` (`email`,`sid`),
  KEY `group` (`gid`),
  KEY `is_gid_primary` (`is_gid_primary`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COMMENT='Users table' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `user_privileges`
--

CREATE TABLE IF NOT EXISTS `user_privileges` (
  `id` int(10) NOT NULL,
  `username` varchar(50) NOT NULL,
  `sid` varchar(36) NOT NULL,
  `path` varchar(260) NOT NULL,
  `command` enum('GET','PUT','POST','DELETE') NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
