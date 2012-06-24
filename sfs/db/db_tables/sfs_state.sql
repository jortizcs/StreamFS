SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;


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
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) NOT NULL,
  `pubtable_id` varchar(24) DEFAULT NULL,
  `rrid` varchar(24) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rrid` (`rrid`),
  KEY `pubtable_id` (`pubtable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

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
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=571 ;

CREATE TABLE IF NOT EXISTS `rest_resources` (
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
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1263 ;

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

CREATE TABLE IF NOT EXISTS `symlinks` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `symlink_uri` varchar(255) NOT NULL,
  `links_to` varchar(255) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `symlink_uri` (`symlink_uri`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=117 ;

CREATE TABLE IF NOT EXISTS `user_privileges` (
  `id` int(10) NOT NULL,
  `username` varchar(50) NOT NULL,
  `sid` varchar(36) NOT NULL,
  `path` varchar(260) NOT NULL,
  `command` enum('GET','PUT','POST','DELETE') NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
