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
package quests.Q20102_TowerOfInsolencePowerOfChaos;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestDialogType;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

import quests.Q20103_TowerOfInsolenceResearchingThePowerOfChaos.Q20103_TowerOfInsolenceResearchingThePowerOfChaos;

/**
 * @author CostyKiller, GM Fix
 */
public class Q20102_TowerOfInsolencePowerOfChaos extends Quest
{
	private static final int QUEST_ID = 20102;
	private static final int NEREA = 34779;
	
	public Q20102_TowerOfInsolencePowerOfChaos()
	{
		super(QUEST_ID);
		addFirstTalkId(NEREA);
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
				
				// 👑 GM 傳送補丁：無視壞掉的 XML 資料庫，直接硬寫死傲慢之塔一樓大廳奈利亞的實體座標！
				// 讓玩家點擊任務傳送時，百分之百直接飛到定點！
				Location insolenceTowerLocation = new Location(114752, 15936, -3500); // 這是傲慢之塔大廳基本座標
				
				if (questState == null)
				{
					teleportToQuestLocation(player, insolenceTowerLocation);
					sendAcceptDialog(player);
				}
				else
				{
					teleportToQuestLocation(player, insolenceTowerLocation);
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
					
					final QuestState nextQuestState = player.getQuestState(Q20103_TowerOfInsolenceResearchingThePowerOfChaos.class.getSimpleName());
					if (nextQuestState == null)
					{
						player.sendPacket(new ExQuestDialog(20103, QuestDialogType.ACCEPT));
					}
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
				if (npc.getId() == NEREA)
				{
					final NewQuest data = getQuestData();
					if (data.getGoal().getItemId() > 0)
					{
						final int itemCount = (int) getQuestItemsCount(player, data.getGoal().getItemId());
						if (itemCount < data.getGoal().getCount())
						{
							giveItems(player, data.getGoal().getItemId(), 1);
							final int newItemCount = (int) getQuestItemsCount(player, data.getGoal().getItemId());
							questState.setCount(newItemCount);
						}
					}
					else
					{
						final int currentCount = questState.getCount();
						if (currentCount != data.getGoal().getCount())
						{
							questState.setCount(currentCount + 1);
						}
					}
					
					if (questState.getCount() >= data.getGoal().getCount())
					{
						questState.setCond(QuestCondType.DONE);
						player.sendPacket(new ExQuestNotification(questState));
					}
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
}
