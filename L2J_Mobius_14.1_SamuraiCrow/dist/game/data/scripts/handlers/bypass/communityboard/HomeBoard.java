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
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package handlers.bypass.communityboard;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.CommunityBoardConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;
import org.l2jmobius.gameserver.network.serverpackets.ability.ExAcquireAPSkillList;

/**
 * Home board.
 * @author Zoey76, Mobius
 */
public class HomeBoard implements IParseBoardHandler
{
	private static final DateTimeFormatter PREMIUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	
	// SQL Queries
	private static final String COUNT_FAVORITES = "SELECT COUNT(*) AS favorites FROM `bbs_favorites` WHERE `playerId`=?";
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	
	// ===== CUSTOM: Server Info / Rank (added) =====
	private static final long SERVER_START = System.currentTimeMillis();
	private static final int RANK_LIMIT = 10;
	private static final String INFO_PATH = "data/html/CommunityBoard/Custom/info.html";
	
	// 其它 Board 自己會填的佔位符，不在這裡警告
	private static final java.util.Set<String> OTHER_BOARD_PLACEHOLDERS = new java.util.HashSet<>(java.util.Arrays.asList("%searchresult%", "%pages%", "%fav_list%", "%fav_bypass%", "%fav_title%", "%fav_add_date%", "%fav_id%", "%fav_count%", "%region_count%", "%clan_count%", "%region_list%", "%region_id%", "%region_name%", "%region_owning_clan%", "%region_owning_clan_alliance%", "%region_tax_rate%", "%regionname%", "%tax%", "%lord%", "%clanname%", "%allyname%", "%siegedate%", "%hallslist%"));
	private static final boolean DEBUG_LOG_PLACEHOLDERS = false;

	// ★★★ 伺服器名稱（抓不到 LoginServerThread 時才用這個常數）★★★
	private static final String SERVER_NAME = "天堂II覺醒（暗鴉武士）";
	
	private static final Map<Integer, String> CLASS_NAMES = new HashMap<>();
	
	private static final String[] DEFAULT_CLASS_NAMES =
	{
		"0=人類戰士", "1=戰士", "2=劍鬥士", "3=傭兵", "4=人類騎士",
		"5=聖騎士", "6=暗騎士", "7=盜賊", "8=寶藏獵人", "9=鷹眼",
		"10=人類法師", "11=人類巫師", "12=術士", "13=死靈法師", "14=法魔",
		"15=牧師", "16=主教", "17=先知", "18=精靈戰士", "19=精靈騎士",
		"20=聖殿騎士", "21=劍術詩人", "22=精靈巡守", "23=大地行者", "24=銀月遊俠",
		"25=精靈法師", "26=精靈巫師", "27=咒術詩人", "28=元素使", "29=神使",
		"30=長老", "31=黑暗戰士", "32=沼澤騎士", "33=席琳騎士", "34=劍刃舞者",
		"35=暗殺者", "36=深淵行者", "37=暗影遊俠", "38=黑暗法師", "39=黑暗巫師",
		"40=狂咒術士", "41=暗影召喚士", "42=席琳神使", "43=席琳長老", "44=半獸人戰士",
		"45=半獸人突襲者", "46=破壞者", "47=半獸人武者", "48=暴君", "49=半獸人法師",
		"50=半獸人巫醫", "51=霸主", "52=戰狂", "53=矮人戰士", "54=收集者",
		"55=賞金獵人", "56=工匠", "57=戰爭工匠", "88=決鬥者", "89=猛將",
		"90=聖鳳騎士", "91=煉獄騎士", "92=人馬", "93=冒險英豪", "94=大魔導士",
		"95=魂狩術士", "96=秘儀召主", "97=樞機主教", "98=昭聖者", "99=伊娃神殿騎士",
		"100=伊娃吟遊詩人", "101=疾風浪人", "102=月光箭靈", "103=伊娃秘術詩人", "104=元素支配者",
		"105=伊娃聖者", "106=席琳冥殿騎士", "107=幽冥舞者", "108=魅影獵者", "109=幽冥箭靈",
		"110=暴風狂嘯者", "111=暗影支配者", "112=席琳聖者", "113=泰坦", "114=卡巴塔里宗師",
		"115=君主", "116=末日戰狂", "117=財富獵人", "118=巨匠", "123=男性闇天使士兵",
		"124=女性闇天使士兵", "125=裝甲突擊兵", "126=狙擊術士", "127=狂戰士", "128=男性碎魂者",
		"129=女性碎魂者", "130=弩弓遊俠", "131=末日使者", "132=男性追魂使", "133=女性追魂使",
		"134=魔彈射手", "135=監察者", "136=裁決者", "139=席格爾騎士", "140=提爾戰士",
		"141=歐瑟遊俠", "142=尤爾弓箭手", "143=菲歐巫師", "144=伊斯附魔師", "145=維因召喚師",
		"146=席格爾治癒師", "148=席格爾聖鳳騎士", "149=席格爾煉獄騎士", "150=席格爾伊娃神殿騎士", "151=席格爾席琳冥殿騎士",
		"152=提爾決鬥者", "153=提爾猛將", "154=提爾泰坦", "155=提爾卡巴塔里宗師", "156=提爾巨匠",
		"157=提爾末日使者", "158=歐瑟冒險英豪", "159=歐瑟疾風浪人", "160=歐瑟魅影獵者", "161=歐瑟財富獵人",
		"162=尤爾人馬", "163=尤爾月光箭靈", "164=尤爾幽冥箭靈", "165=尤爾魔彈射手", "166=菲歐大魔導士",
		"167=菲歐魂狩術士", "168=菲歐伊娃秘術詩人", "169=菲歐暴風狂嘯者", "170=菲歐追魂使", "171=伊斯昭聖者",
		"172=伊斯伊娃吟遊詩人", "173=伊斯幽冥舞者", "174=伊斯君主", "175=伊斯末日戰狂", "176=維因秘儀召主",
		"177=維因元素支配者", "178=維因暗影支配者", "179=艾羅樞機主教", "180=艾羅伊娃聖者", "181=艾羅席琳聖者",
		"182=翼人戰士", "183=翼人巫師", "184=掠奪者", "185=威嚇者", "186=拳鬥士",
		"187=暴風威嚇者", "188=重力拳鬥士", "189=沙哈預知者", "212=死亡戰士", "213=死亡武者",
		"214=死亡狂戰士", "215=死亡騎士", "216=席格爾死亡騎士", "231=矮人魔法學徒", "232=魔法使",
		"233=梅芙魔法使", "234=星光魔法使", "235=光輝魔法使", "255=覺醒者", "256=野獸覺醒者",
		"257=獸化覺醒者", "258=餓狼覺醒者", "259=狂狼覺醒者", "264=闇天使暗鴉武士", "265=初階武士",
		"266=正規武士", "267=浪人", "268=暗鴉武士"
	};

