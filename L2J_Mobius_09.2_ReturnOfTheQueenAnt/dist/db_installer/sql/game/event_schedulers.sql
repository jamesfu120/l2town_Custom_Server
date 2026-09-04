DROP TABLE IF EXISTS `event_schedulers`;
CREATE TABLE IF NOT EXISTS `event_schedulers` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `eventName` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `schedulerName` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lastRun` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `eventName_schedulerName` (`eventName`,`schedulerName`) USING BTREE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;