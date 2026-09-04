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
package quests.Q20227_SnowyVolcanoMemoryShards2;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestDialogType;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * @author CostyKiller
 */
public class Q20227_SnowyVolcanoMemoryShards2 extends Quest
{
	private static final int QUEST_ID = 20227;
	private static final int[] FROZEN_EXPLORERS =
	{
		32662,
		32663
	};
	
	// Item
	private final static int MEMORY_CRYSTAL = 83626;
	
	// NPC
	private static final int TORIAN = 32661;
	
	// Frozen Explorers Search locations
	private static final Location[] SEARCH_LOCATIONS =
	{
		new Location(155669, -153048, -3464),
		new Location(139232, -145145, -3160),
		new Location(148338, -157632, -3360),
		new Location(153935, -139187, -3480)
	};
	
	// Frozen Explorers locations
	private static final Location[] EXPLORER_LOCATIONS =
	{
		new Location(159353, -153576, -3408, 26056),
		new Location(138106, -147176, -2240, 16383),
		new Location(145414, -158378, -2816, 534),
		new Location(156049, -137025, -2240, 27461),
	};
	
	private static final NpcStringId[] EXPLORER_MSG =
	{
		NpcStringId.TO_FIND_THE_INVESTIGATOR_GO_EAST_TO_THE_RIVER_OF_LAVA,
		NpcStringId.TO_FIND_THE_INVESTIGATOR_GO_THROUGH_A_LONG_MYSTERIOUS_TUNNEL,
		NpcStringId.TO_FIND_THE_INVESTIGATOR_SURVEY_THE_SURROUNDINGS_FROM_THE_HIGH_GROUND,
		NpcStringId.TO_FIND_THE_INVESTIGATOR_CLIMB_TWO_STAIRWAYS
	};
	