	// ★★★ 自訂職業譯名覆寫：看到「職業(246)」這種，就加一行 "246=你要的名稱" ★★★
	private static final String[] CLASS_OVERRIDE =
	{
		// "246=未知職業",
	};
	
	static
	{
		loadClassNames();
		System.out.println("[HomeBoard] CUSTOM BUILD V19 LOADED :: _bbsinfo / _bbsrank / classes=" + CLASS_NAMES.size());
		System.out.println("[HomeBoard] 伺服器名稱: " + readServerName() + " / 倍率: " + readServerRate());
	}
	// ===== END CUSTOM =====
	
	private static final String[] COMMANDS =
	{
		"_bbshome",
		"_bbstop",
		"_bbsinfo",
		"_bbsrank",
	};
	
	private static final String[] CUSTOM_COMMANDS =
	{
		PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && CommunityBoardConfig.COMMUNITY_PREMIUM_SYSTEM_ENABLED ? "_bbspremium" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS ? "_bbsexcmultisell" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS ? "_bbsmultisell" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS ? "_bbssell" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_TELEPORTS ? "_bbsteleport" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_BUFFS ? "_bbsbuff" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_HEAL ? "_bbsheal" : null,
		CommunityBoardConfig.COMMUNITYBOARD_ENABLE_DELEVEL ? "_bbsdelevel" : null
	};
	
	private static final BiPredicate<String, Player> COMBAT_CHECK = (command, player) ->
	{
		boolean commandCheck = false;
		for (String c : CUSTOM_COMMANDS)
		{
			if ((c != null) && command.startsWith(c))
			{
				commandCheck = true;
				break;
			}
		}
		
		return commandCheck && (player.isCastingNow() || player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || (player.getPvpFlag() > 0) || player.isAlikeDead() || player.isOnEvent() || player.isInStoreMode());
	};
	
	private static final Predicate<Player> KARMA_CHECK = player -> CommunityBoardConfig.COMMUNITYBOARD_KARMA_DISABLED && (player.getReputation() < 0);
	
