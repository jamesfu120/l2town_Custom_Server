package handlers.chat.commands.voiced;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.FakePlayerPopulationManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 假人雙向組隊與一鍵召喚指令 (.fakelist / .allcome) - L2jMobius 14.1 完全體（支援整隊合併）
 */
public class AllCome implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"allcome",
		"fakelist",
		"fakejoin"
	};
	
	// 嚴格限制每頁最多 22 筆，確保 HTML 封包低於 5000 bytes，徹底防止客戶端 NCHtmlTable 閃退
	private static final int PAGE_SIZE = 22;
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		// ==========================================
		// 指令 1：.fakelist 查看在線假人（安全分頁：最多 15 隊伍 + 90 名單身假人）
		// ==========================================
		if (command.equalsIgnoreCase("fakelist"))
		{
			int page = 1;
			if ((params != null) && !params.trim().isEmpty())
			{
				try
				{
					page = Math.max(1, Integer.parseInt(params.trim()));
				}
				catch (NumberFormatException ignored)
				{
					page = 1;
				}
			}
			
			final List<Player> partyLeaders = new ArrayList<>();
			final List<Player> soloPlayers = new ArrayList<>();
			final Set<Party> seenParties = new HashSet<>();
			
			for (Player player : FakePlayerPopulationManager.getPlayers())
			{
				if ((player == null) || !player.isSpawned() || !player.isOnline() || player.isDead())
				{
					continue;
				}
				
				// 排除已經在自己隊伍裡的假人隊友
				if (activeChar.isInParty() && activeChar.getParty().getMembers().contains(player))
				{
					continue;
				}
				
				// 排除正在開店、開工房或稱號為「隨機商店」的擺攤假人
				if (player.isInStoreMode() || ((player.getTitle() != null) && (player.getTitle().contains("隨機商店") || player.getTitle().contains("百貨") || player.getTitle().contains("工房"))))
				{
					continue;
				}
				
				final Party targetParty = player.getParty();
				if (targetParty == null)
				{
					// 未組隊的單身戰鬥假人（最多收錄 90 名）
					if (soloPlayers.size() < 90)
					{
						soloPlayers.add(player);
					}
				}
				else
				{
					// 已組隊且未滿員（< 9 人）的隊伍：每支隊伍只取一位代表，最多列出 15 隊
					if ((targetParty.getMemberCount() < 9) && !seenParties.contains(targetParty) && (partyLeaders.size() < 15))
					{
						seenParties.add(targetParty);
						final Player leader = targetParty.getLeader() != null ? targetParty.getLeader() : player;
						partyLeaders.add(leader);
					}
				}
			}
			
			// 合併總清單：隊伍排在最前面，單身假人排在後面
			final List<Player> allEntries = new ArrayList<>(partyLeaders.size() + soloPlayers.size());
			allEntries.addAll(partyLeaders);
			allEntries.addAll(soloPlayers);
			
			final int totalEntries = allEntries.size();
			final int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
			if (page > totalPages)
			{
				page = totalPages;
			}
			
			final int startIndex = (page - 1) * PAGE_SIZE;
			final int endIndex = Math.min(startIndex + PAGE_SIZE, totalEntries);
			
			final StringBuilder sb = new StringBuilder();
			sb.append("<html><title>在線假人組隊大廳</title><body>");
			sb.append("<center><font color=\"LEVEL\">假人組隊大廳（").append(partyLeaders.size()).append(" 隊伍 / ").append(soloPlayers.size()).append(" 單身）</font><br1>");
			sb.append("<font color=\"B09878\">組好隊伍後，輸入 .allcome 即可召喚至身邊</font></center><br>");
			sb.append("<table width=280 border=0 bgcolor=\"000000\">");
			
			for (int i = startIndex; i < endIndex; i++)
			{
				final Player p = allEntries.get(i);
				final String className = ClassListData.getInstance().getClass(p.getPlayerClass()).getClassName();
				final Party pParty = p.getParty();
				
				sb.append("<tr>");
				sb.append("<td width=46><font color=\"00FF00\">Lv.").append(p.getLevel()).append("</font></td>");
				sb.append("<td width=124>").append(p.getName()).append("<br1><font color=\"A0A0A0\">[").append(className).append("]</font></td>");
				
				if (pParty == null)
				{
					sb.append("<td width=42><font color=\"808080\">單身</font></td>");
					sb.append("<td width=68><button value=\"拉他入隊\" action=\"bypass -h voice .fakejoin ").append(p.getName()).append("\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					sb.append("<td width=42><font color=\"00FFFF\">").append(pParty.getMemberCount()).append("人團</font></td>");
					sb.append("<td width=68><button value=\"加入此團\" action=\"bypass -h voice .fakejoin ").append(p.getName()).append("\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				sb.append("</tr>");
			}
			
			sb.append("</table><br>");
			
			if (totalEntries == 0)
			{
				sb.append("<center><font color=\"FF0000\">目前沒有可加入的在線假人。</font></center>");
			}
			else
			{
				// 分頁導覽按鈕列
				sb.append("<center><table width=270 border=0><tr>");
				if (page > 1)
				{
					sb.append("<td width=80 align=center><button value=\"上一頁\" action=\"bypass -h voice .fakelist ").append(page - 1).append("\" width=70 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					sb.append("<td width=80 align=center><font color=\"666666\">[第一頁]</font></td>");
				}
				
				sb.append("<td width=110 align=center><font color=\"LEVEL\">第 ").append(page).append(" / ").append(totalPages).append(" 頁</font></td>");
				
				if (page < totalPages)
				{
					sb.append("<td width=80 align=center><button value=\"下一頁\" action=\"bypass -h voice .fakelist ").append(page + 1).append("\" width=70 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					sb.append("<td width=80 align=center><font color=\"666666\">[最末頁]</font></td>");
				}
				sb.append("</tr></table></center>");
			}
			
			sb.append("</body></html>");
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			activeChar.sendPacket(html);
			return true;
		}
		
		// ==========================================
		// 指令 2：.fakejoin 處理「拉他入隊」與「加入此團（支援整隊合併）」
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
					if (activeChar.getParty().getMemberCount() >= 9)
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
				final Party fakeParty = targetFake.getParty();
				
				// 情況 A：你目前還沒組隊 -> 直接加入該假人團隊並接任隊長
				if (!activeChar.isInParty())
				{
					if (fakeParty.getMemberCount() >= 9)
					{
						activeChar.sendMessage("該假人隊伍人數已滿！");
						return false;
					}
					activeChar.joinParty(fakeParty);
					fakeParty.changePartyLeader(activeChar.getName());
					activeChar.sendMessage("已成功加入假人團隊，並取得隊長權限！現在可輸入 .allcome 召喚全隊。");
				}
				// 情況 B：你已經有隊伍 -> 只要兩隊相加 <= 9 人，直接將該假人團整隊合併進來！
				else
				{
					final Party myParty = activeChar.getParty();
					if (!myParty.isLeader(activeChar))
					{
						activeChar.sendMessage("你不是目前隊伍的隊長，無法合併其他假人團隊。");
						return false;
					}
					
					final List<Player> fakeMembers = new ArrayList<>(fakeParty.getMembers());
					if ((myParty.getMemberCount() + fakeMembers.size()) > 9)
					{
						activeChar.sendMessage("隊伍人數超過上限（9人），無法合併此假人團隊！（目前 " + myParty.getMemberCount() + " 人 + 對方 " + fakeMembers.size() + " 人）");
						return false;
					}
					
					int mergedCount = 0;
					for (Player fMember : fakeMembers)
					{
						if ((fMember != null) && fMember.isFakePlayer())
						{
							fMember.leaveParty();
							fMember.joinParty(myParty);
							mergedCount++;
						}
					}
					activeChar.sendMessage("已成功將該假人團隊（共 " + mergedCount + " 名成員）合併至你的隊伍！現在可輸入 .allcome 召喚全隊。");
				}
			}
			
			onCommand("fakelist", activeChar, "1");
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
				
				member.setTeleporting(false, false);
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
