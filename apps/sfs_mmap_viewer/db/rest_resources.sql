-- phpMyAdmin SQL Dump
-- version 3.3.2deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jul 22, 2010 at 06:59 PM
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
-- Table structure for table `rest_resources`
--

CREATE TABLE IF NOT EXISTS `rest_resources` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `path` varchar(700) NOT NULL,
  `properties` longblob,
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `path` (`path`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=308 ;

--
-- Dumping data for table `rest_resources`
--

INSERT INTO `rest_resources` (`id`, `path`, `properties`, `creation_time`) VALUES
(151, '/is4/context/', NULL, '2010-06-25 21:23:40'),
(153, '/is4/Cory/lt/Campus/xform-west/panel-msb0/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f78666f726d2d776573742f70616e656c2d6d7362302f70726f70657274696573227d, '2010-06-25 21:26:08'),
(154, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-05/', NULL, '2010-06-25 21:26:08'),
(155, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-14/', NULL, '2010-06-25 21:26:08'),
(156, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-02/', NULL, '2010-06-25 21:26:08'),
(157, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-11/', NULL, '2010-06-25 21:26:08'),
(158, '/is4/Cory/lt/Campus/xform-east/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f78666f726d2d656173742f227d, '2010-06-25 21:26:08'),
(159, '/is4/Cory/lt/Campus/xform-west/panel-msb0/main/', NULL, '2010-06-25 21:26:08'),
(160, '/is4/Cory/lt/Campus/xform-west/', NULL, '2010-06-25 21:26:08'),
(161, '/is4/Cory/lt/Campus/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a2243616d70757320656c656d656e74227d, '2010-06-25 21:26:08'),
(162, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-03/', NULL, '2010-06-25 21:26:08'),
(163, '/is4/Cory/lt/Campus/xform-west/panel-msb0/', NULL, '2010-06-25 21:26:08'),
(164, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-08/', NULL, '2010-06-25 21:26:09'),
(165, '/is4/Cory/lt/Campus/branch/CB-02/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f63622d30322f70726f706572746965732f227d, '2010-06-25 21:26:09'),
(166, '/is4/Cory/lt/Campus/branch/CB-01/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a22636972637569742070726f7073227d, '2010-06-25 21:26:09'),
(167, '/is4/Cory/lt/Campus/branch/CB-02/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a2274657374227d, '2010-06-25 21:26:09'),
(168, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/', NULL, '2010-06-25 21:26:09'),
(169, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-01/properties/', NULL, '2010-06-25 21:26:09'),
(170, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/', NULL, '2010-06-25 21:26:09'),
(171, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-02/properties/', NULL, '2010-06-25 21:26:09'),
(172, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-06/', NULL, '2010-06-25 21:26:09'),
(173, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-15/', NULL, '2010-06-25 21:26:09'),
(174, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-03/properties/', NULL, '2010-06-25 21:26:09'),
(175, '/is4/Cory/lt/Campus/main/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f6d61696e227d, '2010-06-25 21:26:09'),
(176, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-12/', NULL, '2010-06-25 21:26:09'),
(177, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-04/properties/', NULL, '2010-06-25 21:26:09'),
(178, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-05/properties/', NULL, '2010-06-25 21:26:09'),
(179, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-04/', NULL, '2010-06-25 21:26:09'),
(180, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-13/', NULL, '2010-06-25 21:26:09'),
(181, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-09/', NULL, '2010-06-25 21:26:09'),
(182, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-10/', NULL, '2010-06-25 21:26:09'),
(183, '/is4/Cory/lt/Campus/branch/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a224272616e636820706172616d657465727320736574227d, '2010-06-25 21:26:09'),
(184, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-01/', NULL, '2010-06-25 21:26:09'),
(185, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/13_panels/', NULL, '2010-06-25 21:26:09'),
(186, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-06/properties/', NULL, '2010-06-25 21:26:09'),
(187, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-07/properties/', NULL, '2010-06-25 21:26:09'),
(188, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-08/properties/', NULL, '2010-06-25 21:26:09'),
(189, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-09/properties/', NULL, '2010-06-25 21:26:09'),
(190, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-07/', NULL, '2010-06-25 21:26:09'),
(191, '/is4/Cory/lt/Campus/branch/CB-01/', NULL, '2010-06-25 21:26:09'),
(192, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/13_panels/properties/', NULL, '2010-06-25 21:26:09'),
(193, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/Lighting-01/', NULL, '2010-06-25 21:26:09'),
(194, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-15/properties/', NULL, '2010-06-25 21:26:09'),
(195, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/Lighting-01/properties/', NULL, '2010-06-25 21:26:10'),
(196, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-14/properties/', NULL, '2010-06-25 21:26:10'),
(197, '/is4/Cory/lt/Campus/xform-west/panel-msb0/main/properties/', NULL, '2010-06-25 21:26:10'),
(198, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-13/properties/', NULL, '2010-06-25 21:26:10'),
(199, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/properties/', NULL, '2010-06-25 21:26:10'),
(200, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-12/properties/', NULL, '2010-06-25 21:26:10'),
(201, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-11/properties/', NULL, '2010-06-25 21:26:10'),
(202, '/is4/Cory/lt/Campus/main/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f6d61696e2f70726f706572746965732f5c725c6e416d7065726167653a5c725c6e566f6c746167653a5c725c6e50686173653a20332d70686173655c725c6e436f6e6e656374696f6e3a2064656c7461227d, '2010-06-25 21:26:10'),
(203, '/is4/Cory/lt/Campus/xform-east/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f78666f726d2d656173742f70726f706572746965732f227d, '2010-06-25 21:26:10'),
(204, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/CB-10/properties/', NULL, '2010-06-25 21:26:10'),
(205, '/is4/Cory/lt/Campus/xform-west/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a22766f6c74733d3430302c2070686173653d3350227d, '2010-06-25 21:26:10'),
(206, '/is4/Cory/lt/Campus/branch/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f43616d7075732f6272616e63682f70726f70657274696573227d, '2010-06-29 21:27:54'),
(207, '/is4/Cory/lt/Campus/xform-west/panel-msb0/branch/properties/', 0x7b2274797065223a2270726f70657274696573222c2264617461223a222f63616d7075732f78666f726d2d776573742f70616e656c2d6d7362302f6272616e63682f70726f70657274696573227d, '2010-06-29 21:38:19'),
(208, '/is4/Cory/lt/Campus/main/devices/', NULL, '2010-07-09 16:41:28'),
(209, '/is4/Cory/lt/Campus/xform-west/main/', NULL, '2010-07-09 17:18:11'),
(210, '/is4/Cory/lt/Campus/xform-west/main/devices/', NULL, '2010-07-09 17:18:11'),
(211, '/is4/Cory/lt/Campus/xform-west/panel-msb0/main/devices/', NULL, '2010-07-09 17:26:20'),
(212, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/main/', NULL, '2010-07-09 17:27:04'),
(213, '/is4/Cory/lt/Campus/xform-west/panel-msb0/xform-01/main/devices/', NULL, '2010-07-09 17:27:23');
