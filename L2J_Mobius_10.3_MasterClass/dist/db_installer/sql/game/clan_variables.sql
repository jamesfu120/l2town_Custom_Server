DROP TABLE IF EXISTS `clan_variables`;
CREATE TABLE IF NOT EXISTS `clan_variables` (
  `clanId` int(10) UNSIGNED NOT NULL,
  `var` varchar(191) NOT NULL,
  `val` text NOT NULL,
  PRIMARY KEY (`clanId`, `var`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
