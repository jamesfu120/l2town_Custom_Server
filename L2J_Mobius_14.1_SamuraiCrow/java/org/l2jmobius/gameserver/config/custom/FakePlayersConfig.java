package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class FakePlayersConfig
{
	// File
	private static final String FAKE_PLAYERS_CONFIG_FILE = "./config/Custom/FakePlayers.ini";
	
	// Constants
	public static boolean FAKE_PLAYERS_ENABLED;
	public static boolean FAKE_PLAYER_CHAT;
	public static String[] FAKE_PLAYER_CHAT_MESSAGES;
	public static int FAKE_PLAYER_CHAT_MIN_INTERVAL;
	public static int FAKE_PLAYER_CHAT_MAX_INTERVAL;
	public static boolean FAKE_PLAYER_USE_SHOTS;
	public static boolean FAKE_PLAYER_KILL_PVP;
	public static boolean FAKE_PLAYER_KILL_KARMA;
	public static boolean FAKE_PLAYER_AUTO_ATTACKABLE;
	public static boolean FAKE_PLAYER_AGGRO_MONSTERS;
	public static boolean FAKE_PLAYER_AGGRO_PLAYERS;
	public static boolean FAKE_PLAYER_AGGRO_FPC;
	public static boolean FAKE_PLAYER_CAN_DROP_ITEMS;
	public static boolean FAKE_PLAYER_CAN_PICKUP;
	
	// 動態假人數量與行為
	public static int FAKE_PLAYER_TOTAL_COUNT;
	public static int FAKE_PLAYER_SHOP_COUNT;
	public static int FAKE_PLAYER_COMBAT_COUNT;
	public static int FAKE_PLAYER_SUPPORT_PERCENT;
	public static int FAKE_PLAYER_SUPPORT_HEAL_PERCENT;
	public static int FAKE_PLAYER_DENSITY_MONSTERS_PER_FAKE;
	public static int FAKE_PLAYER_DENSITY_SPARSE_GROUP_MIN;
	public static int FAKE_PLAYER_DENSITY_SPARSE_GROUP_MAX;
	public static int FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN;
	public static int FAKE_PLAYER_DENSITY_DENSE_GROUP_MAX;
	public static int FAKE_PLAYER_DENSITY_REFERENCE_COUNT;
	public static int FAKE_PLAYER_MAX_PARTY_MEMBERS;
	public static boolean FAKE_PLAYER_ALL_COME_ENABLED;
	public static int FAKE_PLAYER_MIN_LEVEL;
	public static int FAKE_PLAYER_MAX_LEVEL;
	public static int FAKE_PLAYER_HUNT_RANGE;
	public static int FAKE_PLAYER_HUNT_SEARCH_RANGE;
	public static int FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF;
	public static int FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF;
	public static int FAKE_PLAYER_HUNT_INTERVAL;
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
	public static int FAKE_PLAYER_DEATH_RESPAWN_DELAY;
	public static int FAKE_PLAYER_HP_POTION_ID;
	public static int FAKE_PLAYER_HP_POTION_MIN_COUNT;
	public static int FAKE_PLAYER_HP_POTION_TARGET_COUNT;
	public static int FAKE_PLAYER_SELL_LOOT_THRESHOLD;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FAKE_PLAYERS_CONFIG_FILE);
		FAKE_PLAYER_CHAT_MIN_INTERVAL = Math.max(5, config.getInt("FakePlayerChatMinInterval", 90));
		FAKE_PLAYER_CHAT_MAX_INTERVAL = Math.max(FAKE_PLAYER_CHAT_MIN_INTERVAL, config.getInt("FakePlayerChatMaxInterval", 150));
		FAKE_PLAYERS_ENABLED = config.getBoolean("EnableFakePlayers", false);
		FAKE_PLAYER_CHAT = config.getBoolean("FakePlayerChat", false);
		FAKE_PLAYER_CHAT_MESSAGES = config.getString("FakePlayerChatMessages", "大家好。,這裡有人一起練嗎？,今天怪物掉寶不錯。,有人要組隊嗎？,l2town.ddns.net 天堂2單機論壇").split(",");
		FAKE_PLAYER_USE_SHOTS = config.getBoolean("FakePlayerUseShots", false);
		FAKE_PLAYER_KILL_PVP = config.getBoolean("FakePlayerKillsRewardPvP", false);
		FAKE_PLAYER_KILL_KARMA = config.getBoolean("FakePlayerUnflaggedKillsKarma", false);
		FAKE_PLAYER_AUTO_ATTACKABLE = config.getBoolean("FakePlayerAutoAttackable", false);
		FAKE_PLAYER_AGGRO_MONSTERS = config.getBoolean("FakePlayerAggroMonsters", false);
		FAKE_PLAYER_AGGRO_PLAYERS = config.getBoolean("FakePlayerAggroPlayers", false);
		FAKE_PLAYER_AGGRO_FPC = config.getBoolean("FakePlayerAggroFPC", false);
		FAKE_PLAYER_CAN_DROP_ITEMS = config.getBoolean("FakePlayerCanDropItems", false);
		FAKE_PLAYER_CAN_PICKUP = config.getBoolean("FakePlayerCanPickup", false);
		
		FAKE_PLAYER_TOTAL_COUNT = config.getInt("FakePlayerTotalCount", 200);
		FAKE_PLAYER_SHOP_COUNT = config.getInt("FakePlayerShopCount", 10);
		FAKE_PLAYER_COMBAT_COUNT = config.getInt("FakePlayerCombatCount", 190);
		FAKE_PLAYER_SUPPORT_PERCENT = config.getInt("FakePlayerSupportPercent", 10);
		FAKE_PLAYER_SUPPORT_HEAL_PERCENT = config.getInt("FakePlayerSupportHealPercent", 80);
		FAKE_PLAYER_DENSITY_MONSTERS_PER_FAKE = config.getInt("FakePlayerDensityMonstersPerFake", 5);
		FAKE_PLAYER_DENSITY_SPARSE_GROUP_MIN = config.getInt("FakePlayerDensitySparseGroupMin", 3);
		FAKE_PLAYER_DENSITY_SPARSE_GROUP_MAX = config.getInt("FakePlayerDensitySparseGroupMax", 5);
		FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN = config.getInt("FakePlayerDensityDenseGroupMin", 10);
		FAKE_PLAYER_DENSITY_DENSE_GROUP_MAX = config.getInt("FakePlayerDensityDenseGroupMax", 15);
		FAKE_PLAYER_DENSITY_REFERENCE_COUNT = config.getInt("FakePlayerDensityReferenceCount", 900);
		FAKE_PLAYER_MAX_PARTY_MEMBERS = config.getInt("FakePlayerMaxPartyMembers", 30);
		FAKE_PLAYER_ALL_COME_ENABLED = config.getBoolean("FakePlayerAllComeEnabled", true);
		FAKE_PLAYER_MIN_LEVEL = config.getInt("FakePlayerMinLevel", 10);
		FAKE_PLAYER_MAX_LEVEL = config.getInt("FakePlayerMaxLevel", 85);
		FAKE_PLAYER_HUNT_RANGE = config.getInt("FakePlayerHuntRange", 1500);
		FAKE_PLAYER_HUNT_SEARCH_RANGE = config.getInt("FakePlayerHuntSearchRange", 5000);
		FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF = config.getInt("FakePlayerHuntMinLevelDiff", -5);
		FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF = config.getInt("FakePlayerHuntMaxLevelDiff", 9);
		FAKE_PLAYER_HUNT_INTERVAL = config.getInt("FakePlayerHuntInterval", 1);
		FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS = config.getBoolean("FakePlayerAutoLearnClassSkills", true);
		FAKE_PLAYER_AUTO_COMMUNITY_BUFF = config.getBoolean("FakePlayerAutoCommunityBuff", false);
		FAKE_PLAYER_LOOT_RANGE = config.getInt("FakePlayerLootRange", 1000);
		FAKE_PLAYER_PICKUP_DISTANCE = config.getInt("FakePlayerPickupDistance", 200);
		FAKE_PLAYER_RANDOM_NAMES = config.getBoolean("FakePlayerRandomNames", true);
		FAKE_PLAYER_AUTO_HUNT = config.getBoolean("FakePlayerAutoHunt", true);
		FAKE_PLAYER_AUTO_QUEST = config.getBoolean("FakePlayerAutoQuest", true);
		FAKE_PLAYER_SHOP_MIN_ITEMS = config.getInt("FakePlayerShopMinItems", 6);
		FAKE_PLAYER_SHOP_MAX_ITEMS = config.getInt("FakePlayerShopMaxItems", 10);
		FAKE_PLAYER_SHOP_MIN_STACK = config.getInt("FakePlayerShopMinStack", 1);
		FAKE_PLAYER_SHOP_MAX_STACK = config.getInt("FakePlayerShopMaxStack", 100);
		FAKE_PLAYER_SHOP_MIN_PRICE = config.getLong("FakePlayerShopMinPrice", 1000L);
		FAKE_PLAYER_SHOP_MAX_PRICE = config.getLong("FakePlayerShopMaxPrice", 100000000L);
		FAKE_PLAYER_ALLOW_PARTY = config.getBoolean("FakePlayerAllowParty", true);
		FAKE_PLAYER_ALLOW_PVP = config.getBoolean("FakePlayerAllowPvP", true);
		FAKE_PLAYER_ALLOW_SIEGE = config.getBoolean("FakePlayerAllowSiege", true);
		FAKE_PLAYER_DEATH_RESPAWN_DELAY = config.getInt("FakePlayerDeathRespawnDelay", 5);
		FAKE_PLAYER_HP_POTION_ID = config.getInt("FakePlayerHpPotionId", 1060);
		FAKE_PLAYER_HP_POTION_MIN_COUNT = config.getInt("FakePlayerHpPotionMinCount", 1);
		FAKE_PLAYER_HP_POTION_TARGET_COUNT = config.getInt("FakePlayerHpPotionTargetCount", 10000);
		FAKE_PLAYER_SELL_LOOT_THRESHOLD = config.getInt("FakePlayerSellLootThreshold", 0);
	}
}
