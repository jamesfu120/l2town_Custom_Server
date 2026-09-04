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
package quests.Q10452_IsItEdible;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.State;

/**
 * Is it Edible? (10452)
 * @URL https://l2wiki.com/Is_it_Edible%3F
 * @author Gigi, Trevor The Third
 */
public class Q10452_IsItEdible extends Quest
{
	// NPCs
	private static final int SALLY = 32743;
	private static final int FANTASY_MUSHROM = 18864;
	private static final int STICKY_MUSHROMS = 18865;
	private static final int VITALIITY_PLANT = 18868;
	
	// Items
	private static final int FANTASY_MUSHROMS_SPORE = 36688;
	private static final int STICKY_MUSHROMS_SPORE = 36689;
	private static final int VITALIITY_LEAF_POUCH = 36690;
	
	// Misc
	private static final int MIN_LEVEL = 81;
	
	// Rewards
	private static final int EXP_REWARD = 14120400;
	private static final int SP_REWARD = 3388;
	private static final int ADENA_REWARD = 299940;
	
	public Q10452_IsItEdible()
	{
		super(10452);
		addStartNpc(SALLY);
		addTalkId(SALLY);
		addKillId(FANTASY_MUSHROM, STICKY_MUSHROMS, VITALIITY_PLANT);
		addCondMinLevel(MIN_LEVEL, "32743-08.htm");
		registerQuestItems(FANTASY_MUSHROMS_SPORE, STICKY_MUSHROMS_SPORE, VITALIITY_LEAF_POUCH);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "32743-02.htm":
			case "32743-07.html":
			case "32743-09.html":
			case "32743-10.html":
			case "32743-11.html":
			{
				htmltext = event;
				break;
			}
			case "32743-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "32743-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "32743-04.html";
						break;
					}
					case 2:
					{
						giveAdena(player, ADENA_REWARD, true);
						addExpAndSp(player, EXP_REWARD, SP_REWARD);
						qs.exitQuest(false, true);
						htmltext = "32743-05.html";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "32743-06.html";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (qs.isCond(1)))
		{
			switch (npc.getId())
			{
				case FANTASY_MUSHROM:
				{
					if (!hasQuestItems(killer, FANTASY_MUSHROMS_SPORE))
					{
						giveItems(killer, FANTASY_MUSHROMS_SPORE, 1);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						break;
					}
				}
				case STICKY_MUSHROMS:
				{
					if (!hasQuestItems(killer, STICKY_MUSHROMS_SPORE))
					{
						giveItems(killer, STICKY_MUSHROMS_SPORE, 1);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						break;
					}
				}
				case VITALIITY_PLANT:
				{
					if (!hasQuestItems(killer, VITALIITY_LEAF_POUCH))
					{
						giveItems(killer, VITALIITY_LEAF_POUCH, 1);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						break;
					}
				}
			}
			
			if ((getQuestItemsCount(killer, FANTASY_MUSHROMS_SPORE) >= 1) && (getQuestItemsCount(killer, STICKY_MUSHROMS_SPORE) >= 1) && (getQuestItemsCount(killer, VITALIITY_LEAF_POUCH) >= 1))
			{
				qs.setCond(2, true);
			}
		}
	}
}
