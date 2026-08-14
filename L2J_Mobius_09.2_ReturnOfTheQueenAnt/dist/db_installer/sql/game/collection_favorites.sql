DROP TABLE IF EXISTS `collection_favorites`;
CREATE TABLE IF NOT EXISTS `collection_favorites` (
  `accountName` VARCHAR(45) NOT NULL DEFAULT '',
  `collectionId` int(3) UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`accountName`,`collectionId`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;