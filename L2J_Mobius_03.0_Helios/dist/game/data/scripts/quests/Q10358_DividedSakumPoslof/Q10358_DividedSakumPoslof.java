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
package quests.Q10358_DividedSakumPoslof;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.NpcLogListHolder;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.State;

import quests.Q10337_SakumsImpact.Q10337_SakumsImpact;

/**
 * Divided Sakum, Poslof (10358)
 * @author St3eT, Trevor The Third
 */
public class Q10358_DividedSakumPoslof extends Quest
{
	// NPCs
	private static final int LEF = 33510;
	private static final int ADVENTURER_GUIDE = 31795;
	private static final int ZOMBIE_WARRIOR = 20458;
	private static final int VEELAN_BUGBEAR_WARRIOR = 20402;
	private static final int POSLOF = 27452;
	
	// Items
	private static final int SAKUM_SKETCH = 17585;
	
	// Misc
	private static final int MIN_LEVEL = 33;
	private static final int MAX_LEVEL = 40;
	
	// Rewards
	private static final int EXP_REWARD = 750000;
	private static final int SP_REWARD = 180;
	private static final int ADENA_REWARD = 1050;
	
	public Q10358_DividedSakumPoslof()
	{
		super(10358);
		addStartNpc(LEF);
		addTalkId(LEF, ADVENTURER_GUIDE);
		addKillId(ZOMBIE_WARRIOR, VEELAN_BUGBEAR_WARRIOR, POSLOF);
		registerQuestItems(SAKUM_SKETCH);
		addCondCompletedQuest(Q10337_SakumsImpact.class.getSimpleName(), "33510-09.html");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33510-09.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "33510-02.htm":
			case "31795-04.html":
			{
				htmltext = event;
				break;
			}
			case "33510-03.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "31795-05.html":
			{
				if (qs.isCond(4))
				{
					giveAdena(player, ADENA_REWARD, true);
					addExpAndSp(player, EXP_REWARD, SP_REWARD);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = npc.getId() == LEF ? "33510-01.htm" : "31795-02.html";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = npc.getId() == LEF ? "33510-04.html" : "31795-01.html";
						break;
					}
					case 2:
					{
						if (npc.getId() == LEF)
						{
							qs.setCond(3);
							giveItems(player, SAKUM_SKETCH, 1);
							htmltext = "33510-05.html";
						}
						else if (npc.getId() == ADVENTURER_GUIDE)
						{
							htmltext = "31795-01.html";
						}
						break;
					}
					case 3:
					{
						htmltext = npc.getId() == LEF ? "33510-06.html" : "31795-01.html";
						break;
					}
					case 4:
					{
						htmltext = npc.getId() == LEF ? "33510-07.html" : "31795-03.html";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = npc.getId() == LEF ? "33510-08.html" : "31795-06.html";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isStarted())
		{
			if (qs.isCond(1))
			{
				int killedZombies = qs.getInt("killed_" + ZOMBIE_WARRIOR);
				int killedVeelans = qs.getInt("killed_" + VEELAN_BUGBEAR_WARRIOR);
				if (npc.getId() == ZOMBIE_WARRIOR)
				{
					if (killedZombies < 20)
					{
						killedZombies++;
						qs.set("killed_" + ZOMBIE_WARRIOR, killedZombies);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				else if (killedVeelans < 23)
				{
					killedVeelans++;
					qs.set("killed_" + VEELAN_BUGBEAR_WARRIOR, killedVeelans);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				if ((killedZombies == 20) && (killedVeelans == 23))
				{
					qs.setCond(2, true);
				}
			}
			else if (qs.isCond(3) && (npc.getId() == POSLOF))
			{
				qs.set("killed_" + POSLOF, 1);
				qs.setCond(4);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted())
		{
			if (qs.isCond(1))
			{
				final Set<NpcLogListHolder> npcLogList = new HashSet<>(2);
				npcLogList.add(new NpcLogListHolder(ZOMBIE_WARRIOR, false, qs.getInt("killed_" + ZOMBIE_WARRIOR)));
				npcLogList.add(new NpcLogListHolder(VEELAN_BUGBEAR_WARRIOR, false, qs.getInt("killed_" + VEELAN_BUGBEAR_WARRIOR)));
				return npcLogList;
			}
			else if (qs.isCond(3))
			{
				final Set<NpcLogListHolder> npcLogList = new HashSet<>(1);
				npcLogList.add(new NpcLogListHolder(POSLOF, false, qs.getInt("killed_" + POSLOF)));
				return npcLogList;
			}
		}
		
		return super.getNpcLogList(player);
	}
}
