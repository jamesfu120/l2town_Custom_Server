package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.InitialEquipmentData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.InitialEquipment;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.SkillCastingType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class FakePlayerPopulationManager {
   private static final Logger LOGGER = Logger.getLogger(FakePlayerPopulationManager.class.getName());
   private static final Pattern QUEST_BYPASS = Pattern.compile("bypass(?:\\s+-h)?\\s+Quest\\s+\\S+\\s+([^\\\"'<> ]+)", 2);
   private static final int QUEST_SCAN_RANGE = 3000;
   private static final List<Player> PLAYERS = new CopyOnWriteArrayList();
   private static final Set<Player> AUTO_PARTY_ALLOWED = ConcurrentHashMap.newKeySet();
   private static final int MAX_AUTO_PARTY_FAKES = 100;
   private static final Map<Player, Integer> LAST_LEVELS = new ConcurrentHashMap();
   private static final Map<Player, Long> LAST_CHAT = new ConcurrentHashMap();
   private static final Map<Player, Long> NEXT_LOOT_CHECK_AT = new ConcurrentHashMap<>();
   private static final Map<Player, Long> NEXT_PARTY_FORM_AT = new ConcurrentHashMap<>();
   private static final Map<Player, Integer> CACHED_AOE_MONSTER_COUNT = new ConcurrentHashMap<>();
   private static final Map<Player, Long> NEXT_AOE_SCAN_AT = new ConcurrentHashMap<>();
   private static final Map<Player, Long> NEXT_AI_THINK_AT = new ConcurrentHashMap();
   private static final Map<Player, Long> NEXT_PARTY_SUPPORT_AT = new ConcurrentHashMap();
   private static final Map<String, Long> SUPPORT_SKILL_RETRY_AT = new ConcurrentHashMap<>();
   private static final Map<Player, Long> NEXT_PARTY_MOVE_AT = new ConcurrentHashMap();
   private static final Map<Player, Location> PARTY_PATROL_POINTS = new ConcurrentHashMap();
   private static final Map<Player, Location> PARTY_PATROL_ANCHORS = new ConcurrentHashMap();
   private static final Map<Player, Integer> PARTY_PATROL_STEPS = new ConcurrentHashMap();
   private static final Map<Player, Integer> LAST_PARTY_SUPPORT_SKILL = new ConcurrentHashMap();
   private static final Map<Player, Integer> LAST_OFFENSIVE_SKILL = new ConcurrentHashMap();
   private static final Map<Player, Long> LEADER_LAST_COMBAT_AT = new ConcurrentHashMap<>();
   private static final Map<Player, Boolean> PARTY_HEALING_MODE = new ConcurrentHashMap();
   private static final Map<Player, Boolean> SUPPORT_ROLE_CACHE = new ConcurrentHashMap();
   private static final Map<Player, Long> NEXT_RESOURCE_SYNC_AT = new ConcurrentHashMap();
   private static final Set<Integer> INFINITE_FAKE_CONSUMABLE_IDS = ConcurrentHashMap.newKeySet();
   private static volatile boolean INFINITE_FAKE_CONSUMABLES_SCANNED;
   private static final Map<Player, Long> NEXT_EQUIPMENT_CHECK_AT = new ConcurrentHashMap();
   private static volatile long NEXT_OCCUPANCY_REBUILD_AT;
   private static final long RESOURCE_SYNC_INTERVAL_MS = 5000L;
   private static final long EQUIPMENT_CHECK_INTERVAL_MS = 30000L;
   private static final Map<Integer, Integer> TARGET_OCCUPANCY = new ConcurrentHashMap();
   private static final Map<Player, Boolean> DEATH_RESPAWN_PENDING = new ConcurrentHashMap();
   private static final AtomicInteger NEXT_VIRTUAL_SHOP_OBJECT_ID = new AtomicInteger(2000000000);
   private static final AtomicInteger NAME_SEQUENCE = new AtomicInteger();
   private static Location LAST_COMBAT_SPAWN_ANCHOR;
   private static int LAST_COMBAT_SPAWN_REMAINING;
   private static final int INITIAL_SPAWN_BATCH = 10;
   private static final int POPULATION_SPAWN_BATCH = 10;
   private static final long POPULATION_SPAWN_INTERVAL_MS = 1000L;
   private static final int MIN_GROUP_ANCHOR_DISTANCE = 2500;
   private static final long MONSTER_SNAPSHOT_REFRESH_MS = 15000L;
   private static volatile long MONSTER_SNAPSHOT_TIME;
   private static volatile List<Monster> MONSTER_SNAPSHOT = List.of();
   private static final Location[] TOWNS = new Location[]{new Location(82698, 148638, -3472), new Location(-80745, 149677, -3043), new Location(147450, 27051, -2208)};
   // 🔥【低等獵場空投解放】：將 T 大原生高等坐標換成古魯丁野地、悲哀廢墟與荒原入口安全坐標！
   // 確保假人一出生或被重置時，100% 落在低等地圖的視野內，徹底解決野外看不見假人的問題！
   private static final Location[] FALLBACK_HUNTING_GROUNDS = new Location[]{
      new Location(-13110, 126084, -3600),  // 悲哀廢墟
      new Location(-24500, 182000, -3300),  // 古魯丁村莊外圍野地
      new Location(12000, 165000, -3500)    // 荒原入口
   };
   private static final String[] GIVEN = new String[]{"天", "月", "星", "龍", "凰", "雪", "霜", "風", "雲", "雷", "夜", "晨", "玄", "紫", "青", "赤", "白", "金", "玉", "幽", "凌", "蘭", "羽", "墨", "寒", "烈", "影", "曦", "瑤", "玥"};
   private static final String[] SURNAME = new String[]{"無雙", "戰魂", "天命", "星河", "月影", "龍吟", "鳳舞", "蒼穹", "破軍", "流光", "霜華", "風痕", "夜行", "孤城", "凌霄", "玄武", "青冥", "紫電", "烈陽", "寒月"};

   private FakePlayerPopulationManager() {
   }
   // =========================================================================
   // 🧠 核心基礎神經區：移至頂部確保全檔案編譯放行
   // =========================================================================
      private static boolean isShopPlayer(Player player) {
      return player != null && (player.getPrivateStoreType() == PrivateStoreType.SELL || player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE);
   }

   private static boolean isSupportClass(Player player) {
      if (player == null) return false;
      for (Skill skill : player.getAllSkills()) {
         if (isSupportSkill(skill)) return true;
      }
      return false;
   }

   private static boolean isSupportClass(PlayerClass playerClass, int level) {
      if (playerClass == null) return false;
      for (SkillLearn learn : SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).values()) {
         if (learn != null && learn.getGetLevel() <= level) {
            Skill skill = SkillData.getInstance().getSkill(learn.getSkillId(), learn.getSkillLevel());
            if (isSupportSkill(skill)) return true;
         }
      }
      return false;
   }

     private static boolean isSupportSkill(Skill skill) {
      if (!isUsablePositiveSupportSkill(skill)) return false;
      AffectScope scope = skill.getAffectScope();
      if (scope == AffectScope.PARTY || scope == AffectScope.PARTY_PLEDGE || scope == AffectScope.RANGE || scope == AffectScope.PLEDGE) return true;
      if (skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL) && skill.getTargetType() != TargetType.SELF) return true;
      return isPositiveBuffSkill(skill);
   }

   private static boolean isPositiveBuffSkill(Skill skill) {
      if (skill == null || skill.hasNegativeEffect() || isFakePartyReturnSkill(skill)) return false;
      if (skill.hasEffectType(EffectType.HEAL, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT)) return false;
      // 只要是持續性的增益狀態（Buff / 歌舞 / 觸發技），包含自體 Buff（TargetType.SELF）全部允許施放！
      if (skill.getBuffType().isBuff() || skill.getBuffType().isDance()) {
         return true;
      }
      TargetType tt = skill.getTargetType();
      AffectScope scope = skill.getAffectScope();
      return scope == AffectScope.PARTY || scope == AffectScope.PARTY_PLEDGE || scope == AffectScope.RANGE || scope == AffectScope.PLEDGE || tt == TargetType.SELF || tt == TargetType.OTHERS || tt == TargetType.MY_PARTY || tt == TargetType.TARGET || tt == TargetType.TARGET_OR_SELF;
   }

   private static boolean isUsablePositiveSupportSkill(Skill skill) {
	  if (isCrashRiskSkill(skill)) return false;
      if (skill == null || skill.isPassive() || !skill.isActive() || skill.isToggle() || skill.hasNegativeEffect()) return false;
      if (isFakePartySummonSkill(skill) || isFakePartyReturnSkill(skill) || skill.hasEffectType(EffectType.TELEPORT)) return false;
      String name = skill.getName() != null ? skill.getName().toLowerCase(Locale.ROOT) : "";
      if (name.contains("初心者") || name.contains("novice") || name.contains("ride") || name.contains("mount") || name.contains("變身") || name.contains("transform")) return false;
      return true;
   }

   private static boolean isFakePartySummonSkill(Skill skill) {
      if (skill == null) return false;
      for (EffectScope effectScope : EffectScope.values()) {
         List<AbstractEffect> effects = skill.getEffects(effectScope);
         if (effects != null) {
            for (AbstractEffect effect : effects) {
               if (effect != null && ("CallParty".equals(effect.getClass().getSimpleName()) || "CallTargetParty".equals(effect.getClass().getSimpleName()))) return true;
            }
         }
      }
      return false;
   }

   private static boolean isFakePartyReturnSkill(Skill skill) {
      if (skill == null) return false;
      return skill.getId() == 1255 || skill.getId() == 11819 || (skill.getName() != null && skill.getName().equalsIgnoreCase("Party Return"));
   }

   private static boolean isForbiddenFakeSupportUtilitySkill(Skill skill) {
      return skill == null || isFakePartySummonSkill(skill) || isFakePartyReturnSkill(skill) || skill.isTransformation();
   }
   
             private static boolean isCrashRiskSkill(Skill skill) {
      if (skill == null) return true;
      // 1. 過濾傳送與目標傳送技能
      if (skill.hasEffectType(EffectType.TELEPORT, EffectType.TELEPORT_TO_TARGET)) {
         return true;
      }
      // 2. 過濾無效的道具/結晶/座騎召喚（保留戰鬥使魔召喚，交給 isBlockedFakeSkill 依職業放行）
      String skillName = skill.getName() != null ? skill.getName().toLowerCase(Locale.ROOT) : "";
      if (skillName.contains("crystal") || skillName.contains("shot") || skillName.contains("cubic") || skillName.contains("portal") || skillName.contains("gate")) {
         return true;
      }
      // 3. 透過效果類別名稱過濾危險位移技（不再無差別封鎖 Summon 使魔效果）
      for (EffectScope effectScope : EffectScope.values()) {
         List<AbstractEffect> effects = skill.getEffects(effectScope);
         if (effects != null) {
            for (AbstractEffect effect : effects) {
               if (effect != null) {
                  String effName = effect.getClass().getSimpleName().toLowerCase(Locale.ROOT);
                  if (effName.contains("charge") || effName.contains("rush") || effName.contains("blink") || effName.contains("dash") || effName.contains("fly") || effName.contains("knockback") || effName.contains("pull") || effName.contains("summonnpc")) {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }
   
         private static boolean isBlockedFakeSkill(Player player, Skill skill) {
      if (skill == null || skill.isTransformation() || isCrashRiskSkill(skill)) {
         return true;
      }
      String skName = skill.getName() != null ? skill.getName().toLowerCase(Locale.ROOT) : "";
      // 1. 永遠封鎖所有座騎與騎乘技能
      if (skName.contains("mount") || skName.contains("ride") || skName.contains("horse")
         || skName.contains("騎乘") || skName.contains("座騎") || skName.contains("戰馬") || skName.contains("幻影")) {
         return true;
      }
      // 2. 只有 85 級以上（四轉覺醒）才過濾舊時代 2~3 轉重複歌舞，避免與奏鳴曲互相覆蓋；84 級以下三轉假人保留原歌舞！
      if (player != null && player.getLevel() >= 85) {
         if (skName.startsWith("dance of ") || skName.startsWith("song of ") || skName.startsWith("chant of ")
            || skName.contains("之舞") || skName.contains("之歌") || skName.contains("吟唱")) {
            return true;
         }
      }
      // 3. 召喚類技能：只有「召喚師職業」才允許保留，其他職業一律過濾避免亂召
      if (skName.contains("summon") || skName.contains("召喚")) {
         boolean isSummoner = player != null && player.getPlayerClass() != null && player.getPlayerClass().isSummoner();
         return !isSummoner;
      }
      return false;
   }


   public static void initialize() {
      cleanupOrphanFakePlayerItems();
      if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT > 0) {
         int shop = Math.min(FakePlayersConfig.FAKE_PLAYER_SHOP_COUNT, FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT);
         int combat = Math.min(FakePlayersConfig.FAKE_PLAYER_COMBAT_COUNT, FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT - shop);
         if (shop + combat < FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT) {
            combat += FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT - shop - combat;
         }

         int initialCount = Math.min(10, shop + combat);
         int initialShops = 0;

         for(int i = 0; i < initialCount; ++i) {
            boolean shopType = initialShops < shop && (i < shop || shop + combat - i <= combat);
            if (shopType) {
               ++initialShops;
            }

            spawnOne(shopType);
         }

         if (FakePlayersConfig.FAKE_PLAYER_AUTO_HUNT) {
            ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::runThinkSafely, 750L, 250L);
            ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::randomChat, 15000L, 15000L);
			         ThreadPool.scheduleAtFixedRate(() -> {
            try { broadcastFakePlayersToNearbyPlayers(); } catch (Throwable t) { LOGGER.log(Level.WARNING, "broadcastFake error", t); }
         }, 3000L, 3000L);

         }

         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::ensurePopulation, 1000L, 1000L);
         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::syncFakePlayersToPartyMatchingList, 3000L, 5000L);
         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::relocateCombatFakesToHuntingGrounds, 15000L, 15000L);
		 ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::exportFakePlayersStatusJson, 3000L, 3000L);

		          // 🔥 註冊全域聊天監聽（免 import 通用版：相容所有 Mobius 14.1 事件路徑）
         org.l2jmobius.gameserver.mechanics.events.Containers.Players().addListener(
            new org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener(
               org.l2jmobius.gameserver.mechanics.events.Containers.Players(),
               EventType.ON_PLAYER_CHAT,
               event -> {
                  try {
                     Player activeChar = (Player) event.getClass().getMethod("getActiveChar").invoke(event);
                     String target = (String) event.getClass().getMethod("getTarget").invoke(event);
                     String text = (String) event.getClass().getMethod("getText").invoke(event);
                     ChatType chatType = (ChatType) event.getClass().getMethod("getChatType").invoke(event);
                     handlePlayerChat(activeChar, target, text, chatType);
                  } catch (Exception ignored) {
                  }
               },
               FakePlayerPopulationManager.class
            )
         );
         LOGGER.fine("FakePlayerPopulationManager: spawned " + PLAYERS.size() + " in-memory fake players (shop=" + shop + ", combat=" + combat + ").");
      } 
   }

   // 貼在 initialize() 結束大括號的外面！
   private static void broadcastFakePlayersToNearbyPlayers() {
      for (Player fake : PLAYERS) {
         if (fake == null || !fake.isSpawned() || !fake.isOnline()) {
            continue;
         }
         World.forEachVisibleObjectInRange(fake, Player.class, 3000, realPlayer -> {
                        if (realPlayer != null && !realPlayer.isFakePlayer() && realPlayer.isOnline()) {
               fake.sendInfo(realPlayer);
            }
         });
      }
   }

   private static void cleanupOrphanFakePlayerItems() {

      try {
         Connection con = DatabaseFactory.getConnection();

         try {
            PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id > 0 AND loc IN ('INVENTORY', 'PAPERDOLL') AND NOT EXISTS (SELECT 1 FROM characters c WHERE c.charId = items.owner_id)");

            try {
               int removed = statement.executeUpdate();
               if (removed > 0) {
                  LOGGER.warning("Removed " + removed + " stale non-persistent fake-player inventory item row(s) from the database.");
               }
            } catch (Throwable var6) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable var7) {
            if (con != null) {
               try {
                  con.close();
               } catch (Throwable var4) {
                  var7.addSuppressed(var4);
               }
            }

            throw var7;
         }

         if (con != null) {
            con.close();
         }
      } catch (Exception e) {
         LOGGER.log(Level.WARNING, "Could not clean stale fake-player inventory item rows. Server startup will continue.", e);
      }

   }

   private static void ensurePopulation() {
      if (FakePlayersConfig.FAKE_PLAYERS_ENABLED) {
         for(Player player : new ArrayList<Player>(PLAYERS)) {
            if (player != null && !player.isDead() && !DEATH_RESPAWN_PENDING.containsKey(player) && (!player.isOnline() || World.getPlayer(player.getObjectId()) != player)) {
               MatchingRoomManager.getInstance().removeFromWaitingList(player);
               PLAYERS.remove(player);
			   AUTO_PARTY_ALLOWED.remove(player);
               NEXT_AI_THINK_AT.remove(player);
               NEXT_PARTY_SUPPORT_AT.remove(player);
               NEXT_PARTY_MOVE_AT.remove(player);
               PARTY_PATROL_POINTS.remove(player);
               PARTY_PATROL_ANCHORS.remove(player);
               PARTY_PATROL_STEPS.remove(player);
               SUPPORT_ROLE_CACHE.remove(player);
               NEXT_RESOURCE_SYNC_AT.remove(player);
               NEXT_EQUIPMENT_CHECK_AT.remove(player);
			   NEXT_LOOT_CHECK_AT.remove(player);
               NEXT_PARTY_FORM_AT.remove(player);
               CACHED_AOE_MONSTER_COUNT.remove(player);
               NEXT_AOE_SCAN_AT.remove(player);
            }
         }

         int targetTotal = Math.max(0, FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT);
         int targetShop = Math.min(targetTotal, Math.max(0, FakePlayersConfig.FAKE_PLAYER_SHOP_COUNT));
         int targetCombat = Math.max(0, targetTotal - targetShop);
         trimFakeTypeToTarget(true, targetShop);
         trimFakeTypeToTarget(false, targetCombat);
         int missing = targetTotal - PLAYERS.size();
         int maxAttempts = Math.min(10, Math.max(0, missing));
         int created = 0;

         for(int attempts = 0; attempts < maxAttempts; ++attempts) {
            long currentShops = PLAYERS.stream().filter(FakePlayerPopulationManager::isShopPlayer).count();
            long currentCombat = (long)PLAYERS.size() - currentShops;
            boolean createShop = currentShops < (long)targetShop && (currentCombat >= (long)targetCombat || currentShops < (long)targetShop);
            int before = PLAYERS.size();
            spawnOne(createShop);
            if (PLAYERS.size() > before) {
               ++created;
            }
         }

         if (PLAYERS.size() != targetTotal && created == 0 && missing > 0) {
            LOGGER.warning("Fake player population mismatch: configured=" + targetTotal + ", actual=" + PLAYERS.size() + ". No fake player could be created in this batch.");
         }

      }
   }

   private static void trimFakeTypeToTarget(boolean shop, int target) {
      List<Player> candidates = PLAYERS.stream().filter((playerx) -> playerx != null && isShopPlayer(playerx) == shop).toList();
      int excess = candidates.size() - Math.max(0, target);

      for(int i = 0; i < excess; ++i) {
         Player player = (Player)candidates.get(i);

         try {
            if (player.getParty() != null) {
               player.getParty().removePartyMember(player, PartyMessageType.DISCONNECTED);
            }

            player.deleteMe();
         } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to remove excess fake player " + player.getName(), e);
         }

         PLAYERS.remove(player);
		 AUTO_PARTY_ALLOWED.remove(player);
         NEXT_AI_THINK_AT.remove(player);
         NEXT_PARTY_SUPPORT_AT.remove(player);
         LAST_PARTY_SUPPORT_SKILL.remove(player);
         LAST_OFFENSIVE_SKILL.remove(player);
         SUPPORT_ROLE_CACHE.remove(player);
         SUPPORT_ROLE_CACHE.remove(player);
         NEXT_RESOURCE_SYNC_AT.remove(player);
         NEXT_EQUIPMENT_CHECK_AT.remove(player);
		 NEXT_LOOT_CHECK_AT.remove(player);
         NEXT_PARTY_FORM_AT.remove(player);
         CACHED_AOE_MONSTER_COUNT.remove(player);
         NEXT_AOE_SCAN_AT.remove(player);
      }

   }

   private static List<PlayerClass> getEligibleFakeClasses(int level) {
      List<PlayerClass> classes = new ArrayList();
      int maxClassStage;
      if (level < 20) {
         maxClassStage = 0;
      } else if (level < 40) {
         maxClassStage = 1;
      } else if (level < 76) {
         maxClassStage = 2;
      } else if (level < 85) {
         maxClassStage = 3;
      } else {
         maxClassStage = 4;
      }

      for(PlayerClass pc : PlayerClass.values()) {
         if (pc.getRace() != null && pc.level() == maxClassStage && PlayerTemplateData.getInstance().getTemplate(pc.getId()) != null) {
            classes.add(pc);
         }
      }

      return classes;
   }

      private static void spawnOne(boolean shop) {
      try {
         // 🔥 修改：直接強制假人生成等級為 70 ~ 110 級
         int minLv = Math.max(1, FakePlayersConfig.FAKE_PLAYER_MIN_LEVEL);
         int maxLv = Math.max(minLv, FakePlayersConfig.FAKE_PLAYER_MAX_LEVEL);
         int level = Rnd.get(minLv, maxLv);
         List<PlayerClass> classes = getEligibleFakeClasses(level);
         if (classes.isEmpty()) {
            LOGGER.warning("No EPIC 14.1 playable class is eligible for fake-player level " + level + ".");
            return;
         }

         long combatCount = PLAYERS.stream().filter((p) -> p != null && !isShopPlayer(p)).count();
         long supportCount = PLAYERS.stream().filter((p) -> p != null && !isShopPlayer(p) && isSupportClass(p)).count();
         int supportPercent = Math.max(0, Math.min(100, FakePlayersConfig.FAKE_PLAYER_SUPPORT_PERCENT));
         int targetCombat = Math.max(0, FakePlayersConfig.FAKE_PLAYER_COMBAT_COUNT);
         long targetSupport = Math.round((double)targetCombat * ((double)supportPercent / (double)100.0F));
         boolean needSupport = !shop && supportCount < targetSupport && supportCount * (long)targetCombat < (combatCount + 1L) * targetSupport;
         List<PlayerClass> spawnClasses = classes;
         if (!shop && FakePlayersConfig.FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS) {
            List<PlayerClass> matchingClasses = classes.stream().filter((candidatex) -> isSupportClass(candidatex, level) == needSupport).toList();
            if (!matchingClasses.isEmpty()) {
               spawnClasses = matchingClasses;
            }
         }

         PlayerClass pc = null;
         PlayerTemplate template = null;
         Player player = null;
         List<PlayerClass> classesToTry = new ArrayList(spawnClasses);

         while(!classesToTry.isEmpty()) {
            PlayerClass candidate = (PlayerClass)classesToTry.remove(Rnd.get(classesToTry.size()));
            PlayerTemplate candidateTemplate = PlayerTemplateData.getInstance().getTemplate(candidate.getId());
            if (candidateTemplate != null) {
               pc = candidate;
               template = candidateTemplate;
               break;
            }
         }

         if (template != null && pc != null) {
            player = Player.createFakePlayer(template, uniqueName(), level);
            if (!shop && FakePlayersConfig.FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS) {
               prepareCombatFakePlayer(player);
            }
         }

         if (player == null || template == null || pc == null) {
            LOGGER.warning("Could not select a valid EPIC 14.1 fake-player class for level " + level + ".");
            return;
         }

         String name = player.getName();
         player.getInventory().addItem(ItemProcessType.REWARD, 57, 10000000L, player, (Object)null);
         PlayerClass equipmentClass = InitialEquipmentData.getInstance().getClassEquipment(pc) != null ? pc : pc.getRootClass();
         Collection<InitialEquipment> initialEquipment = InitialEquipmentData.getInstance().getClassEquipment(equipmentClass);
         if (initialEquipment != null) {
            for(InitialEquipment equipment : initialEquipment) {
               Item equippedItem = player.getInventory().addItem(ItemProcessType.REWARD, equipment.getId(), equipment.getCount(), player, (Object)null);
               if (equippedItem != null && equippedItem.isEquipable() && equipment.isEquipped()) {
                  player.getInventory().equipItem(equippedItem);
               }
            }
         } else {
            Logger var10000 = LOGGER;
            int var10001 = pc.getId();
            var10000.warning("No initial equipment data for fake player class " + var10001 + " or root class " + pc.getRootClass().getId());
         }

         if (!shop) {
            removeFakeEquipmentGradePenalty(player);
            prepareFakeConsumables(player);
            if (!FakePlayersConfig.FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS) {
               prepareCombatFakePlayer(player);
            }

            applyFixedBGradeEquipment(player);
         }

                  Location loc = shop ? TOWNS[Rnd.get(TOWNS.length)] : findCombatSpawnLocation(player);
         if (loc == null || (loc.getX() == 0 && loc.getY() == 0)) {
            loc = TOWNS[Rnd.get(TOWNS.length)];
         }
                  player.spawnMe(loc.getX() + Rnd.get(-150, 150), loc.getY() + Rnd.get(-150, 150), loc.getZ());
         if (!player.isSpawned() || World.getPlayer(player.getObjectId()) != player) {
            Location fallback = FALLBACK_HUNTING_GROUNDS[Rnd.get(FALLBACK_HUNTING_GROUNDS.length)];
            player.spawnMe(fallback.getX() + Rnd.get(-150, 150), fallback.getY() + Rnd.get(-150, 150), fallback.getZ());
         }

         if (!player.isSpawned() || World.getPlayer(player.getObjectId()) != player) {
            LOGGER.warning("Fake player " + name + " could not be spawned and will not be retained.");
            return;
         }

         player.setCurrentHp((double)player.getMaxHp());
         player.setCurrentMp((double)player.getMaxMp());
         player.setCurrentCp((double)player.getMaxCp());
         player.getAI().setIntentionActive();
         player.getAI().notifyActionThink();
         NEXT_AI_THINK_AT.put(player, System.currentTimeMillis() + (long)Rnd.get(350, 3000));
         FakePlayerData.getInstance().addDynamicFakePlayerName(name);
         if (shop) {
            setupShop(player);
         } else {
            player.setTitle(String.valueOf(level));
            LAST_LEVELS.put(player, level);
            configureAutomaticPotion(player);
         }
            if (!shop && AUTO_PARTY_ALLOWED.size() < MAX_AUTO_PARTY_FAKES) {
            AUTO_PARTY_ALLOWED.add(player);
         }
         PLAYERS.add(player);
         MatchingRoomManager.getInstance().addToWaitingList(player);
         player.broadcastInfo();
         player.broadcastUserInfo();
         LOGGER.fine("Fake player ready: name=" + name + ", type=" + (shop ? "SHOP" : "COMBAT") + ", objectId=" + player.getObjectId() + ", spawned=" + player.isSpawned() + ", online=" + player.isOnline() + ", enteredWorld=" + player.hasEnteredWorld() + ", instanceId=" + player.getInstanceId() + ", region=" + String.valueOf(player.getWorldRegion()) + ", location=" + player.getX() + "," + player.getY() + "," + player.getZ());
      } catch (Exception e) {
         LOGGER.log(Level.WARNING, "Failed to create fake player.", e);
      }

   }

   private static List<Monster> getMonsterSnapshot() {
      long now = System.currentTimeMillis();
      if (now - MONSTER_SNAPSHOT_TIME < 15000L && !MONSTER_SNAPSHOT.isEmpty()) {
         return MONSTER_SNAPSHOT;
      } else {
         synchronized(FakePlayerPopulationManager.class) {
            if (now - MONSTER_SNAPSHOT_TIME >= 15000L || MONSTER_SNAPSHOT.isEmpty()) {
               List<Monster> snapshot = new ArrayList();

               for(WorldObject object : World.getVisibleObjects()) {
                  if (object instanceof Monster) {
                     Monster monster = (Monster)object;
                     if (monster.isSpawned() && !monster.isDead()) {
                        snapshot.add(monster);
                     }
                  }
               }

               MONSTER_SNAPSHOT = List.copyOf(snapshot);
               MONSTER_SNAPSHOT_TIME = now;
            }

            return MONSTER_SNAPSHOT;
         }
      }
   }

   private static Location findCombatSpawnLocation(Player player) {
            if (LAST_COMBAT_SPAWN_ANCHOR != null && LAST_COMBAT_SPAWN_REMAINING > 0) {
         --LAST_COMBAT_SPAWN_REMAINING;
         int tx = LAST_COMBAT_SPAWN_ANCHOR.getX() + Rnd.get(-150, 150);
         int ty = LAST_COMBAT_SPAWN_ANCHOR.getY() + Rnd.get(-150, 150);
         int tz = LAST_COMBAT_SPAWN_ANCHOR.getZ();
         return GeoEngine.getInstance().getValidLocation(LAST_COMBAT_SPAWN_ANCHOR.getX(), LAST_COMBAT_SPAWN_ANCHOR.getY(), tz, tx, ty, tz, null);
      } else {
         List<Monster> candidates = getMonsterSnapshot().stream().filter((monster) -> isSuitableHuntingMonster(player, monster)).toList();
         if (candidates.isEmpty()) {
            return FALLBACK_HUNTING_GROUNDS[Rnd.get(FALLBACK_HUNTING_GROUNDS.length)];
         } else {
            Monster best = (Monster)candidates.get(Rnd.get(candidates.size()));
            int bestDensity = 1;
            List<Monster> spreadCandidates = candidates.stream().filter((candidatex) -> LAST_COMBAT_SPAWN_ANCHOR == null || Math.hypot((double)(candidatex.getX() - LAST_COMBAT_SPAWN_ANCHOR.getX()), (double)(candidatex.getY() - LAST_COMBAT_SPAWN_ANCHOR.getY())) >= (double)2500.0F).toList();
            List<Monster> samplePool = spreadCandidates.isEmpty() ? candidates : spreadCandidates;
            List<Monster> samplesPoolUnique = new ArrayList();
            int samples = Math.min(48, samplePool.size());

            for(int i = 0; i < samples; ++i) {
               Monster candidate = (Monster)samplePool.get(Rnd.get(samplePool.size()));
               if (!samplesPoolUnique.contains(candidate)) {
                  samplesPoolUnique.add(candidate);
               }
            }

            int lowestFakeCount = Integer.MAX_VALUE;
            List<Monster> preferredAreas = new ArrayList();

            for(Monster candidate : samplesPoolUnique) {
               int fakeCount = (int)PLAYERS.stream().filter((existing) -> existing != null && existing.isFakePlayer() && !isShopPlayer(existing) && existing.isSpawned()).filter((existing) -> Math.hypot((double)(existing.getX() - candidate.getX()), (double)(existing.getY() - candidate.getY())) < (double)2500.0F).count();
               if (fakeCount < lowestFakeCount) {
                  lowestFakeCount = fakeCount;
                  preferredAreas.clear();
                  preferredAreas.add(candidate);
               } else if (fakeCount == lowestFakeCount) {
                  preferredAreas.add(candidate);
               }
            }

            if (!preferredAreas.isEmpty()) {
               best = (Monster)preferredAreas.get(Rnd.get(preferredAreas.size()));
               bestDensity = Math.max(1, World.getVisibleObjectsInRange(best, Monster.class, 1800, (m) -> isSuitableHuntingMonster(player, m)).size());
               int densitySamples = Math.min(12, preferredAreas.size());

               for(int i = 0; i < densitySamples; ++i) {
                  Monster candidate = (Monster)preferredAreas.get(Rnd.get(preferredAreas.size()));
                  int density = Math.max(1, World.getVisibleObjectsInRange(candidate, Monster.class, 1800, (m) -> isSuitableHuntingMonster(player, m)).size());
                  if (density > bestDensity || density == bestDensity && Rnd.get(2) == 0) {
                     best = candidate;
                     bestDensity = density;
                  }
               }
            }

            LAST_COMBAT_SPAWN_ANCHOR = new Location(best.getX(), best.getY(), best.getZ());
            int monstersPerFake = FakePlayersConfig.FAKE_PLAYER_DENSITY_MONSTERS_PER_FAKE;
            boolean dense = bestDensity >= monstersPerFake * FakePlayersConfig.FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN;
            int groupMin = dense ? FakePlayersConfig.FAKE_PLAYER_DENSITY_DENSE_GROUP_MIN : FakePlayersConfig.FAKE_PLAYER_DENSITY_SPARSE_GROUP_MIN;
            int groupMax = dense ? FakePlayersConfig.FAKE_PLAYER_DENSITY_DENSE_GROUP_MAX : FakePlayersConfig.FAKE_PLAYER_DENSITY_SPARSE_GROUP_MAX;
            LAST_COMBAT_SPAWN_REMAINING = Math.max(0, Rnd.get(groupMin, groupMax) - 1);
            return new Location(best.getX() + Rnd.get(-500, 500), best.getY() + Rnd.get(-500, 500), best.getZ());
         }
      }
   }

   private static void relocateCombatFakesToHuntingGrounds() {
      if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && FakePlayersConfig.FAKE_PLAYER_AUTO_HUNT) {
         for(Player player : PLAYERS) {
            if (player != null && player.isSpawned() && !player.isDead() && !isShopPlayer(player)) {
               try {
                  Location town = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
                  if (town != null && isInsideTown(player, town)) {
                     Location huntingGround = findCombatSpawnLocation(player);
                     if (huntingGround != null) {
                        player.setTarget((WorldObject)null);
                        player.teleToLocation(new Location(huntingGround.getX() + Rnd.get(-250, 250), huntingGround.getY() + Rnd.get(-250, 250), huntingGround.getZ()), true);
                        Logger var10000 = LOGGER;
                        String var10001 = player.getName();
                        var10000.fine("Fake player moved from town to distributed hunting ground: name=" + var10001 + ", level=" + player.getLevel() + ", location=" + huntingGround.getX() + "," + huntingGround.getY() + "," + huntingGround.getZ());
                     }
                  }
               } catch (Exception e) {
                  LOGGER.log(Level.FINE, "Failed to relocate combat fake player to hunting ground.", e);
               }
            }
         }

      }
   }

     private static void setupShop(Player player) {
      // 50% 機率開立矮人製作工房，50% 機率開立實用百貨商店
      if (Rnd.get(100) < 50 && setupManufactureWorkshop(player)) {
         return;
      }

      player.setTitle("百貨商人");
      int stockCount = Rnd.get(FakePlayersConfig.FAKE_PLAYER_SHOP_MIN_ITEMS, FakePlayersConfig.FAKE_PLAYER_SHOP_MAX_ITEMS);
      int added = 0;

      for (int i = 0; i < stockCount * 15 && added < stockCount; ++i) {
         ItemTemplate template = getRandomShopItem();
         if (template != null && template.isSellable() && template.isTradeable() && !template.isQuestItem() && !containsBlockedEquipmentName(template.getName())) {
            long count = template.isStackable() ? (long) Rnd.get(FakePlayersConfig.FAKE_PLAYER_SHOP_MIN_STACK, FakePlayersConfig.FAKE_PLAYER_SHOP_MAX_STACK) : 1L;
            long referencePrice = Math.max(1000L, template.getReferencePrice());
            long minPrice = Math.max(FakePlayersConfig.FAKE_PLAYER_SHOP_MIN_PRICE, referencePrice / 2L);
            long maxPrice = Math.max(minPrice, Math.min(FakePlayersConfig.FAKE_PLAYER_SHOP_MAX_PRICE, referencePrice * 2L));
            long price = Rnd.get(minPrice, maxPrice);
            TradeItem virtualItem = new TradeItem(template, count, price);
            virtualItem.setObjectId(NEXT_VIRTUAL_SHOP_OBJECT_ID.getAndDecrement());
            if (!player.getSellList().getItems().stream().anyMatch(existing -> existing.getItem().getId() == template.getId())) {
               player.getSellList().getItems().add(virtualItem);
               ++added;
            }
         }
      }

            player.getSellList().setTitle("精選裝備／卷軸／藥水／材料");
      player.setPrivateStoreType(PrivateStoreType.SELL);
      player.sitDown();
      player.broadcastUserInfo();
      player.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell(player));

   }

         @SuppressWarnings({"rawtypes", "unchecked"})
     private static boolean setupManufactureWorkshop(Player player) {
      try {
         List<RecipeHolder> validRecipes = new ArrayList<>();
         for (int id = 1; id <= 2500; ++id) {
            RecipeHolder r = RecipeData.getInstance().getRecipe(id);
            if (r != null && r.getItemId() > 0 && ItemData.getInstance().getTemplate(r.getItemId()) != null) {
               validRecipes.add(r);
            }
         }
         if (validRecipes.isEmpty()) {
            return false;
         }

                  player.setTitle("矮人工房");
         int recipeCount = Rnd.get(6, 12);
         int added = 0;
         Map<Integer, Long> craftItems = new java.util.HashMap<>();

         int maxLoops = recipeCount * 10;
         for (int i = 0; i != maxLoops; ++i) {
            if (added >= recipeCount) {
               break;
            }
            RecipeHolder recipe = validRecipes.get(Rnd.get(validRecipes.size()));
            if (recipe.isDwarvenRecipe()) {
               player.registerDwarvenRecipeList(recipe, false);
            } else {
               player.registerCommonRecipeList(recipe, false);
            }
            if (!craftItems.containsKey(recipe.getId())) {
               long craftFee = Rnd.get(10000L, 500000L);
               craftItems.put(recipe.getId(), craftFee);
               ++added;
            }
         }
         player.setManufactureItems(craftItems);

         if (added == 0) {
            return false;
         }

                  player.setStoreName("【矮人工房】裝備／靈魂彈代工");
         player.setPrivateStoreType(PrivateStoreType.MANUFACTURE);
         player.sitDown();
         player.broadcastUserInfo();
         player.broadcastPacket(new RecipeShopMsg(player));
         return true;
      } catch (Exception e) {
         LOGGER.log(Level.WARNING, "Failed to setup fake dwarf workshop for " + player.getName(), e);
         return false;
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static ItemTemplate getRandomShopItem() {
      int roll = Rnd.get(100);
      if (roll >= 80) {
         List others = new ArrayList();
         for (EtcItem item : ItemData.getInstance().getAllEtcItems()) {
            if (item != null && (item.isPotion() || item.isElixir() || (!item.isQuestItem() && item.getItemType() != EtcItemType.ARROW && item.getItemType() != EtcItemType.BOLT))) {
               others.add(item);
            }
         }
         return getRandom(others);
      } else if (roll >= 60) {
         List scrolls = new ArrayList();
         for (EtcItem item : ItemData.getInstance().getAllEtcItems()) {
            if (item != null && item.isScroll()) {
               scrolls.add(item);
            }
         }
         return getRandom(scrolls);
      } else if (roll >= 30) {
         List armors = new ArrayList();
         for (Armor a : ItemData.getInstance().getAllArmors()) {
            if (a != null && a.getItemGrade() != ItemGrade.NONE && a.getItemGrade() != ItemGrade.D && a.getItemGrade() != ItemGrade.C) {
               armors.add(a);
            }
         }
         return getRandom(armors);
      } else {
         List weapons = new ArrayList();
         for (Weapon w : ItemData.getInstance().getAllWeapons()) {
            if (w != null && w.getItemGrade() != ItemGrade.NONE && w.getItemGrade() != ItemGrade.D && w.getItemGrade() != ItemGrade.C) {
               weapons.add(w);
            }
         }
         return getRandom(weapons);
      }
   }

   private static <T extends ItemTemplate> T getRandom(Collection<T> items) {
      return items != null && !items.isEmpty() ? items.stream().skip((long)Rnd.get(items.size())).findFirst().orElse(null) : null;
   }

   private static String uniqueName() {
      int poolSize = GIVEN.length * SURNAME.length;

      for(int attempt = 0; attempt < poolSize + 100000; ++attempt) {
         int sequence = NAME_SEQUENCE.getAndIncrement();
         int baseIndex = Math.floorMod(sequence, poolSize);
         String var10000 = GIVEN[baseIndex / SURNAME.length];
         String baseName = var10000 + SURNAME[baseIndex % SURNAME.length];
         int suffix = sequence / poolSize;
         String name = suffix == 0 ? baseName : baseName + String.format("%03d", suffix);
         if (World.getPlayer(name) == null) {
            return name;
         }
      }

      throw new IllegalStateException("Unable to allocate a unique fake-player name after exhausting the configured name space.");
   }

   public static void handleFakePlayerDeath(Player player) {
      if (player != null && player.isFakePlayer() && player.isDead() && player.isOnline()) {
         if (DEATH_RESPAWN_PENDING.putIfAbsent(player, Boolean.TRUE) == null) {
            ThreadPool.schedule(() -> {
               try {
                  if (player.isFakePlayer() && player.isOnline() && player.isDead()) {
                     player.setTarget((WorldObject)null);
                     player.setIsPendingRevive(false);
                     player.doRevive();
                     player.fullRestore();
                     player.setTitle(String.valueOf(player.getLevel()));
                     configureAutomaticPotion(player);
                     prepareCombatFakePlayer(player);
                     player.getAI().setIntentionActive();
                     Logger var10000 = LOGGER;
                     String var10001 = player.getName();
                     var10000.fine("Fake player auto-respawned at death location: name=" + var10001 + ", location=" + player.getX() + "," + player.getY() + "," + player.getZ());
                     return;
                  }
               } catch (Exception e) {
                  LOGGER.log(Level.WARNING, "Failed to auto-respawn fake player " + player.getName(), e);
                  return;
               } finally {
                  DEATH_RESPAWN_PENDING.remove(player);
               }

            }, (long)FakePlayersConfig.FAKE_PLAYER_DEATH_RESPAWN_DELAY * 1000L);
         }
      }
   }
    private static void prepareCombatFakePlayer(Player player) {
      if (player != null && player.isFakePlayer()) {
         player.getEffectList().stopEffects(AbnormalType.BOT_PENALTY);
               if (FakePlayersConfig.FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS) {
            try {
               Collection<Skill> skills = SkillTreeData.getInstance().getAllAvailableSkills(player, player.getTemplate().getPlayerClass(), true, true, true, true);
               int added = 0;

                              for (Skill skill : skills) {
                  if (skill != null && player.getKnownSkill(skill.getId()) != skill) {
                     // 🔥 源頭攔截：學技能當下直接過濾坐騎、騎乘、變身與高風險技能，絕不加入技能欄
                     if (isBlockedFakeSkill(player, skill)) { 
					 continue; 
                     }
                     player.addSkill(skill, false);
                     ++added;
                  }
               }

               // 雙重保險：清理角色模板自帶的基礎坐騎/變身技能（僅在初始化與升級時執行一次）
               for (Skill existing : new ArrayList<>(player.getAllSkills())) {
                  if (isBlockedFakeSkill(player, existing)) { 
                     player.removeSkill(existing, false);
                  }
               }

               player.getStat().recalculateStats(false);
               if (added > 0) {
                  LOGGER.fine("Fake player class skills prepared: name=" + player.getName() + ", class=" + player.getPlayerClass().name() + ", level=" + player.getLevel() + ", added=" + added);
               }
            } catch (Exception e) {
               LOGGER.log(Level.WARNING, "Could not prepare class skills for fake player " + player.getName(), e);
            }
         }

                  // 🔥【修正位置】：僅在假人已進入世界（isSpawned）且技能不含 CallSkill 連鎖觸發時才開啟光環
         if (player.isSpawned()) {
            for (Skill skill : player.getAllSkills()) {
               if (skill != null && skill.isToggle() && !skill.hasNegativeEffect() && !isCrashRiskSkill(skill)) {
                  boolean hasCallSkill = false;
                  for (EffectScope scope : EffectScope.values()) {
                     List<AbstractEffect> effects = skill.getEffects(scope);
                     if (effects != null) {
                        for (AbstractEffect eff : effects) {
                           if (eff != null && eff.getClass().getSimpleName().contains("CallSkill")) {
                              hasCallSkill = true;
                              break;
                           }
                        }
                     }
                  }
                  if (!hasCallSkill && !player.isAffectedBySkill(skill.getId())) {
                     skill.applyEffects(player, player);
                  }
               }
            }
         }

         player.setCurrentHp((double)player.getMaxHp());
         player.setCurrentMp((double)player.getMaxMp());
         player.setCurrentCp((double)player.getMaxCp());
      }
   }

      private static List<Skill> findOffensiveMagicSkills(Player player) {
      List<Skill> result = new ArrayList<>();
      if (player == null) {
         return result;
      }
      for (Skill skill : player.getAllSkills()) {
         if (findOffensiveMagicSkillCandidate(skill)) {
            result.add(skill);
         }
      }
      return result;
   }

   private static boolean findOffensiveMagicSkillCandidate(Skill skill) {
      if (skill != null && !skill.isPassive() && skill.isActive() && !skill.isToggle() && skill.isMagic() && !isForbiddenFakeSupportUtilitySkill(skill) && !isFakePartySummonSkill(skill) && !skill.hasEffectType(EffectType.TELEPORT, EffectType.TELEPORT_TO_TARGET)) {
         if (!skill.getBuffType().isBuff() && !skill.getBuffType().isDance() && !skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT)) {
            TargetType targetType = skill.getTargetType();
            boolean hostileTarget = targetType == TargetType.ENEMY || targetType == TargetType.ENEMY_ONLY || skill.hasNegativeEffect();
            boolean damageEffect = skill.hasEffectType(EffectType.MAGICAL_ATTACK, EffectType.MAGICAL_DMG_OVER_TIME, EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT, EffectType.HP_DRAIN, EffectType.DEATH_LINK);
            return hostileTarget || damageEffect;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

      // 🔥【替換位置 2】：修正輔助職業不被誤判為法師、放寬物理技能限制、加入動態連招與 AOE 判斷
   private static boolean isMagicCombatClass(Player player) {
      if (player == null) {
         return false;
      }
      PlayerClass playerClass = player.getPlayerClass();
      if (playerClass != null && (playerClass.isMage() || playerClass.isSummoner())) {
         return true;
      }
      // 只有真正擁有攻擊魔法的職業才走法師邏輯，避免拿雙刀的伊斯輔助發呆
      return !findOffensiveMagicSkills(player).isEmpty();
   }

     private static boolean isPhysicalOffensiveSkill(Skill skill) {
      if (skill == null || skill.isPassive() || !skill.isActive() || skill.isToggle() || skill.isMagic()) {
         return false;
      }
      if (isCrashRiskSkill(skill) || isForbiddenFakeSupportUtilitySkill(skill) || isFakePartySummonSkill(skill)) {
         return false;
      }
      if (skill.getBuffType().isBuff() || skill.getBuffType().isDance()) {
         return false;
      }
      TargetType tt = skill.getTargetType();
      boolean hostile = (tt == TargetType.ENEMY || tt == TargetType.ENEMY_ONLY || skill.hasNegativeEffect());
      return hostile && skill.hasEffectType(
         EffectType.PHYSICAL_ATTACK,
         EffectType.PHYSICAL_ATTACK_HP_LINK,
         EffectType.DEATH_LINK,
         EffectType.HP_DRAIN
      );
   }

   private static List<Skill> findOffensiveCombatSkills(Player player) {
      List<Skill> magicSkills = new ArrayList<>();
      List<Skill> physicalSkills = new ArrayList<>();
      if (player == null) {
         return magicSkills;
      }

      for (Skill skill : player.getAllSkills()) {
         if (skill != null && !skill.isPassive() && skill.isActive() && !skill.isToggle() && !isCrashRiskSkill(skill)) {
            if (skill.isMagic()) {
               if (findOffensiveMagicSkillCandidate(skill)) {
                  magicSkills.add(skill);
               }
            } else if (isPhysicalOffensiveSkill(skill)) {
               physicalSkills.add(skill);
            }
         }
      }

      if (isMagicCombatClass(player) && !magicSkills.isEmpty()) {
         return magicSkills;
      }
      return physicalSkills.isEmpty() ? magicSkills : physicalSkills;
   }

   private static int calculateDynamicSkillScore(Player player, Skill skill) {
      int score = skill.getLevel() * 100;

      // 1. 避免無限重複放同一招，強制形成連招循環
      if (LAST_OFFENSIVE_SKILL.getOrDefault(player, -1) == skill.getId()) {
         score -= 150000;
      }

      // 2. 冷卻時間 >= 3 秒的通常為高傷大招或破防技，優先施放
      if (skill.getReuseDelay() >= 3000) {
         score += 30000;
      }

            // 3. 範圍技（AOE）智慧切換：每 1.5 秒快取一次周圍怪數，避免單回合多招範圍技重複掃描地圖
      if (skill.getAffectScope() == AffectScope.RANGE || skill.getAffectScope() == AffectScope.FAN) {
         long now = System.currentTimeMillis();
         int nearbyMonsters;
         if (now >= NEXT_AOE_SCAN_AT.getOrDefault(player, 0L)) {
            nearbyMonsters = World.getVisibleObjectsInRange(player, Monster.class, 300, m -> m != null && !m.isDead()).size();
            CACHED_AOE_MONSTER_COUNT.put(player, nearbyMonsters);
            NEXT_AOE_SCAN_AT.put(player, now + 1500L);
         } else {
            nearbyMonsters = CACHED_AOE_MONSTER_COUNT.getOrDefault(player, 1);
         }
         if (nearbyMonsters >= 2) {
            score += 80000;
         } else {
            score -= 50000;
         }
      }

      if (skill.isMagic()) {
         score += 200000;
      }
      return score;
   }

   private static boolean tryUseOffensiveCombatSkill(Player player, Monster monster) {
      if (player == null || monster == null || monster.isDead() || !monster.isSpawned()) {
         return false;
      }
      if (player.isCastingNow()) {
         return true;
      }

      List<Skill> skills = findOffensiveCombatSkills(player);
      if (skills.isEmpty()) {
         prepareCombatFakePlayer(player);
         skills = findOffensiveCombatSkills(player);
      }
      if (skills.isEmpty()) {
         return false;
      }

      // 每次出手前根據當前戰況（單體/群怪、上一招施放紀錄）動態排序技能
      skills.sort((a, b) -> Integer.compare(calculateDynamicSkillScore(player, b), calculateDynamicSkillScore(player, a)));

      player.setTarget(monster);
      for (Skill skill : skills) {
         if (skill != null && !player.isSkillDisabled(skill)) {
            double castRange = skill.isMagic()
               ? Math.max(150.0, player.getMagicalAttackRange(skill))
               : Math.max(80.0, skill.getCastRange());

            if (player.calculateDistance3D(monster) > castRange) {
               player.getAI().setIntentionMoveTo(new Location(monster.getX(), monster.getY(), monster.getZ()));
               return true;
            }

            player.setCurrentMp(player.getMaxMp());
            player.rechargeShots(true, true, false);
            if (SkillCaster.castSkill(player, monster, skill, null, SkillCastingType.NORMAL, false, false) != null) {
               LAST_OFFENSIVE_SKILL.put(player, skill.getId());
               return true;
            }
         }
      }
      return false;
   }

                  private static ItemGrade getDesiredFakeGrade(Player player) {
      return ItemGrade.R;
   }

   private static int getDesiredFakeEnchant(Player player) {
      return 10;
   }

   private static void applyFixedBGradeEquipment(Player player) {
      if (player != null && !player.isDead() && !isShopPlayer(player)) {
         PlayerClass playerClass = player.getPlayerClass();
         int desiredEnchant = getDesiredFakeEnchant(player);
         boolean highTier = player.getLevel() >= 95;

         for (Item equipped : new ArrayList<>(player.getInventory().getPaperdollItems())) {
            if (equipped != null && (equipped.isWeapon() || equipped.isArmor())) {
               BodyPart bodyPart = BodyPart.fromItem(equipped);
               if (bodyPart != null) {
                  player.getInventory().unEquipItemInBodySlot(bodyPart);
               }
               player.getInventory().destroyItem(ItemProcessType.DESTROY, equipped, equipped.getCount(), player, null);
            }
         }

                  String className = playerClass != null ? playerClass.name().toLowerCase() : "";
         boolean isMagic = playerClass != null && (playerClass.isMage() || playerClass.isSummoner() || className.contains("bishop") || className.contains("cardinal") || className.contains("elder") || className.contains("saint") || className.contains("prophet") || className.contains("hierophant") || className.contains("dominator") || className.contains("doomcryer") || className.contains("aeore") || className.contains("feoh") || className.contains("wynn"));
         boolean isLight = !isMagic && (className.contains("rogue") || className.contains("dagger") || className.contains("archer") || className.contains("ranger") || className.contains("hawkeye") || className.contains("sagittarius") || className.contains("warg") || className.contains("samurai") || className.contains("tyrant") || className.contains("grand") || className.contains("trickster") || className.contains("berserker") || className.contains("doombringer") || className.contains("soul_") || className.contains("inspector") || className.contains("judicator") || className.contains("kamael") || className.contains("othell") || className.contains("yul_") || className.contains("shine"));

         int[] armorSetIds;
         if (isMagic) {
            armorSetIds = highTier ? new int[]{17441, 17442, 17443, 17444, 17445} : new int[]{17315, 17316, 17317, 17318, 17319};
         } else if (isLight) {
            armorSetIds = highTier ? new int[]{17436, 17437, 17438, 17439, 17440} : new int[]{17310, 17311, 17312, 17313, 17314};
         } else {
            armorSetIds = highTier ? new int[]{17430, 17431, 17432, 17433, 17434} : new int[]{17304, 17305, 17306, 17307, 17308};
         }

         for (int armorId : armorSetIds) {
            ItemTemplate tpl = ItemData.getInstance().getTemplate(armorId);
            if (tpl != null) {
               equipGeneratedArmor(player, tpl, desiredEnchant);
            }
         }
		 
         ensureCombatCloak(player);
         ensureCombatJewels(player);
         ensureCombatWeapon(player);
		 

         if (playerClass != null) {
            player.broadcastUserInfo();
         }
      }
   }
      private static void ensureCombatJewels(Player player) {
      if (player == null || player.isDead() || isShopPlayer(player)) {
         return;
      }

      int desiredEnchant = getDesiredFakeEnchant(player);
      boolean highTier = player.getLevel() >= 95;

      // 依等級選擇 赫利奧斯(95+) 或 鎮魂(85~94) 的 [項鍊, 耳環1, 耳環2, 戒指1, 戒指2]
      int[] jewelIds = highTier
         ? new int[]{18099, 18100, 18100, 18101, 18101}
         : new int[]{17623, 17624, 17624, 17625, 17625};

      for (int jewelId : jewelIds) {
         ItemTemplate tpl = ItemData.getInstance().getTemplate(jewelId);
         if (tpl != null) {
            equipGeneratedArmor(player, tpl, desiredEnchant);
         }
      }
   }
           private static void ensureCombatCloak(Player player) {
      if (player == null || player.isDead() || isShopPlayer(player) || player.getLevel() < 85) {
         return;
      }

      PlayerClass playerClass = player.getPlayerClass();
      String className = playerClass != null ? playerClass.name().toLowerCase() : "";
      int cloakId = 30311; // 預設給帥氣的紅色提爾斗篷（適合閃耀使者/矮人/新職業）

      if (className.contains("sigel") || className.contains("knight") || className.contains("templar")) {
         cloakId = 30310; // 席格爾斗篷
      } else if (className.contains("othell") || className.contains("adventurer") || className.contains("wind_rider") || className.contains("ghost_hunter") || className.contains("fortune_seeker")) {
         cloakId = 30312; // 歐瑟斗篷
      } else if (className.contains("yul_") || className.contains("sagittarius") || className.contains("moonlight_sentinel") || className.contains("ghost_sentinel") || className.contains("trickster")) {
         cloakId = 30313; // 尤爾斗篷
      } else if (className.contains("feoh") || className.contains("archmage") || className.contains("soultaker") || className.contains("mystic_muse") || className.contains("storm_screamer") || className.contains("soul_hound")) {
         cloakId = 30314; // 費歐斗篷
      } else if (className.contains("iss_") || className.contains("hierophant") || className.contains("sword_muse") || className.contains("spectral_dancer") || className.contains("dominator") || className.contains("doomcryer")) {
         cloakId = 30315; // 伊斯斗篷
      } else if (className.contains("wynn") || className.contains("arcana_lord") || className.contains("elemental_master") || className.contains("spectral_master")) {
         cloakId = 30316; // 維因斗篷
      } else if (className.contains("aeore") || className.contains("cardinal") || className.contains("eva_saint") || className.contains("shillien_saint")) {
         cloakId = 30317; // 艾羅斗篷
      }

      ItemTemplate cloakTpl = ItemData.getInstance().getTemplate(cloakId);
      if (cloakTpl != null) {
         // 1. 先嘗試正常穿戴
         if (!equipGeneratedArmor(player, cloakTpl, 0)) {
            // 2. 若因 14.1 新職業（如閃耀使者）被舊職業條件擋住，直接生成並強制裝備到背部欄位！
                        org.l2jmobius.gameserver.entity.item.instance.Item item = player.addItem(ItemProcessType.REWARD, cloakId, 1, player, false);
            if (item != null) {
               player.getInventory().equipItem(item);
            }
         }
      }
   }

      private static void ensureCombatWeapon(Player player) {
      if (player != null && !player.isDead() && !isShopPlayer(player)) {
         int desiredEnchant = getDesiredFakeEnchant(player);
         boolean highTier = player.getLevel() >= 95;
         Item activeWeaponInstance = player.getActiveWeaponInstance();
         if (activeWeaponInstance != null && activeWeaponInstance.getEnchantLevel() == desiredEnchant && activeWeaponInstance.getTemplate().getItemGrade().name().startsWith("R")) {
            return;
         }

         PlayerClass playerClass = player.getPlayerClass();
         String className = playerClass != null ? playerClass.name().toLowerCase() : "";
         int weaponId;

         // 1. 伊斯輔助系（吟遊詩人 / 劍舞 / 戰狂 / 霸主）-> 拿雙刀（帥氣且可放歌舞技能）
         if (className.contains("iss_") || className.contains("enchanter") || className.contains("duelist") || className.contains("gladiator") || className.contains("bladedancer") || className.contains("swordsinger") || className.contains("spectral_dancer") || className.contains("sword_muse")) {
            weaponId = highTier ? 17427 : 17978; // 赫利奧斯雙刀 / 祝福的鎮魂雙刀
         }
         // 2. 純法師 / 召喚師 / 補師 / 戰狂霸主 -> 拿雙手魔杖
else if (playerClass != null && (playerClass.isMage() || playerClass.isSummoner() || className.contains("feoh") || className.contains("wynn") || className.contains("aeore") || className.contains("bishop") || className.contains("cardinal") || className.contains("elder") || className.contains("saint") || className.contains("prophet") || className.contains("hierophant") || className.contains("dominator") || className.contains("doomcryer") || className.contains("overlord") || className.contains("warcryer"))) {
   weaponId = highTier ? 17426 : 17977; // 赫利奧斯報恨者 / 祝福的鎮魂報恨者
         }
		 
         // 3. 14.1 新職業（死亡騎士 / 武士 / 閃耀使者）-> 拿單手劍
         else if (player.isDeathKnight() || player.isSamurai() || player.isShineMaker() || className.contains("death") || className.contains("samurai") || className.contains("shine")) {
            weaponId = highTier ? 17417 : 17968; // 赫利奧斯切刃 / 祝福的鎮魂切刃
         }
         // 4. 狼人 / 暴君（提爾卡巴塔里）-> 拿拳套
         else if (player.isWarg() || className.contains("warg") || className.contains("tyrant") || className.contains("khavatari")) {
            weaponId = highTier ? 17420 : 17971; // 赫利奧斯戰爪 / 祝福的鎮魂戰爪
         }
         // 5. 弓手系（尤爾射手 / 鷹眼 / 銀月 / 暗影）-> 拿弓 + 補 R 級箭矢
         else if (className.contains("yul_") || className.contains("archer") || className.contains("ranger") || className.contains("hawkeye") || className.contains("sagittarius") || className.contains("sentinel") || className.contains("trickster") || className.contains("arbalester")) {
            weaponId = highTier ? 17422 : 17973; // 赫利奧斯投弓 / 祝福的鎮魂投弓
            if (player.getInventory().getInventoryItemCount(18550, -1) < 1000) {
               player.getInventory().addItem(ItemProcessType.REWARD, 18550, 10000, player, null);
            }
         }
         // 6. 刺客系（歐瑟盜賊 / 寶藏 / 大地 / 深淵）-> 拿匕首
         else if (className.contains("othell") || className.contains("dagger") || className.contains("rogue") || className.contains("adventurer") || className.contains("wind") || className.contains("ghost")) {
            weaponId = highTier ? 17416 : 17967; // 赫利奧斯鉋刀 / 祝福的鎮魂鉋刀
         }
         // 7. 長槍系（提爾巨匠 / 傭兵）-> 拿長槍
         else if (className.contains("dreadnought") || className.contains("warlord") || className.contains("pole")) {
            weaponId = highTier ? 17421 : 17972; // 赫利奧斯風暴者 / 祝福的鎮魂風暴者
         }
         // 8. 雙手巨劍系（提爾泰坦 / 破壞者 / 末日狂戰士）-> 拿雙手劍
else if (className.contains("titan") || className.contains("destroyer") || className.contains("berserker") || className.contains("doombringer")) {
   weaponId = highTier ? 17418 : 17969; // 赫利奧斯殺刃 / 祝福的鎮魂殺刃
         }
         // 9. 預設單手劍（席格爾騎士 / 戰士通用）
         else {
            weaponId = highTier ? 17417 : 17968;
         }

         if (activeWeaponInstance != null) {
            BodyPart bodyPart = BodyPart.fromItem(activeWeaponInstance);
            if (bodyPart != null) {
               player.getInventory().unEquipItemInBodySlot(bodyPart);
            }
            player.getInventory().destroyItem(ItemProcessType.DESTROY, activeWeaponInstance, activeWeaponInstance.getCount(), player, null);
         }

         ItemTemplate weaponTpl = ItemData.getInstance().getTemplate(weaponId);
         if (weaponTpl != null) {
            equipGeneratedEquipment(player, weaponTpl);
         }
      }
   }

   private static boolean isValidNormalArmor(Armor armor, ItemGrade desiredGrade, BodyPart bodyPart) {
      return armor != null
         && armor.getItemGrade() == desiredGrade
         && armor.getBodyPart() == bodyPart
         && armor.isEquipable()
         && !armor.isQuestItem()
         && armor.isSellable()
         && armor.isDropable()
         && armor.isTradeable()
         && !containsBlockedEquipmentName(armor.getName());
   }

   private static boolean equipGeneratedArmor(Player player, ItemTemplate template, int enchantLevel) {
      if (player != null && template != null) {
         Item item = player.getInventory().addItem(ItemProcessType.REWARD, template.getId(), 1L, player, (Object)null);
         if (item != null && item.isEquipable() && player.canUseEquipment(item)) {
            item.setEnchantLevel(enchantLevel);
            player.getInventory().equipItem(item);
            return true;
         } else {
            if (item != null) {
               player.getInventory().destroyItem(ItemProcessType.DESTROY, item, item.getCount(), player, (Object)null);
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static PlayerClass findNativeInitialEquipmentClass(PlayerClass playerClass) {
      for(PlayerClass current = playerClass; current != null; current = current.getParent()) {
         if (InitialEquipmentData.getInstance().getClassEquipment(current) != null) {
            return current;
         }
      }

      return playerClass;
   }

   private static Set<ArmorType> getPreferredArmorTypes(Player player) {
      Set<ArmorType> result = new HashSet();
      if (player != null && player.getPlayerClass() != null) {
         PlayerClass playerClass = player.getPlayerClass();
         if (!playerClass.isMage() && !playerClass.isSummoner()) {
            Collection<InitialEquipment> equipment = InitialEquipmentData.getInstance().getClassEquipment(findNativeInitialEquipmentClass(playerClass));
            if (equipment != null) {
               for(InitialEquipment holder : equipment) {
                  if (holder.isEquipped()) {
                     ItemTemplate item = ItemData.getInstance().getTemplate(holder.getId());
                     if (item != null && item.isArmor()) {
                        ItemType var8 = item.getItemType();
                        if (var8 instanceof ArmorType) {
                           ArmorType armorType = (ArmorType)var8;
                           if (armorType != ArmorType.SHIELD) {
                              result.add(armorType);
                           }
                        }
                     }
                  }
               }
            }

            if (result.isEmpty()) {
               result.add(ArmorType.HEAVY);
            }

            return result;
         } else {
            result.add(ArmorType.MAGIC);
            return result;
         }
      } else {
         return result;
      }
   }

      private static Set<WeaponType> getPreferredWeaponTypes(Player player) {
      Set<WeaponType> result = new HashSet<>();
      if (player != null && player.getPlayerClass() != null) {
         PlayerClass playerClass = player.getPlayerClass();
         String className = playerClass.name().toLowerCase();

         // 1. 法師、召喚師、輔助系（包含半獸人霸主 OVERLORD、戰狂 DOOMCRYER、主教、長老等）-> 強制拿雙手魔杖
         if (playerClass.isMage() || playerClass.isSummoner() || isSupportClass(player) || className.contains("overlord") || className.contains("dominator") || className.contains("warcryer") || className.contains("doomcryer")) {
            result.add(WeaponType.BLUNT);
            return result;
         }
          
         // 1.5 14.1 新職業專屬武器限制
         if (player.isDeathKnight() || player.isSamurai() || player.isShineMaker() || className.contains("death") || className.contains("samurai") || className.contains("shine")) {
            result.add(WeaponType.SWORD);
            return result;
         }
         if (player.isWarg() || className.contains("warg")) {
            result.add(WeaponType.FIST);
            return result;
         }

         // 2. 弓手系 -> 強制拿弓，並自動補 10000 支箭矢防罰站
                  if (className.contains("archer") || className.contains("ranger") || className.contains("hawkeye") || className.contains("sagittarius") || className.contains("sentinel") || className.contains("trickster") || className.contains("arbalester")) {
            result.add(WeaponType.BOW);
            if (player.getInventory().getInventoryItemCount(18550, -1) < 1000) {
               player.getInventory().addItem(ItemProcessType.REWARD, 18550, 10000, player, null);
            }
            return result;
         }

         // 3. 其餘物理戰士系 -> 維持原本的新手武器偏好判定
         Collection<InitialEquipment> equipment = InitialEquipmentData.getInstance().getClassEquipment(findNativeInitialEquipmentClass(playerClass));
         if (equipment != null) {
            for (InitialEquipment holder : equipment) {
               if (holder.isEquipped()) {
                  ItemTemplate item = ItemData.getInstance().getTemplate(holder.getId());
                  if (item != null && item.isWeapon()) {
                     ItemType var8 = item.getItemType();
                     if (var8 instanceof WeaponType) {
                        WeaponType weaponType = (WeaponType) var8;
                        result.add(weaponType);
                     }
                  }
               }
            }
         }
      }
      return result;
    }

   private static boolean containsBlockedEquipmentName(String name) {
      if (name == null) {
         return true;
      } else {
         String value = name.toLowerCase();
         return value.contains("package") || value.contains("box") || value.contains("event") || value.contains("unused") || value.contains("not used") || value.contains("未供給") || value.contains("不使用") || value.contains("未使用");
      }
   }

   private static boolean equipCompatibleWeapon(Player player, List<ItemTemplate> candidates) {
      if (player != null && candidates != null && !candidates.isEmpty()) {
         List<ItemTemplate> pool = new ArrayList(candidates);
         ItemTemplate current = player.getActiveWeaponItem();
         if (current != null) {
            pool.removeIf((item) -> item.getId() == current.getId());
         }

         while(!pool.isEmpty()) {
            ItemTemplate candidate = (ItemTemplate)pool.remove(Rnd.get(pool.size()));
            if (equipGeneratedEquipment(player, candidate)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean equipGeneratedEquipment(Player player, ItemTemplate template) {
      if (player != null && template != null) {
                  Item item = player.getInventory().addItem(ItemProcessType.REWARD, template.getId(), 1L, player, (Object)null);
         if (item != null && item.isEquipable()) {
            item.setEnchantLevel(getDesiredFakeEnchant(player));
            player.getInventory().equipItem(item);
            Weapon activeWeapon = player.getActiveWeaponItem();
            return activeWeapon != null && activeWeapon.getId() == item.getId();
         } else {
            if (item != null) {
               player.getInventory().destroyItem(ItemProcessType.DESTROY, item, item.getCount(), player, (Object)null);
            }

            return false;
         }
      } else {
         return false;
      }
   }

            private static boolean isSuitableHuntingMonster(Player player, Monster monster) {
      if (monster == null || !monster.isSpawned() || monster.isDead() || monster.isFakePlayer()
         || monster.isRaid() || monster.isRaidMinion() || monster.isQuestMonster()) {
         return false;
      }
      if (monster.getInstanceId() != player.getInstanceId()) {
         return false;
      }
            // 1. 強制過濾所有超出地圖合法邊界（X/Y/Z）與水下的怪物（避開 Y < -150000 的極北無地形區域）
      int x = monster.getX();
      int y = monster.getY();
      int z = monster.getZ();
      if (x < -120000 || x > 190000 || y < -150000 || y > 240000 || z < -5000 || z > 8000) {
         return false;
      }
      if (monster.isInsideZone(org.l2jmobius.gameserver.entity.zone.ZoneId.WATER)) {
         return false;
      }
      // 2. 組隊跟隨狀態下，只要座標合法就直接允許打怪
      if (player.isInParty()) {
         return true;
      }
      // 3. 單人假人限制等級差尋怪
      if (monster.getLevel() > 115) {
         return false;
      }
      int level = player.getLevel();
      int diff = monster.getLevel() - level;
      return diff >= FakePlayersConfig.FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF && diff <= FakePlayersConfig.FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF;
   }

   private static Monster findSuitableMonsterNearby(Player player, int range) {
      List<Monster> monsters = World.<Monster>getVisibleObjectsInRange(player, Monster.class, range, (m) -> isSuitableHuntingMonster(player, m));
      return monsters.isEmpty() ? null : chooseBestHuntingMonster(player, monsters);
   }

   private static Monster findGlobalSuitableMonster(Player player) {
      if (player == null) {
         return null;
      } else {
         List<Monster> candidates = getMonsterSnapshot().stream().filter((monster) -> isSuitableHuntingMonster(player, monster)).toList();
         return candidates.isEmpty() ? null : chooseBestHuntingMonster(player, candidates);
      }
   }

   private static Monster chooseBestHuntingMonster(Player player, List<Monster> monsters) {
      if (player != null && !monsters.isEmpty()) {
         int playerLevel = player.getLevel();
         List<Monster> preferred = monsters.stream().filter((m) -> m.getLevel() >= playerLevel).toList();
         List<Monster> candidates = preferred.isEmpty() ? monsters : preferred;
         double nearestDistance = Double.MAX_VALUE;

         for(Monster monster : candidates) {
            if (isAnyNormalCombatMonster(monster, player)) {
               double distance = player.calculateDistance3D(monster);
               if (distance < nearestDistance) {
                  nearestDistance = distance;
               }
            }
         }

         if (nearestDistance == Double.MAX_VALUE) {
            return null;
         } else {
            double localLimit = Math.max(nearestDistance + (double)1200.0F, nearestDistance * (double)1.75F);
            Monster best = null;
            int bestLoad = Integer.MAX_VALUE;
            double bestDistance = Double.MAX_VALUE;

            for(Monster monster : candidates) {
               if (isAnyNormalCombatMonster(monster, player)) {
                  double distance = player.calculateDistance3D(monster);
                  if (!(distance > localLimit)) {
                     int load = (Integer)TARGET_OCCUPANCY.getOrDefault(monster.getObjectId(), 0);
                     if (load < bestLoad || load == bestLoad && distance < bestDistance || load == bestLoad && Math.abs(distance - bestDistance) < (double)250.0F && Rnd.get(2) == 0) {
                        best = monster;
                        bestLoad = load;
                        bestDistance = distance;
                     }
                  }
               }
            }

            return best;
         }
      } else {
         return null;
      }
   }

   private static void rebuildTargetOccupancy() {
      TARGET_OCCUPANCY.clear();

      for(Player player : PLAYERS) {
         if (player != null && !player.isDead() && player.isSpawned() && !isShopPlayer(player)) {
            WorldObject var3 = player.getTarget();
            if (var3 instanceof Monster) {
               Monster monster = (Monster)var3;
               if (isAnyNormalCombatMonster(monster, player)) {
                  TARGET_OCCUPANCY.merge(monster.getObjectId(), 1, Integer::sum);
               }
            }
         }
      }

   }

   private static void reserveMonsterTarget(Player player, Monster monster) {
      if (player != null && monster != null) {
         WorldObject oldTarget = player.getTarget();
         if (oldTarget instanceof Monster) {
            Monster oldMonster = (Monster)oldTarget;
            if (oldMonster != monster) {
               TARGET_OCCUPANCY.computeIfPresent(oldMonster.getObjectId(), (id, count) -> count <= 1 ? null : count - 1);
            }
         }

         if (oldTarget != monster) {
            TARGET_OCCUPANCY.merge(monster.getObjectId(), 1, Integer::sum);
         }

         player.setTarget(monster);
      }
   }

   private static void configureAutomaticPotion(Player player) {
      if (player != null && player.isFakePlayer()) {
         if (player.getCurrentHp() < (double)player.getMaxHp() * 0.55) {
            player.setCurrentHp((double)player.getMaxHp());
         }

         player.setCurrentMp((double)player.getMaxMp());
      }
   }

      private static boolean manageLootAndSupplies(Player player) {
      if (player == null || player.isDead() || isShopPlayer(player)) {
         return false;
      }
            configureAutomaticPotion(player);

      // 若設定檔未開啟撿物，或假人正在施法中，則不執行撿物
      if (!FakePlayersConfig.FAKE_PLAYER_CAN_PICKUP || player.isCastingNow() || player.isMovementDisabled()) {
         return false;
      }

      // 每 2.5 秒掃描一次地面掉落物，大幅降低無掉落物時的周邊物件遍歷開銷
      long now = System.currentTimeMillis();
      if (now < NEXT_LOOT_CHECK_AT.getOrDefault(player, 0L)) {
         return false;
      }
      NEXT_LOOT_CHECK_AT.put(player, now + 2500L);

      // 搜尋範圍內的地面掉落物 (Item)
      List<Item> droppedItems = World.getVisibleObjectsInRange(player, Item.class, FakePlayersConfig.FAKE_PLAYER_LOOT_RANGE, item ->
         item != null && item.isSpawned() && item.getInstanceId() == player.getInstanceId()
         && (item.getOwnerId() == 0 || item.getOwnerId() == player.getObjectId())
      );

      if (droppedItems.isEmpty()) {
         return false;
      }

      // 找出距離最近的掉落物
      Item nearestItem = droppedItems.stream()
         .min((a, b) -> Double.compare(player.calculateDistance3D(a), player.calculateDistance3D(b)))
         .orElse(null);

      if (nearestItem == null) {
         return false;
      }

            double distance = player.calculateDistance3D(nearestItem);
      if (distance <= FakePlayersConfig.FAKE_PLAYER_PICKUP_DISTANCE) {
         player.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.GetItem(nearestItem, player.getObjectId()));
         if (player.isInParty() && player.getParty() != null) {
            Player leader = player.getParty().getLeader();
            if (leader != null && !leader.isFakePlayer() && leader.isOnline()) {
               if (nearestItem.getId() == 57) {
                    player.getParty().distributeAdena(leader, nearestItem.getCount(), leader);
               } else {
                  player.getParty().distributeItem(leader, nearestItem);
               }
               nearestItem.decayMe();
               return true;
            }
         }
         // 單人假人：維持直接銷毀，不寫入資料庫
         nearestItem.decayMe();
         return true;
      } else {
         // 距離較遠：先走過去掉落物旁邊
         player.getAI().setIntentionMoveTo(new Location(nearestItem.getX(), nearestItem.getY(), nearestItem.getZ()));
         return true;
      }
   }

   private static boolean isInsideTown(Player player, Location town) {
      return player.calculateDistance3D(town) < (double)2500.0F;
   }

   private static void runThinkSafely() {
      try {
         think();
      } catch (Throwable t) {
         LOGGER.log(Level.WARNING, "Fake player AI watchdog recovered from an unexpected error.", t);
      }

   }

           private static void syncFakePlayersToPartyMatchingList() {
      // 1. 清理已經解散或隊長離線的假人組隊房間
      java.util.Map<Integer, org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom> existingRooms = MatchingRoomManager.getInstance().getPartyMathchingRooms();
      int activeFakeRooms = 0;
      if (existingRooms != null) {
         for (org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom room : new ArrayList<>(existingRooms.values())) {
            Player roomLeader = room.getLeader();
            if (roomLeader != null && roomLeader.isFakePlayer()) {
               if (!roomLeader.isOnline() || !roomLeader.isSpawned() || roomLeader.isDead() || !roomLeader.isInParty()) {
                  room.disbandRoom();
               } else {
                  ++activeFakeRooms;
               }
            }
         }
      }

      // 2. 同步未組隊假人至等待名單（90 名單身假人），並將已組隊的假人隊長註冊成大廳隊伍房間（上限 15 隊）
      for (Player player : PLAYERS) {
         if (player == null || !player.isOnline() || !player.isSpawned()) {
            continue;
         }
         if (isShopPlayer(player)) {
            MatchingRoomManager.getInstance().removeFromWaitingList(player);
            continue;
         }

         if (player.isInParty() && player.getParty() != null) {
            // 已經在隊伍裡的假人，從「單人等待名單」移除
            MatchingRoomManager.getInstance().removeFromWaitingList(player);

            // 如果是該假人小隊的隊長，且尚未建立大廳房間、目前房間數未滿 15 隊 -> 自動建立組隊大廳房間！
            Party party = player.getParty();
            if (party.getLeader() == player && player.getMatchingRoom() == null && activeFakeRooms < 15 && party.getMemberCount() < 9) {
               try {
                  String roomTitle = "【L2TOWN】Lv." + player.getLevel() + " 野外自動練功團 (" + party.getMemberCount() + "/9)";
                  org.l2jmobius.gameserver.entity.groups.matching.PartyMatchingRoom room =
                     new org.l2jmobius.gameserver.entity.groups.matching.PartyMatchingRoom(roomTitle, 1, 9, 1, 115, player);
                  for (Player member : party.getMembers()) {
                     if (member != null && member != player && member.getMatchingRoom() == null) {
                        room.addMember(member);
                     }
                  }
                  ++activeFakeRooms;
               } catch (Exception ignored) {
               }
            }
         } else if (player.getMatchingRoom() == null) {
            // 沒有組隊的單身戰鬥假人 -> 放進「等待組隊名單」供真人挑選
            MatchingRoomManager.getInstance().addToWaitingList(player);
         }
      }
   }

      private static void think() {
      long now = System.currentTimeMillis();
      if (now >= NEXT_OCCUPANCY_REBUILD_AT) {
         NEXT_OCCUPANCY_REBUILD_AT = now + 1000L;
         rebuildTargetOccupancy();
      }

           for (Player player : PLAYERS) {
         if (player == null || !player.isSpawned() || isShopPlayer(player)) {
            continue;
         }

         // 🔥 新增：若假人死亡，檢查是否收到祝復（秒起），或自動觸發死亡復活排程（不再永久躺平）
         if (player.isDead()) {
            if (player.isPendingRevive()) {
               // 玩家對假人用了祝復或復活術 -> 0.25秒內自動幫假人按「確認復活」站起來！
               player.reviveAnswer(1);
               player.fullRestore();
               player.getAI().setIntentionActive();
            } else {
               // 一般陣亡 -> 確保觸發自動復活排程
               handleFakePlayerDeath(player);
            }
            continue;
         }

         if (now < NEXT_AI_THINK_AT.getOrDefault(player, 0L)) {
            continue;
         }
         NEXT_AI_THINK_AT.put(player, now + Rnd.get(650, 950));

                          try {
                        // 🔥 已在 prepareCombatFakePlayer 源頭移除坐騎/變身技能，此處僅保留極輕量的狀態防呆
            if (player.isMounted()) {
               player.dismount();
            }
            if (player.isTransformed()) {
               player.stopTransformation(true);
            }

            player.getEffectList().stopEffects(AbnormalType.BOT_PENALTY);
            if (!player.isRunning()) {
               player.setRunning();
            }

            if (player.getWorldRegion() != null && !player.getWorldRegion().isActive()) {
               player.getWorldRegion().setActive(true);
            }
            if (player.isTeleporting()) player.setTeleporting(false, false);
            if (player.isOverloaded()) player.setOverloaded(false);
			                       // 防呆：若假人超出地圖邊界（X/Y 越界，或掉進深淵/飛上高空），立即拉回安全地面並刷新視野
            int px = player.getX();
            int py = player.getY();
            int pz = player.getZ();
            if (px < -130000 || px > 220000 || py < -260000 || py > 260000 || pz < -6000 || pz > 10000) {
               player.setTarget(null);
               Location safeLoc = FALLBACK_HUNTING_GROUNDS[Rnd.get(FALLBACK_HUNTING_GROUNDS.length)];
               player.teleToLocation(safeLoc, true);
               player.revalidateZone(true);
               player.broadcastInfo();
               player.broadcastUserInfo();
               continue;
            }

                        // 1. 定期補充物資、開啟靈魂彈，並同步假人組隊狀態圖示給隊友
            if (now >= NEXT_RESOURCE_SYNC_AT.getOrDefault(player, 0L)) {
               NEXT_RESOURCE_SYNC_AT.put(player, now + RESOURCE_SYNC_INTERVAL_MS);
               prepareFakeConsumables(player);
               broadcastFakePartyEffects(player);
            }

                        // 2. 定期檢查武器是否符合等級，並強制同步外觀封包給周圍玩家
            if (now >= NEXT_EQUIPMENT_CHECK_AT.getOrDefault(player, 0L)) {
               NEXT_EQUIPMENT_CHECK_AT.put(player, now + EQUIPMENT_CHECK_INTERVAL_MS);
               ensureCombatWeapon(player);
               player.broadcastUserInfo();
            }

            // 3. 升級自動偵測
            int currentLevel = player.getLevel();
            Integer lastLevel = LAST_LEVELS.put(player, currentLevel);
            if (lastLevel != null && lastLevel != currentLevel) {
               player.setTarget(null);
               player.setTitle(String.valueOf(currentLevel));
               prepareCombatFakePlayer(player);
               SUPPORT_ROLE_CACHE.remove(player);
               applyFixedBGradeEquipment(player);
               configureAutomaticPotion(player);
            }

            // 4. 清除已死亡的怪物目標，並立即檢查地面掉落物
            if (player.getTarget() instanceof Monster m && m.isDead()) {
               player.setTarget(null);
            }
            if (manageLootAndSupplies(player)) {
               player.getAI().notifyActionThink();
               continue;
            }

            // 5.5 假人自主社交：若尚未入隊且允許組隊，定時邀請周圍孤單假人組成練功小隊
            if (!player.isInParty() && Rnd.get(100) < 15) {
               autoFormFakeParty(player);
            }
		    // 5.6 全方位輔助、歌舞與自體 Buff AI（優先於反擊，確保戰鬥中也能跳滿全隊歌舞）
            boolean needHeal = player.isInParty() && checkPartyNeedsHealing(player);
            if (needHeal || now >= NEXT_PARTY_SUPPORT_AT.getOrDefault(player, 0L)) {
               if (needHeal && player.isAttackingNow()) {
                  player.abortAttack();
               }
               boolean casted = performPartySupport(player);
               NEXT_PARTY_SUPPORT_AT.put(player, now + (casted ? 25000L : 10000L));
               if (casted) {
                  player.getAI().notifyActionThink();
                  continue;
               }
            }

            // 6. 優先反擊正在攻擊自己的怪物
            Monster attacker = null;
            Set<?> attackBy = player.getAttackByList();
            if (attackBy != null && !attackBy.isEmpty()) {
               for (Object obj : attackBy) {
                  if (obj instanceof Monster m && isAnyNormalCombatMonster(m, player)) {
                     attacker = m;
                     break;
                  }
               }
            }
            if (attacker != null) {
               reserveMonsterTarget(player, attacker);
               if (!tryUseOffensiveCombatSkill(player, attacker)) {
                  forceFakePhysicalAttack(player, attacker);
               }
               player.getAI().notifyActionThink();
               continue;
            }

                // 7. 組隊跟隨與集火 AI（優先跟隊長打同一隻；若隊長 5 秒沒打怪則放行自主找怪）
            if (player.isInParty() && player.getParty() != null) {
               Party party = player.getParty();
               Player leader = party.getLeader();

               // 🔥 新增：如果目前隊長是假人，但隊伍裡有真人玩家，立刻自動把隊長移交給真人！
               if (leader != null && leader.isFakePlayer() && leader == player) {
                  for (Player member : party.getMembers()) {
                     if (member != null && !member.isFakePlayer() && member.isOnline()) {
                        party.changePartyLeader(member.getName());
                        leader = member;
                        break;
                     }
                  }
               }

                if (leader != null && leader != player && leader.isSpawned() && !leader.isDead()) {
                  if (leader.isInvisible()) {
                     continue;
                  }
                  // 距離過遠（> 600 碼）或跨副本，交給原本的傳送與跟隨邏輯
                  if (leader.getInstanceId() != player.getInstanceId() || player.calculateDistance3D(leader) > 600.0) {
                     if (handlePartyFollowAndAssist(player)) {
                        player.getAI().notifyActionThink();
                        continue;
                     }
                  }
                  // 隊長 600 碼內：檢查隊長是否正在選取/攻擊活著的怪物
                  if (leader.getTarget() instanceof Monster leaderTarget && !leaderTarget.isDead() && isAnyNormalCombatMonster(leaderTarget, player)) {
                     LEADER_LAST_COMBAT_AT.put(leader, now);
                     PARTY_PATROL_POINTS.remove(player);
                     reserveMonsterTarget(player, leaderTarget);
                     if (!tryUseOffensiveCombatSkill(player, leaderTarget)) {
                        forceFakePhysicalAttack(player, leaderTarget);
                     }
                     player.getAI().notifyActionThink();
                     continue;
                  }
                  // 隊長目前沒選怪：判斷是否已經超過 5 秒（5000ms）沒打怪
                  long lastCombat = LEADER_LAST_COMBAT_AT.getOrDefault(leader, 0L);
                  if (now - lastCombat < 5000L) {
                     // 5 秒緩衝期內：先停手待命跟隨隊長，不亂開其他怪
                     if (player.getTarget() instanceof Monster m && !m.isDead()) {
                        player.setTarget(null);
                        player.getAI().setIntentionActive();
                     }
                     continue;
                  }
                  // 超過 5 秒隊長仍未開怪 -> 自動放行往下執行第 8 步（handleAutonomousHunting）自主找怪！
               }
            }

                       // 8. 自主打怪與全圖找怪傳送
            handleAutonomousHunting(player);
            player.getAI().notifyActionThink();

         } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "AI tick error: " + player.getName(), t);
         }
      }
   }

   private static boolean canPartyMoveNow(Player player) {
      long now = System.currentTimeMillis();
      if (now < NEXT_PARTY_MOVE_AT.getOrDefault(player, 0L)) return false;
      NEXT_PARTY_MOVE_AT.put(player, now + 1500L);
      return true;
   }

   private static boolean handlePartyFollowAndAssist(Player player) {
      if (player == null || !player.isInParty() || player.getParty() == null) return false;
      Player leader = player.getParty().getLeader();
      if (leader == null || leader == player || leader.isFakePlayer() || !leader.isOnline() || !leader.isSpawned() || leader.isDead()) return false;

      // 跨副本自動傳送跟隨隊長
      if (player.getInstanceId() != leader.getInstanceId() && !player.isCastingNow()) {
         player.setInstance(leader.getInstanceWorld());
         player.teleToLocation(new Location(leader.getX() + Rnd.get(-100, 100), leader.getY() + Rnd.get(-100, 100), leader.getZ()), false);
         return true;
      }

      // 距離隊長超過 2500 碼直接傳送至隊長身邊，避免卡地形
      double distToLeader = player.calculateDistance3D(leader);
      if (distToLeader > 2500.0 && !player.isCastingNow()) {
         player.teleToLocation(new Location(leader.getX() + Rnd.get(-120, 120), leader.getY() + Rnd.get(-120, 120), leader.getZ()), true);
         return true;
      }

      // 集火隊長正在攻擊的怪物
      if (leader.getTarget() instanceof Monster leaderTarget && !leaderTarget.isDead() && isAnyNormalCombatMonster(leaderTarget, player)) {
         PARTY_PATROL_POINTS.remove(player);
         reserveMonsterTarget(player, leaderTarget);
         if (!tryUseOffensiveCombatSkill(player, leaderTarget)) {
            forceFakePhysicalAttack(player, leaderTarget);
         }
         return true;
      }

      // 隊長沒打怪時，若假人目標超出隊長周圍 1200 碼則取消目標
      if (player.getTarget() instanceof Monster currentMonster && leader.calculateDistance3D(currentMonster) > 1200.0) {
         player.setTarget(null);
         player.getAI().setIntentionActive();
      }

      // 距離隊長超過 900 碼主動跑向隊長
      if (distToLeader > 900.0 && !player.isMovementDisabled() && !player.isCastingNow()) {
         if (canPartyMoveNow(player)) {
            player.getAI().setIntentionMoveTo(new Location(leader.getX() + Rnd.get(-150, 150), leader.getY() + Rnd.get(-150, 150), leader.getZ()));
         }
         return true;
      }
      return false;
   }

      private static boolean handleAutonomousHunting(Player player) {
      boolean followingRealLeader = player.isInParty() && player.getParty() != null && player.getParty().getLeader() != null && !player.getParty().getLeader().isFakePlayer();
      Player leader = followingRealLeader ? player.getParty().getLeader() : null;
	  // 只要隊長有選取/攻擊活著的怪物，立刻放棄原本的怪，轉火隊長的目標
      if (leader != null && !leader.isInvisible() && leader.getTarget() instanceof Monster leaderTarget && !leaderTarget.isDead() && isAnyNormalCombatMonster(leaderTarget, player)) {
         reserveMonsterTarget(player, leaderTarget);
         if (!tryUseOffensiveCombatSkill(player, leaderTarget)) {
            forceFakePhysicalAttack(player, leaderTarget);
         }
         return true;
      }
      Monster targetMonster = (player.getTarget() instanceof Monster existing && isAnyNormalCombatMonster(existing, player)) ? existing : null;

      if (targetMonster == null) {
         targetMonster = followingRealLeader && leader != null
            ? findSuitableMonsterNearby(leader, 1200)
            : findSuitableMonsterNearby(player, FakePlayersConfig.FAKE_PLAYER_HUNT_SEARCH_RANGE);
      }

      // 周圍找不到怪時的處理
      if (targetMonster == null) {
         if (followingRealLeader && leader != null) {
            // 組隊狀態：在隊長周圍環形走動巡邏
            Location patrol = PARTY_PATROL_POINTS.get(player);
            if (patrol == null || player.calculateDistance3D(patrol) <= 100.0 || leader.calculateDistance3D(patrol) > 500.0) {
               int step = PARTY_PATROL_STEPS.merge(player, 1, Integer::sum);
               double angle = ((player.getObjectId() * 137.508 + step * 67.0) % 360.0) * Math.PI / 180.0;
               patrol = new Location(leader.getX() + (int)(Math.cos(angle) * 280.0), leader.getY() + (int)(Math.sin(angle) * 280.0), leader.getZ());
               PARTY_PATROL_POINTS.put(player, patrol);
            }
            if (player.calculateDistance3D(patrol) > 100.0 && !player.isMovementDisabled() && canPartyMoveNow(player)) {
               player.getAI().setIntentionMoveTo(patrol);
            }
            return true;
         } else {
            // 單人練功：全圖找怪並直接傳送過去（含完整 XYZ 邊界防呆）
            Monster globalTarget = findGlobalSuitableMonster(player);
            if (globalTarget != null) {
               int gx = globalTarget.getX();
               int gy = globalTarget.getY();
               int gz = globalTarget.getZ();
               if (gx >= -130000 && gx <= 220000 && gy >= -260000 && gy <= 260000 && gz > -6000 && gz < 10000) {
                  reserveMonsterTarget(player, globalTarget);
                  player.teleToLocation(new Location(gx, gy, gz + 25), true);
                  player.revalidateZone(true);
                  player.broadcastInfo();
                  player.broadcastUserInfo();
                  player.getAI().setIntentionActive();
               }
            }
            return true;
         }
      }

      reserveMonsterTarget(player, targetMonster);
      if (!tryUseOffensiveCombatSkill(player, targetMonster)) {
         forceFakePhysicalAttack(player, targetMonster);
      }
      return true;
   }

   private static boolean checkPartyNeedsHealing(Player player) {
      List<Player> members = player.getParty().getMembers().stream().filter(m -> m != null && m.isOnline() && m.isSpawned() && !m.isDead()).toList();
      if (members.size() < 2) return false;
      double threshold = FakePlayersConfig.FAKE_PLAYER_SUPPORT_HEAL_PERCENT / 100.0;
      boolean lowHp = members.stream().anyMatch(m -> m.getMaxHp() > 0 && (m.getCurrentHp() / m.getMaxHp()) < threshold);
      if (lowHp) PARTY_HEALING_MODE.put(player, true);
      else if (members.stream().allMatch(m -> m.getCurrentHp() >= m.getMaxHp() * 0.95)) PARTY_HEALING_MODE.remove(player);
      return PARTY_HEALING_MODE.getOrDefault(player, false);
   }

         private static boolean isIssThirtyMinMelody(Skill skill) {
      if (skill == null || skill.getName() == null) {
         return false;
      }
      String name = skill.getName().toLowerCase(Locale.ROOT);
      return name.contains("horn") || name.contains("drum") || name.contains("organ") || name.contains("guitar") || name.contains("harp")
         || name.contains("lute") || name.contains("melody")
         || name.contains("角笛") || name.contains("鼓聲") || name.contains("管風琴") || name.contains("吉他") || name.contains("豎琴") || name.contains("旋律");
		 }	 
		    private static boolean isIssSonataOrHarmony(Skill skill) {
      if (skill == null || skill.getName() == null) {
         return false;
      }
      String name = skill.getName().toLowerCase(Locale.ROOT);
      return name.contains("sonata") || name.contains("harmony") || name.contains("rhapsody")
         || name.contains("奏鳴曲") || name.contains("和聲") || name.contains("狂想曲");
   }
   
    private static boolean performPartySupport(Player player) {
      if (player == null || player.isDead() || player.isCastingNow()) return false;

      List<Player> members = (player.isInParty() && player.getParty() != null)
         ? player.getParty().getMembers().stream().filter(m -> m != null && m.isOnline() && m.isSpawned() && !m.isDead()).toList()
         : List.of(player);

      // 1. 【隊伍治療】優先治療血量低於設定百分比的隊友（支援 HEAL、HP、HEAL_PERCENT 等所有補血類型）
      double threshold = FakePlayersConfig.FAKE_PLAYER_SUPPORT_HEAL_PERCENT / 100.0;
      Player lowest = members.stream().filter(m -> m.getMaxHp() > 0 && (m.getCurrentHp() / m.getMaxHp()) < threshold)
         .min(Comparator.comparingDouble(m -> m.getCurrentHp() / m.getMaxHp())).orElse(null);

      if (lowest != null) {
         for (Skill skill : player.getAllSkills()) {
            if (isUsablePositiveSupportSkill(skill) && !player.isSkillDisabled(skill)
                             && skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL)) {
               Player target = (skill.getAffectScope() == AffectScope.PARTY || skill.getAffectScope() == AffectScope.RANGE) ? player : lowest;
               if (player.calculateDistance3D(target) <= 900.0) {
                  if (player.isAttackingNow()) {
                     player.abortAttack();
                  }
                  player.setCurrentMp(player.getMaxMp());
                  if (SkillCaster.castSkill(player, target, skill, null, SkillCastingType.NORMAL, false, false) != null) return true;
               }
            }
         }
      }

                  // 🔥 移除原本的 if (player.isAttackingNow()) return false; 改為需要放歌舞時才暫停普攻
      long now = System.currentTimeMillis();
      List<Skill> sortedSkills = new ArrayList<>(player.getAllSkills());
            // 優先順序：1. 四大 30 分鐘旋律（角笛/鼓聲/管風琴/吉他/豎琴）最優先 -> 2. 技能 ID 由大到小（4轉奏鳴曲優先）
      sortedSkills.sort((a, b) -> {
         boolean aMelody = isIssThirtyMinMelody(a);
         boolean bMelody = isIssThirtyMinMelody(b);
         if (aMelody != bMelody) {
            return aMelody ? -1 : 1;
         }
         return Integer.compare(b != null ? b.getId() : 0, a != null ? a.getId() : 0);
      });

      for (Skill skill : sortedSkills) {
         if (!isUsablePositiveSupportSkill(skill) || !isPositiveBuffSkill(skill) || player.isSkillDisabled(skill)) {
            continue;
         }

                  // 單一技能 29 分鐘內若已施放過則跳過（防止同類 Buff 互相覆蓋），但若有真人隊友缺少此狀態則立刻補放
         String retryKey = player.getObjectId() + ":" + skill.getId();
         long retryDeadline = SUPPORT_SKILL_RETRY_AT.getOrDefault(retryKey, 0L);

         // 確保有足夠精靈礦石 (ID: 3031) 或魔石 (ID: 1785)
         if (skill.getItemConsumeId() > 0 && player.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()) {
            player.getInventory().addItem(ItemProcessType.REWARD, skill.getItemConsumeId(), 500, player, null);
         }

         AffectScope scope = skill.getAffectScope();
         TargetType targetType = skill.getTargetType();

         // 情況 A：純自體增益技能（29 分鐘冷卻防互蓋）
         if (targetType == TargetType.SELF && scope == AffectScope.SINGLE) {
            if (now >= retryDeadline && !player.isAffectedBySkill(skill.getId()) && LAST_PARTY_SUPPORT_SKILL.getOrDefault(player, -1) != skill.getId()) {
               if (player.isAttackingNow()) player.abortAttack();
               player.setCurrentMp(player.getMaxMp());
               if (SkillCaster.castSkill(player, player, skill, null, SkillCastingType.NORMAL, false, false) != null) {
                  LAST_PARTY_SUPPORT_SKILL.put(player, skill.getId());
                  SUPPORT_SKILL_RETRY_AT.put(retryKey, now + 1740000L);
                  broadcastFakePartyEffects(player);
                  return true;
               }
            }
            continue;
         }

         // 情況 B：君主專屬血盟技能（AffectScope.PLEDGE）-> 直接跳過，避免對無血盟隊友空放罰站
         if (scope == AffectScope.PLEDGE) {
            continue;
         }

         // 情況 C：一般隊伍範圍 Buff / 戰狂 / 歌舞 / 奏鳴曲 / 四大 30 分鐘旋律 / 單體和聲
         if (scope == AffectScope.PARTY || scope == AffectScope.RANGE || isIssSonataOrHarmony(skill)) {
            boolean isMelody = isIssThirtyMinMelody(skill);
            boolean isSonataOrHarmony = isIssSonataOrHarmony(skill);
            int xmlDurationSec = skill.getAbnormalTime();
            long smartCooldownMs = (xmlDurationSec > 75) ? ((xmlDurationSec - 15) * 1000L) : 290000L;

            // 1. 單體「和聲（Harmony）」：逐一幫缺少和聲的隊友施放（且防三種和聲互蓋）
            if (scope == AffectScope.SINGLE && isSonataOrHarmony) {
               if (player.isInParty()) {
                  for (Player m : members) {
                     if (player.calculateDistance3D(m) <= 900.0 && !m.isAffectedBySkill(skill.getId())) {
                        boolean alreadyHasHarmony = m.getEffectList().getEffects().stream()
                           .anyMatch(e -> e != null && e.getSkill() != null && (e.getSkill().getName().toLowerCase(Locale.ROOT).contains("harmony") || e.getSkill().getName().contains("和聲")));
                        if (!alreadyHasHarmony && (now >= retryDeadline)) {
                           SUPPORT_SKILL_RETRY_AT.put(retryKey, now + smartCooldownMs);
                           if (player.isAttackingNow()) player.abortAttack();
                           player.setCurrentMp(player.getMaxMp());
                           if (SkillCaster.castSkill(player, m, skill, null, SkillCastingType.NORMAL, false, false) != null) {
                              broadcastFakePartyEffects(m);
                              return true;
                           }
                        }
                     }
                  }
               }
               continue;
            }

            // 2. 全隊「三大奏鳴曲（前進/鬥志/舒緩）」與「四大旋律」
            boolean anyMissing = members.stream().anyMatch(m -> player.calculateDistance3D(m) <= 900.0 && !m.isAffectedBySkill(skill.getId()));
            boolean realLeaderMissing = members.stream().anyMatch(m -> !m.isFakePlayer() && player.calculateDistance3D(m) <= 900.0 && !m.isAffectedBySkill(skill.getId()));
            boolean shouldCast = (isMelody || isSonataOrHarmony)
               ? (anyMissing || now >= retryDeadline)
               : (anyMissing && (now >= retryDeadline || realLeaderMissing));

                       if (shouldCast) {
               SUPPORT_SKILL_RETRY_AT.put(retryKey, now + smartCooldownMs);
               if (player.isAttackingNow()) player.abortAttack();
               player.setCurrentMp(player.getMaxMp());
               if (SkillCaster.castSkill(player, player, skill, null, SkillCastingType.NORMAL, false, false) != null) {
                  for (Player m : members) {
                     broadcastFakePartyEffects(m);
                  }
                  return true;
               }
            }

            continue;
         }

         // 情況 D：單體指定輔助技能（僅在有組隊時幫隊友施放）
         if (player.isInParty()) {
            for (Player m : members) {
               if (player.calculateDistance3D(m) <= 900.0 && !m.isAffectedBySkill(skill.getId())) {
                  if (now >= retryDeadline || !m.isFakePlayer()) {
                     if (player.isAttackingNow()) player.abortAttack();
                     player.setCurrentMp(player.getMaxMp());
                     if (SkillCaster.castSkill(player, m, skill, null, SkillCastingType.NORMAL, false, false) != null) {
                        SUPPORT_SKILL_RETRY_AT.put(retryKey, now + 1740000L);
                        broadcastFakePartyEffects(m);
                        return true;
                     }
                  }
               }
            }
         }
	  }
      return false;
     }

   public static void forceFakePhysicalAttack(Player player, Monster monster) {
      if (player == null || monster == null || player.isDead() || monster.isDead()) return;
      reserveMonsterTarget(player, monster);
      double distance = player.calculateDistance3D(monster);
      double range = Math.max(100.0, (double)player.getPhysicalAttackRange());
      if (distance > range) {
         if (!player.isMovementDisabled() && !player.isCastingNow() && !player.isMoving()) {
            double radius = Math.min(450.0, Math.max(80.0, range * 0.65));
            double angle = (double)player.getObjectId() * 137.508 % 360.0 * Math.PI / 180.0;
            int x = monster.getX() + (int)Math.round(Math.cos(angle) * radius);
            int y = monster.getY() + (int)Math.round(Math.sin(angle) * radius);
            player.getAI().setIntentionMoveTo(new Location(x, y, monster.getZ()));
         }
      } else {
         if (player.getAI().getIntention() != Intention.ATTACK || player.getAI().getTarget() != monster) {
            player.getAI().setIntentionAttack(monster);
         }
      }
   }

   private static boolean isAnyNormalCombatMonster(Monster monster, Player player) {
      if (monster == null || player == null || monster.isDead() || !monster.isSpawned() || monster.isFakePlayer()) return false;
      if (monster.isRaid() || monster.isRaidMinion()) return false;
      return monster.getInstanceId() == player.getInstanceId();
   }

   private static void prepareFakeConsumables(Player player) {
      if (player == null || !player.isFakePlayer()) return;
      int[] items = {FakePlayersConfig.FAKE_PLAYER_HP_POTION_ID, 1343, 1344, 32252, 32253, 29510};
      for (int id : items) {
         if (player.getInventory().getInventoryItemCount(id, -1) < 500) {
            player.getInventory().addItem(ItemProcessType.REWARD, id, 1000, player, null);
         }
      }
	   activateAllAutoShots(player); // 加上這一行，讓假人自動開啟靈魂彈與魔靈彈
   }

      private static void activateAllAutoShots(Player player) {
      if (player == null || !player.isFakePlayer() || isShopPlayer(player)) return;
      int level = player.getLevel();
      // 依據假人等級只發放對應階級的 1 種靈魂彈 + 1 種祝福魔靈彈，避免負重 100% 卡死不能動
      int soulshotId = 17754;
      int spiritshotId = 19442;

      if (player.getInventory().getInventoryItemCount(soulshotId, -1) < 200) {
         player.getInventory().addItem(ItemProcessType.REWARD, soulshotId, 500, player, null);
      }
      if (player.getInventory().getInventoryItemCount(spiritshotId, -1) < 200) {
         player.getInventory().addItem(ItemProcessType.REWARD, spiritshotId, 500, player, null);
      }
      player.addAutoSoulShot(soulshotId);
      player.addAutoSoulShot(spiritshotId);
      player.rechargeShots(true, true, false);
   }
   
   private static void removeFakeEquipmentGradePenalty(Player player) {
      if (player != null && player.getInventory().getInventoryItemCount(29510, -1) < 1) {
         player.getInventory().addItem(ItemProcessType.REWARD, 29510, 1, player, null);
      }
   }

         private static void randomChat() {
      if (!FakePlayersConfig.FAKE_PLAYER_CHAT) {
         return;
      }
      String[] msg = FakePlayersConfig.FAKE_PLAYER_CHAT_MESSAGES;
      if (msg == null || msg.length == 0) {
         return;
      }
      long now = System.currentTimeMillis();
      long minMs = FakePlayersConfig.FAKE_PLAYER_CHAT_MIN_INTERVAL * 1000L;
      long maxMs = FakePlayersConfig.FAKE_PLAYER_CHAT_MAX_INTERVAL * 1000L;
      for (Player fake : PLAYERS) {
         if (fake != null && fake.isSpawned() && fake.isOnline() && now >= LAST_CHAT.getOrDefault(fake, 0L) && Rnd.get(100) < 22) {
            String text = msg[Rnd.get(msg.length)].trim();
            if (!text.isEmpty()) {
               fake.broadcastPacket(new CreatureSay(fake, ChatType.GENERAL, fake.getName(), text));
               LAST_CHAT.put(fake, now + Rnd.get(minMs, maxMs));
            }
         }
      }
   }

            public static void handlePlayerChat(Player speaker, String targetName, String text, ChatType type) {
      if (speaker == null || speaker.isFakePlayer() || text == null) {
         return;
      }
      String msg = text.trim().toLowerCase();

      // 功能 A：真人在隊伍中，任何頻道喊「集合」或「過來」直接把全隊假人拉到身邊並解除傳送鎖定
      if (speaker.isInParty() && speaker.getParty() != null) {
         if (msg.contains("集合") || msg.contains("過來") || msg.contains("summon")) {
            int summoned = 0;
            for (Player member : speaker.getParty().getMembers()) {
               if (member != null && member != speaker && member.isFakePlayer()) {
                  if (member.isDead()) {
                     member.doRevive();
                     member.fullRestore();
                  }
                  member.abortAttack();
                  member.abortCast();
                  member.setTarget(null);
                  if (member.getInstanceId() != speaker.getInstanceId()) {
                     member.setInstance(speaker.getInstanceWorld());
                  }
                  member.teleToLocation(new Location(speaker.getX() + Rnd.get(-100, 100), speaker.getY() + Rnd.get(-100, 100), speaker.getZ()), true);
                  member.onTeleported();
                  member.revalidateZone(true);
                  member.broadcastInfo();
                  member.broadcastUserInfo();
                  member.getAI().setIntentionActive();
                  // 🔥 強制刷新隊長畫面的組隊小視窗狀態，解決隊友名字變暗灰色的顯示問題
                                    if (speaker.getParty() != null) {
                     speaker.getParty().broadcastToPartyMembers(member, new org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate(member, true));
                  }
                  ++summoned;
               }
            }
            if (summoned > 0) {
               speaker.sendMessage("【系統】已將 " + summoned + " 名假人隊友召集至您的位置。");
            }
            return;
         }
      }

      // 功能 B：真人尚未入隊，喊「111 / 組隊 / 帶我」自動組隊並把隊長移交給真人
      if (!speaker.isInParty() && (msg.contains("111") || msg.contains("組隊") || msg.contains("帶我") || msg.contains("party"))) {
         Player chosenFake = null;

         // 1. 優先尋找附近 1500 碼內的戰鬥假人
         for (Player fake : PLAYERS) {
            if (fake != null && fake.isSpawned() && !fake.isDead() && !isShopPlayer(fake) && fake.calculateDistance3D(speaker) <= 1500.0) {
               if (fake.getParty() == null || fake.getParty().getMemberCount() < 9) {
                  chosenFake = fake;
                  break;
               }
            }
         }

         // 2. 若身邊沒假人，直接從野外挑選一名空閒的戰鬥假人傳送過來支援
         if (chosenFake == null) {
            for (Player fake : PLAYERS) {
               if (fake != null && fake.isSpawned() && !fake.isDead() && !isShopPlayer(fake)) {
                  if (fake.getParty() == null || fake.getParty().getMemberCount() < 9) {
                     chosenFake = fake;
                     break;
                  }
               }
            }
         }

         if (chosenFake != null) {
            Party party = chosenFake.getParty();
            if (party == null) {
               party = new Party(chosenFake, org.l2jmobius.gameserver.entity.groups.PartyDistributionType.FINDERS_KEEPERS);
               chosenFake.setParty(party);
            }
            // 讓真人加入隊伍，並立刻將隊長移交給真人！
            speaker.joinParty(party);
            party.changePartyLeader(speaker.getName());

            // 讓全隊假人隊友立刻傳送到真人隊長身邊就位並解除傳送鎖定
            for (Player member : party.getMembers()) {
               if (member != null && member.isFakePlayer() && !member.isDead()) {
                  member.setTarget(null);
                  if (member.getInstanceId() != speaker.getInstanceId()) {
                     member.setInstance(speaker.getInstanceWorld());
                  }
                  member.teleToLocation(new Location(speaker.getX() + Rnd.get(-120, 120), speaker.getY() + Rnd.get(-120, 120), speaker.getZ()), true);
                  member.onTeleported();
                  member.revalidateZone(true);
                  member.broadcastInfo();
                  member.broadcastUserInfo();
                  member.getAI().setIntentionActive();
               }
            }

            chosenFake.broadcastPacket(new CreatureSay(chosenFake, ChatType.PARTY, chosenFake.getName(), "隊長好！全隊已傳送至你身邊待命，請下達攻擊目標！"));
         }
      }
   }

      private static void autoFormFakeParty(Player leader) {
      if (leader == null || leader.isDead() || isShopPlayer(leader) || !FakePlayersConfig.FAKE_PLAYER_ALLOW_PARTY) {
         return;
      }
      if (!AUTO_PARTY_ALLOWED.contains(leader)) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now < NEXT_PARTY_FORM_AT.getOrDefault(leader, 0L)) {
         return;
      }
      NEXT_PARTY_FORM_AT.put(leader, now + 12000L);
      try {
         Party party = leader.getParty();
         int maxMembers = Math.min(9, FakePlayersConfig.FAKE_PLAYER_MAX_PARTY_MEMBERS);
         if (party != null && (!party.isLeader(leader) || party.getMemberCount() >= maxMembers)) {
            return;
         }

         List<Player> nearbyFakes = World.getVisibleObjectsInRange(leader, Player.class, 1200, p -> 
            p != null && p.isFakePlayer() && p != leader && !p.isDead() && !p.isInParty() && !isShopPlayer(p)
            && AUTO_PARTY_ALLOWED.contains(p)
         );

         for (Player candidate : nearbyFakes) {
            if (party == null) {
               party = new Party(leader, org.l2jmobius.gameserver.entity.groups.PartyDistributionType.FINDERS_KEEPERS);
               leader.setParty(party);
            }
            if (party.getMemberCount() < maxMembers) {
               candidate.joinParty(party);
               LOGGER.fine("[假人自主組隊] 假人 " + leader.getName() + " 在獵場邀請了 " + candidate.getName() + " 組成練功小隊。");
            } else {
               break;
            }
         }
      } catch (Exception e) {
         LOGGER.log(Level.FINE, "假人自主組隊異常", e);
      }
   }
              private static boolean manageFakeSummonerServitors(Player player) {
      if (player == null || player.isDead() || player.isCastingNow()) {
         return false;
      }
      int currentCount = player.getServitors().size();
      // 1. 維因召喚師在 14.1 版本最多可同時帶 2 隻使魔（若未滿 2 隻則繼續召喚，最多嘗試 2 隻上限）
      if (currentCount < 2) {
         int[] crystals = {17371, 1462, 1461, 3031};
         for (int cid : crystals) {
            if (player.getInventory().getInventoryItemCount(cid, -1) < 50) {
               player.getInventory().addItem(ItemProcessType.REWARD, cid, 200, player, null);
            }
         }
         for (Skill skill : player.getAllSkills()) {
            if (skill != null && skill.isActive() && !skill.isPassive() && !skill.isToggle() && !player.isSkillDisabled(skill)) {
               String skName = skill.getName() != null ? skill.getName().toLowerCase(Locale.ROOT) : "";
               if (skName.contains("summon") || skName.contains("召喚")) {
                  player.setCurrentMp(player.getMaxMp());
                  if (SkillCaster.castSkill(player, player, skill, null, SkillCastingType.NORMAL, false, false) != null) {
                     return true;
                  }
               }
            }
         }
      }

      // 2. 只要場上有使魔（1 隻或 2 隻），立刻發放使魔彈藥並命令全部使魔一起圍毆目標！
      if (currentCount > 0) {
         int[] beastShots = {6645, 6647};
         for (int shotId : beastShots) {
            if (player.getInventory().getInventoryItemCount(shotId, -1) < 200) {
               player.getInventory().addItem(ItemProcessType.REWARD, shotId, 1000, player, null);
            }
            player.addAutoSoulShot(shotId);
         }

         Monster targetMonster = (player.getTarget() instanceof Monster m && !m.isDead() && m.isSpawned()) ? m : null;
         for (var servitor : player.getServitors().values()) {
            if (servitor != null && !servitor.isDead()) {
               servitor.rechargeShots(true, true, false);
               if (targetMonster != null && servitor.getTarget() != targetMonster) {
                  servitor.setTarget(targetMonster);
                  servitor.getAI().setIntentionAttack(targetMonster);
               }
            }
         }
      }
      return false;
   }

        private static void broadcastFakePartyEffects(Player fake) {
      if (fake == null || !fake.isFakePlayer() || !fake.isInParty() || fake.getParty() == null) {
         return;
      }
      try {
         org.l2jmobius.gameserver.network.serverpackets.PartySpelled ps = new org.l2jmobius.gameserver.network.serverpackets.PartySpelled(fake);
         // 自動相容 Mobius 14.1 的 BuffInfo 寫入方法（若建構子未自動填入則透過反射補入）
         java.lang.reflect.Method addSkillMethod = null;
         for (java.lang.reflect.Method m : ps.getClass().getMethods()) {
            if (m.getName().equals("addSkill") && m.getParameterCount() == 1) {
               addSkillMethod = m;
               break;
            }
         }
         if (addSkillMethod != null) {
            for (var info : fake.getEffectList().getEffects()) {
               if (info != null && info.isInUse() && !info.getSkill().isToggle()) {
                  addSkillMethod.invoke(ps, info);
               }
            }
         }
         fake.getParty().broadcastToPartyMembers(fake, ps);
      } catch (Exception ignored) {
      }
   }
      private static void exportFakePlayersStatusJson() {
      try {
         StringBuilder sb = new StringBuilder();
         sb.append("{\"updatedAt\":").append(System.currentTimeMillis()).append(",\"players\":[");
         boolean first = true;
         for (Player p : PLAYERS) {
            if (p == null || !p.isSpawned()) {
               continue;
            }
            if (!first) {
               sb.append(",");
            }
            first = false;
            String action = isShopPlayer(p) ? "擺攤/工房中"
               : (p.isDead() ? "陣亡待復活"
               : (p.isCastingNow() ? "施放技能中"
               : (p.isAttackingNow() ? "戰鬥輸出中"
               : (p.isMoving() ? "尋路移動中" : "巡邏警戒中"))));
            String targetName = (p.getTarget() != null) ? p.getTarget().getName().replace("\"", "'") : "無";
            sb.append("{")
              .append("\"name\":\"").append(p.getName()).append("\",")
              .append("\"level\":").append(p.getLevel()).append(",")
              .append("\"class\":\"").append(p.getPlayerClass().name()).append("\",")
              .append("\"hp\":").append((int) p.getCurrentHp()).append(",")
              .append("\"maxHp\":").append(p.getMaxHp()).append(",")
              .append("\"mp\":").append((int) p.getCurrentMp()).append(",")
              .append("\"maxMp\":").append(p.getMaxMp()).append(",")
              .append("\"x\":").append(p.getX()).append(",")
              .append("\"y\":").append(p.getY()).append(",")
              .append("\"inParty\":").append(p.isInParty()).append(",")
              .append("\"isShop\":").append(isShopPlayer(p)).append(",")
              .append("\"action\":\"").append(action).append("\",")
              .append("\"target\":\"").append(targetName).append("\"")
              .append("}");
         }
         sb.append("]}");
         java.nio.file.Files.writeString(
            java.nio.file.Paths.get("D:/xampp/htdocs/fake_status.json"),
            sb.toString(),
            java.nio.charset.StandardCharsets.UTF_8
         );
      } catch (Exception ignored) {
      }
   }
   public static List<Player> getPlayers() {
      return PLAYERS;
   }
}
