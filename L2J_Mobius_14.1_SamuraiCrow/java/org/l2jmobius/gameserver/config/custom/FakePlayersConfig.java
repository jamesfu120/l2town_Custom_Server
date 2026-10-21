/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the custom fake player related configurations.
 * @author Mobius
 */
public class FakePlayersConfig
{
	// File
	private static final String FAKE_PLAYERS_CONFIG_FILE = "./config/Custom/FakePlayers.ini";
	
	// Constants
	public static boolean FAKE_PLAYERS_ENABLED;
	public static boolean FAKE_PLAYER_CHAT;
	public static boolean FAKE_PLAYER_USE_SHOTS;
	public static boolean FAKE_PLAYER_KILL_PVP;
	public static boolean FAKE_PLAYER_KILL_KARMA;
	public static boolean FAKE_PLAYER_AUTO_ATTACKABLE;
	public static boolean FAKE_PLAYER_AGGRO_MONSTERS;
	public static boolean FAKE_PLAYER_AGGRO_PLAYERS;
	public static boolean FAKE_PLAYER_AGGRO_FPC;
	public static boolean FAKE_PLAYER_CAN_DROP_ITEMS;
	public static boolean FAKE_PLAYER_CAN_PICKUP;
	
	// ==================== 🌟 以下為論壇版超級 AI 擴充變數 ====================
	public static int FAKE_PLAYER_TOTAL_COUNT;
	public static int FAKE_PLAYER_SHOP_COUNT;
	public static int FAKE_PLAYER_COMBAT_COUNT;
	public static int FAKE_PLAYER_MIN_LEVEL;
	public static int FAKE_PLAYER_MAX_LEVEL;
	public static int FAKE_PLAYER_HUNT_RANGE;
	public static int FAKE_PLAYER_HUNT_SEARCH_RANGE;
	public static int FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF;
	public static int FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF;
	public static int FAKE_PLAYER_HUNT_INTERVAL;
	public static int FAKE_PLAYER_DEATH_RESPAWN_DELAY;
	public static boolean FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS;
	public static boolean FAKE_PLAYER_AUTO_COMMUNITY_BUFF;
	public static int FAKE_PLAYER_LOOT_RANGE;
	public static int FAKE_PLAYER_PICKUP_DISTANCE;
	public static boolean FAKE_PLAYER_RANDOM_NAMES;
	public static boolean FAKE_PLAYER_AUTO_HUNT;
	public static boolean FAKE_PLAYER_AUTO_QUEST;
	public static int FAKE_PLAYER_SHOP_MIN_ITEMS;
	public static int FAKE_PLAYER_SHOP_MAX_ITEMS;
	public static int FAKE_PLAYER_SHOP_MIN_STACK;
	public static int FAKE_PLAYER_SHOP_MAX_STACK;
	public static long FAKE_PLAYER_SHOP_MIN_PRICE;
	public static long FAKE_PLAYER_SHOP_MAX_PRICE;
	public static boolean FAKE_PLAYER_ALLOW_PARTY;
	public static boolean FAKE_PLAYER_ALLOW_PVP;
	public static boolean FAKE_PLAYER_ALLOW_SIEGE;
	public static int FAKE_PLAYER_HP_POTION_ID;
	public static int FAKE_PLAYER_HP_POTION_MIN_COUNT;
	public static int FAKE_PLAYER_HP_POTION_TARGET_COUNT;
	public static int FAKE_PLAYER_SELL_LOOT_THRESHOLD;
	public static int FAKE_PLAYER_SUPPORT_PERCENT;
	public static int FAKE_PLAYER_SUPPORT_HEAL_PERCENT;
	public static int FAKE_PLAYER_DENSITY_MONSTERS_PER_FAKE;
	public static int FAKE_PLAYER_DENSITY_SPARSE_GROUP_MIN;
	public static int FAKE_PLAYER_DENSITY_SPARSE_GROUP_MAX;
	public static int FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN;
	public static int FAKE_PLAYER_DENSITY_DENSE_GROUP_MAX;
	public static int FAKE_PLAYER_DENSITY_REFERENCE_COUNT;
	public static boolean FAKE_PLAYER_ALLCOME_ENABLED;
	public static int FAKE_PLAYER_MAX_PARTY_MEMBERS;
	public static boolean FAKE_PLAYER_1_TO_10_STAT_BOOST;
	public static boolean FAKE_PLAYER_11_TO_20_STAT_BOOST;
	public static boolean FAKE_PLAYER_21_TO_30_STAT_BOOST;
	public static boolean FAKE_PLAYER_31_TO_40_STAT_BOOST;
	public static boolean FAKE_PLAYER_41_TO_50_STAT_BOOST;
	public static boolean FAKE_PLAYER_51_TO_60_STAT_BOOST;
	public static boolean FAKE_PLAYER_61_TO_70_STAT_BOOST;
	public static boolean FAKE_PLAYER_71_TO_80_STAT_BOOST;
	public static boolean FAKE_PLAYER_81_TO_90_STAT_BOOST;
	public static boolean FAKE_PLAYER_91_TO_100_STAT_BOOST;
	public static boolean FAKE_PLAYER_101_TO_110_STAT_BOOST;
	public static boolean FAKE_PLAYER_111_TO_120_STAT_BOOST;
	public static double FAKE_PLAYER_1_TO_10_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_11_TO_20_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_21_TO_30_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_31_TO_40_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_41_TO_50_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_51_TO_60_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_61_TO_70_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_71_TO_80_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_81_TO_90_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_91_TO_100_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_101_TO_110_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_111_TO_120_ATTACK_MULTIPLIER;
	public static double FAKE_PLAYER_1_TO_10_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_11_TO_20_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_21_TO_30_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_31_TO_40_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_41_TO_50_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_51_TO_60_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_61_TO_70_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_71_TO_80_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_81_TO_90_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_91_TO_100_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_101_TO_110_DEFENSE_MULTIPLIER;
	public static double FAKE_PLAYER_111_TO_120_DEFENSE_MULTIPLIER;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FAKE_PLAYERS_CONFIG_FILE);
		FAKE_PLAYERS_ENABLED = config.getBoolean("EnableFakePlayers", false);
		FAKE_PLAYER_CHAT = config.getBoolean("FakePlayerChat", false);
		FAKE_PLAYER_USE_SHOTS = config.getBoolean("FakePlayerUseShots", false);
		FAKE_PLAYER_KILL_PVP = config.getBoolean("FakePlayerKillsRewardPvP", false);
		FAKE_PLAYER_KILL_KARMA = config.getBoolean("FakePlayerUnflaggedKillsKarma", false);
		FAKE_PLAYER_AUTO_ATTACKABLE = config.getBoolean("FakePlayerAutoAttackable", false);
		FAKE_PLAYER_AGGRO_MONSTERS = config.getBoolean("FakePlayerAggroMonsters", false);
		FAKE_PLAYER_AGGRO_PLAYERS = config.getBoolean("FakePlayerAggroPlayers", false);
		FAKE_PLAYER_AGGRO_FPC = config.getBoolean("FakePlayerAggroFPC", false);
		FAKE_PLAYER_CAN_DROP_ITEMS = config.getBoolean("FakePlayerCanDropItems", false);
		FAKE_PLAYER_CAN_PICKUP = config.getBoolean("FakePlayerCanPickup", false);
				// ==================== 🌟 以下為論壇版 INI 欄位解析對接 ====================
		FAKE_PLAYER_TOTAL_COUNT = Math.max(0, config.getInt("FakePlayerTotalCount", 50));
		FAKE_PLAYER_SHOP_COUNT = Math.max(0, config.getInt("FakePlayerShopCount", 10));
		FAKE_PLAYER_COMBAT_COUNT = Math.max(0, config.getInt("FakePlayerCombatCount", 40));
		FAKE_PLAYER_MIN_LEVEL = Math.max(1, config.getInt("FakePlayerMinLevel", 1));
		FAKE_PLAYER_MAX_LEVEL = Math.min(120, Math.max(FAKE_PLAYER_MIN_LEVEL, config.getInt("FakePlayerMaxLevel", 120)));
		FAKE_PLAYER_HUNT_RANGE = Math.max(300, config.getInt("FakePlayerHuntRange", 1500));
		FAKE_PLAYER_HUNT_SEARCH_RANGE = Math.max(1000, config.getInt("FakePlayerHuntSearchRange", 5000));
		FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF = Math.min(0, config.getInt("FakePlayerHuntMinLevelDiff", -5));
		FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF = Math.max(0, config.getInt("FakePlayerHuntMaxLevelDiff", 5));
		FAKE_PLAYER_HUNT_INTERVAL = Math.max(1, config.getInt("FakePlayerHuntInterval", 3));
		FAKE_PLAYER_DEATH_RESPAWN_DELAY = Math.max(1, config.getInt("FakePlayerDeathRespawnDelay", 5));
		FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS = config.getBoolean("FakePlayerAutoLearnClassSkills", true);
		FAKE_PLAYER_AUTO_COMMUNITY_BUFF = config.getBoolean("FakePlayerAutoCommunityBuff", false);
		FAKE_PLAYER_LOOT_RANGE = Math.max(100, config.getInt("FakePlayerLootRange", 1000));
		FAKE_PLAYER_PICKUP_DISTANCE = Math.max(50, config.getInt("FakePlayerPickupDistance", 200));
		FAKE_PLAYER_RANDOM_NAMES = config.getBoolean("FakePlayerRandomNames", true);
		FAKE_PLAYER_AUTO_HUNT = config.getBoolean("FakePlayerAutoHunt", true);
		FAKE_PLAYER_AUTO_QUEST = config.getBoolean("FakePlayerAutoQuest", true);
		FAKE_PLAYER_SHOP_MIN_ITEMS = Math.max(1, config.getInt("FakePlayerShopMinItems", 6));
		FAKE_PLAYER_SHOP_MAX_ITEMS = Math.max(FAKE_PLAYER_SHOP_MIN_ITEMS, config.getInt("FakePlayerShopMaxItems", 10));
		FAKE_PLAYER_SHOP_MIN_STACK = Math.max(1, config.getInt("FakePlayerShopMinStack", 1));
		FAKE_PLAYER_SHOP_MAX_STACK = Math.max(FAKE_PLAYER_SHOP_MIN_STACK, config.getInt("FakePlayerShopMaxStack", 100));
		FAKE_PLAYER_SHOP_MIN_PRICE = Math.max(1L, config.getLong("FakePlayerShopMinPrice", 1000L));
		FAKE_PLAYER_SHOP_MAX_PRICE = Math.max(FAKE_PLAYER_SHOP_MIN_PRICE, config.getLong("FakePlayerShopMaxPrice", 100000000L));
		FAKE_PLAYER_ALLOW_PARTY = config.getBoolean("FakePlayerAllowParty", true);
		FAKE_PLAYER_ALLOW_PVP = config.getBoolean("FakePlayerAllowPvP", true);
		FAKE_PLAYER_ALLOW_SIEGE = config.getBoolean("FakePlayerAllowSiege", true);
		FAKE_PLAYER_HP_POTION_ID = Math.max(1, config.getInt("FakePlayerHpPotionId", 1060));
		FAKE_PLAYER_HP_POTION_MIN_COUNT = Math.max(1, config.getInt("FakePlayerHpPotionMinCount", 10));
		FAKE_PLAYER_HP_POTION_TARGET_COUNT = Math.max(FAKE_PLAYER_HP_POTION_MIN_COUNT, config.getInt("FakePlayerHpPotionTargetCount", 100));
		FAKE_PLAYER_SELL_LOOT_THRESHOLD = Math.max(1, config.getInt("FakePlayerSellLootThreshold", 12));
		FAKE_PLAYER_SUPPORT_PERCENT = Math.max(0, Math.min(100, config.getInt("FakePlayerSupportPercent", 30)));
		FAKE_PLAYER_SUPPORT_HEAL_PERCENT = Math.max(1, Math.min(99, config.getInt("FakePlayerSupportHealPercent", 70)));
		FAKE_PLAYER_DENSITY_MONSTERS_PER_FAKE = Math.max(1, config.getInt("FakePlayerDensityMonstersPerFake", 5));
		FAKE_PLAYER_DENSITY_SPARSE_GROUP_MIN = Math.max(1, config.getInt("FakePlayerDensitySparseGroupMin", 3));
		FAKE_PLAYER_DENSITY_SPARSE_GROUP_MAX = Math.max(FAKE_PLAYER_DENSITY_SPARSE_GROUP_MIN, config.getInt("FakePlayerDensitySparseGroupMax", 5));
		FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN = Math.max(FAKE_PLAYER_DENSITY_SPARSE_GROUP_MAX, config.getInt("FakePlayerDensityDenseGroupMin", 10));
		FAKE_PLAYER_DENSITY_DENSE_GROUP_MAX = Math.max(FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN, config.getInt("FakePlayerDensityDenseGroupMax", 15));
		FAKE_PLAYER_DENSITY_REFERENCE_COUNT = Math.max(1, config.getInt("FakePlayerDensityReferenceCount", 900));
		FAKE_PLAYER_ALLCOME_ENABLED = config.getBoolean("FakePlayerAllComeEnabled", true);
		FAKE_PLAYER_MAX_PARTY_MEMBERS = Math.max(1, Math.min(30, config.getInt("FakePlayerMaxPartyMembers", 30)));
		FAKE_PLAYER_1_TO_10_STAT_BOOST = config.getBoolean("FakePlayer1To10StatBoost", true);
		FAKE_PLAYER_11_TO_20_STAT_BOOST = config.getBoolean("FakePlayer11To20StatBoost", true);
		FAKE_PLAYER_21_TO_30_STAT_BOOST = config.getBoolean("FakePlayer21To30StatBoost", true);
		FAKE_PLAYER_31_TO_40_STAT_BOOST = config.getBoolean("FakePlayer31To40StatBoost", true);
		FAKE_PLAYER_41_TO_50_STAT_BOOST = config.getBoolean("FakePlayer41To50StatBoost", true);
		FAKE_PLAYER_51_TO_60_STAT_BOOST = config.getBoolean("FakePlayer51To60StatBoost", true);
		FAKE_PLAYER_61_TO_70_STAT_BOOST = config.getBoolean("FakePlayer61To70StatBoost", true);
		FAKE_PLAYER_71_TO_80_STAT_BOOST = config.getBoolean("FakePlayer71To80StatBoost", true);
		FAKE_PLAYER_81_TO_90_STAT_BOOST = config.getBoolean("FakePlayer81To90StatBoost", true);
		FAKE_PLAYER_91_TO_100_STAT_BOOST = config.getBoolean("FakePlayer91To100StatBoost", true);
		FAKE_PLAYER_101_TO_110_STAT_BOOST = config.getBoolean("FakePlayer101To110StatBoost", true);
		FAKE_PLAYER_111_TO_120_STAT_BOOST = config.getBoolean("FakePlayer111To120StatBoost", true);
		FAKE_PLAYER_1_TO_10_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer1To10AttackMultiplier", 1.0));
		FAKE_PLAYER_11_TO_20_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer11To20AttackMultiplier", 1.0));
		FAKE_PLAYER_21_TO_30_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer21To30AttackMultiplier", 1.0));
		FAKE_PLAYER_31_TO_40_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer31To40AttackMultiplier", 1.0));
		FAKE_PLAYER_41_TO_50_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer41To50AttackMultiplier", 1.0));
		FAKE_PLAYER_51_TO_60_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer51To60AttackMultiplier", 1.0));
		FAKE_PLAYER_61_TO_70_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer61To70AttackMultiplier", 1.0));
		FAKE_PLAYER_71_TO_80_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer71To80AttackMultiplier", 1.0));
		FAKE_PLAYER_81_TO_90_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer81To90AttackMultiplier", 1.0));
		FAKE_PLAYER_91_TO_100_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer91To100AttackMultiplier", 1.0));
		FAKE_PLAYER_101_TO_110_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer101To110AttackMultiplier", 1.0));
		FAKE_PLAYER_111_TO_120_ATTACK_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer111To120AttackMultiplier", 1.0));
		FAKE_PLAYER_1_TO_10_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer1To10DefenseMultiplier", 1.0));
		FAKE_PLAYER_11_TO_20_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer11To20DefenseMultiplier", 1.0));
		FAKE_PLAYER_21_TO_30_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer21To30DefenseMultiplier", 1.0));
		FAKE_PLAYER_31_TO_40_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer31To40DefenseMultiplier", 1.0));
		FAKE_PLAYER_41_TO_50_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer41To50DefenseMultiplier", 1.0));
		FAKE_PLAYER_51_TO_60_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer51To60DefenseMultiplier", 1.0));
		FAKE_PLAYER_61_TO_70_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer61To70DefenseMultiplier", 1.0));
		FAKE_PLAYER_71_TO_80_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer71To80DefenseMultiplier", 1.0));
		FAKE_PLAYER_81_TO_90_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer81To90DefenseMultiplier", 1.0));
		FAKE_PLAYER_91_TO_100_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer91To100DefenseMultiplier", 1.0));
		FAKE_PLAYER_101_TO_110_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer101To110DefenseMultiplier", 1.0));
		FAKE_PLAYER_111_TO_120_DEFENSE_MULTIPLIER = Math.max(0.0, config.getDouble("FakePlayer111To120DefenseMultiplier", 1.0));
		org.l2jmobius.gameserver.config.PlayerConfig.ALT_PARTY_MAX_MEMBERS = FAKE_PLAYER_MAX_PARTY_MEMBERS;
	}

	// ==================== 🌟 以下為新增的等級增幅判斷接口方法 ====================
	public static double getFakeAttackMultiplier(int level) {
		if (level <= 10) return FAKE_PLAYER_1_TO_10_STAT_BOOST ? FAKE_PLAYER_1_TO_10_ATTACK_MULTIPLIER : 1.0;
		if (level <= 20) return FAKE_PLAYER_11_TO_20_STAT_BOOST ? FAKE_PLAYER_11_TO_20_ATTACK_MULTIPLIER : 1.0;
		if (level <= 30) return FAKE_PLAYER_21_TO_30_STAT_BOOST ? FAKE_PLAYER_21_TO_30_ATTACK_MULTIPLIER : 1.0;
		if (level <= 40) return FAKE_PLAYER_31_TO_40_STAT_BOOST ? FAKE_PLAYER_31_TO_40_ATTACK_MULTIPLIER : 1.0;
		if (level <= 50) return FAKE_PLAYER_41_TO_50_STAT_BOOST ? FAKE_PLAYER_41_TO_50_ATTACK_MULTIPLIER : 1.0;
		if (level <= 60) return FAKE_PLAYER_51_TO_60_STAT_BOOST ? FAKE_PLAYER_51_TO_60_ATTACK_MULTIPLIER : 1.0;
		if (level <= 70) return FAKE_PLAYER_61_TO_70_STAT_BOOST ? FAKE_PLAYER_61_TO_70_ATTACK_MULTIPLIER : 1.0;
		if (level <= 80) return FAKE_PLAYER_71_TO_80_STAT_BOOST ? FAKE_PLAYER_71_TO_80_ATTACK_MULTIPLIER : 1.0;
		if (level <= 90) return FAKE_PLAYER_81_TO_90_STAT_BOOST ? FAKE_PLAYER_81_TO_90_ATTACK_MULTIPLIER : 1.0;
		if (level <= 100) return FAKE_PLAYER_91_TO_100_STAT_BOOST ? FAKE_PLAYER_91_TO_100_ATTACK_MULTIPLIER : 1.0;
		if (level <= 110) return FAKE_PLAYER_101_TO_110_STAT_BOOST ? FAKE_PLAYER_101_TO_110_ATTACK_MULTIPLIER : 1.0;
		return FAKE_PLAYER_111_TO_120_STAT_BOOST ? FAKE_PLAYER_111_TO_120_ATTACK_MULTIPLIER : 1.0;
	}

		public static double getFakeDefenseMultiplier(int level) {
		if (level <= 10) return FAKE_PLAYER_1_TO_10_STAT_BOOST ? FAKE_PLAYER_1_TO_10_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 20) return FAKE_PLAYER_11_TO_20_STAT_BOOST ? FAKE_PLAYER_11_TO_20_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 30) return FAKE_PLAYER_21_TO_30_STAT_BOOST ? FAKE_PLAYER_21_TO_30_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 40) return FAKE_PLAYER_31_TO_40_STAT_BOOST ? FAKE_PLAYER_31_TO_40_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 50) return FAKE_PLAYER_41_TO_50_STAT_BOOST ? FAKE_PLAYER_41_TO_50_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 60) return FAKE_PLAYER_51_TO_60_STAT_BOOST ? FAKE_PLAYER_51_TO_60_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 70) return FAKE_PLAYER_61_TO_70_STAT_BOOST ? FAKE_PLAYER_61_TO_70_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 80) return FAKE_PLAYER_71_TO_80_STAT_BOOST ? FAKE_PLAYER_71_TO_80_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 90) return FAKE_PLAYER_81_TO_90_STAT_BOOST ? FAKE_PLAYER_81_TO_90_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 100) return FAKE_PLAYER_91_TO_100_STAT_BOOST ? FAKE_PLAYER_91_TO_100_DEFENSE_MULTIPLIER : 1.0;
		if (level <= 110) return FAKE_PLAYER_101_TO_110_STAT_BOOST ? FAKE_PLAYER_101_TO_110_DEFENSE_MULTIPLIER : 1.0;
		return FAKE_PLAYER_111_TO_120_STAT_BOOST ? FAKE_PLAYER_111_TO_120_DEFENSE_MULTIPLIER : 1.0;
	}
}