	public Q20227_SnowyVolcanoMemoryShards2()
	{
		super(QUEST_ID);
		addFirstTalkId(FROZEN_EXPLORERS);
		addFirstTalkId(TORIAN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
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
					giveItems(player, MEMORY_CRYSTAL, 4);
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
			case "useMemoryCrystal":
			{
				final QuestState questState = getQuestState(player, false);
				final NewQuest data = getQuestData();
				final int currentCount = questState.getCount();
				
				player.getInventory().destroyItemByItemId(ItemProcessType.QUEST, MEMORY_CRYSTAL, 1, player, npc);
				questState.setCount(currentCount + 1);
				
				if (npc.getLocation().equals(EXPLORER_LOCATIONS[0]))
				{
					questState.set("AREA1_DONE", 1);
					htmltext = "explorer-01.html";
				}
				else if (npc.getLocation().equals(EXPLORER_LOCATIONS[1]))
				{
					questState.set("AREA2_DONE", 1);
					htmltext = "explorer-02.html";
				}
				else if (npc.getLocation().equals(EXPLORER_LOCATIONS[2]))
				{
					questState.set("AREA3_DONE", 1);
					htmltext = "explorer-03.html";
				}
				else if (npc.getLocation().equals(EXPLORER_LOCATIONS[3]))
				{
					questState.set("AREA4_DONE", 1);
					htmltext = "explorer-04.html";
				}
				if (questState.getCount() >= data.getGoal().getCount())
				{
					questState.setCond(QuestCondType.DONE);
					player.sendPacket(new ExQuestNotification(questState));
				}
				return htmltext;
			}
			case "teleportToNE":
			{
				teleportToQuestLocation(player, SEARCH_LOCATIONS[0]);
				ThreadPool.schedule(() ->
				{
					player.broadcastPacket(new ExShowScreenMessage(EXPLORER_MSG[0], 2, 5000, true));
				}, 2000);
				break;
			}
			case "teleportToW":
			{
				teleportToQuestLocation(player, SEARCH_LOCATIONS[1]);
				ThreadPool.schedule(() ->
				{
					player.broadcastPacket(new ExShowScreenMessage(EXPLORER_MSG[1], 2, 5000, true));
				}, 2000);
				break;
			}
			case "teleportToN":
			{
				teleportToQuestLocation(player, SEARCH_LOCATIONS[2]);
				ThreadPool.schedule(() ->
				{
					player.broadcastPacket(new ExShowScreenMessage(EXPLORER_MSG[2], 2, 5000, true));
				}, 2000);
				break;
			}
			case "teleportToSE":
			{
				teleportToQuestLocation(player, SEARCH_LOCATIONS[3]);
				ThreadPool.schedule(() ->
				{
					player.broadcastPacket(new ExShowScreenMessage(EXPLORER_MSG[3], 2, 5000, true));
				}, 2000);
				break;
			}
			case "32661.html":
			{
				showQuestHtml(player);
				break;
			}
			case "32661-01.html":
			case "32661-02.html":
			case "32661-03.html":
			case "32661-04.html":
			case "32661-done.html":
			{
				htmltext = event;
				return htmltext;
			}
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && !questState.isCompleted())
		{
			if (questState.isCond(QuestCondType.STARTED))
			{
				if (ArrayUtil.contains(FROZEN_EXPLORERS, npc.getId()))
				{
					if (npc.getLocation().equals(EXPLORER_LOCATIONS[0]))
					{
						if (questState.getInt("AREA1_DONE") == 1)
						{
							return htmltext = "explorer-done.html";
						}
					}
					else if (npc.getLocation().equals(EXPLORER_LOCATIONS[1]))
					{
						if (questState.getInt("AREA2_DONE") == 1)
						{
							return htmltext = "explorer-done.html";
						}
					}
					else if (npc.getLocation().equals(EXPLORER_LOCATIONS[2]))
					{
						if (questState.getInt("AREA3_DONE") == 1)
						{
							return htmltext = "explorer-done.html";
						}
					}
					else if (npc.getLocation().equals(EXPLORER_LOCATIONS[3]))
					{
						if (questState.getInt("AREA4_DONE") == 1)
						{
							return htmltext = "explorer-done.html";
						}
					}
					
					htmltext = "explorer.html";
					return htmltext;
				}
				else if (npc.getId() == TORIAN)
				{
					showQuestHtml(player);
				}
			}
			
			if (questState.isCond(QuestCondType.NONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.START));
			}
			else if (questState.isCond(QuestCondType.DONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.END));
			}
		}
		
		npc.showChatWindow(player);
		return null;
		
	}
	
	/**
	 * Helper method to build and send the updated quest HTML
	 * @param player the current character
	 */
	private void showQuestHtml(Player player)
	{
		final QuestState questState = getQuestState(player, false);
		// Build HTML with current values
		String content = HtmCache.getInstance().getHtm(player, "data/scripts/quests/Q20227_SnowyVolcanoMemoryShards2/32661.html");
		
		// Area 1
		content = content.replace("%area1_button_param%", questState.getInt("AREA1_DONE") == 1 ? "32661-done.html" : "32661-01.html");
		content = content.replace("%area1_button_text%", questState.getInt("AREA1_DONE") == 1 ? "<font color=\"808080\"> Explore the Snowy Volcano's north-eastern part (Completed)</font>" : "Explore the Snowy Volcano's north-eastern part");
		
		// Area 2
		content = content.replace("%area2_button_param%", questState.getInt("AREA2_DONE") == 1 ? "32661-done.html" : "32661-02.html");
		content = content.replace("%area2_button_text%", questState.getInt("AREA2_DONE") == 1 ? "<font color=\"808080\"> Explore the Snowy Volcano's western part (Completed)</font>" : "Explore the Snowy Volcano's western part");
		
		// Area 3
		content = content.replace("%area3_button_param%", questState.getInt("AREA3_DONE") == 1 ? "32661-done.html" : "32661-03.html");
		content = content.replace("%area3_button_text%", questState.getInt("AREA3_DONE") == 1 ? "<font color=\"808080\"> Explore the Snowy Volcano's northern part (Completed)</font>" : "Explore the Snowy Volcano's northern part");
		
		// Area 4
		content = content.replace("%area4_button_param%", questState.getInt("AREA4_DONE") == 1 ? "32661-done.html" : "32661-04.html");
		content = content.replace("%area4_button_text%", questState.getInt("AREA4_DONE") == 1 ? "<font color=\"808080\"> Explore the Snowy Volcano's south-eastern part (Completed)</font>" : "Explore the Snowy Volcano's south-eastern part");
		
		player.sendPacket(new NpcHtmlMessage(0, 0, content));
	}
}
