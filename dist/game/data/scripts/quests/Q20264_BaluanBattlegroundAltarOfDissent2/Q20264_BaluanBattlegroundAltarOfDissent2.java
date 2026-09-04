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
package quests.Q20264_BaluanBattlegroundAltarOfDissent2;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

/**
 * @author CostyKiller
 */
public class Q20264_BaluanBattlegroundAltarOfDissent2 extends Quest
{
	private static final int QUEST_ID = 20264;
	
	// Npcs
	private static final int ALTAR_OF_DISSENT = 32669;
	
	// Items
	private static final int FIRE_ENERGY = 83627;
	private static final int COLD_ENERGY = 83628;
	
	// Altar of Dissent States
	private static final int FIRE_STATE = 2;
	private static final int COLD_STATE = 3;
	private static final int[] ALTAR_STATES =
	{
		FIRE_STATE,
		COLD_STATE,
	};
	
	private static final int[] MONSTERS =
	{
		// Monsters on Baluan Battleground
		23920, // Calderis' Guardian
		23921, // Calderis' Warlock
		23922, // Calderis' Berserker
		23923, // Calderis' Golem
		23924, // Calderis' Large Golem
	};
	
	private static final int[] DROPS =
	{
		FIRE_ENERGY, // Chaotic Fire Energy
		COLD_ENERGY, // Chaotic Cold Energy
	};
	
	private static final int DROP_CHANCE = 30;
	
	public Q20264_BaluanBattlegroundAltarOfDissent2()
	{
		super(QUEST_ID);
		addFirstTalkId(ALTAR_OF_DISSENT);
		addSpawnId(ALTAR_OF_DISSENT);
		addKillId(MONSTERS);
		registerQuestItems(FIRE_ENERGY, COLD_ENERGY);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ACCEPT":
			{
				if (!canStartQuest(player))
				{
					break;
				}
				
				final QuestState questState = getQuestState(player, true);
				if (!questState.isStarted() && !questState.isCompleted())
				{
					questState.startQuest();
				}
				break;
			}
			case "TELEPORT":
			{
				final QuestState questState = getQuestState(player, false);
				final NewQuestLocation questLocation = getQuestData().getLocation();
				if (questState == null)
				{
					final Location location = TeleportListData.getInstance().getTeleport(questLocation.getStartLocationId()).getLocation();
					teleportToQuestLocation(player, location);
					sendAcceptDialog(player);
				}
				else if (questState.isCond(QuestCondType.STARTED))
				{
					if (questLocation.getQuestLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getQuestLocationId()).getLocation();
						teleportToQuestLocation(player, location);
					}
				}
				else if (questState.isCond(QuestCondType.DONE) && !questState.isCompleted())
				{
					if (questLocation.getEndLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getEndLocationId()).getLocation();
						if (teleportToQuestLocation(player, location))
						{
							sendEndDialog(player);
						}
					}
				}
				break;
			}
			case "COMPLETE":
			{
				final QuestState questState = getQuestState(player, false);
				if (questState == null)
				{
					break;
				}
				
				if (questState.isCond(QuestCondType.DONE) && !questState.isCompleted())
				{
					questState.exitQuest(false, true);
					rewardPlayer(player);
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && !questState.isCompleted())
		{
			if (questState.isCond(QuestCondType.STARTED))
			{
				if (npc.getId() == ALTAR_OF_DISSENT)
				{
					final String var = questState.get("ALTAR_OBJECT_IDS");
					final int objId = npc.getObjectId();
					if ((var != null) && var.contains(String.valueOf(objId)))
					{
						return "32669-notAllowed.html";
					}
					
					final NewQuest data = getQuestData();
					final int currentCount = questState.getCount();
					final int goalCount = data.getGoal().getCount();
					if (npc.getDisplayEffect() == FIRE_STATE)
					{
						if (!player.destroyItemByItemId(ItemProcessType.QUEST, COLD_ENERGY, 1, player, true))
						{
							return "32669-noItem.html";
						}
						
						npc.setDisplayEffect(COLD_STATE);
						
						questState.set("ALTAR_OBJECT_IDS", ((var == null) || var.isEmpty()) ? String.valueOf(objId) : var + "," + objId);
						
						if (currentCount < goalCount)
						{
							questState.setCount(currentCount + 1);
						}
						
						if (questState.getCount() == goalCount)
						{
							questState.setCond(QuestCondType.DONE);
							player.sendPacket(new ExQuestNotification(questState));
						}
						
						return "32669-cold.html";
					}
					else if (npc.getDisplayEffect() == COLD_STATE)
					{
						if (!player.destroyItemByItemId(ItemProcessType.QUEST, FIRE_ENERGY, 1, player, true))
						{
							return "32669-noItem.html";
						}
						
						npc.setDisplayEffect(FIRE_STATE);
						
						questState.set("ALTAR_OBJECT_IDS", ((var == null) || var.isEmpty()) ? String.valueOf(objId) : var + "," + objId);
						
						if (currentCount < goalCount)
						{
							questState.setCount(currentCount + 1);
						}
						
						if (questState.getCount() == goalCount)
						{
							questState.setCond(QuestCondType.DONE);
							player.sendPacket(new ExQuestNotification(questState));
						}
						
						return "32669-fire.html";
					}
				}
			}
		}
		
		npc.showChatWindow(player);
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState questState = getQuestState(killer, false);
		if ((questState != null) && questState.isCond(QuestCondType.STARTED))
		{
			final NewQuest data = getQuestData();
			if (data.getGoal().getItemId() > 0)
			{
				final int itemCount = (int) getQuestItemsCount(killer, data.getGoal().getItemId());
				if ((itemCount < data.getGoal().getCount()) && (getRandom(100) < DROP_CHANCE))
				{
					giveItems(killer, getRandomEntry(DROPS), 1);
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setDisplayEffect(getRandomEntry(ALTAR_STATES));
	}
}