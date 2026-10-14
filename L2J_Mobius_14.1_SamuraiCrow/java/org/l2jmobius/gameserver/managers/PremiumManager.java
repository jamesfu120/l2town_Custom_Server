package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class PremiumManager
{
	private static final Logger LOGGER = Logger.getLogger(PremiumManager.class.getName());
	
	// 控制升級消耗的道具 ID (預設 6673)
	private static final int VIP_UPGRADE_ITEM_ID = 6673;
	
	// 每一級晉升需要收取的 6673 數量
	private static final Map<Integer, Integer> VIP_UPGRADE_COSTS = new HashMap<>();
	
	// 每一級發放的被動技能 ID
	private static final Map<Integer, Integer> VIP_PASSIVE_SKILLS = new HashMap<>();
	
	// 每一級點擊「晉升」時發放的【多重實體道具/寶箱大禮包】
	private static final Map<Integer, int[][]> VIP_REWARDS = new HashMap<>();
	
	// 專屬彩色稱號
	public static final Map<Integer, String> VIP_TITLES = new HashMap<>();
	
	// VIP 每日物資福利 (V3開始可以每日領取)
	private static final Map<Integer, int[]> VIP_DAILY_ITEMS = new HashMap<>();

	static
	{
		// 基礎升級與技能地基 (V1~V10)
		for (int i = 1; i <= 10; i++)
		{
			VIP_UPGRADE_COSTS.put(i, i * 1000);
			VIP_PASSIVE_SKILLS.put(i, 99100 + i);
		}
		
		// 稱號名稱設定
		VIP_TITLES.put(1, "初生之犢"); 
		VIP_TITLES.put(5, "天2老手"); 
		VIP_TITLES.put(10, "天2尊者"); 
		
		// 🌟【自訂設定區：VIP 一次性晉升多重禮包】
		VIP_REWARDS.put(1, new int[][] { {57, 1000000}, {95001, 1} }); // V1 送 100萬天幣 + 1個寶箱
		VIP_REWARDS.put(2, new int[][] { {57, 3000000}, {1232, 1} });
		VIP_REWARDS.put(3, new int[][] { {57, 5000000}, {95002, 1} }); // V3 送 500萬天幣 + 1個防具箱
		VIP_REWARDS.put(10, new int[][] { {57, 100000000}, {1232, 500} });
		
		// VIP 每日福利物資配置 (V3 級別起開放)
		VIP_DAILY_ITEMS.put(3, new int[] {57, 50000});     // V3 每日送 5萬金幣
		VIP_DAILY_ITEMS.put(4, new int[] {57, 100000});    // V4 每日送 10萬金幣
		VIP_DAILY_ITEMS.put(5, new int[] {6673, 5});       // V5 每日送 5個紀念章
		VIP_DAILY_ITEMS.put(10, new int[] {1232, 10});     // V10 每日送 10個錢幣
	}

	public int getVipLevel(Player player)
	{
		return player == null ? 0 : player.getVariables().getInt("Custom_Vip_Level", 0);
	}
	public void claimDailyReward(Player player)
	{
		if (player == null) return;
		final int vipLevel = getVipLevel(player);
		if (vipLevel < 3)
		{
			player.sendMessage("【資格不足】每日物資福利僅開放給 VIP 3 以上的尊榮會員。");
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		final long lastClaimTime = player.getVariables().getLong("Vip_Daily_Claim_Time", 0);
		if (lastClaimTime > 0)
		{
			final long hoursPassed = (currentTime - lastClaimTime) / (1000 * 60 * 60);
			if (hoursPassed < 24)
			{
				final long remainingHours = 24 - hoursPassed;
				player.sendMessage("【領取失敗】今天你已經領取過 VIP 福利了！請等待 " + remainingHours + " 小時後再度前來。");
				return;
			}
		}
		
		final int[] reward = VIP_DAILY_ITEMS.get(vipLevel);
		if (reward != null)
		{
			player.addItem(ItemProcessType.REWARD, reward[0], reward[1], player, true);
			player.getVariables().set("Vip_Daily_Claim_Time", currentTime);
			player.getVariables().storeMe();
			player.sendMessage("【領取成功】順利領取了 VIP " + vipLevel + " 級的每日修煉物資！");
		}
		displayVipWindow(player, 0);
	}

	public void selectVipTitle(Player player, int titleIndex)
	{
		if (player == null) return;
		final int vipLevel = getVipLevel(player);
		if (titleIndex > vipLevel || !VIP_TITLES.containsKey(titleIndex))
		{
			player.sendMessage("【配戴失敗】你的 VIP 等級不足，或該等級尚未解鎖專屬稱號。");
			return;
		}
		
		final String titleName = VIP_TITLES.get(titleIndex);
		player.setTitle(titleName);
		player.broadcastUserInfo();
		player.sendMessage("【稱號更換】成功配戴 VIP 專屬稱號：【" + titleName + "】！");
		
		org.l2jmobius.gameserver.handler.CommunityBoardHandler.getInstance().showTitleWardrobe(player);
	}

	public void requestVipUpgrade(Player player)
	{
		if (player == null) return;
		final int currentLevel = getVipLevel(player);
		if (currentLevel >= 10) return;
		
		final int nextLevel = currentLevel + 1;
		final int cost = VIP_UPGRADE_COSTS.getOrDefault(nextLevel, 999999);
		
		final Item item = player.getInventory().getItemByItemId(VIP_UPGRADE_ITEM_ID);
		if ((item == null) || (item.getCount() < cost))
		{
			player.sendMessage("【晉升失敗】你需要 " + cost + " 個節慶紀念章來晉升至 VIP " + nextLevel + "！");
			return;
		}
		
		player.getInventory().destroyItem(ItemProcessType.DESTROY, item, cost, player, null);
		player.getVariables().set("Custom_Vip_Level", nextLevel);
		player.getVariables().storeMe();
		rewardVipSkills(player);
		
		if (VIP_REWARDS.containsKey(nextLevel))
		{
			for (int[] rewardInfo : VIP_REWARDS.get(nextLevel))
			{
				player.addItem(ItemProcessType.REWARD, rewardInfo[0], rewardInfo[1], player, true);
			}
		}
		
		player.sendMessage("【晉升成功】恭喜你成功晉升至 VIP " + nextLevel + " 級別！");
		player.broadcastUserInfo();
		player.broadcastStatusUpdate();
		
		displayVipWindow(player, 0);
	}

	public void rewardVipSkills(Player player)
	{
		if (player == null) return;
		final int currentLevel = getVipLevel(player);
		for (int skillId : VIP_PASSIVE_SKILLS.values())
		{
			final Skill oldSkill = player.getKnownSkill(skillId);
			if (oldSkill != null) player.removeSkill(oldSkill, true);
		}
		if (currentLevel > 0 && VIP_PASSIVE_SKILLS.containsKey(currentLevel))
		{
			final Skill newSkill = SkillData.getInstance().getSkill(VIP_PASSIVE_SKILLS.get(currentLevel), 1);
			if (newSkill != null) player.addSkill(newSkill, true);
		}
		player.sendSkillList();
	}
	public void displayVipWindow(Player player, int objectId)
	{
		if (player == null) return;
		final int currentLevel = getVipLevel(player);
		final long tokenCount = player.getInventory().getItemByItemId(VIP_UPGRADE_ITEM_ID) != null ? player.getInventory().getItemByItemId(VIP_UPGRADE_ITEM_ID).getCount() : 0;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body><center><br><br>");
		sb.append("<font color=\"LEVEL\">【 VIP 尊榮修煉中心 】</font><br><br>");
		
		sb.append("<table width=260 border=0 cellpadding=1 cellspacing=1>");
		sb.append("<tr><td align=center>目前等級：<font color=\"00FF00\">VIP ").append(currentLevel).append("</font></td></tr>");
		sb.append("<tr><td align=center>擁有紀念章：<font color=\"FFFF99\">").append(tokenCount).append(" 個</font></td></tr>");
		sb.append("</table><br>");
		
		if (currentLevel >= 3)
		{
			sb.append("<button value=\"領取每日修煉物資\" action=\"bypass _bbs_vip_daily\" width=140 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
		}
		
		sb.append("<img src=\"L2UI.SquareBlank\" width=260 height=1><br><br>");
		
		if (currentLevel < 10)
		{
			final int nextLevel = currentLevel + 1;
			sb.append("<table width=260 border=0 cellpadding=1 cellspacing=1>");
			sb.append("<tr><td align=center>下一階級：<font color=\"FFDD99\">VIP ").append(nextLevel).append("</font></td></tr>");
			sb.append("<tr><td align=center>消耗紀念章：<font color=\"FF6666\">").append(VIP_UPGRADE_COSTS.get(nextLevel)).append(" 個</font></td></tr>");
			sb.append("</table><br>");
			sb.append("<button value=\"立刻晉升階級\" action=\"bypass _bbs_vip_upgrade\" width=130 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br><br>");
		}
		else
		{
			sb.append("<font color=\"FFFF00\">恭喜！你已達到最高頂峰 VIP 10 級！</font><br><br>");
		}
		
		sb.append("<button value=\"打開隨身稱號衣櫃\" action=\"bypass _bbs_open_wardrobe\" width=140 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
		sb.append("</center></body></html>");
		
		org.l2jmobius.gameserver.util.HtmlUtil.sendCBHtml(player, sb.toString());
	}

	public long getPremiumExpiration(String accountName)
	{
		for (org.l2jmobius.gameserver.entity.actor.Player player : org.l2jmobius.gameserver.entity.World.getPlayers())
		{
			if (player != null && accountName.equalsIgnoreCase(player.getAccountName()) && getVipLevel(player) > 0)
			{
				return System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
			}
		}
		return 0L;
	}

	public void addPremiumTime(String accountName, int timeValue, java.util.concurrent.TimeUnit timeUnit)
	{
		for (org.l2jmobius.gameserver.entity.actor.Player player : org.l2jmobius.gameserver.entity.World.getPlayers())
		{
			if (player != null && accountName.equalsIgnoreCase(player.getAccountName()))
			{
				final int cur = getVipLevel(player);
				if (cur < 10)
				{
					player.getVariables().set("Custom_Vip_Level", cur + 1);
					player.getVariables().storeMe();
					rewardVipSkills(player);
					player.sendMessage("【管理員發放】你的 VIP 等級已被提升至 VIP " + (cur + 1) + " ！");
					player.broadcastUserInfo();
				}
				break;
			}
		}
	}

	public void removePremiumStatus(String accountName, boolean checkOnline)
	{
		for (org.l2jmobius.gameserver.entity.actor.Player player : org.l2jmobius.gameserver.entity.World.getPlayers())
		{
			if (player != null && accountName.equalsIgnoreCase(player.getAccountName()))
			{
				player.getVariables().remove("Custom_Vip_Level");
				player.getVariables().storeMe();
				rewardVipSkills(player);
				player.sendMessage("【管理員變更】你的 VIP 尊榮修煉資格已被重置。");
				player.broadcastUserInfo();
				break;
			}
		}
	}

	public static PremiumManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PremiumManager INSTANCE = new PremiumManager();
	}
}
