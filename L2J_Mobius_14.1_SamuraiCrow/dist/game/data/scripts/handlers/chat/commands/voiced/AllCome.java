package handlers.chat.commands.voiced;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 假人雙向組隊與一鍵召喚指令 (.fakelist / .allcome) - L2jMobius 14.1 完全體
 */
public class AllCome implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"allcome",
		"fakelist",
		"fakejoin"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		// ==========================================
		// 指令 1：.fakelist 查看在線假人（含職業名稱、過濾已入隊成員）
		// ==========================================
		if (command.equalsIgnoreCase("fakelist"))
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<html><title>在線假人組隊大廳</title><body>");
			sb.append("<center><font color=\"LEVEL\">在線假人組隊列表（含已組隊未滿員）</font><br1>");
			sb.append("<font color=\"B09878\">組好隊伍後，輸入 .allcome 即可召喚至身邊</font></center><br>");
			sb.append("<table width=295 border=0 bgcolor=\"000000\">");
			
			int count = 0;
			for (Player player : World.getInstance().getPlayers())
			{
				if (!player.isFakePlayer())
				{
					continue;
				}
				
				// 排除已經在自己隊伍裡的假人隊友
				if (activeChar.isInParty() && activeChar.getParty().getMembers().contains(player))
				{
					continue;
				}
				
				// 排除正在開店、或稱號為「隨機商店」的擺攤假人
				if (player.isInStoreMode() || ((player.getTitle() != null) && player.getTitle().contains("隨機商店")))
				{
					continue;
				}
				
				// 檢查隊伍狀態：若假人已有隊伍，但隊伍已滿（7人），則跳過不顯示
				final Party targetParty = player.getParty();
				if ((targetParty != null) && (targetParty.getMemberCount() >= 7))
				{
					continue;
				}
				
				// 取得假人職業名稱 (L2jMobius 14.1 改為 getPlayerClass())
                final String className = ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName();
				
				count++;
				if (count <= 30)
				{
					   sb.append("<tr>");
                       sb.append("<td width=50><font color=\"00FF00\">Lv.").append(player.getLevel()).append("</font></td>");
                       sb.append("<td width=130>").append(player.getName()).append("<br1><font color=\"A0A0A0\">[").append(className).append("]</font></td>");
					
					if (targetParty == null)
					{
						sb.append("<td width=45><font color=\"808080\">單身</font></td>");
						sb.append("<td width=70><button value=\"拉他入隊\" action=\"bypass -h voice .fakejoin ").append(player.getName()).append("\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					}
					else
					{
						sb.append("<td width=45><font color=\"00FFFF\">").append(targetParty.getMemberCount()).append("人團</font></td>");
						sb.append("<td width=70><button value=\"加入此團\" action=\"bypass -h voice .fakejoin ").append(player.getName()).append("\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					}
					sb.append("</tr>");
				}
			}
			
			sb.append("</table><br>");
			if (count == 0)
			{
				sb.append("<center><font color=\"FF0000\">目前沒有可加入的在線假人。</font></center>");
			}
			else
			{
				sb.append("<center>共找到 <font color=\"LEVEL\">").append(count).append("</font> 名可組隊假人（顯示前 30 名）</center>");
			}
			sb.append("</body></html>");
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			activeChar.sendPacket(html);
			return true;
		}
		
		// ==========================================
		// 指令 2：.fakejoin 處理「拉他入隊」與「加入此團（並取得隊長）」
		// ==========================================
		if (command.equalsIgnoreCase("fakejoin"))
		{
			if ((params == null) || params.trim().isEmpty())
			{
				return false;
			}
			
			final Player targetFake = World.getInstance().getPlayer(params.trim());
			if ((targetFake == null) || !targetFake.isFakePlayer())
			{
				activeChar.sendMessage("找不到該假人玩家。");
				return false;
			}
			
			if (!targetFake.isInParty())
			{
				if (!activeChar.isInParty())
				{
					final Party newParty = new Party(activeChar, PartyDistributionType.FINDERS_KEEPERS);
					activeChar.setParty(newParty);
					targetFake.joinParty(newParty);
				}
				else
				{
					if (!activeChar.getParty().isLeader(activeChar))
					{
						activeChar.sendMessage("你不是隊長，無法邀請新成員。");
						return false;
					}
					if (activeChar.getParty().getMemberCount() >= 7)
					{
						activeChar.sendMessage("你的隊伍人數已滿！");
						return false;
					}
					targetFake.joinParty(activeChar.getParty());
				}
				activeChar.sendMessage("已成功將假人「" + targetFake.getName() + "」拉入你的隊伍！");
			}
			else
			{
				if (activeChar.isInParty())
				{
					activeChar.sendMessage("你目前已經在隊伍中，請先退隊再加入其他假人團隊。");
					return false;
				}
				
				final Party fakeParty = targetFake.getParty();
				if (fakeParty.getMemberCount() >= 7)
				{
					activeChar.sendMessage("該假人隊伍人數已滿！");
					return false;
				}
				
				activeChar.joinParty(fakeParty);
				fakeParty.changePartyLeader(activeChar.getName());
				activeChar.sendMessage("已成功加入假人團隊，並取得隊長權限！現在可輸入 .allcome 召喚全隊。");
			}
			
			onCommand("fakelist", activeChar, "");
			return true;
		}
		
		// ==========================================
		// 指令 3：.allcome 隊長一鍵召喚假人隊友至身邊
		// ==========================================
		if (command.equalsIgnoreCase("allcome"))
		{
			final Party party = activeChar.getParty();
			if ((party == null) || !party.isLeader(activeChar))
			{
				activeChar.sendMessage("你必須是隊長才能使用此指令。");
				return false;
			}
			
			if (activeChar.isInCombat() || activeChar.isDead() || activeChar.isInOlympiadMode() || activeChar.isInsideZone(ZoneId.SIEGE) || (activeChar.getInstanceId() > 0))
			{
				activeChar.sendMessage("目前狀態或所在區域無法召喚隊友。");
				return false;
			}
			
			int summonedCount = 0;
			for (Player member : party.getMembers())
			{
				if ((member == activeChar) || !member.isFakePlayer())
				{
					continue;
				}
				
				if (member.isInStoreMode() || ((member.getTitle() != null) && member.getTitle().contains("隨機商店")))
				{
					continue;
				}
				
				if (member.isDead())
				{
					member.doRevive();
				}
				
				member.abortAttack();
				member.abortCast();
				member.stopMove(null);
				
				final int offsetX = Rnd.get(-80, 80);
				final int offsetY = Rnd.get(-80, 80);
				
				member.setTeleporting(false);
				member.teleToLocation(activeChar.getX() + offsetX, activeChar.getY() + offsetY, activeChar.getZ(), activeChar.getHeading(), activeChar.getInstanceId());
				member.onTeleported();
				member.broadcastUserInfo();
				summonedCount++;
			}
			
			if (summonedCount > 0)
			{
				activeChar.sendMessage("已成功將 " + summonedCount + " 名假人隊友召喚至身邊！");
			}
			else
			{
				activeChar.sendMessage("隊伍中沒有可召喚的假人隊友。");
			}
			
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
