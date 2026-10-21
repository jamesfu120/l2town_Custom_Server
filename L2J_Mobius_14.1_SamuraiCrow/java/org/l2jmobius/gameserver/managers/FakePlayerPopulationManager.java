package org.l2jmobius.gameserver.managers;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class FakePlayerPopulationManager {
   private static final Logger LOGGER = Logger.getLogger(FakePlayerPopulationManager.class.getName());
   private static final List<Player> PLAYERS = new CopyOnWriteArrayList<>();
   private static final Map<Player, Long> LAST_CHAT = new ConcurrentHashMap<>();
   private static final Map<Player, Boolean> DEATH_RESPAWN_PENDING = new ConcurrentHashMap<>();
   private static final AtomicInteger NAME_SEQUENCE = new AtomicInteger();
   private static final Location[] TOWNS = new Location[]{
      new Location(82698, 148638, -3472), 
      new Location(-80745, 149677, -3043), 
      new Location(147450, 27051, -2208)
   };

   private static final String[] GIVEN = new String[]{"天", "月", "星", "龍", "凰", "雪", "霜", "風", "雲", "雷", "夜", "晨", "玄", "紫", "青", "赤", "白", "金", "玉", "幽", "凌", "蘭", "羽", "墨", "寒", "烈", "影", "曦", "瑤", "玥"};
   private static final String[] SURNAME = new String[]{"無雙", "戰魂", "天命", "星河", "月影", "龍吟", "鳳舞", "蒼穹", "破軍", "流光", "霜華", "風痕", "夜行", "孤城", "凌霄", "玄武", "青冥", "紫電", "烈陽", "寒月"};

   private FakePlayerPopulationManager() {
   }

    public static void initialize() {
      if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT > 0) {
         // 開機核心定時器啟動
         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::runThinkSafely, 1000L, 1000L);
         ThreadPool.scheduleAtFixedRate(FakePlayerPopulationManager::randomChat, 15000L, 15000L);
         
         // 核心大絕招：開機自動全自動召喚假人循環
         try {
            int totalToSpawn = FakePlayersConfig.FAKE_PLAYER_TOTAL_COUNT;
            int shopCount = FakePlayersConfig.FAKE_PLAYER_SHOP_COUNT;
            
            for (int i = 0; i < totalToSpawn; i = i + 1) {
               // 判斷當前要生成的假人是商店型還是戰鬥型
               boolean isShop = (i < shopCount);
               
               // 利用新版 14.1 核心機制創立並生成假人實體
               org.l2jmobius.gameserver.data.xml.PlayerTemplateData templateData = org.l2jmobius.gameserver.data.xml.PlayerTemplateData.getInstance();
               // 隨機獲取 1 級的基礎職業模板 (職業 ID 0 為人類戰士)
               org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate template = templateData.getTemplate(0);
               
               if (template != null) {
                  // 呼叫我們寫好的不重複姓名生成器，隨機決定假人等級 (例如 80 級)
                  String fName = uniqueName();
                  Player fakePlayer = Player.createFakePlayer(template, fName, 80);
                  
                  if (fakePlayer != null) {
                     // 根據類型，隨機飛往城鎮或者是練功獵場
                     Location spawnLoc = isShop ? TOWNS[org.l2jmobius.commons.util.Rnd.get(TOWNS.length)] : findCombatSpawnLocation(fakePlayer);
                     fakePlayer.spawnMe(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
                     
                     // 如果是商店型假人，自動呼叫我們編譯成功的擺攤大腦
                     if (isShop) {
                        setupShop(fakePlayer);
                     } else {
                        // 戰鬥型假人自動啟動神裝發放大腦
                        applyFixedBGradeEquipment(fakePlayer);
                     }
                     
                     // 正式納入假人管理快取清單中，開始進行打怪主循環
                     PLAYERS.add(fakePlayer);
                  }
               }
            }
            LOGGER.info("FakePlayerPopulationManager: 開機全自動暴力召喚 " + PLAYERS.size() + " 名假人軍團成功登入世界！");
         } catch (Exception e) {
            LOGGER.warning("開機自動暴力召喚假人時發生異常: " + e.getMessage());
         }
      }
   }

   private static void runThinkSafely() {
      try {
         for (Player player : PLAYERS) {
            if (player != null && player.isOnline() && !player.isDead()) {
               if (FakePlayersConfig.FAKE_PLAYER_AUTO_HUNT) {
                  WorldObject target = player.getTarget();
                  if (target == null || !(target instanceof Monster) || ((Monster) target).isDead()) {
                     Monster nearestMonster = findSuitableMonsterNearby(player, FakePlayersConfig.FAKE_PLAYER_HUNT_SEARCH_RANGE);
                     if (nearestMonster != null) {
                        player.setTarget(nearestMonster);
                     }
                  }
                  
                  if (player.getTarget() instanceof Monster) {
                     Monster targetMonster = (Monster) player.getTarget();
                     if (!tryUseOffensiveCombatSkill(player, targetMonster)) {
                        forceFakePhysicalAttack(player, targetMonster);
                     }
                  }
               }
               player.getAI().notifyActionThink();
            }
         }
      } catch (Throwable t) {
         LOGGER.warning("假人戰鬥主循環守護執行緒發生異常: " + t.getMessage());
      }
   }

   private static void randomChat() {
      if (!FakePlayersConfig.FAKE_PLAYER_CHAT) {
         return;
      }
      long now = System.currentTimeMillis();
      String[] messages = new String[]{
         "大家好。", "這裡有人一起練嗎？", "今天怪物掉寶不錯。", "有人要組隊嗎？", 
         "L2town 天堂2單機論壇", "本模擬器可在L2town免費下載", "歡迎大家交流遊戲玩法。"
      };

      for (Player fake : PLAYERS) {
         if (fake != null && fake.isSpawned() && fake.isOnline() && (now - LAST_CHAT.getOrDefault(fake, 0L) >= 30000L) && Rnd.get(100) < 22) {
            fake.broadcastPacket(new CreatureSay(fake, ChatType.GENERAL, fake.getName(), messages[Rnd.get(messages.length)]));
            LAST_CHAT.put(fake, now);
         }
      }
   }

   public static void handlePlayerChat(Player speaker, String targetName, String text, ChatType type) {
      if (!FakePlayersConfig.FAKE_PLAYER_CHAT || speaker == null || text == null) {
         return;
      }
      for (Player fake : PLAYERS) {
         if (fake != null && fake.isSpawned() && fake.isOnline() && fake.calculateDistance3D(speaker) <= 2000.0) {
            String lower = text.toLowerCase();
            String answer = lower.contains("你好") || lower.contains("hi") ? "你好呀。" : (lower.contains("組隊") ? "可以，一起走。" : null);
            if (answer != null) {
               ThreadPool.schedule(() -> fake.broadcastPacket(new CreatureSay(fake, ChatType.GENERAL, fake.getName(), answer)), (long) Rnd.get(1500, 4000));
            }
         }
      }
   }
   public static void setupShop(Player player) {
      if (player == null) {
         return;
      }
      player.setTitle("隨機商店");
      try {
         if (player.getSellList() != null && player.getSellList().getItems() != null) {
            player.setPrivateStoreType(org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType.SELL);
            player.getSellList().setTitle("隨機物資商店");
         }
      } catch (Exception e) {
         LOGGER.warning("設定假人商店失敗: " + e.getMessage());
      }
   }

   private static Location findCombatSpawnLocation(Player player) {
      Location[] fallbackGrounds = new Location[]{
         new Location(113000, 142000, -2480),
         new Location(115000, 115000, -2500),
         new Location(155000, 116000, -2800),
         new Location(185000, 145000, -3000)
      };
      
      try {
         Location baseLoc = fallbackGrounds[Rnd.get(fallbackGrounds.length)];
         return new Location(
            baseLoc.getX() + Rnd.get(-500, 500),
            baseLoc.getY() + Rnd.get(-500, 500),
            baseLoc.getZ()
         );
      } catch (Exception e) {
         return fallbackGrounds[0];
      }
   }

   private static void relocateCombatFakesToHuntingGrounds() {
      if (!FakePlayersConfig.FAKE_PLAYER_AUTO_HUNT) {
         return;
      }
      
      try {
         for (Player player : PLAYERS) {
            if (player != null && player.isOnline() && !player.isDead() && (player.getPrivateStoreType() == org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType.NONE)) {
               double distanceToTown = player.calculateDistance3D(TOWNS[0]);
               if (distanceToTown < 2500.0) {
                  Location huntingGround = findCombatSpawnLocation(player);
                  if (huntingGround != null) {
                     player.setTarget(null);
                     player.teleToLocation(huntingGround, true);
                     LOGGER.info("假人 " + player.getName() + " 已從城鎮自動派遣傳送到分散獵場練功。");
                  }
               }
            }
         }
      } catch (Exception e) {
         LOGGER.warning("分發假人前往獵場時發生異常: " + e.getMessage());
      }
   }
   private static String uniqueName() {
      int poolSize = GIVEN.length * SURNAME.length;
      for (int attempt = 0; attempt < poolSize + 100000; ++attempt) {
         int sequence = NAME_SEQUENCE.getAndIncrement();
         int baseIndex = Math.floorMod(sequence, poolSize);
         String name = GIVEN[baseIndex / SURNAME.length] + SURNAME[baseIndex % SURNAME.length];
         int suffix = sequence / poolSize;
         if (suffix > 0) {
            name += String.format("%03d", suffix);
         }
         if (World.getPlayer(name) == null) {
            return name;
         }
      }
      return "假人" + Rnd.get(1000, 9999);
   }

   public static void handleFakePlayerDeath(Player player) {
      if (player != null && player.isFakePlayer() && player.isDead() && player.isOnline()) {
         if (DEATH_RESPAWN_PENDING.putIfAbsent(player, Boolean.TRUE) == null) {
            ThreadPool.schedule(() -> {
               try {
                  if (player.isFakePlayer() && player.isOnline() && player.isDead()) {
                     player.setTarget(null);
                     player.doRevive();
                     player.fullRestore();
                     player.getAI().setIntentionActive();
                     LOGGER.info("假人 " + player.getName() + " 戰死後已完成自動原地復活並重新投入戰鬥。");
                  }
               } catch (Exception e) {
                  LOGGER.warning("假人自動復活失敗: " + e.getMessage());
               } finally {
                  DEATH_RESPAWN_PENDING.remove(player);
               }
            }, (long) FakePlayersConfig.FAKE_PLAYER_DEATH_RESPAWN_DELAY * 1000L);
         }
      }
   }

   public static void applyFixedBGradeEquipment(Player player) {
      if (player == null || player.isDead()) {
         return;
      }
      try {
         if (player.getInventory() == null || player.getPlayerClass() == null) {
            return;
         }

         // 1. 精準職業型態判定
         boolean isMage = player.getPlayerClass().isMage() || player.getPlayerClass().isSummoner();
         // 利用核心現成的職業名稱或特性判定是否為弓手 (包含潛行者、鷹眼、銀月、暗影等)
         String className = player.getPlayerClass().name().toLowerCase();
         boolean isArcher = className.contains("archer") || className.contains("ranger") || className.contains("scout") || className.contains("rogue");

         // 2. 初始化各部位 B 級頂級裝備 ID 容器 (預設為戰士重裝：末日套裝)
         int weaponId = 76;   // 物理職：長劍 (瑟魯基長劍)
         int chestId = 2378;  // 末日金屬盔甲 (上衣)
         int legsId = 2419;   // 末日金屬護腿 (下褲)
         int glovesId = 2465; // 末日手套
         int bootsId = 2439;  // 末日靴子

         // 3. 根據四分法智慧調整裝備套組 ID
         if (isMage) {
            // 法師組：阿巴敦法袍套組 + 靈魂之眼雙手杖
            weaponId = 71;    // 靈魂之眼 (雙手杖)
            chestId = 2407;   // 阿巴敦外衣
            legsId = 2424;    // 阿巴敦長褲
            glovesId = 2466;  // 阿巴敦手套
            bootsId = 2441;   // 阿巴敦靴子
         } else if (isArcher) {
            // 弓手組：青狼輕甲套組 + 冰晶弓
            weaponId = 286;   // 冰晶弓
            chestId = 2392;   // 青狼皮革盔甲
            legsId = 2421;    // 青狼護腿
            glovesId = 2463;  // 青狼手套
            bootsId = 2436;   // 青狼靴子
         }

         // 4. 實裝發放與自動強制穿戴 (武器與防具部位)
         int[] equipmentIds = new int[]{weaponId, chestId, legsId, glovesId, bootsId};
         for (int itemId : equipmentIds) {
            org.l2jmobius.gameserver.entity.item.instance.Item item = player.getInventory().addItem(org.l2jmobius.gameserver.entity.item.enums.ItemProcessType.REWARD, itemId, 1, player, null);
            if (item != null) {
               item.setEnchantLevel(6); // 全部自動強化 +6
               player.getInventory().equipItem(item); // 強制裝備
            }
         }

         // 弓手特供：全自動發放 10000 支長箭，避免弓手沒箭罰站
         if (isArcher) {
            player.getInventory().addItem(org.l2jmobius.gameserver.entity.item.enums.ItemProcessType.REWARD, 17, 10000, player, null);
         }

         // 5. 終極飾品大腦：發放全套 A 級頂級聖靈飾品 (魔防大撐高)
         // 924:聖威項鍊, 870:聖靈耳環(發2個), 893:聖威戒指(發2個)
         int[] jewelryIds = new int[]{924, 870, 870, 893, 893};
         for (int jId : jewelryIds) {
            org.l2jmobius.gameserver.entity.item.instance.Item jewelry = player.getInventory().addItem(org.l2jmobius.gameserver.entity.item.enums.ItemProcessType.REWARD, jId, 1, player, null);
            if (jewelry != null) {
               jewelry.setEnchantLevel(6);
               player.getInventory().equipItem(jewelry); // 自動掛上耳環、戒指、項鍊
            }
         }

         LOGGER.info("[大腦實裝] 已成功為假人 " + player.getName() + " 分配專屬全套 B 級 +6 頂級神裝與全套黑珠寶飾品！");
      } catch (Exception e) {
         LOGGER.warning("為假人配置全職業B級飾品神裝時發生異常: " + e.getMessage());
      }
   }


   private static boolean tryUseOffensiveCombatSkill(Player player, Monster monster) {
      if (player == null || monster == null || monster.isDead()) {
         return false;
      }
      try {
         for (org.l2jmobius.gameserver.mechanics.skill.Skill skill : player.getAllSkills()) {
            if (skill != null && !skill.isPassive() && skill.isActive() && !skill.isToggle()) {
               if (!player.isSkillDisabled(skill)) {
                  player.setTarget(monster);
                  player.getAI().setIntentionCast(skill, monster, null, true, false);
                  return true;
               }
            }
         }
      } catch (Exception e) {
         LOGGER.warning("假人嘗試使用戰鬥技能時發生異常: " + e.getMessage());
      }
      return false;
   }

   public static void forceFakePhysicalAttack(Player player, Monster monster) {
      if (player == null || monster == null || monster.isDead()) {
         return;
      }
      try {
         player.setTarget(monster);
         player.getAI().setIntentionAttack(monster);
      } catch (Exception e) {
         LOGGER.warning("假人強制物理攻擊時發生異常: " + e.getMessage());
      }
   }
   public static boolean isSuitableHuntingMonster(Player player, Monster monster) {
      if (player == null || monster == null) {
         return false;
      }
      try {
         if (!monster.isSpawned() || monster.isDead()) {
            return false;
         }
         if (monster.getInstanceId() != player.getInstanceId()) {
            return false;
         }
         if (monster.isRaid() || monster.isRaidMinion() || monster.isQuestMonster()) {
            return false;
         }
         int pLevel = player.getLevel();
         int mLevel = monster.getLevel();
         int diff = mLevel - pLevel;
         
         int minDiff = FakePlayersConfig.FAKE_PLAYER_HUNT_MIN_LEVEL_DIFF;
         int maxDiff = FakePlayersConfig.FAKE_PLAYER_HUNT_MAX_LEVEL_DIFF;
         
         if (diff >= minDiff && diff <= maxDiff) {
            return true;
         }
         return false;
      } catch (Exception e) {
         return false;
      }
   }

   public static Monster findSuitableMonsterNearby(Player player, int range) {
      if (player == null) {
         return null;
      }
      try {
         Monster closestMonster = null;
         double closestDistance = Double.MAX_VALUE;
         List<Monster> visibleMonsters = World.getInstance().getVisibleObjectsInRange(player, Monster.class, range);
         for (Monster monster : visibleMonsters) {
            if (monster != null) {
               if (isSuitableHuntingMonster(player, monster)) {
                  double distance = player.calculateDistance3D(monster);
                  if (Double.compare(distance, closestDistance) == -1) {
                     closestDistance = distance;
                     closestMonster = monster;
                  }
               }
            }
         }
         return closestMonster;
      } catch (Exception e) {
         LOGGER.warning("假人雷達掃描周圍適合怪物時發生異常: " + e.getMessage());
         return null;
      }
   }

   public static void processPartySupport(Player support) {
      if (support == null || support.isDead() || !support.isInParty()) {
         return;
      }
      try {
         org.l2jmobius.gameserver.entity.groups.Party party = support.getParty();
         if (party == null) {
            return;
         }
         Player weakestMember = null;
         double lowestHpRatio = 1.0;
         double healThreshold = ((double) FakePlayersConfig.FAKE_PLAYER_SUPPORT_HEAL_PERCENT) / 100.0;
         for (Player member : party.getMembers()) {
            if (member != null && member.isOnline() && !member.isDead() && (member.getInstanceId() == support.getInstanceId())) {
               double hpRatio = ((double) member.getCurrentHp()) / member.getMaxHp();
               
               boolean checkThreshold = (Double.compare(hpRatio, healThreshold) == -1);
               boolean checkLowest = (Double.compare(hpRatio, lowestHpRatio) == -1);
               if (checkThreshold && checkLowest) {
                  lowestHpRatio = hpRatio;
                  weakestMember = member;
               }
            }
         }
         if (weakestMember != null) {
            for (org.l2jmobius.gameserver.mechanics.skill.Skill skill : support.getAllSkills()) {
               if (skill != null && !skill.isPassive() && skill.isActive()) {
                  String skillName = skill.getName() != null ? skill.getName().toLowerCase() : "";
                  if (skillName.contains("heal") || skillName.contains("治癒") || skillName.contains("回復")) {
                     if (!support.isSkillDisabled(skill)) {
                        support.setTarget(weakestMember);
                        support.getAI().setIntentionCast(skill, weakestMember, null, true, false);
                        LOGGER.info("輔助假人 " + support.getName() + " 自動對受傷隊友 " + weakestMember.getName() + " 施展治癒術補血。");
                        return;
                     }
                  }
               }
            }
         }
         for (org.l2jmobius.gameserver.mechanics.skill.Skill skill : support.getAllSkills()) {
            if (skill != null && !skill.isPassive() && skill.isActive() && !skill.isToggle()) {
               String skillName = skill.getName() != null ? skill.getName().toLowerCase() : "";
               if (skillName.contains("shield") || skillName.contains("might") || skillName.contains("buff") || skillName.contains("歌") || skillName.contains("舞") || skillName.contains("輔助")) {
                  if (!support.isSkillDisabled(skill)) {
                     support.setTarget(support);
                     support.getAI().setIntentionCast(skill, support, null, true, false);
                     return;
                  }
               }
            }
         }
      } catch (Exception e) {
         LOGGER.warning("假人組隊補血大腦運作異常: " + e.getMessage());
      }
   }

   public static List<Player> getPlayers() {
      return PLAYERS;
   }
}
