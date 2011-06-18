/*
 * "Copyright (c) 2010-11 The Regents of the University  of California. 
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Author:  Jorge Ortiz (jortiz@cs.berkeley.edu)
 * IS4 release version 1.0
 */
-- phpMyAdmin SQL Dump
-- version 3.3.2deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jul 25, 2010 at 06:46 AM
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
-- Table structure for table `context_stream`
--

CREATE TABLE IF NOT EXISTS `context_stream` (
  `id` varchar(36) NOT NULL,
  `context_desc` text,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `contextStreamObject` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `DataRepository`
--

CREATE TABLE IF NOT EXISTS `DataRepository` (
  `id` varchar(36) NOT NULL,
  `name` varchar(500) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rawdata` longblob NOT NULL,
  KEY `id` (`id`),
  KEY `name` (`name`),
  KEY `timestamp` (`timestamp`),
  KEY `id_2` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Device`
--

CREATE TABLE IF NOT EXISTS `Device` (
  `id` varchar(36) NOT NULL,
  `Type` varchar(50) NOT NULL,
  `Make` varchar(50) NOT NULL,
  `Model` varchar(50) NOT NULL,
  `Specs` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) NOT NULL,
  `pubtable_id` varchar(24) NOT NULL,
  `rrid` varchar(24) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rrid` (`rrid`),
  KEY `pubtable_id` (`pubtable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `device_context_bindings`
--

CREATE TABLE IF NOT EXISTS `device_context_bindings` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `rrid` int(24) NOT NULL,
  `device_id` int(24) NOT NULL,
  `bind_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `rrid` (`rrid`,`bind_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `FormatRepository`
--

CREATE TABLE IF NOT EXISTS `FormatRepository` (
  `id` varchar(36) NOT NULL,
  `formatUrl` varchar(500) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rawdata` longblob NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`formatUrl`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Formatting`
--

CREATE TABLE IF NOT EXISTS `Formatting` (
  `id` varchar(36) NOT NULL,
  `UnitofMeasure` enum('kW','kWh','kVARh','kVAh','m3','ft3','btu','kpa','lph','gph','PF','kVAR','kVA','A','V','HZ') NOT NULL,
  `Multiplier` int(5) unsigned DEFAULT NULL,
  `Divisor` int(5) unsigned DEFAULT NULL,
  `UnitofTime` enum('microsecond','millisecond','second','minute','hour','day','week','month','year','decade') NOT NULL,
  `MeterType` enum('electric','gas','water','thermal','pressure','heat','cooling') NOT NULL,
  `Version` int(11) NOT NULL,
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Location`
--

CREATE TABLE IF NOT EXISTS `Location` (
  `id` varchar(36) NOT NULL,
  `Origin` varchar(50) DEFAULT NULL,
  `Coordinate` varchar(50) DEFAULT NULL,
  `CoordinateSystem` varchar(50) DEFAULT NULL,
  `CoordinateUnit` varchar(50) DEFAULT NULL,
  `Neighbors` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `logic_stream`
--

CREATE TABLE IF NOT EXISTS `logic_stream` (
  `id` varchar(36) NOT NULL,
  `functions` text,
  `dataschema` text NOT NULL,
  `resource` text,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Meta`
--

CREATE TABLE IF NOT EXISTS `Meta` (
  `id` varchar(36) NOT NULL,
  `Specs` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `MeterReading`
--

CREATE TABLE IF NOT EXISTS `MeterReading` (
  `id` varchar(36) NOT NULL,
  `Reading` int(10) DEFAULT NULL,
  `ReadingSequence` int(10) unsigned DEFAULT NULL,
  `ReadingInterval` int(10) DEFAULT NULL,
  `SummationDelivered` int(10) NOT NULL,
  `SummationReceived` int(10) DEFAULT NULL,
  `SummationInterval` int(10) DEFAULT NULL,
  `PowerFactor` smallint(3) DEFAULT NULL,
  `Max` int(5) DEFAULT NULL,
  `Min` int(5) DEFAULT NULL,
  `RateInstantaneous` int(5) DEFAULT NULL,
  `Version` int(3) NOT NULL DEFAULT '0',
  `ReadingTime` int(10) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `id` (`id`),
  KEY `timestamp` (`timestamp`),
  KEY `ReadingTime` (`ReadingTime`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `object_stream`
--

CREATE TABLE IF NOT EXISTS `object_stream` (
  `id` varchar(36) NOT NULL,
  `device_name` varchar(500) NOT NULL,
  `make` varchar(50) NOT NULL,
  `model` varchar(50) DEFAULT NULL,
  `desc` text NOT NULL,
  `address` varchar(50) DEFAULT NULL,
  `sensors` text,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `device_name` (`device_name`),
  KEY `timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Parameter`
--

CREATE TABLE IF NOT EXISTS `Parameter` (
  `id` varchar(36) NOT NULL,
  `SamplingPeriod` int(5) DEFAULT NULL,
  `UnitofTime` enum('microsecond','millisecond','second','minute','hour','day','week','month','year','decade') NOT NULL,
  `Settings` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Profile`
--

CREATE TABLE IF NOT EXISTS `Profile` (
  `id` varchar(36) NOT NULL,
  `EndTime` int(10) NOT NULL,
  `Status` smallint(3) NOT NULL,
  `IntervalPeriod` smallint(3) NOT NULL,
  `NumberofPeriod` smallint(3) NOT NULL,
  `Intervals` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `EndTime` (`EndTime`,`Status`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `publishers`
--

CREATE TABLE IF NOT EXISTS `publishers` (
  `id` int(24) NOT NULL AUTO_INCREMENT,
  `pubid` varchar(36) NOT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `smap_server` varchar(255) NOT NULL,
  `smap_port` int(10) NOT NULL,
  `smap_uri` varchar(255) CHARACTER SET utf8 NOT NULL,
  `report_uri` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `smap_report_id` varchar(15) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pubid` (`pubid`),
  UNIQUE KEY `smap_url` (`smap_uri`),
  KEY `report_url` (`report_uri`),
  KEY `smap_server` (`smap_server`),
  KEY `alias` (`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `ResourceListing`
--

CREATE TABLE IF NOT EXISTS `ResourceListing` (
  `id` varchar(36) NOT NULL,
  `List` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `rest_resources`
--

CREATE TABLE IF NOT EXISTS `rest_resources` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `path` varchar(700) NOT NULL,
  `properties` longblob,
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `path` (`path`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2 ;

-- --------------------------------------------------------

--
-- Table structure for table `UnitLabels`
--

CREATE TABLE IF NOT EXISTS `UnitLabels` (
  `Label` varchar(5) NOT NULL,
  `description` varchar(20) NOT NULL,
  PRIMARY KEY (`Label`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
