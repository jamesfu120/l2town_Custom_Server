DROP TABLE IF EXISTS `party_matching_history`;
CREATE TABLE `party_matching_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(21) DEFAULT NULL,
  `leader` VARCHAR(35) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;