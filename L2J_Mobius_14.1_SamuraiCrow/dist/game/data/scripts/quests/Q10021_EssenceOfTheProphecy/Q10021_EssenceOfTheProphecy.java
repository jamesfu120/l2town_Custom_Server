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
package quests.Q10021_EssenceOfTheProphecy;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestDialogType;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

import quests.Q10022_HelpersIdentity.Q10022_HelpersIdentity;

/**
 * @author Stayway, Anti-Lockup Fix Version
 */
public class Q10021_EssenceOfTheProphecy extends Quest
{
	private static final int QUEST_ID = 10021;
	private static final int PROPHECY_FRAGMENT = 39537; // 預言裝置碎片 ID
	private static final int TELEPORT_GUIDE = 82955;    // 大神官傳送指南 ID
	private static final int RAYMOND = 30134;           // 大神官 雷門德 NPC ID
	private static final int TALANEY = 32139;           // 🌟 補上：塔勒妮 NPC ID（接任務的 NPC）
	
	public Q10021_EssenceOfTheProphecy()
	{
		super(QUEST_ID);
		// 註冊兩個 NPC 的對話監聽，不管點誰都能強制解鎖
		addStartNpc(TALANEY);
		addTalkId(TALANEY);
		addStartNpc(RAYMOND);
		addTalkId(RAYMOND);
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
					giveItems(player, TELEPORT_GUIDE, 1);
					giveItems(player, PROPHECY_FRAGMENT, 1);
					
					// 🌟 核心解鎖防卡死：接取任務時，立刻在後台強行把狀態進度推到最頂端！
					questState.setCond(QuestCondType.DONE);
					player.sendPacket(new ExQuestNotification(questState));
				}
				break;
			}
			case "TELEPORT":
			{
				QuestState questState = getQuestState(player, false);
				if (questState == null)
				{
					if (!canStartQuest(player))
					{
						break;
					}
					
					questState = getQuestState(player, true);
					
					final NewQuestLocation questLocation = getQuestData().getLocation();
					if (questLocation.getStartLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getStartLocationId()).getLocation();
						if (teleportToQuestLocation(player, location))
						{
							questState.setCond(QuestCondType.ACT);
							sendAcceptDialog(player);
						}
					}
					break;
				}
				
				final NewQuestLocation questLocation = getQuestData().getLocation();
				if (questState.isCond(QuestCondType.STARTED))
				{
					if (questLocation.getQuestLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getQuestLocationId()).getLocation();
						if (teleportToQuestLocation(player, location) && (questLocation.getQuestLocationId() == questLocation.getEndLocationId()))
						{
							questState.setCond(QuestCondType.DONE);
							sendEndDialog(player);
						}
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
					takeItems(player, PROPHECY_FRAGMENT, 1);
					
					final QuestState nextQuestState = player.getQuestState(Q10022_HelpersIdentity.class.getSimpleName());
					if (nextQuestState == null)
					{
						player.sendPacket(new ExQuestDialog(10022, QuestDialogType.ACCEPT));
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
			// 🌟 暴力解鎖關鍵：只要玩家此時點擊任何一個任務相關 NPC（塔勒妮或大神官）
			// 程式碼在對話瞬間會強行在後台將進度刷成 DONE 狀態，瞬間衝破客戶端按鈕鎖死的限制！
			questState.setCond(QuestCondType.DONE);
			player.sendPacket(new ExQuestNotification(questState));
			
			if (npc.getId() == RAYMOND)
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.END));
			}
			else
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.START));
			}
		}
		
		npc.showChatWindow(player);
		return null;
	}
}
