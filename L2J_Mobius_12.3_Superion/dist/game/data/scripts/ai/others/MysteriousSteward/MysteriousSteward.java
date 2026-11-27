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
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.others.MysteriousSteward;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Mysterious Steward AI (Ceremony of Chaos).
 * @author Notorion
 * @date 2026-11-21
 */
public class MysteriousSteward extends Script
{
	// NPC
	private static final int MYSTERIOUS_STEWARD = 33685;
	
	private MysteriousSteward()
	{
		addStartNpc(MYSTERIOUS_STEWARD);
		addTalkId(MYSTERIOUS_STEWARD);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "greatestWarriorReward":
			{
				final int trueHeroId = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.COC_TRUE_HERO, 0);
				if (player.isTrueHero() || (player.getObjectId() == trueHeroId))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
					html.setHtml("<html><body scroll=\"no\"><table border=0 cellpadding=0 cellspacing=0 width=292 height=358 background=\"L2UI_CH3.refinewnd_back_Pattern\"><tr><td valign=\"top\" align=\"center\"><table border=0 cellpadding=0 cellspacing=0><tr><td width=256 height=70></td></tr></table><table border=0 cellpadding=0 cellspacing=0 width=250><tr><td align=center>Congratulations, <font color=\"LEVEL\">Greatest Warrior</font>!<br>Your monthly rewards have been delivered to you upon entering the world.</td></tr><tr><td height=60></td></tr><tr><td align=center><button action=\"bypass npc_" + npc.getObjectId() + "_Chat 1\" value=\"Back\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_Back_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Back\"></td></tr></table></td></tr></table></body></html>");
					player.sendPacket(html);
				}
				else
				{
					sendDefaultHtml(player, npc, "data/html/default/33685-5.htm", null, null);
				}
				break;
			}
			case "bestClan":
			{
				final int heroId = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.COC_TRUE_HERO, 0);
				String message = "There is no Greatest Clan selected for this month yet.";
				if (heroId > 0)
				{
					final String heroName = CharInfoTable.getInstance().getNameById(heroId);
					final int clanId = CharInfoTable.getInstance().getClanIdById(heroId);
					final Clan clan = clanId > 0 ? ClanTable.getInstance().getClan(clanId) : null;
					if ((heroName != null) && (clan != null))
					{
						message = "Through the efforts of <font color=\"LEVEL\">" + heroName + "</font>, <font color=\"LEVEL\">" + clan.getName() + "</font> was selected as this month's Greatest Clan.";
					}
					else if (heroName != null)
					{
						message = "The Greatest Warrior of this month is <font color=\"LEVEL\">" + heroName + "</font>, who does not belong to a clan.";
					}
				}
				sendDefaultHtml(player, npc, "data/html/default/33685-6.htm", "%message%", message);
				break;
			}
			case "bestCandidate":
			{
				final int topMarks = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.COC_TOP_MARKS, 0);
				sendDefaultHtml(player, npc, "data/html/default/33685-7.htm", "%marks%", String.valueOf(topMarks));
				break;
			}
		}
		
		return null;
	}
	
	private void sendDefaultHtml(Player player, Npc npc, String path, String placeholder, String value)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, path);
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		if ((placeholder != null) && (value != null))
		{
			html.replace(placeholder, value);
		}
		player.sendPacket(html);
	}
	
	public static void main(String[] args)
	{
		new MysteriousSteward();
	}
}