	@Override
	public String[] getCommandList()
	{
		final List<String> commands = new ArrayList<>();
		commands.addAll(Arrays.asList(COMMANDS));
		commands.addAll(Arrays.asList(CUSTOM_COMMANDS));
		return commands.stream().filter(Objects::nonNull).toArray(String[]::new);
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (CommunityBoardConfig.COMMUNITYBOARD_COMBAT_DISABLED && COMBAT_CHECK.test(command, player))
		{
			player.sendMessage("You can't use the Community Board right now.");
			return false;
		}
		
		if (KARMA_CHECK.test(player))
		{
			player.sendMessage("Players with Karma cannot use the Community Board.");
			return false;
		}
		
		if (CommunityBoardConfig.COMMUNITYBOARD_PEACE_ONLY && !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("Community Board cannot be used out of peace zone.");
			return false;
		}
		
		String returnHtml = null;
		String navigation = null;
		
		if (CommunityBoardConfig.CUSTOM_CB_ENABLED)
		{
			navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		}
		
		if (command.startsWith("_bbstop;"))
		{
			final String customPath = CommunityBoardConfig.CUSTOM_CB_ENABLED ? "Custom/" : "";
			final String path = command.replace("_bbstop;", "");
			if (!path.isEmpty() && path.endsWith(".html"))
			{
				String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + customPath + path);
				
				if ((html == null) || html.isEmpty())
				{
					final String alt = findFirstHtml(customPath, path);
					if (alt != null)
					{
						html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + customPath + alt);
						if (html != null)
						{
							System.out.println("[HomeBoard] 自動導向: " + path + " -> " + alt);
						}
					}
				}
				
				if ((html == null) || html.isEmpty())
				{
					html = buildMissingPage(path);
				}
				
				if ((html != null) && !html.isEmpty())
				{
					final String lower = path.toLowerCase();
					if (lower.contains("rank/"))
					{
						final String type = path.substring(path.lastIndexOf('/') + 1).replace(".html", "");
						html = fillRank(html, type);
					}
					else if (lower.contains("info") || lower.contains("home") || html.contains("%onlineAll%") || html.contains("%serverUpTime%") || html.contains("%gameTime%"))
					{
						html = fillServerInfo(html);
					}
					
					html = fillPlayerInfo(html, player);
					returnHtml = html;
				}
			}
		}
		else if (command.startsWith("_bbshome") || command.equals("_bbstop"))
		{
			final String customPath = CommunityBoardConfig.CUSTOM_CB_ENABLED ? "Custom/" : "";
			CommunityBoardHandler.getInstance().addBypass(player, "Home", command);
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + customPath + "home.html");
			if (!CommunityBoardConfig.CUSTOM_CB_ENABLED)
			{
				returnHtml = returnHtml.replace("%fav_count%", Integer.toString(getFavoriteCount(player)));
				returnHtml = returnHtml.replace("%region_count%", Integer.toString(getRegionCount(player)));
				returnHtml = returnHtml.replace("%clan_count%", Integer.toString(ClanTable.getInstance().getClanCount()));
			}
			
			if (returnHtml != null)
			{
				if (returnHtml.contains("%onlineAll%") || returnHtml.contains("%serverUpTime%") || returnHtml.contains("%gameTime%"))
				{
					returnHtml = fillServerInfo(returnHtml);
				}
				returnHtml = fillPlayerInfo(returnHtml, player);
			}
		}
		else if (command.startsWith("_bbsinfo"))
		{
			String path = command.replace("_bbsinfo;", "");
			if (path.isEmpty() || !path.endsWith(".html") || path.contains(".."))
			{
				path = "info.html";
			}
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + path);
			if ((html == null) || html.isEmpty())
			{
				html = HtmCache.getInstance().getHtm(player, INFO_PATH);
			}
			html = fillServerInfo(html);
			html = fillPlayerInfo(html, player);
			returnHtml = html;
		}
		else if (command.startsWith("_bbsrank"))
		{
			String path = command.replace("_bbsrank;", "");
			if (path.isEmpty() || !path.endsWith(".html") || path.contains(".."))
			{
				path = "rank/level.html";
			}
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + path);
			if ((html != null) && !html.isEmpty())
			{
				String type = path.substring(path.lastIndexOf('/') + 1).replace(".html", "");
				returnHtml = fillRank(html, type);
			}
		}
		else if (command.startsWith("_bbsmultisell"))
		{
			final String fullBypass = command.replace("_bbsmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
			ThreadPool.schedule(() -> MultisellData.getInstance().separateAndSend(multisellId, player, null, false), 100);
		}
		else if (command.startsWith("_bbsexcmultisell"))
		{
			final String fullBypass = command.replace("_bbsexcmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
			ThreadPool.schedule(() -> MultisellData.getInstance().separateAndSend(multisellId, player, null, true), 100);
		}
		else if (command.startsWith("_bbssell"))
		{
			final String page = command.replace("_bbssell;", "");
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
			ThreadPool.schedule(() ->
			{
				player.sendPacket(new ExBuySellList(BuyListData.getInstance().getBuyList(423), player, 0));
				player.sendPacket(new ExBuySellList(player, false));
			}, 100);
		}
		else if (command.startsWith("_bbsteleport"))
		{
			final String teleBuypass = command.replace("_bbsteleport;", "");
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < CommunityBoardConfig.COMMUNITYBOARD_TELEPORT_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else if (CommunityBoardConfig.COMMUNITY_AVAILABLE_TELEPORTS.get(teleBuypass) != null)
			{
				player.disableAllSkills();
				player.sendPacket(new ShowBoard());
				player.destroyItemByItemId(ItemProcessType.FEE, CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_TELEPORT_PRICE, player, true);
				player.setInstanceById(0);
				player.teleToLocation(CommunityBoardConfig.COMMUNITY_AVAILABLE_TELEPORTS.get(teleBuypass), 0);
				ThreadPool.schedule(player::enableAllSkills, 3000);
			}
		}
		else if (command.startsWith("_bbsbuff"))
		{
			final String fullBypass = command.replace("_bbsbuff;", "");
			final String[] buypassOptions = fullBypass.split(";");
			final int buffCount = buypassOptions.length - 1;
			final String page = buypassOptions[buffCount];
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < (CommunityBoardConfig.COMMUNITYBOARD_BUFF_PRICE * buffCount))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId(ItemProcessType.FEE, CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_BUFF_PRICE * buffCount, player, true);
				final Pet pet = player.getPet();
				final List<Creature> targets = new ArrayList<>(4);
				targets.add(player);
				if (pet != null)
				{
					targets.add(pet);
				}
				
				player.getServitors().values().forEach(targets::add);
				
				for (int i = 0; i < buffCount; i++)
				{
					final Skill skill = SkillData.getInstance().getSkill(Integer.parseInt(buypassOptions[i].split(",")[0]), Integer.parseInt(buypassOptions[i].split(",")[1]));
					if (!CommunityBoardConfig.COMMUNITY_AVAILABLE_BUFFS.contains(skill.getId()))
					{
						continue;
					}
					
					for (Creature target : targets)
					{
						if (skill.isSharedWithSummon() || target.isPlayer())
						{
							skill.applyEffects(player, target);
							if (CommunityBoardConfig.COMMUNITYBOARD_CAST_ANIMATIONS)
							{
								player.sendPacket(new MagicSkillUse(player, target, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
							}
						}
					}
				}
			}
			
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
		}
		else if (command.startsWith("_bbsheal"))
		{
			final String page = command.replace("_bbsheal;", "");
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < (CommunityBoardConfig.COMMUNITYBOARD_HEAL_PRICE))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId(ItemProcessType.FEE, CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_HEAL_PRICE, player, true);
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				if (player.hasPet())
				{
					player.getPet().setCurrentHp(player.getPet().getMaxHp());
					player.getPet().setCurrentMp(player.getPet().getMaxMp());
					player.getPet().setCurrentCp(player.getPet().getMaxCp());
				}
				
				for (Summon summon : player.getServitors().values())
				{
					summon.setCurrentHp(summon.getMaxHp());
					summon.setCurrentMp(summon.getMaxMp());
					summon.setCurrentCp(summon.getMaxCp());
				}
				
				player.sendMessage("You used heal!");
			}
			
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
		}
		else if (command.equals("_bbsdelevel"))
		{
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < CommunityBoardConfig.COMMUNITYBOARD_DELEVEL_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else if (player.getLevel() == 1)
			{
				player.sendMessage("You are at minimum level!");
			}
			else
			{
				player.destroyItemByItemId(ItemProcessType.FEE, CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_DELEVEL_PRICE, player, true);
				final int newLevel = player.getLevel() - 1;
				player.setExp(ExperienceData.getInstance().getExpForLevel(newLevel));
				player.getStat().setLevel(newLevel);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				if (CommunityBoardConfig.COMMUNITYBOARD_DELEVEL_REMOVE_ABILITIES)
				{
					for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
					{
						final Skill skill = player.getKnownSkill(sk.getSkillId());
						if (skill != null)
						{
							player.removeSkill(skill);
						}
					}
					
					player.setAbilityPointsUsed(0, true);
					player.setHonorPointsUsed(0, true);
					player.sendPacket(new ExAcquireAPSkillList(player));
				}
				
				player.broadcastUserInfo();
				player.checkPlayerSkills();
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/delevel/complete.html");
				player.sendMessage("Your level is set to " + newLevel + "!");
			}
		}
		else if (command.startsWith("_bbspremium"))
		{
			final String fullBypass = command.replace("_bbspremium;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int premiumDays = Integer.parseInt(buypassOptions[0]);
			if ((premiumDays < 1) || (premiumDays > 30) || (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITY_PREMIUM_COIN_ID, -1) < (CommunityBoardConfig.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays)))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				final String premiumKey = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName() : player.getName();
				player.destroyItemByItemId(ItemProcessType.FEE, CommunityBoardConfig.COMMUNITY_PREMIUM_COIN_ID, CommunityBoardConfig.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays, player, true);
				PremiumManager.getInstance().addPremiumTime(premiumKey, premiumDays, TimeUnit.DAYS);
				player.sendMessage("You will now have premium status until " + PREMIUM_FORMAT.format(Instant.ofEpochMilli(PremiumManager.getInstance().getPremiumExpiration(premiumKey)).atZone(java.time.ZoneId.systemDefault())) + ".");
				if (PremiumSystemConfig.PC_CAFE_RETAIL_LIKE)
				{
					PcCafePointsManager.getInstance().run(player);
				}
				
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/premium/thankyou.html");
			}
		}
		
		if (returnHtml != null)
		{
			if (CommunityBoardConfig.CUSTOM_CB_ENABLED && (navigation != null))
			{
				returnHtml = returnHtml.replace("%navigation%", navigation);
			}
			
			returnHtml = fillServerInfo(returnHtml);
			returnHtml = fillPlayerInfo(returnHtml, player);
			returnHtml = fillExtraPlaceholders(returnHtml, player);
			
			CommunityBoardHandler.separateAndSend(returnHtml, player);
		}
		
		return false;
	}
	
	// ===== CUSTOM: Server Info / Rank helpers =====
	
	private static void loadClassNames()
	{
		for (String s : DEFAULT_CLASS_NAMES)
		{
			putClassName(s);
		}
		
		for (String s : CLASS_OVERRIDE)
		{
			putClassName(s);
		}
	}
	
	private static void putClassName(String s)
	{
		if ((s == null) || s.trim().isEmpty())
		{
			return;
		}
		
		final int p = s.indexOf('=');
		if (p > 0)
		{
			try
			{
				CLASS_NAMES.put(Integer.parseInt(s.substring(0, p).trim()), s.substring(p + 1).trim());
			}
			catch (Exception e)
			{
				// ignore
			}
		}
	}
	
	private static String findFirstHtml(String customPath, String path)
	{
		try
		{
			final int slash = path.lastIndexOf('/');
			if (slash < 0)
			{
				return null;
			}
			
			final String dir = "data/html/CommunityBoard/" + customPath + path.substring(0, slash + 1);
			final File[] files = new File(dir).listFiles();
			if (files == null)
			{
				return null;
			}
			
			String first = null;
			for (File f : files)
			{
				if (!f.isFile())
				{
					continue;
				}
				
				final String name = f.getName();
				final String lower = name.toLowerCase();
				if (lower.endsWith(".html") || lower.endsWith(".htm"))
				{
					final String rel = path.substring(0, slash + 1) + name;
					if (lower.startsWith("main") || lower.startsWith("index"))
					{
						return rel;
					}
					if (first == null)
					{
						first = rel;
					}
				}
			}
			return first;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	private static String buildMissingPage(String path)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html noscrollbar><body>");
		sb.append("<center><br><font color=LEVEL>找不到頁面</font><br1>");
		sb.append("<font color=\"009393\">要求：</font><font color=\"EEDD82\">").append(path).append("</font><br1>");
		sb.append("<font color=\"009393\">路徑：</font><font color=\"EEDD82\">data/html/CommunityBoard/Custom/").append(path).append("</font><br><br>");
		
		final int slash = path.lastIndexOf('/');
		final String relDir = (slash < 0) ? "" : path.substring(0, slash + 1);
		final File dir = new File("data/html/CommunityBoard/Custom/" + relDir);
		final File[] files = dir.listFiles();
		
		if (files == null)
		{
			sb.append("<font color=ff5151>資料夾不存在：</font>").append(dir.getPath()).append("<br>");
			sb.append("<font color=\"009393\">根目錄實際內容：</font><br>");
			final File[] root = new File("data/html/CommunityBoard/Custom").listFiles();
			if (root != null)
			{
				for (File f : root)
				{
					sb.append("<font color=\"EEDD82\">").append(f.isDirectory() ? "[DIR] " : "").append(f.getName()).append("</font><br1>");
				}
			}
		}
		else
		{
			sb.append("<font color=\"009393\">此資料夾實際檔案（點了就能開）：</font><br>");
			for (File f : files)
			{
				final String name = f.getName();
				final String lower = name.toLowerCase();
				if (f.isDirectory())
				{
					sb.append("<font color=\"009393\">[DIR] ").append(name).append("</font><br1>");
				}
				else if (lower.endsWith(".html") || lower.endsWith(".htm"))
				{
					sb.append("<button value=\"").append(relDir).append(name).append("\" action=\"bypass _bbstop;").append(relDir).append(name).append("\" width=300 height=20 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\"><br1>");
				}
				else
				{
					sb.append("<font color=\"EEDD82\">").append(name).append("</font><br1>");
				}
			}
		}
		
		sb.append("<br><button value=\"回上一頁\" action=\"bypass _bbshome\" width=150 height=22 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
		sb.append("</center></body></html>");
		
		System.out.println("[HomeBoard] 找不到頁面: data/html/CommunityBoard/Custom/" + path + "  (列出目錄)");
		return sb.toString();
	}
	
	private static String getClassName(int classId)
	{
		final String n = CLASS_NAMES.get(classId);
		if (n != null)
		{
			return n;
		}
		
		final String fromData = queryClassNameFromServer(classId);
		if (fromData != null)
		{
			return fromData;
		}
		
		System.out.println("[HomeBoard] 未知職業 classId=" + classId + " ，請在 CLASS_OVERRIDE 加一行：\"" + classId + "=你的譯名\"");
		return "ID:" + classId;
	}
	
	private static String queryClassNameFromServer(int classId)
	{
		try
		{
			final Class<?> cld = Class.forName("org.l2jmobius.gameserver.data.xml.ClassListData");
			Object inst = null;
			try
			{
				inst = cld.getMethod("getInstance").invoke(null);
			}
			catch (Exception e)
			{
				try
				{
					inst = cld.getDeclaredConstructor().newInstance();
				}
				catch (Exception e2)
				{
					return null;
				}
			}
			
			if (inst == null)
			{
				return null;
			}
			
			Object pc = null;
			for (String m : new String[] { "getClass", "getClassById", "getPlayerClass", "getClassInfo" })
			{
				try
				{
					pc = cld.getMethod(m, int.class).invoke(inst, classId);
					if (pc != null)
					{
						break;
					}
				}
				catch (Exception e)
				{
					// try next
				}
			}
			
			if (pc == null)
			{
				return null;
			}
			
			for (String m : new String[] { "getClassName", "getClientName", "getName", "toString" })
			{
				try
				{
					final Object v = pc.getClass().getMethod(m).invoke(pc);
					if ((v != null) && !v.toString().isEmpty() && !v.toString().equals("null"))
					{
						return v.toString();
					}
				}
				catch (Exception e)
				{
					// try next
				}
			}
		}
		catch (Exception e)
		{
			// ClassListData 不存在 -> 靜默略過
		}
		return null;
	}
	
	private static int queryInt(String sql, int defValue)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				return rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			System.out.println("[HomeBoard] queryInt failed [" + sql + "] " + e.getMessage());
		}
		return defValue;
	}
	
	private static String readServerName()
	{
		// 優先用伺服器的 LoginServerThread 取得名稱（與 AdminLogin 的 %server_name% 同源）
		try
		{
			final Class<?> c = Class.forName("org.l2jmobius.gameserver.LoginServerThread");
			final Object inst = c.getMethod("getInstance").invoke(null);
			final Object v = c.getMethod("getServerName").invoke(inst);
			if ((v != null) && !v.toString().trim().isEmpty())
			{
				final String n = v.toString().trim();
				return n;
			}
		}
		catch (Throwable t)
		{
			// 類別或方法不存在 -> 用本檔常數（靜默，不洗版 log）
		}
		return SERVER_NAME;
	}
	
	private static String getUpTime()
	{
		long sec = (System.currentTimeMillis() - SERVER_START) / 1000;
		final long d = sec / 86400;
		sec %= 86400;
		final long h = sec / 3600;
		sec %= 3600;
		final long m = sec / 60;
		if (d > 0)
		{
			return d + " 天 " + h + " 小時 " + m + " 分";
		}
		return h + " 小時 " + m + " 分";
	}
	
	private static String getGameTime()
	{
		try
		{
			final Class<?> c = Class.forName("org.l2jmobius.gameserver.GameTimeController");
			final Object inst = c.getMethod("getInstance").invoke(null);
			final int t = ((Number) c.getMethod("getGameTime").invoke(inst)).intValue();
			return String.format("%02d:%02d", (t / 60) % 24, t % 60);
		}
		catch (Throwable t)
		{
			return "--:--";
		}
	}
	
	// ★★★ 伺服器版本顯示 ★★★
	private static final String SERVER_VERSION = "(542)暗鴉武士";
	// ★★★ Exp/SP 倍率：抓不到伺服器設定時才用這個值 ★★★
	private static final String SERVER_RATE_FALLBACK = "1";
	
	private static final String Q_PLAYER_CLASS = "SELECT classid FROM characters WHERE charId=?";
	private static final String Q_PLAYER_CLAN = "SELECT c.clan_name AS clan_name FROM characters AS ch LEFT JOIN clan_data AS c ON ch.clanid = c.clan_id WHERE ch.charId=?";
	private static final String Q_PLAYER_TIME = "SELECT onlinetime FROM characters WHERE charId=?";
	
	private static String fillExtraPlaceholders(String html, Player player)
	{
		if ((html == null) || (player == null) || !html.contains("%"))
		{
			return html;
		}
		
		final String[][] pairs =
		{
			{ "playername", player.getName() },
			{ "playerlevel", Integer.toString(player.getLevel()) },
			{ "servername", SERVER_NAME },
			{ "serverver", SERVER_VERSION },
			{ "serverrate", readServerRate() },
		};
		
		for (String[] kv : pairs)
		{
			if (kv[1] == null)
			{
				continue;
			}
			html = replaceIgnoreCase(html, kv[0], kv[1]);
		}
		
		if (DEBUG_LOG_PLACEHOLDERS)
		{
			final java.util.regex.Matcher m = java.util.regex.Pattern.compile("%[A-Za-z_][A-Za-z0-9_]*%").matcher(html);
			final java.util.Set<String> left = new java.util.HashSet<>();
			while (m.find())
			{
				final String ph = m.group();
				if (!OTHER_BOARD_PLACEHOLDERS.contains(ph.toLowerCase()))
				{
					left.add(ph);
				}
			}
			if (!left.isEmpty())
			{
				System.out.println("[HomeBoard] 未處理佔位符: " + left);
			}
		}
		
		return html;
	}
	
	private static String replaceIgnoreCase(String html, String key, String value)
	{
		return html.replaceAll("(?i)%" + key + "%", java.util.regex.Matcher.quoteReplacement(value));
	}
	
	private static String fillPlayerInfo(String html, Player player)
	{
		if ((html == null) || (player == null))
		{
			return html;
		}
		
		if (!html.contains("%player") && !html.contains("%serverrate") && !html.contains("%serverver") && !html.contains("%servername"))
		{
			return html;
		}
		
		final int objId = player.getObjectId();
		
		html = html.replace("%playername%", player.getName());
		html = html.replace("%playerlevel%", Integer.toString(player.getLevel()));
		
		String cls = "-";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(Q_PLAYER_CLASS))
		{
			ps.setInt(1, objId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					cls = getClassName(rs.getInt("classid"));
				}
			}
		}
		catch (Exception e)
		{
			// 欄位不存在也不影響其他顯示
		}
		html = html.replace("%playerclass%", cls);
		
		String clan = "無";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(Q_PLAYER_CLAN))
		{
			ps.setInt(1, objId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					final String n = rs.getString("clan_name");
					if ((n != null) && !n.isEmpty())
					{
						clan = n;
					}
				}
			}
		}
		catch (Exception e)
		{
			// 忽略
		}
		html = html.replace("%playerclan%", clan);
		
		long sec = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(Q_PLAYER_TIME))
		{
			ps.setInt(1, objId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					sec = rs.getLong("onlinetime");
				}
			}
		}
		catch (Exception e)
		{
			// 忽略
		}
		if (sec < 0)
		{
			sec = 0;
		}
		html = html.replace("%playedday%", Long.toString(sec / 86400L));
		html = html.replace("%playedhour%", Long.toString((sec / 3600L) % 24L));
		html = html.replace("%playedminute%", Long.toString((sec / 60L) % 60L));
		
		String premium = "一般會員";
		String premiumChr = "一般會員";
		try
		{
			if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
			{
				final String key = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName() : player.getName();
				final long expire = PremiumManager.getInstance().getPremiumExpiration(key);
				if (expire > System.currentTimeMillis())
				{
					premium = "尊榮會員";
					premiumChr = "尊榮";
				}
			}
		}
		catch (Exception e)
		{
			// 版本差異時略過
		}
		html = html.replace("%playerpremium%", premium);
		html = html.replace("%playerpremiumchr%", premiumChr);
		
		html = html.replace("%servername%", readServerName());
		html = html.replace("%serverver%", SERVER_VERSION);
		html = html.replace("%serverrate%", readServerRate());
		return html;
	}
	
	/**
	 * 讀取伺服器 Exp 倍率：RatesConfig（複數！）為 542 正確類別名。
	 */
	private static String readServerRate()
	{
		final String[] names =
		{
			"org.l2jmobius.gameserver.config.RatesConfig",
			"org.l2jmobius.gameserver.config.RateConfig",
			"org.l2jmobius.gameserver.config.custom.RatesConfig",
			"org.l2jmobius.gameserver.config.custom.RateConfig"
		};
		final String[] fields =
		{
			"RATE_XP", "XP", "RATE_XP_SP", "RATE_EXP"
		};
		for (String cn : names)
		{
			try
			{
				final Class<?> c = Class.forName(cn);
				for (String fn : fields)
				{
					try
					{
						final Object v = c.getField(fn).get(null);
						if (v != null)
						{
							// 去掉多餘的 .0，讓倍率顯示成 1000 而不是 1000.0
							String rate = String.valueOf(v);
							if (rate.endsWith(".0"))
							{
								rate = rate.substring(0, rate.length() - 2);
							}
							return rate;
						}
					}
					catch (NoSuchFieldException e2)
					{
						// 換下一個欄位名
					}
				}
			}
			catch (Exception e)
			{
				// 換下一個類別名
			}
		}
		// 抓不到就靜默使用預設值，不洗版 log
		return SERVER_RATE_FALLBACK;
	}
	
	private static String fillServerInfo(String html)
	{
		if (html == null)
		{
			return null;
		}
		
		final int online = queryInt("SELECT COUNT(*) FROM characters WHERE online=1", 0);
		final int offlineTrade = queryInt("SELECT COUNT(*) FROM character_offline_trade", 0);
		final int onlineGm = queryInt("SELECT COUNT(*) FROM characters WHERE online=1 AND accesslevel>0", 0);
		final int charCount = queryInt("SELECT COUNT(*) FROM characters", 0);
		final int clanCount = queryInt("SELECT COUNT(*) FROM clan_data", 0);
		
		html = html.replace("%serverName%", readServerName());
		html = html.replace("%servername%", readServerName());
		html = html.replace("%serverver%", SERVER_VERSION);
		html = html.replace("%serverrate%", readServerRate());
		html = html.replace("%onlineAll%", Integer.toString(online));
		html = html.replace("%offlineTrade%", Integer.toString(offlineTrade));
		html = html.replace("%onlineGM%", Integer.toString(onlineGm));
		html = html.replace("%charCount%", Integer.toString(charCount));
		html = html.replace("%clanCount%", Integer.toString(clanCount));
		html = html.replace("%gameTime%", getGameTime());
		html = html.replace("%serverTime%", DateTimeFormatter.ofPattern("HH:mm").format(Instant.now().atZone(java.time.ZoneId.systemDefault())));
		html = html.replace("%serverUpTime%", getUpTime());
		return html;
	}
	
	private static String rankSql(String type)
	{
		switch (type)
		{
			case "adena":
				return "SELECT c.char_name AS nm, c.classid AS cid, c.online AS onl, cl.clan_name AS cn, COALESCE(SUM(i.count),0) AS cnt FROM characters c LEFT JOIN clan_data cl ON c.clanid=cl.clan_id LEFT JOIN items i ON i.owner_id=c.charId AND i.item_id=57 WHERE c.accesslevel>=0 GROUP BY c.charId, c.char_name, c.classid, c.online, cl.clan_name ORDER BY cnt DESC LIMIT " + RANK_LIMIT;
			case "pk":
				return "SELECT c.char_name AS nm, c.classid AS cid, c.online AS onl, cl.clan_name AS cn, c.pkkills AS cnt FROM characters c LEFT JOIN clan_data cl ON c.clanid=cl.clan_id WHERE c.accesslevel>=0 ORDER BY c.pkkills DESC LIMIT " + RANK_LIMIT;
			case "pvp":
				return "SELECT c.char_name AS nm, c.classid AS cid, c.online AS onl, cl.clan_name AS cn, c.pvpkills AS cnt FROM characters c LEFT JOIN clan_data cl ON c.clanid=cl.clan_id WHERE c.accesslevel>=0 ORDER BY c.pvpkills DESC LIMIT " + RANK_LIMIT;
			case "raidboss":
				return "SELECT c.char_name AS nm, c.classid AS cid, c.online AS onl, cl.clan_name AS cn, c.raidbosspoints AS cnt FROM characters c LEFT JOIN clan_data cl ON c.clanid=cl.clan_id WHERE c.accesslevel>=0 ORDER BY c.raidbosspoints DESC LIMIT " + RANK_LIMIT;
			default:
				return "SELECT c.char_name AS nm, c.classid AS cid, c.online AS onl, cl.clan_name AS cn, c.level AS cnt FROM characters c LEFT JOIN clan_data cl ON c.clanid=cl.clan_id WHERE c.accesslevel>=0 ORDER BY c.level DESC LIMIT " + RANK_LIMIT;
		}
	}
	
	private static String fillRank(String html, String type)
	{
		String work = html;
		int idx = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(rankSql(type));
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next() && (idx < RANK_LIMIT))
			{
				String nm = rs.getString("nm");
				if (nm == null)
				{
					nm = "-";
				}
				String cn = rs.getString("cn");
				if ((cn == null) || cn.isEmpty())
				{
					cn = "-";
				}
				final String cid = getClassName(rs.getInt("cid"));
				final String onl = (rs.getInt("onl") == 1) ? "線上" : "離線";
				final String cnt = Long.toString(rs.getLong("cnt"));
				
				work = work.replace("%name_" + idx + "%", nm);
				work = work.replace("%clan_" + idx + "%", cn);
				work = work.replace("%class_" + idx + "%", cid);
				work = work.replace("%on_" + idx + "%", onl);
				work = work.replace("%count_" + idx + "%", cnt);
				idx++;
			}
		}
		catch (Exception e)
		{
			System.out.println("[HomeBoard] rank query failed (" + type + "): " + e.getMessage());
		}
		
		for (int i = idx; i < RANK_LIMIT; i++)
		{
			work = work.replace("%name_" + i + "%", "-");
			work = work.replace("%clan_" + i + "%", "-");
			work = work.replace("%class_" + i + "%", "-");
			work = work.replace("%on_" + i + "%", "-");
			work = work.replace("%count_" + i + "%", "-");
		}
		
		return work;
	}
	// ===== END CUSTOM =====
	
	private static int getFavoriteCount(Player player)
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(COUNT_FAVORITES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					count = rs.getInt("favorites");
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("[HomeBoard] Coudn't load favorites count for " + player);
		}
		
		return count;
	}
	
	private static int getRegionCount(Player player)
	{
		return 0;
	}
}
