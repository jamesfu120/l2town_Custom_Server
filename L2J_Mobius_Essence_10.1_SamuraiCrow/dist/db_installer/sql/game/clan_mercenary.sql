SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `clan_mercenary`;
CREATE TABLE `clan_mercenary`  (
  `player_id` int(11) NOT NULL DEFAULT 0,
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `player_name` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `classid` tinyint(3) UNSIGNED NOT NULL,
  PRIMARY KEY (`player_id`) USING BTREE
) CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
