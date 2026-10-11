/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://gnu.org>.
 */
package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.HtmlUtil;

public class CommunityBoardHandler implements IHandler<IParseBoardHandler, String>
{
	private static final Logger LOG = Logger.getLogger(CommunityBoardHandler.class.getName());
	private final Map<String, IParseBoardHandler> _datatable = new HashMap<>();
	private final Map<Integer, String> _bypasses = new ConcurrentHashMap<>();
	
	protected CommunityBoardHandler()
	{
	}
	
	@Override
	public void registerHandler(IParseBoardHandler handler)
	{
		for (String cmd : handler.getCommandList()) { _datatable.put(cmd.toLowerCase(), handler); }
	}
	
	@Override
	public synchronized void removeHandler(IParseBoardHandler handler)
	{
		for (String cmd : handler.getCommandList()) { _datatable.remove(cmd.toLowerCase()); }
	}
	
	@Override
	public IParseBoardHandler getHandler(String cmd)
	{
		for (IParseBoardHandler cb : _datatable.values())
		{
			for (String command : cb.getCommandList()) { if (cmd.toLowerCase().startsWith(command.toLowerCase())) return cb; }
		}
		return null;
	}
	
	@Override
	public int size() { return _datatable.size(); }
	public boolean isCommunityBoardCommand(String cmd) { return getHandler(cmd) != null; }

