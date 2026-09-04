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
package instances.KashaOrcFortressCenter;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q10575_KashasAvatar.Q10575_KashasAvatar;

/**
 * @author CostyKiller
 */
public class KashaOrcFortressCenter extends InstanceScript
{
	// NPCs
	private static final int ATRUS = 32697;
	private static final int ATRUS_SPIRIT = 32698;
	
	// Atrus Boss
	private static final int ATRUS_BOSS = 27641;
	
	// Atrus Boss Messages
	private static final NpcStringId[] BOSS_MESSAGES =
	{
		NpcStringId.I_LL_TAKE_YOUR_SOUL_AS_A_PUNISHMENT_AND_YOUR_BODY_WILL_STAY_SLAYING_THE_HIGH_ORCS,
		NpcStringId.I_HAVE_A_LOT_OF_TROUBLE_BECAUSE_OF_YOU_I_WON_T_FORGET_YOUR_NAME
	};
	
	// Template
	private static final int TEMPLATE_ID = 350;
	
	public KashaOrcFortressCenter()
	{
		super(TEMPLATE_ID);
		addStartNpc(ATRUS);
		addFirstTalkId(ATRUS, ATRUS_SPIRIT);
		addKillId(ATRUS_BOSS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				startQuestTimer("START_MESSAGE", 2000, null, player);
				break;
			}
			case "reenterInstance":
			{
				final Instance activeInstance = getPlayerInstance(player);
				if (isInInstance(activeInstance))
				{
					enterInstance(player, npc, activeInstance.getTemplateId());
				}
				break;
			}
			case "START_MESSAGE":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.TO_COMPLETE_THE_MISSION_TALK_TO_ATRUS, ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "startMission":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					startQuestTimer("PRAY_FOR_ATRUS", 2000, null, player);
					npc.decayMe();
					break;
				}
			}
			case "PRAY_FOR_ATRUS":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.COME_ATRUS_AND_TELL_US_YOUR_STORY, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("SPAWN_ATRUS_SPIRIT", 7000, null, player);
					break;
				}
			}
			case "SPAWN_ATRUS_SPIRIT":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.WAR_SPIRIT_ATRUS_HAS_APPEARED_TALK_TO_HIM, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("ATRUS_SPIRIT");
					break;
				}
			}
			case "spawnAtrusBoss":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.KASHA_S_AVATAR_ATRUS_HAS_APPEARED_KILL_HIM, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("ATRUS_BOSS");
					startQuestTimer("ATRUS_BOSS_MESSAGE", 7000, null, player);
					startQuestTimer("ATRUS_RANDOM_MESSAGE", 30000, null, player, true);
					npc.decayMe();
					break;
				}
			}
			case "ATRUS_BOSS_MESSAGE":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.S1_IT_WAS_YOU_WHO_RUINED_MY_PLANS_IN_THE_KASHA_ORC_FORTRESS, ExShowScreenMessage.TOP_CENTER, 5000, true, player.getName());
					startQuestTimer("PLAYER_MESSAGE", 10000, null, player);
					break;
				}
			}
			case "ATRUS_RANDOM_MESSAGE":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, getRandomEntry(BOSS_MESSAGES), ExShowScreenMessage.TOP_CENTER, 5000, true, player.getName());
					break;
				}
			}
			case "PLAYER_MESSAGE":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.YOU_VE_LOST_ATRUS_HOW_DARE_YOU_SAY_MY_NAME_WITHOUT_PERMISSION, ExShowScreenMessage.TOP_CENTER, 5000, true);
					break;
				}
			}
			case "COMPLETE_TASK_MESSAGE":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.YOU_DO_NOT_HEAR_THE_UNKNOWN_VOICE_ANYMORE_TO_COMPLETE_THE_TASK_TALK_TO_TARKAI_ONCE_AGAIN, ExShowScreenMessage.TOP_CENTER, 5000, true);
					break;
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			showOnScreenMsg(world, NpcStringId.KASHA_S_AVATAR_ATRUS_IS_DEFEATED_TALK_TO_TARKAI_ZU_DUDA_MARA, ExShowScreenMessage.TOP_CENTER, 5000, true);
			startQuestTimer("COMPLETE_TASK_MESSAGE", 10000, null, killer);
			cancelQuestTimer("ATRUS_RANDOM_MESSAGE", null, killer);
			
			final QuestState questState = killer.getQuestState(Q10575_KashasAvatar.class.getSimpleName());
			questState.setCount(questState.getCount() + 1);
			questState.setCond(QuestCondType.DONE);
			
			world.finishInstance();
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final QuestState questState = player.getQuestState(Q10575_KashasAvatar.class.getSimpleName());
			if ((questState != null) && !questState.isCompleted() && questState.isCond(QuestCondType.STARTED))
			{
				switch (npc.getId())
				{
					case ATRUS:
					{
						htmltext = "32697.html";
						break;
					}
					case ATRUS_SPIRIT:
					{
						htmltext = "32698.html";
						break;
					}
				}
			}
		}
		
		npc.showChatWindow(player);
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new KashaOrcFortressCenter();
	}
}
