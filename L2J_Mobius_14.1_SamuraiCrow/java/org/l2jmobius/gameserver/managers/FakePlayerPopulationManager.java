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
   private static final Map<Player, Integer> LAST_LEVELS = new ConcurrentHashMap();
   private static final Map<Player, Long> LAST_CHAT = new ConcurrentHashMap();
   private static final Map<Player, Long> NEXT_AI_THINK_AT = new ConcurrentHashMap();
   private static final Map<Player, Long> NEXT_PARTY_SUPPORT_AT = new ConcurrentHashMap();
   private static final Map<Player, Long> NEXT_PARTY_MOVE_AT = new ConcurrentHashMap();
   private static final Map<Player, Location> PARTY_PATROL_POINTS = new ConcurrentHashMap();
   private static final Map<Player, Location> PARTY_PATROL_ANCHORS = new ConcurrentHashMap();
   private static final Map<Player, Integer> PARTY_PATROL_STEPS = new ConcurrentHashMap();
   private static final Map<Player, Integer> LAST_PARTY_SUPPORT_SKILL = new ConcurrentHashMap();
   private static final Map<Player, Integer> LAST_OFFENSIVE_SKILL = new ConcurrentHashMap();
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
      return player != null && player.getPrivateStoreType() == PrivateStoreType.SELL;
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
      if (skill.getAffectScope() == AffectScope.SINGLE && skill.getTargetType() == TargetType.SELF) return false;
      if (skill.hasEffectType(EffectType.HEAL, EffectType.MANAHEAL_BY_LEVEL)) return false;
      TargetType tt = skill.getTargetType();
      AffectScope scope = skill.getAffectScope();
      return scope == AffectScope.PARTY || scope == AffectScope.RANGE || scope == AffectScope.PLEDGE || tt == TargetType.OTHERS || tt == TargetType.MY_PARTY || tt == TargetType.TARGET || tt == TargetType.TARGET_OR_SELF;
   }

   private static boolean isUsablePositiveSupportSkill(Skill skill) {
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
         }

         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::ensurePopulation, 1000L, 1000L);
         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::syncFakePlayersToPartyMatchingList, 3000L, 5000L);
         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::relocateCombatFakesToHuntingGrounds, 15000L, 15000L);
         LOGGER.fine("FakePlayerPopulationManager: spawned " + PLAYERS.size() + " in-memory fake players (shop=" + shop + ", combat=" + combat + ").");
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
               NEXT_AI_THINK_AT.remove(player);
               NEXT_PARTY_SUPPORT_AT.remove(player);
               NEXT_PARTY_MOVE_AT.remove(player);
               PARTY_PATROL_POINTS.remove(player);
               PARTY_PATROL_ANCHORS.remove(player);
               PARTY_PATROL_STEPS.remove(player);
               SUPPORT_ROLE_CACHE.remove(player);
               NEXT_RESOURCE_SYNC_AT.remove(player);
               NEXT_EQUIPMENT_CHECK_AT.remove(player);
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
         NEXT_AI_THINK_AT.remove(player);
         NEXT_PARTY_SUPPORT_AT.remove(player);
         LAST_PARTY_SUPPORT_SKILL.remove(player);
         LAST_OFFENSIVE_SKILL.remove(player);
         SUPPORT_ROLE_CACHE.remove(player);
         SUPPORT_ROLE_CACHE.remove(player);
         NEXT_RESOURCE_SYNC_AT.remove(player);
         NEXT_EQUIPMENT_CHECK_AT.remove(player);
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
         int level = Rnd.get(FakePlayersConfig.FAKE_PLAYER_MIN_LEVEL, Math.min(120, FakePlayersConfig.FAKE_PLAYER_MAX_LEVEL));
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
         player.spawnMe(loc.getX() + Rnd.get(-200, 200), loc.getY() + Rnd.get(-200, 200), loc.getZ());
         if (!player.isSpawned() || World.getPlayer(player.getObjectId()) != player) {
            LOGGER.warning("Fake player " + name + " failed world registration/spawn; retrying once. objectId=" + player.getObjectId() + ", spawned=" + player.isSpawned());
            player.spawnMe(loc.getX(), loc.getY(), loc.getZ());
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
         return new Location(LAST_COMBAT_SPAWN_ANCHOR.getX() + Rnd.get(-800, 800), LAST_COMBAT_SPAWN_ANCHOR.getY() + Rnd.get(-800, 800), LAST_COMBAT_SPAWN_ANCHOR.getZ());
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
      player.setTitle("隨機商店");
      int stockCount = Rnd.get(FakePlayersConfig.FAKE_PLAYER_SHOP_MIN_ITEMS, FakePlayersConfig.FAKE_PLAYER_SHOP_MAX_ITEMS);
      int added = 0;

      for(int i = 0; i < stockCount * 10 && added < stockCount; ++i) {
         ItemTemplate template = getRandomShopItem();
         if (template != null && template.isSellable() && template.isTradeable() && !template.isQuestItem()) {
            long count = template.isStackable() ? (long)Rnd.get(FakePlayersConfig.FAKE_PLAYER_SHOP_MIN_STACK, FakePlayersConfig.FAKE_PLAYER_SHOP_MAX_STACK) : 1L;
            long referencePrice = Math.max(1L, template.getReferencePrice());
            long minPrice = Math.max(FakePlayersConfig.FAKE_PLAYER_SHOP_MIN_PRICE, referencePrice / 2L);
            long maxPrice = Math.max(minPrice, Math.min(FakePlayersConfig.FAKE_PLAYER_SHOP_MAX_PRICE, referencePrice * 3L));
            long price = Rnd.get(minPrice, maxPrice);
            TradeItem virtualItem = new TradeItem(template, count, price);
            virtualItem.setObjectId(NEXT_VIRTUAL_SHOP_OBJECT_ID.getAndDecrement());
            if (!player.getSellList().getItems().stream().anyMatch((existing) -> existing.getItem().getId() == template.getId())) {
               player.getSellList().getItems().add(virtualItem);
               ++added;
            }
         }
      }

      player.getSellList().setTitle("隨機武器／裝備／藥水／材料");
      player.setPrivateStoreType(PrivateStoreType.SELL);
   }

   private static ItemTemplate getRandomShopItem() {
      int roll = Rnd.get(100);
      if (roll < 30) {
         return getRandom(ItemData.getInstance().getAllWeapons());
      } else if (roll < 65) {
         return getRandom(ItemData.getInstance().getAllArmors());
      } else if (roll >= 85) {
         List<ItemTemplate> materials = new ArrayList();

         for(EtcItem item : ItemData.getInstance().getAllEtcItems()) {
            if (!item.isPotion() && !item.isElixir() && !item.isScroll() && item.getItemType() != EtcItemType.ARROW && item.getItemType() != EtcItemType.BOLT) {
               materials.add(item);
            }
         }

         return getRandom(materials);
      } else {
         List<ItemTemplate> potions = new ArrayList();

         for(EtcItem item : ItemData.getInstance().getAllEtcItems()) {
            if (item.isPotion() || item.getItemType() == EtcItemType.ELIXIR) {
               potions.add(item);
            }
         }

         return getRandom(potions);
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

               for(Skill skill : skills) {
                  if (skill != null && player.getKnownSkill(skill.getId()) != skill) {
                     player.addSkill(skill, false);
                     ++added;
                  }
               }

               player.getStat().recalculateStats(false);
               if (added > 0) {
                  Logger var10000 = LOGGER;
                  String var10001 = player.getName();
                  var10000.fine("Fake player class skills prepared: name=" + var10001 + ", class=" + player.getPlayerClass().name() + ", level=" + player.getLevel() + ", added=" + added);
               }
            } catch (Exception e) {
               LOGGER.log(Level.WARNING, "Could not prepare class skills for fake player " + player.getName(), e);
            }
         }

         player.setCurrentHp((double)player.getMaxHp());
         player.setCurrentMp((double)player.getMaxMp());
         player.setCurrentCp((double)player.getMaxCp());
      }
   }

   private static boolean isMagicCombatClass(Player player) {
      if (player == null) {
         return false;
      } else {
         PlayerClass playerClass = player.getPlayerClass();
         if (playerClass == null || !playerClass.isMage() && !playerClass.isSummoner()) {
            if (isSupportClass(player)) {
               return true;
            } else {
               return !findOffensiveMagicSkills(player).isEmpty();
            }
         } else {
            return true;
         }
      }
   }

   private static List<Skill> findOffensiveMagicSkills(Player player) {
      List<Skill> result = new ArrayList();
      if (player == null) {
         return result;
      } else {
         for(Skill skill : player.getAllSkills()) {
            if (findOffensiveMagicSkillCandidate(skill)) {
               result.add(skill);
            }
         }

         result.sort((a, b) -> Integer.compare(offensiveSkillScore(b), offensiveSkillScore(a)));
         return result;
      }
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

   private static List<Skill> findOffensiveCombatSkills(Player player) {
      List<Skill> magicSkills = new ArrayList();
      List<Skill> physicalSkills = new ArrayList();
      if (player == null) {
         return magicSkills;
      } else {
         for(Skill skill : player.getAllSkills()) {
            if (skill != null && !skill.isPassive() && skill.isActive() && !skill.isToggle() && !isForbiddenFakeSupportUtilitySkill(skill) && !isFakePartySummonSkill(skill) && !skill.hasEffectType(EffectType.TELEPORT, EffectType.TELEPORT_TO_TARGET)) {
               if (skill.isMagic()) {
                  if (findOffensiveMagicSkillCandidate(skill)) {
                     magicSkills.add(skill);
                  }
               } else if (skill.hasNegativeEffect() && skill.hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK, EffectType.DEATH_LINK, EffectType.HP_DRAIN, EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT)) {
                  physicalSkills.add(skill);
               }
            }
         }

         magicSkills.sort((a, b) -> Integer.compare(offensiveSkillScore(b), offensiveSkillScore(a)));
         physicalSkills.sort((a, b) -> Integer.compare(offensiveSkillScore(b), offensiveSkillScore(a)));
         if (isMagicCombatClass(player)) {
            return findOffensiveMagicSkills(player);
         } else {
            return physicalSkills.isEmpty() ? magicSkills : physicalSkills;
         }
      }
   }

   private static int offensiveSkillScore(Skill skill) {
      int score = skill.getLevel() * 100 + Math.min(skill.getCastRange(), 5000);
      if (skill.isMagic()) {
         score += 200000;
      }

      if (skill.hasEffectType(EffectType.MAGICAL_ATTACK, EffectType.MAGICAL_DMG_OVER_TIME)) {
         score += 50000;
      }

      if (skill.hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK)) {
         score += 25000;
      }

      return score;
   }

   private static boolean tryUseOffensiveCombatSkill(Player player, Monster monster) {
      if (player != null && monster != null && !monster.isDead() && monster.isSpawned()) {
         List<Skill> skills = findOffensiveCombatSkills(player);
         if (skills.isEmpty() && FakePlayersConfig.FAKE_PLAYER_AUTO_LEARN_CLASS_SKILLS) {
            prepareCombatFakePlayer(player);
            skills = findOffensiveCombatSkills(player);
         }

         if (skills.isEmpty()) {
            return false;
         } else {
            player.setTarget(monster);
            int previousSkillId = (Integer)LAST_OFFENSIVE_SKILL.getOrDefault(player, -1);
            int startIndex = 0;

            for(int i = 0; i < skills.size(); ++i) {
               if (((Skill)skills.get(i)).getId() == previousSkillId) {
                  startIndex = (i + 1) % skills.size();
                  break;
               }
            }

            for(int offset = 0; offset < skills.size(); ++offset) {
               Skill skill = (Skill)skills.get((startIndex + offset) % skills.size());
               if (skill != null && !player.isSkillDisabled(skill) && !player.isCastingNow()) {
                  player.setCurrentMp((double)player.getMaxMp());
                  player.getAI().setIntentionAttack(monster);
                  player.getAI().setIntentionCast(skill, monster, (Item)null, true, false);
                  player.getAI().notifyActionThink();
                  boolean accepted = player.isCastingNow() || player.getAI().getIntention() == Intention.CAST;
                  if (accepted) {
                     LAST_OFFENSIVE_SKILL.put(player, skill.getId());
                     long wakeDelay = Math.max(200L, (long)skill.getHitTime() + 100L);
                     ThreadPool.schedule(() -> {
                        try {
                           if (player.isOnline() && player.isSpawned() && !player.isDead()) {
                              WorldObject patt0$temp = player.getTarget();
                              if (patt0$temp instanceof Monster) {
                                 Monster target = (Monster)patt0$temp;
                                 if (!target.isDead()) {
                                    player.getAI().setIntentionAttack(target);
                                    player.getAI().notifyActionThink();
                                 }
                              }
                           }
                        } catch (Throwable var3) {
                        }

                     }, wakeDelay);
                     return true;
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static void applyFixedBGradeEquipment(Player player) {
      if (player != null && !player.isDead() && !isShopPlayer(player)) {
         PlayerClass playerClass = player.getPlayerClass();
         ItemGrade desiredGrade = player.getLevel() <= 60 ? ItemGrade.B : ItemGrade.A;
         int desiredEnchant = player.getLevel() <= 60 ? 6 : 10;
         Set<WeaponType> preferredWeapons = getPreferredWeaponTypes(player);
         Set<ArmorType> preferredArmor = getPreferredArmorTypes(player);

         for(Item equipped : new ArrayList<Item>(player.getInventory().getPaperdollItems(new Predicate[0]))) {
            if (equipped != null && (equipped.isWeapon() || equipped.isArmor())) {
               BodyPart bodyPart = BodyPart.fromItem(equipped);
               if (bodyPart != null) {
                  player.getInventory().unEquipItemInBodySlot(bodyPart);
               }

               player.getInventory().destroyItem(ItemProcessType.DESTROY, equipped, equipped.getCount(), player, (Object)null);
            }
         }

         List<ItemTemplate> weaponCandidates = new ArrayList();

         for(ItemTemplate item : ItemData.getInstance().getAllWeapons()) {
            if (isValidFakeWeapon(player, item, preferredWeapons, desiredGrade)) {
               weaponCandidates.add(item);
            }
         }

         if (playerClass != null && (playerClass.isMage() || playerClass.isSummoner())) {
            List<ItemTemplate> staffCandidates = weaponCandidates.stream().filter((itemx) -> itemx.getItemType() == WeaponType.BLUNT && itemx.getBodyPart() == BodyPart.LR_HAND).toList();
            if (!staffCandidates.isEmpty()) {
               weaponCandidates.clear();
               weaponCandidates.addAll(staffCandidates);
            }
         }

         if (!weaponCandidates.isEmpty()) {
            equipCompatibleWeapon(player, weaponCandidates);
         }

         Set<BodyPart> armorParts = new HashSet();
         PlayerClass equipmentClass = findNativeInitialEquipmentClass(playerClass);
         Collection<InitialEquipment> initialEquipment = InitialEquipmentData.getInstance().getClassEquipment(equipmentClass);
         if (initialEquipment != null) {
            for(InitialEquipment holder : initialEquipment) {
               if (holder.isEquipped()) {
                  ItemTemplate item = ItemData.getInstance().getTemplate(holder.getId());
                  if (item != null && item.isArmor() && item.getBodyPart() != BodyPart.NONE) {
                     armorParts.add(item.getBodyPart());
                  }
               }
            }
         }

         if (armorParts.isEmpty()) {
            armorParts.add(BodyPart.HEAD);
            armorParts.add(BodyPart.CHEST);
            armorParts.add(BodyPart.LEGS);
            armorParts.add(BodyPart.GLOVES);
            armorParts.add(BodyPart.FEET);
         }

         for(BodyPart bodyPart : armorParts) {
            List<ItemTemplate> armorCandidates = new ArrayList();

            for(Armor armor : ItemData.getInstance().getAllArmors()) {
               if (((ItemTemplate)armor).getItemGrade() == desiredGrade && ((ItemTemplate)armor).getBodyPart() == bodyPart && !((ItemTemplate)armor).isQuestItem() && !containsBlockedEquipmentName(((ItemTemplate)armor).getName())) {
                  ItemType var17 = ((ItemTemplate)armor).getItemType();
                  if (var17 instanceof ArmorType) {
                     ArmorType armorType = (ArmorType)var17;
                     if (preferredArmor.contains(armorType)) {
                        armorCandidates.add(armor);
                     }
                  }
               }
            }

            if (armorCandidates.isEmpty()) {
               for(Armor armor : ItemData.getInstance().getAllArmors()) {
                  if (((ItemTemplate)armor).getItemGrade() == desiredGrade && ((ItemTemplate)armor).getBodyPart() == bodyPart && !((ItemTemplate)armor).isQuestItem() && !containsBlockedEquipmentName(((ItemTemplate)armor).getName())) {
                     armorCandidates.add(armor);
                  }
               }
            }

            while(!armorCandidates.isEmpty()) {
               ItemTemplate armor = (ItemTemplate)armorCandidates.remove(Rnd.get(armorCandidates.size()));
               if (equipGeneratedArmor(player, armor, desiredEnchant)) {
                  break;
               }
            }
         }

         if (playerClass != null) {
            player.broadcastUserInfo();
         }

      }
   }

   private static void ensureCombatWeapon(Player player) {
      if (player != null && !player.isDead() && !isShopPlayer(player)) {
         PlayerClass playerClass = player.getPlayerClass();
         ItemGrade desiredGrade = player.getLevel() <= 60 ? ItemGrade.B : ItemGrade.A;
         int desiredEnchant = player.getLevel() <= 60 ? 6 : 10;
         Weapon activeWeapon = player.getActiveWeaponItem();
         Item activeWeaponInstance = player.getActiveWeaponInstance();
         Set<WeaponType> preferredTypes = getPreferredWeaponTypes(player);
         boolean magicClass = playerClass != null && (playerClass.isMage() || playerClass.isSummoner());
         if (activeWeaponInstance != null && activeWeapon != null && activeWeapon.getItemGrade() == desiredGrade && activeWeaponInstance.getEnchantLevel() == desiredEnchant) {
            if (!magicClass) {
               if (preferredTypes.isEmpty() || preferredTypes.contains(activeWeapon.getItemType())) {
                  return;
               }
            } else if (activeWeapon.getItemType() == WeaponType.BLUNT && activeWeaponInstance.getTemplate().getBodyPart() == BodyPart.LR_HAND) {
               return;
            }
         }

         List<ItemTemplate> candidates = new ArrayList();

         for(ItemTemplate item : ItemData.getInstance().getAllWeapons()) {
            if (isValidFakeWeapon(player, item, preferredTypes, desiredGrade) && (!magicClass || item.getItemType() == WeaponType.BLUNT && item.getBodyPart() == BodyPart.LR_HAND)) {
               candidates.add(item);
            }
         }

         if (candidates.isEmpty()) {
            if (magicClass) {
               LOGGER.warning("No suitable grade/enchant LR_HAND BLUNT weapon candidate is available for fake mage " + player.getName() + ".");
            }

         } else {
            if (activeWeaponInstance != null) {
               BodyPart bodyPart = BodyPart.fromItem(activeWeaponInstance);
               if (bodyPart != null) {
                  player.getInventory().unEquipItemInBodySlot(bodyPart);
               }

               player.getInventory().destroyItem(ItemProcessType.DESTROY, activeWeaponInstance, activeWeaponInstance.getCount(), player, (Object)null);
            }

            equipCompatibleWeapon(player, candidates);
         }
      }
   }

   private static boolean isValidFakeWeapon(Player player, ItemTemplate item, Set<WeaponType> preferredTypes, ItemGrade desiredGrade) {
      return player != null && item != null && item.isWeapon() && item.isEquipable() && !item.isQuestItem() && item.getItemGrade() == desiredGrade && !containsBlockedEquipmentName(item.getName()) && (preferredTypes.isEmpty() || preferredTypes.contains(item.getItemType()));
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
      Set<WeaponType> result = new HashSet();
      if (player != null && player.getPlayerClass() != null) {
         PlayerClass playerClass = player.getPlayerClass();
         if (!playerClass.isMage() && !playerClass.isSummoner()) {
            Collection<InitialEquipment> equipment = InitialEquipmentData.getInstance().getClassEquipment(findNativeInitialEquipmentClass(playerClass));
            if (equipment != null) {
               for(InitialEquipment holder : equipment) {
                  if (holder.isEquipped()) {
                     ItemTemplate item = ItemData.getInstance().getTemplate(holder.getId());
                     if (item != null && item.isWeapon()) {
                        ItemType var8 = item.getItemType();
                        if (var8 instanceof WeaponType) {
                           WeaponType weaponType = (WeaponType)var8;
                           result.add(weaponType);
                        }
                     }
                  }
               }
            }

            return result;
         } else {
            result.add(WeaponType.BLUNT);
            return result;
         }
      } else {
         return result;
      }
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
         if (item != null && item.isEquipable() && player.canUseEquipment(item)) {
            item.setEnchantLevel(player.getLevel() <= 60 ? 6 : 10);
            player.useEquippableItem(item, false);
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
      if (monster != null && monster.isSpawned() && !monster.isDead() && !monster.isFakePlayer() && !monster.isRaid() && !monster.isRaidMinion() && !monster.isQuestMonster()) {
         if (monster.getInstanceId() != player.getInstanceId()) {
            return false;
         } else {
            int level = player.getLevel();
            int diff = monster.getLevel() - level;
            return diff >= FakePlayersConfig.FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF && diff <= FakePlayersConfig.FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF;
         }
      } else {
         return false;
      }
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
      if (player != null && !player.isDead() && !isShopPlayer(player)) {
         configureAutomaticPotion(player);
         return false;
      } else {
         return false;
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
      for(Player player : PLAYERS) {
         if (player != null && player.isOnline() && player.isSpawned() && player.getMatchingRoom() == null) {
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

      for(Player player : PLAYERS) {
         if (player == null || !player.isSpawned() || player.isDead()) {
            continue;
         }
         if (isShopPlayer(player)) {
            continue;
         }

         try {
            player.getAI().setIntentionActive();

            // 🌟 屍體清除鎖定：打死怪立刻清除目標，拒絕散步發呆
            WorldObject currentTarget = player.getTarget();
            if (currentTarget instanceof Monster && ((Monster) currentTarget).isDead()) {
               player.setTarget(null);
            }

            // 5秒補充一次物資
            long nextResourceSync = LAST_CHAT.getOrDefault(player, 0L); 
            if (now >= nextResourceSync) {
               LAST_CHAT.put(player, now + 5000L);
               prepareFakeConsumables(player);
               manageLootAndSupplies(player);
            }

            // 🌟【終極解鎖】：修正反編譯泛型擦除，使用 Object 接收再精準轉換為 Creature / Monster！
            Monster attacker = null;
            Set attackBy = player.getAttackByList();
            if (attackBy != null && !attackBy.isEmpty()) {
               for (Object obj : attackBy) {
                  if (obj instanceof Monster) {
                     Monster m = (Monster) obj;
                     if (!m.isDead()) {
                        attacker = m;
                        break;
                     }
                  }
               }
            }


            // 優先反擊揍我的怪，沒被打就自動尋找下一隻怪
            if (attacker != null) {
               reserveMonsterTarget(player, attacker);
               if (!tryUseOffensiveCombatSkill(player, attacker)) {
                  forceFakePhysicalAttack(player, attacker);
               }
            } else {
               WorldObject targetCheck = player.getTarget();
               if (targetCheck == null || !(targetCheck instanceof Monster)) {
                  Monster nearest = findSuitableMonsterNearby(player, FakePlayersConfig.FAKE_PLAYER_HUNT_SEARCH_RANGE);
                  if (nearest != null) {
                     reserveMonsterTarget(player, nearest);
                  }
               }

               WorldObject finalTarget = player.getTarget();
               if (finalTarget instanceof Monster && !((Monster) finalTarget).isDead()) {
                  Monster targetMonster = (Monster) finalTarget;
                  if (!tryUseOffensiveCombatSkill(player, targetMonster)) {
                     forceFakePhysicalAttack(player, targetMonster);
                  }
               }
            }

            player.getAI().notifyActionThink();

         } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "AI tick error: " + player.getName(), t);
         }
      }
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
   }

   private static void removeFakeEquipmentGradePenalty(Player player) {
      if (player != null && player.getInventory().getInventoryItemCount(29510, -1) < 1) {
         player.getInventory().addItem(ItemProcessType.REWARD, 29510, 1, player, null);
      }
   }

   private static void randomChat() {
      long now = System.currentTimeMillis();
      String[] msg = {"大家好。", "這裡有人一起練嗎？", "今天怪物掉寶不錯。", "有人要組隊嗎？", "l2tw.com 天堂2單機論壇"};
      for (Player fake : PLAYERS) {
         if (fake != null && fake.isSpawned() && fake.isOnline() && (now - LAST_CHAT.getOrDefault(fake, 0L) >= 30000L) && Rnd.get(100) < 22) {
            fake.broadcastPacket(new CreatureSay(fake, ChatType.GENERAL, fake.getName(), msg[Rnd.get(msg.length)]));
            LAST_CHAT.put(fake, now);
         }
      }
   }

   public static void handlePlayerChat(Player speaker, String targetName, String text, ChatType type) {
   }

   public static List<Player> getPlayers() {
      return PLAYERS;
   }
}