	public void handleParseCommand(String command, Player player)
	{
		if (player == null) return;
		if (!GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return;
		}
		
		// 🛠️ 優先權攔截：如果玩家點擊的是自訂的 VIP 與稱號衣櫃按鈕，直接交給我們寫好的方法處理
		if (command.startsWith("_bbs_vip_") || command.startsWith("_bbs_open_") || command.startsWith("_bbs_select_"))
		{
			if (handleCustomVipBypass(player, command)) return;
		}
		
		final IParseBoardHandler cb = getHandler(command);
		if (cb == null)
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find parse handler for command " + command + "!");
			return;
		}
		cb.onCommand(command, player);
	}
	// 🛠️ 核心大功能：在 BBS 內動態渲染生成「修仙轉生稱號」與「VIP 彩色稱號」二合一隨身稱號衣櫃
	public void showTitleWardrobe(Player player)
	{
		if (player == null) return;
		final int rebirthCount = org.l2jmobius.gameserver.managers.RebirthManager.getInstance().getRebirthLevel(player);
		final int vipLevel = org.l2jmobius.gameserver.managers.PremiumManager.getInstance().getVipLevel(player);
		final org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage html = new org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage(0);
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body><center><br><font color=\"LEVEL\">【 BBS 隨身稱號衣櫃 - 每日自由切換 】</font><br><br>");
		sb.append("<table width=420 border=0><tr>");
		
		// === 左側：修仙轉生稱號衣櫃 ===
		sb.append("<td width=210 valign=top align=center><font color=\"66CC66\">【 修仙境界封號 】</font><br><br>");
		if (rebirthCount <= 0)
		{
			sb.append("<font color=AAAAAA>尚未完成轉生渡劫</font>");
		}
		else
		{
			sb.append("<table width=200>");
			final int maxConfigStages = org.l2jmobius.gameserver.config.custom.RebirthConfig.REBIRTH_TITLE_STAGES.size();
			for (int i = 1; i <= rebirthCount; i++)
			{
				String titleName = (i <= maxConfigStages) ? org.l2jmobius.gameserver.config.custom.RebirthConfig.REBIRTH_TITLE_STAGES.get(i - 1) : "轉生強者 Lv." + i;
				sb.append("<tr><td width=120><font color=\"FFFF00\">").append(titleName).append("</font></td>");
				sb.append("<td width=80><button value=\"配戴\" action=\"bypass _bbs_select_rebirth_title ").append(i).append("\" width=45 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
			}
			sb.append("</table>");
		}
		sb.append("</td>");
		
		// === 右側：VIP 尊榮彩色稱號衣櫃 ===
		sb.append("<td width=210 valign=top align=center><font color=\"FFDD99\">【 VIP 尊榮稱號 】</font><br><br>");
		sb.append("<table width=200>");
		for (java.util.Map.Entry<Integer, String> entry : org.l2jmobius.gameserver.managers.PremiumManager.VIP_TITLES.entrySet())
		{
			final int reqLevel = entry.getKey().intValue();
			final String titleName = entry.getValue();
			sb.append("<tr><td width=120>");
			if (vipLevel >= reqLevel)
			{
				sb.append("<font color=\"00FFFF\">").append(titleName).append("</font>");
				sb.append("</td><td width=80><button value=\"配戴\" action=\"bypass _bbs_select_vip_title ").append(reqLevel).append("\" width=45 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
			}
			else
			{
				sb.append("<font color=\"555555\">").append(titleName).append(" (V").append(reqLevel).append("解鎖)</font></td><td width=80><font color=777777>未解鎖</font></td></tr>");
			}
		}
		sb.append("</table></td></tr></table>");
		sb.append("<br><img src=\"L2UI.SquareBlank\" width=400 height=1><br>");
		sb.append("<button value=\"返回 VIP 中心\" action=\"bypass _bbs_vip_open\" width=130 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</center></body></html>");
		
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	public boolean handleCustomVipBypass(Player player, String command)
	{
		if (command == null || player == null) return false;
		if (command.equals("_bbs_vip_open"))
		{
			org.l2jmobius.gameserver.managers.PremiumManager.getInstance().displayVipWindow(player, 0);
			return true;
		}
		else if (command.equals("_bbs_vip_upgrade"))
		{
			org.l2jmobius.gameserver.managers.PremiumManager.getInstance().requestVipUpgrade(player);
			return true;
		}
		else if (command.equals("_bbs_vip_daily"))
		{
			org.l2jmobius.gameserver.managers.PremiumManager.getInstance().claimDailyReward(player);
			return true;
		}
		else if (command.equals("_bbs_open_wardrobe"))
		{
			showTitleWardrobe(player);
			return true;
		}
		else if (command.startsWith("_bbs_select_vip_title"))
		{
			final int titleIndex = Integer.parseInt(command.substring(22).trim());
			org.l2jmobius.gameserver.managers.PremiumManager.getInstance().selectVipTitle(player, titleIndex);
			return true;
		}
		else if (command.startsWith("_bbs_select_rebirth_title"))
		{
			final int titleIndex = Integer.parseInt(command.substring(26).trim());
			org.l2jmobius.gameserver.managers.RebirthManager.getInstance().selectRebirthTitle(player, 0, titleIndex);
			showTitleWardrobe(player);
			return true;
		}
		return false;
	}
	public void handleWriteCommand(Player player, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		if (player == null) return;
		if (!GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return;
		}
		String cmd = "";
		switch (url)
		{
			case "Topic": cmd = "_bbstop"; break;
			case "Post": cmd = "_bbspos"; break;
			case "Region": cmd = "_bbsloc"; break;
			case "Notice": cmd = "_bbsclan"; break;
			default:
				separateAndSend("<html><body><br><br><center>The command: " + url + " is not implemented yet.</center><br><br></body></html>", player);
				return;
		}
		final IParseBoardHandler cb = getHandler(cmd);
		if (cb == null) return;
		if (!(cb instanceof IWriteBoardHandler)) return;
		((IWriteBoardHandler) cb).writeCommunityBoardCommand(player, arg1, arg2, arg3, arg4, arg5);
	}
	
	public void addBypass(Player player, String title, String bypass) { _bypasses.put(player.getObjectId(), title + "&" + bypass); }
	public String removeBypass(Player player) { return _bypasses.remove(player.getObjectId()); }	

	public static void separateAndSend(String html, Player player)
	{
		String processedHtml = html;
		if (processedHtml != null)
		{
			final StringBuilder sb = new StringBuilder();
			int count = 0;
			final Object[] objects = org.l2jmobius.gameserver.entity.World.getPlayers().toArray();
			for (Object obj : objects)
			{
				if (obj instanceof Player)
				{
					final Player onlinePlayer = (Player) obj;
					if (!onlinePlayer.isInvisible()) { sb.append(onlinePlayer.getName()).append(", "); count++; }
				}
			}
			final String playerListStr = sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "目前無玩家在線";
			processedHtml = processedHtml.replace("%online_players%", playerListStr);
			processedHtml = processedHtml.replace("%online_count%", String.valueOf(count));
		}
		org.l2jmobius.gameserver.util.HtmlUtil.sendCBHtml(player, processedHtml);
	}
	
	public static CommunityBoardHandler getInstance() { return SingletonHolder.INSTANCE; }
	private static class SingletonHolder { protected static final CommunityBoardHandler INSTANCE = new CommunityBoardHandler(); }
}
