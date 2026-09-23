/*
 * Copyright (c) 2013 L2jMobius
 */
package handlers.bypass.communityboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class DropSearchBoard implements IParseBoardHandler
{
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String[] COMMAND = { "_bbs_search_item", "_bbs_search_drop" };
	private final Map<Integer, List<CBDropHolder>> DROP_INDEX_CACHE = new HashMap<>();
	private static final Set<Integer> BLOCK_ID = new HashSet<>();
	static { BLOCK_ID.add(Inventory.ADENA_ID); }
	
	private class CBDropHolder
	{
		final int itemId, npcId, npcLevel;
		final long min, max;
		final double chance;
		final boolean isSpoil, isRaid;
		public CBDropHolder(NpcTemplate npcTemplate, DropHolder dropHolder)
		{
			isSpoil = dropHolder.getDropType() == DropType.SPOIL;
			itemId = dropHolder.getItemId();
			npcId = npcTemplate.getId();
			npcLevel = npcTemplate.getLevel();
			min = dropHolder.getMin();
			max = dropHolder.getMax();
			chance = dropHolder.getChance();
			isRaid = npcTemplate.getType().equals("RaidBoss") || npcTemplate.getType().equals("GrandBoss");
		}
	}
	
	public DropSearchBoard() { buildDropIndex(); }
	private void buildDropIndex()
	{
		NpcData.getInstance().getTemplates(npc -> npc.getDropGroups() != null).forEach(npcTemplate -> {
			for (DropGroupHolder dropGroup : npcTemplate.getDropGroups()) {
				final double chance = dropGroup.getChance() / 100;
				for (DropHolder dropHolder : dropGroup.getDropList()) {
					addToDropList(npcTemplate, new DropHolder(dropHolder.getDropType(), dropHolder.getItemId(), dropHolder.getMin(), dropHolder.getMax(), dropHolder.getChance() * chance));
				}
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getDropList() != null).forEach(npcTemplate -> {
			for (DropHolder dropHolder : npcTemplate.getDropList()) { addToDropList(npcTemplate, dropHolder); }
		});
		NpcData.getInstance().getTemplates(npc -> npc.getSpoilList() != null).forEach(npcTemplate -> {
			for (DropHolder dropHolder : npcTemplate.getSpoilList()) { addToDropList(npcTemplate, dropHolder); }
		});
		DROP_INDEX_CACHE.values().forEach(l -> l.sort((d1, d2) -> Integer.compare(l.size(), d2.npcLevel)));
	}
	
	private void addToDropList(NpcTemplate npcTemplate, DropHolder dropHolder)
	{
		if (BLOCK_ID.contains(dropHolder.getItemId())) return;
		List<CBDropHolder> dropList = DROP_INDEX_CACHE.computeIfAbsent(dropHolder.getItemId(), k -> new ArrayList<>());
		dropList.add(new CBDropHolder(npcTemplate, dropHolder));
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		final String navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		final String[] params = command.split(" ");
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/dropsearch/main.html");
		
		if (command.contains("_bbs_search_item"))
		{
			html = html.replace("%searchResult%", buildItemSearchResult(buildItemName(params)));
		}
		else if (command.contains("_bbs_search_drop"))
		{
			int parsedItemId = 0, parsedPage = 1;
			try {
				if (params != null && params.length > 1) parsedItemId = Integer.parseInt(String.valueOf(java.lang.reflect.Array.get(params, 1)).trim());
				if (params != null && params.length > 2) parsedPage = Integer.parseInt(String.valueOf(java.lang.reflect.Array.get(params, 2)).trim());
			} catch (Exception e) {}
			final int itemId = parsedItemId;
			final List<CBDropHolder> list = DROP_INDEX_CACHE.get(itemId);
			int pages = (list != null) ? (list.size() / 14) : 0;
			if (pages == 0) pages++;
			final int start = (parsedPage - 1) * 14;
			final int end = (list != null) ? Math.min(list.size() - 1, start + 14) : -1;
			final StringBuilder builder = new StringBuilder();
			final PlayerStat stat = player.getStat();
			final double dropAdena = stat.getMul(Stat.BONUS_DROP_ADENA, 1);
			final double dropAmt = stat.getMul(Stat.BONUS_DROP_AMOUNT, 1);
			final double dropRate = stat.getMul(Stat.BONUS_DROP_RATE, 1);
			final double spoilRate = stat.getMul(Stat.BONUS_SPOIL_RATE, 1);
			if (list != null && end >= 0)
			{
				final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
				for (int index = start; index <= end; index++)
				{
					final CBDropHolder cbDropHolder = list.get(index);
					double rateChance = 1, rateAmount = 1;
					if (cbDropHolder.isSpoil) {
						rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER * spoilRate;
						rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus()) {
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
							rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
						}
					} else {
						final ItemTemplate item = ItemData.getInstance().getTemplate(cbDropHolder.itemId);
						if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null) {
							rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							if ((cbDropHolder.itemId == Inventory.ADENA_ID) && (rateChance > 100)) rateChance = 100;
						}
						else if (item.hasExImmediateEffect()) rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
						else if (cbDropHolder.isRaid) rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
						else rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
						
						if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null) rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
						else if (item.hasExImmediateEffect()) rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
						else if (cbDropHolder.isRaid) rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
						else rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
						
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus()) {
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null) rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							else if (!item.hasExImmediateEffect() && !cbDropHolder.isRaid) rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null) rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
							else if (!item.hasExImmediateEffect() && !cbDropHolder.isRaid) rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
						}
						rateAmount *= dropAmt;
						if (item.getId() == Inventory.ADENA_ID) rateAmount *= dropAdena;
						rateChance *= dropRate;
					}
					builder.append("<" + "tr" + ">");
					builder.append("<" + "td width=30" + ">").append(cbDropHolder.npcLevel).append("<" + "/td" + ">");
					
					// 🌟【終極通關通道】直接拋出原廠絕對支援的 admin_search_location 封包，強行獲取地圖引導！
					builder.append("<" + "td width=170" + ">").append("<" + "a action=\"bypass -h admin_search_location " + cbDropHolder.npcId + "\"" + ">").append("&@").append(cbDropHolder.npcId).append(';').append("<" + "/a" + ">").append("<" + "/td" + ">");
					
					builder.append("<" + "td width=80 align=CENTER" + ">").append(cbDropHolder.min * rateAmount).append('-').append(cbDropHolder.max * rateAmount).append("<" + "/td" + ">");
					builder.append("<" + "td width=50 align=CENTER" + ">").append(chanceFormat.format(cbDropHolder.chance * rateChance)).append('%').append("<" + "/td" + ">");
					builder.append("<" + "td width=50 align=CENTER" + ">").append(cbDropHolder.isSpoil ? "Spoil" : "Drop").append("<" + "/td" + ">");
					builder.append("<" + "/tr" + ">");
				}
			}
			html = html.replace("%searchResult%", builder.toString());
			builder.setLength(0);
			builder.append("<" + "tr" + ">");
			for (int p = 1; p <= pages; p++) {
				builder.append("<" + "td" + ">").append("<" + "a action=\"bypass -h _bbs_search_drop " + itemId + " " + p + " $order $level\"" + ">").append(p).append("<" + "/a" + ">").append("<" + "/td" + ">");
			}
			builder.append("<" + "/tr" + ">");
			html = html.replace("%pages%", builder.toString());
		}
		if (html != null) {
			html = html.replace("%navigation%", navigation);
			CommunityBoardHandler.separateAndSend(html, player);
		}
		return false;
	}
	
	private String buildItemSearchResult(String itemName)
	{
		final Set<Integer> existInDropData = DROP_INDEX_CACHE.keySet();
		final List<ItemTemplate> items = new ArrayList<>();
		for (ItemTemplate item : ItemData.getInstance().getAllItems()) {
			if (item == null || !existInDropData.contains(item.getId())) continue;
			if (item.getName().toLowerCase().contains(itemName.toLowerCase())) {
				items.add(item);
				if (items.size() == 14) break;
			}
		}
		if (items.isEmpty()) return "<" + "tr" + "><" + "td width=100 align=CENTER" + ">No Match<" + "/td" + "><" + "/tr" + ">";
		int line = 0, i = 0;
		final StringBuilder builder = new StringBuilder(items.size() * 28);
		for (ItemTemplate item : items) {
			i++;
			if (i == 1) { line++; builder.append("<" + "tr" + ">"); }
			String icon = item.getIcon();
			if (icon == null) icon = "icon.etc_question_mark_i00";
			builder.append("<" + "td" + "><" + "button value=\".\" action=\"bypass _bbs_search_drop " + item.getId() + " 1 $order $level\" width=32 height=32 back=\"" + icon + "\" fore=\"" + icon + "\"><" + "/td" + ">");
			builder.append("<" + "td width=200" + ">&#" + item.getId() + ";<" + "/td" + ">");
			if (i == 2) { builder.append("<" + "/tr" + ">"); i = 0; }
		}
		if ((i % 2) == 1) builder.append("<" + "/tr" + ">");
		if (line < 7) {
			for (i = 0; i < (7 - line); i++) builder.append("<" + "tr" + "><" + "td height=36" + "><" + "/td" + "><" + "/tr" + ">");
		}
		return builder.toString();
	}
	
	private String buildItemName(String[] params)
	{
		final StringJoiner joiner = new StringJoiner(" ");
		for (int i = 1; i < params.length; i++) {
			if (params != null) joiner.add(String.valueOf(java.lang.reflect.Array.get(params, i)));
		}
		return joiner.toString();
	}
	
	@Override
	public String[] getCommandList() { return COMMAND; }
}
