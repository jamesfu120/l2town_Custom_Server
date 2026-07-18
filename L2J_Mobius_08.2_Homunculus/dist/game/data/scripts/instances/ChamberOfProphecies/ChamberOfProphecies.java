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
package instances.ChamberOfProphecies;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCastingType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExShowUsm;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import quests.Q11027_PathOfDestinyOvercome.Q11027_PathOfDestinyOvercome;

/**
 * This class handles the logic for the {@code ChamberOfProphecies} instance.<br>
 * It manages the scripts and events required for players to complete this specific dungeon.
 * @author Gigi, Mobius, Shifu
 */
public class ChamberOfProphecies extends InstanceScript
{
	private static final Logger LOGGER = Logger.getLogger(ChamberOfProphecies.class.getName());
	
	private enum RoomState
	{
		ROOM1_COMBAT(0),
		ROOM2_SPAWNED(1),
		ROOM2_COMBAT(2),
		ROOM3_SPAWNED(3),
		ROOM3_BOSS(4),
		CHOICE_PHASE(5),
		FINAL_PHASE(6);
		
		private final int value;
		
		RoomState(int value)
		{
			this.value = value;
		}
		
		int getValue()
		{
			return value;
		}
		
		static RoomState fromInt(int value)
		{
			for (RoomState state : values())
			{
				if (state.value == value)
				{
					return state;
				}
			}
			return null;
		}
	}
	
	// NPCs
	private static final int KAIN_VAN_HALTER = 31639;
	private static final int GRAIL = 33996;
	private static final int MYSTERIOUS_WIZARD = 33980;
	
	// Helper NPCs
	private static final int HELPER_VAN_HALTER = 33999;
	private static final int HELPER_FERIN = 34001;
	
	// Boss
	private static final int MAKKUM = 19571;
	
	// Misc
	private static final int DOOR_2 = 17230102;
	private static final int DOOR_3 = 17230103;
	private static final int DOOR_4 = 17230104;
	private static final int TEMPLATE_ID = 255;
	private static final int PROPHECY_MACHINE = 39540;
	private static final int ATELIA = 39542;
	private static final Location FIRST_ROOM_LOC = new Location(-88503, 184754, -10440, 48891);
	private static final Location THIRD_ROOM_LOC = new Location(-88506, 177151, -10445, 48891);
	private static final Location FOURTH_ROOM_LOC = new Location(-88506, 173400, -10476, 32768);
	private static final int SPAWN_OFFSET = 120;
	private static final int HEAL_SKILL_ID = 14901;
	private static final int HEAL_SKILL_LEVEL = 1;
	
	/**
	 * Initializes the {@code ChamberOfProphecies} instance script.<br>
	 * This constructor sets up the starting NPCs and their associated dialogue IDs.
	 */
	public ChamberOfProphecies()
	{
		super(TEMPLATE_ID);
		addStartNpc(KAIN_VAN_HALTER);
		addFirstTalkId(KAIN_VAN_HALTER, GRAIL, MYSTERIOUS_WIZARD);
		addTalkId(KAIN_VAN_HALTER, GRAIL, MYSTERIOUS_WIZARD);
	}
	
	private boolean changeInstanceStatus(Instance world, RoomState newState)
	{
		final RoomState current = RoomState.fromInt(world.getStatus());
		if (current == null)
		{
			return false;
		}
		final int nextOrdinal = current.ordinal() + 1;
		if (newState.ordinal() != nextOrdinal)
		{
			return false;
		}
		world.setStatus(newState.getValue());
		return true;
	}
	
	private RoomState getState(Instance world)
	{
		return RoomState.fromInt(world.getStatus());
	}
	
	@Override
	protected void onEnter(Player player, Instance instance, boolean isFirstEntry)
	{
		if (!isFirstEntry)
		{
			final RoomState enterState = getState(instance);
			if ((enterState != null) && (enterState.ordinal() <= RoomState.ROOM3_BOSS.ordinal()))
			{
				cancelAllHelperTimers(instance.getNpc(HELPER_VAN_HALTER), instance.getNpc(HELPER_FERIN), player);
				deleteAllNpcs(instance, KAIN_VAN_HALTER, HELPER_VAN_HALTER, HELPER_FERIN);
				instance.despawnGroup("wof_room2");
				instance.despawnGroup("wof_room2_1");
				instance.despawnGroup("wof_room3");
				instance.despawnGroup("wof_room3_2");
				instance.despawnGroup("wof_room4");
				
				final boolean isRoom3 = ((enterState == RoomState.ROOM3_SPAWNED) || (enterState == RoomState.ROOM3_BOSS));
				if (isRoom3)
				{
					instance.openCloseDoor(DOOR_2, true);
					instance.openCloseDoor(DOOR_3, enterState == RoomState.ROOM3_SPAWNED);
					instance.openCloseDoor(DOOR_4, false);
					instance.setStatus(RoomState.ROOM3_SPAWNED.getValue());
					player.teleToLocation(THIRD_ROOM_LOC, instance);
					instance.spawnGroup("wof_room3");
				}
				else
				{
					instance.openCloseDoor(DOOR_2, enterState != RoomState.ROOM1_COMBAT);
					instance.openCloseDoor(DOOR_3, false);
					instance.openCloseDoor(DOOR_4, false);
					instance.setStatus(RoomState.ROOM1_COMBAT.getValue());
					player.teleToLocation(FIRST_ROOM_LOC, instance);
				}
				
				final Npc vanHalter = addSpawn(HELPER_VAN_HALTER, getOffsetLocation(player, -SPAWN_OFFSET), false, 0, false, instance.getId());
				final Npc ferin = addSpawn(HELPER_FERIN, getOffsetLocation(player, SPAWN_OFFSET), false, 0, false, instance.getId());
				initHelperNpc(vanHalter, player);
				initHelperNpc(ferin, player);
				startHelperTimers(vanHalter, ferin, player);
				startCombatMonitoring(vanHalter, player);
			}
			else if (enterState == RoomState.CHOICE_PHASE)
			{
				cancelAllHelperTimers(instance.getNpc(HELPER_VAN_HALTER), instance.getNpc(HELPER_FERIN), player);
				deleteAllNpcs(instance, KAIN_VAN_HALTER, HELPER_VAN_HALTER, HELPER_FERIN);
				
				if (instance.getNpc(MAKKUM) == null)
				{
					instance.openCloseDoor(DOOR_4, false);
					player.teleToLocation(FOURTH_ROOM_LOC, instance);
				}
				else
				{
					instance.openCloseDoor(DOOR_4, true);
					final Npc existingGrail = instance.getNpc(GRAIL);
					if (existingGrail == null)
					{
						instance.spawnGroup("q10753_16_instance_grail");
					}
					player.teleToLocation(THIRD_ROOM_LOC, instance);
					
					final Npc vanHalter = addSpawn(HELPER_VAN_HALTER, getOffsetLocation(player, -SPAWN_OFFSET), false, 0, false, instance.getId());
					final Npc ferin = addSpawn(HELPER_FERIN, getOffsetLocation(player, SPAWN_OFFSET), false, 0, false, instance.getId());
					initHelperNpc(vanHalter, player);
					startQuestTimer("ATTACK2", 200, vanHalter, player, false);
					
					if (ferin != null)
					{
						ferin.setRunning();
						ferin.setTarget(vanHalter);
						ferin.getAI().setIntention(Intention.FOLLOW, vanHalter);
					}
					startQuestTimer("CLOSE", 0, null, player);
				}
			}
			else if (enterState == RoomState.FINAL_PHASE)
			{
				cancelAllHelperTimers(instance.getNpc(HELPER_VAN_HALTER), instance.getNpc(HELPER_FERIN), player);
				player.teleToLocation(FOURTH_ROOM_LOC, instance);
			}
			else
			{
				teleportPlayerIn(player, instance);
			}
		}
		else
		{
			teleportPlayerIn(player, instance);
		}
	}
	
	private static void deleteAllNpcs(Instance world, int... npcIds)
	{
		for (int npcId : npcIds)
		{
			for (Npc npc : world.getNpcs(npcId))
			{
				npc.deleteMe();
			}
		}
	}
	
	private void cancelAllHelperTimers(Npc vanHalter, Npc ferin, Player player)
	{
		if (vanHalter != null)
		{
			cancelQuestTimer("FOLLOW", vanHalter, player);
			cancelQuestTimer("CHECK_STATUS", vanHalter, player);
			cancelQuestTimer("ATTACK", vanHalter, player);
			cancelQuestTimer("ATTACK1", vanHalter, player);
			cancelQuestTimer("ATTACK2", vanHalter, player);
		}
		if (ferin != null)
		{
			cancelQuestTimer("FOLLOW", ferin, player);
			cancelQuestTimer("HEAL_CHECK", ferin, player);
			cancelQuestTimer("BROADCAST_TEXT", ferin, player);
		}
	}
	
	private void initHelperNpc(Npc npc, Player player)
	{
		npc.setRunning();
		if (npc.asAttackable() != null)
		{
			npc.asAttackable().setCanReturnToSpawnPoint(false);
		}
		npc.setTarget(player);
		npc.getAI().setIntention(Intention.FOLLOW, player);
	}
	
	private void startHelperTimers(Npc vanHalter, Npc ferin, Player player)
	{
		if (vanHalter != null)
		{
			startQuestTimer("FOLLOW", 200, vanHalter, player, false);
			startQuestTimer("ATTACK", 500, vanHalter, player, false);
		}
		if (ferin != null)
		{
			startQuestTimer("FOLLOW", 200, ferin, player, false);
			startQuestTimer("HEAL_CHECK", 5000, ferin, player);
			startQuestTimer("BROADCAST_TEXT", 12000, ferin, player);
		}
	}
	
	private void startCombatMonitoring(Npc npc, Player player)
	{
		startQuestTimer("CHECK_STATUS", 7000, npc, player);
	}
	
	private static Location getOffsetLocation(Player player, int offset)
	{
		final double headingRad = Math.toRadians(player.getHeading() / (65535.0 / 360.0));
		final double perpX = -Math.sin(headingRad);
		final double perpY = Math.cos(headingRad);
		return new Location((int) (player.getX() + (perpX * offset)), (int) (player.getY() + (perpY * offset)), player.getZ(), player.getHeading());
	}
	
	/**
	 * Handles various events within the Chamber of Prophecies instance.<br>
	 * It manages quest progression, NPC behaviors, and world state changes.
	 * @param event The unique identifier for the script event to execute.
	 * @param npc The {@code Npc} object involved in the current event.
	 * @param player The {@code Player} object interacting with the instance.
	 * @return A {@code String} representing the HTML text to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "31639-01.html":
			case "33996-01.html":
			case "33980-01.html":
			case "33980-02.html":
			{
				htmltext = event;
				break;
			}
			case "33996-02.html":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.openCloseDoor(DOOR_4, false);
				world.broadcastPacket(ExShowUsm.USM_Q015_E);
				world.despawnGroup("q10753_16_instance_grail");
				world.spawnGroup("q10753_16_instance_wizard");
				final Npc wizard = world.getNpc(MYSTERIOUS_WIZARD);
				if (wizard != null)
				{
					wizard.setSummoner(player);
					wizard.setTitle(player.getAppearance().getVisibleName());
					wizard.broadcastInfo();
				}
				giveItems(player, ATELIA, 1);
				showOnScreenMsg(player, NpcStringId.TALK_TO_THE_MYSTERIOUS_WIZARD, ExShowScreenMessage.TOP_CENTER, 6000);
				htmltext = event;
				break;
			}
			case "33980-03.html":
			case "33980-04.html":
			{
				showOnScreenMsg(player, NpcStringId.THIS_CHOICE_CANNOT_BE_REVERSED, ExShowScreenMessage.TOP_CENTER, 6000);
				htmltext = event;
				break;
			}
			case "33980-05.html":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.spawnGroup("q10753_16_instance_halter_2");
				changeInstanceStatus(world, RoomState.FINAL_PHASE);
				startQuestTimer("DESPAWN_WIZARD", 2000, npc, player);
				htmltext = event;
				break;
			}
			case "enterInstance":
			{
				final QuestState qs = player.getQuestState(Q11027_PathOfDestinyOvercome.class.getSimpleName());
				if (qs != null)
				{
					enterInstance(player, npc, TEMPLATE_ID);
					if (hasQuestItems(player, PROPHECY_MACHINE))
					{
						takeItems(player, PROPHECY_MACHINE, 1);
					}
					
					qs.setCond(22, true);
				}
				break;
			}
			case "teleport":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				cancelAllHelperTimers(world.getNpc(HELPER_VAN_HALTER), world.getNpc(HELPER_FERIN), player);
				deleteAllNpcs(world, KAIN_VAN_HALTER, HELPER_VAN_HALTER, HELPER_FERIN);
				
				final RoomState teleportState = getState(world);
				if ((teleportState != null) && (teleportState.ordinal() < RoomState.ROOM3_SPAWNED.ordinal()))
				{
					player.teleToLocation(FIRST_ROOM_LOC);
				}
				else
				{
					player.teleToLocation(THIRD_ROOM_LOC);
				}
				
				addSpawn(HELPER_VAN_HALTER, getOffsetLocation(player, -SPAWN_OFFSET), false, 0, false, world.getId());
				addSpawn(HELPER_FERIN, getOffsetLocation(player, SPAWN_OFFSET), false, 0, false, world.getId());
				
				if ((getState(world) == RoomState.ROOM1_COMBAT) && world.getAliveNpcs(Monster.class).isEmpty())
				{
					world.spawnGroup("wof_room1");
				}
				
				final Npc vanHalter = world.getNpc(HELPER_VAN_HALTER);
				final Npc ferin = world.getNpc(HELPER_FERIN);
				if (vanHalter != null)
				{
					initHelperNpc(vanHalter, player);
				}
				if (ferin != null)
				{
					initHelperNpc(ferin, player);
				}
				startHelperTimers(vanHalter, ferin, player);
				
				if (vanHalter != null)
				{
					startCombatMonitoring(vanHalter, player);
				}
				break;
			}
			case "status":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final RoomState statusState = getState(world);
				if ((statusState != null) && (statusState.ordinal() < RoomState.CHOICE_PHASE.ordinal()))
				{
					htmltext = "31639-01.html";
				}
				else
				{
					htmltext = "31639-02.html";
				}
				break;
			}
			case "CHECK_STATUS":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final Npc ferin = world.getNpc(HELPER_FERIN);
				final Npc vanHalter = world.getNpc(HELPER_VAN_HALTER);
				final RoomState state = getState(world);
				if (state == null)
				{
					return null;
				}
				
				if (!world.getAliveNpcs(Monster.class).isEmpty())
				{
					if ((state == RoomState.ROOM1_COMBAT) || (state == RoomState.ROOM2_SPAWNED) || (state == RoomState.ROOM2_COMBAT) || (state == RoomState.ROOM3_SPAWNED) || (state == RoomState.ROOM3_BOSS))
					{
						startCombatMonitoring(npc, player);
					}
					break;
				}
				
				switch (state)
				{
					case ROOM1_COMBAT:
					{
						cancelQuestTimer("SEY2", ferin, player);
						cancelQuestTimer("SEY_KAIN", vanHalter, player);
						startQuestTimer("SEY2", 14000, ferin, player);
						startQuestTimer("SEY_KAIN", 24000, vanHalter, player);
						startQuestTimer("OPEN_DOOR1", 5000, npc, player);
						break;
					}
					case ROOM2_SPAWNED:
					{
						world.spawnGroup("wof_room2_1");
						changeInstanceStatus(world, RoomState.ROOM2_COMBAT);
						startCombatMonitoring(npc, player);
						break;
					}
					case ROOM2_COMBAT:
					{
						cancelQuestTimer("SEY3", ferin, player);
						startQuestTimer("SEY3", 8000, ferin, player);
						startQuestTimer("OPEN_DOOR2", 5000, npc, player);
						break;
					}
					case ROOM3_SPAWNED:
					{
						changeInstanceStatus(world, RoomState.ROOM3_BOSS);
						world.spawnGroup("wof_room3_2");
						world.openCloseDoor(DOOR_3, false);
						startQuestTimer("SEY_KAIN_1", 5000, vanHalter, player);
						startCombatMonitoring(npc, player);
						break;
					}
					case ROOM3_BOSS:
					{
						changeInstanceStatus(world, RoomState.CHOICE_PHASE);
						world.spawnGroup("wof_room4");
						cancelQuestTimer("FOLLOW", vanHalter, player);
						cancelQuestTimer("ATTACK", vanHalter, player);
						cancelQuestTimer("ATTACK1", vanHalter, player);
						cancelQuestTimer("ATTACK2", vanHalter, player);
						if (vanHalter != null)
						{
							vanHalter.getAI().setIntention(Intention.IDLE, player);
						}
						cancelQuestTimer("FOLLOW", ferin, player);
						cancelQuestTimer("HEAL_CHECK", ferin, player);
						if ((ferin != null) && (vanHalter != null))
						{
							ferin.setRunning();
							ferin.setTarget(vanHalter);
							ferin.getAI().setIntention(Intention.FOLLOW, vanHalter);
						}
						final Npc makkum = world.getNpc(MAKKUM);
						if (makkum != null)
						{
							makkum.setInvul(true);
						}
						startQuestTimer("SEY_KAIN_2", 3000, vanHalter, player);
						startQuestTimer("SEY4", 7000, ferin, player);
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
			case "FOLLOW":
			{
				if (npc == null)
				{
					break;
				}
				final Instance followWorld = player.getInstanceWorld();
				if (!isInInstance(followWorld))
				{
					break;
				}
				final boolean isFerin = npc.getId() == HELPER_FERIN;
				final boolean monstersAlive = !followWorld.getAliveNpcs(Monster.class).isEmpty();
				if (isFerin || !monstersAlive)
				{
					if (!npc.isCastingNow())
					{
						npc.setRunning();
						npc.setTarget(player);
						npc.getAI().setIntention(Intention.FOLLOW, player);
					}
					startQuestTimer("FOLLOW", 2000, npc, player, false);
				}
				break;
			}
			case "ATTACK":
			case "ATTACK1":
			case "ATTACK2":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world) || (npc == null))
				{
					return null;
				}
				
				npc.setRunning();
				final boolean monstersAlive = !world.getAliveNpcs(Monster.class).isEmpty();
				if (npc.getId() == HELPER_FERIN)
				{
					npc.setTarget(player);
					npc.getAI().setIntention(Intention.FOLLOW, player);
				}
				else if (!monstersAlive)
				{
					npc.setTarget(player);
					npc.getAI().setIntention(Intention.FOLLOW, player);
				}
				else
				{
					boolean found = false;
					for (Monster monster : world.getAliveNpcs(Monster.class))
					{
						if (monster.calculateDistance2D(npc) <= 2500)
						{
							addAttackDesire(npc, monster);
							found = true;
							break;
						}
					}
					if (!found)
					{
						npc.setTarget(player);
						npc.getAI().setIntention(Intention.FOLLOW, player);
					}
				}
				if (monstersAlive)
				{
					startQuestTimer(event, 500, npc, player, false);
				}
				break;
			}
			case "OPEN_DOOR1":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				changeInstanceStatus(world, RoomState.ROOM2_SPAWNED);
				world.openCloseDoor(DOOR_2, true);
				world.spawnGroup("wof_room2");
				startCombatMonitoring(npc, player);
				break;
			}
			case "OPEN_DOOR2":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final Npc vanHalter = world.getNpc(HELPER_VAN_HALTER);
				cancelQuestTimer("ATTACK", vanHalter, player);
				cancelQuestTimer("ATTACK1", vanHalter, player);
				startQuestTimer("ATTACK2", 200, vanHalter, player, true);
				changeInstanceStatus(world, RoomState.ROOM3_SPAWNED);
				world.spawnGroup("wof_room3");
				world.openCloseDoor(DOOR_3, true);
				startCombatMonitoring(npc, player);
				break;
			}
			case "BROADCAST_TEXT":
			{
				if (npc == null)
				{
					break;
				}
				npc.setTarget(player);
				npc.setRunning();
				npc.getAI().setIntention(Intention.FOLLOW, player);
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.THAT_GUY_KAIN_HAS_A_SMARMY_FACE));
				player.sendPacket(new PlaySound(3, "Npcdialog1.apple_quest_7", 0, 0, 0, 0, 0));
				break;
			}
			case "SEY2":
			{
				if ((npc != null) && (npc.getId() == HELPER_FERIN))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.YOU_CAN_T_DIE_HERE_I_DIDN_T_LEARN_RESURRECT_YET));
					player.sendPacket(new PlaySound(3, "Npcdialog1.apple_quest_4", 0, 0, 0, 0, 0));
				}
				break;
			}
			case "SEY_KAIN":
			{
				if ((npc != null) && (npc.getId() == HELPER_VAN_HALTER))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.GISELLE_WAS_SUCH_A_SWEET_CHILD));
					player.sendPacket(new PlaySound(3, "Npcdialog1.holter_quest_1", 0, 0, 0, 0, 0));
					startQuestTimer("ATTACK1", 200, npc, player, true);
				}
				break;
			}
			case "SEY3":
			{
				if ((npc != null) && (npc.getId() == HELPER_FERIN))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.DO_YOU_THINK_I_LL_GROW_TALLER_IF_I_EAT_LOTS_AND_LOTS));
					player.sendPacket(new PlaySound(3, "Npcdialog1.apple_quest_6", 0, 0, 0, 0, 0));
				}
				break;
			}
			case "SEY_KAIN_1":
			{
				if ((npc != null) && (npc.getId() == HELPER_VAN_HALTER))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.SUCH_MONSTERS_IN_A_PLACE_LIKE_THIS_UNBELIEVABLE));
					startQuestTimer("ATTACK2", 200, npc, player, true);
				}
				break;
			}
			case "SEY_KAIN_2":
			{
				if ((npc != null) && (npc.getId() == HELPER_VAN_HALTER))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.THAT_S_THE_MONSTER_THAT_ATTACKED_FAERON_YOU_RE_OUTMATCHED_HERE_GO_AHEAD_I_LL_CATCH_UP));
					player.sendPacket(new PlaySound(3, "Npcdialog1.holter_quest_6", 0, 0, 0, 0, 0));
					startQuestTimer("SEY_KAIN_3", 7000, npc, player);
				}
				break;
			}
			case "SEY4":
			{
				if ((npc != null) && (npc.getId() == HELPER_FERIN))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.GO_NOW_KAIN_CAN_HANDLE_THIS));
				}
				
				startQuestTimer("REST", 5000, npc, player);
				break;
			}
			case "SEY_KAIN_3":
			{
				if ((npc != null) && (npc.getId() == HELPER_VAN_HALTER))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.LEAVE_THIS_TO_ME_GO));
					cancelQuestTimer("ATTACK2", npc, player);
					startQuestTimer("ATTACK2", 200, npc, player, false);
					startQuestTimer("SEY_KAIN_4", 1000, npc, player);
				}
				break;
			}
			case "REST":
			{
				if ((npc != null) && (npc.getId() == HELPER_FERIN))
				{
					npc.getAI().setIntention(Intention.IDLE, player);
					cancelQuestTimer("BROADCAST_TEXT", npc, player);
				}
				break;
			}
			case "SEY_KAIN_4":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.spawnGroup("q10753_16_instance_grail");
				showOnScreenMsg(player, NpcStringId.LEAVE_THIS_PLACE_TO_KAIN_NGO_TO_THE_NEXT_ROOM, ExShowScreenMessage.TOP_CENTER, 6000);
				world.openCloseDoor(DOOR_4, true);
				startQuestTimer("CLOSE", 0, null, player);
				break;
			}
			case "CLOSE":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final Npc grail = world.getNpc(GRAIL);
				if (grail == null)
				{
					break;
				}
				if (player.calculateDistance2D(grail) < 390)
				{
					world.openCloseDoor(DOOR_4, false);
					world.despawnGroup("q10753_16_instance_halter_1_1");
					world.despawnGroup("wof_room4");
					deleteAllNpcs(world, HELPER_VAN_HALTER, HELPER_FERIN);
				}
				else
				{
					startQuestTimer("CLOSE", 3000, npc, player);
				}
				break;
			}
			case "DESPAWN_WIZARD":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.despawnGroup("q10753_16_instance_wizard");
				break;
			}
			case "HEAL_CHECK":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					break;
				}
				
				Npc ferin = world.getNpc(HELPER_FERIN);
				if ((ferin == null) || ferin.isDead())
				{
					if (ferin == null)
					{
						for (Npc npcCandidate : world.getNpcs())
						{
							if ((npcCandidate != null) && !npcCandidate.isDead())
							{
								ferin = npcCandidate;
								break;
							}
						}
					}
					if (ferin != null)
					{
						startQuestTimer("HEAL_CHECK", 5000, ferin, player);
					}
					break;
				}
				
				if (player.isDead() || !player.isOnline())
				{
					startQuestTimer("HEAL_CHECK", 5000, ferin, player);
					break;
				}
				
				if (player.getCurrentHpPercent() < 80)
				{
					final Skill healSkill = SkillData.getInstance().getSkill(HEAL_SKILL_ID, HEAL_SKILL_LEVEL);
					if (healSkill != null)
					{
						if (!GeoEngine.getInstance().canSeeTarget(ferin, player))
						{
							LOGGER.info("[HEAL_CHECK] Geodata blocked, respawning Ferin for " + player.getName());
							cancelQuestTimer("FOLLOW", ferin, player);
							cancelQuestTimer("HEAL_CHECK", ferin, player);
							deleteAllNpcs(world, HELPER_FERIN);
							ferin = addSpawn(HELPER_FERIN, getOffsetLocation(player, SPAWN_OFFSET), false, 0, false, world.getId());
							initHelperNpc(ferin, player);
							startQuestTimer("FOLLOW", 200, ferin, player, false);
						}
						ferin.setTarget(player);
						ferin.removeSkillCaster(SkillCastingType.NORMAL);
						ferin.doCast(healSkill);
						ferin.broadcastPacket(new NpcSay(ferin.getObjectId(), ChatType.NPC_GENERAL, HELPER_FERIN, NpcStringId.DID_SOMEONE_CRY_MEDIC_HERE_BE_HEALED));
					}
				}
				startQuestTimer("HEAL_CHECK", 5000, ferin, player);
				break;
			}
			case "SEY_KAIN_REENTRY":
			{
				if ((npc != null) && (npc.getId() == KAIN_VAN_HALTER))
				{
					final Instance world = player.getInstanceWorld();
					if (!isInInstance(world))
					{
						break;
					}
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.LEAVE_THIS_TO_ME_GO));
					player.sendPacket(new PlaySound(3, "Npcdialog1.holter_quest_6", 0, 0, 0, 0, 0));
					final boolean monstersAlive = !world.getAliveNpcs(Monster.class).isEmpty();
					if (monstersAlive)
					{
						startQuestTimer("SEY_KAIN_REENTRY", 10000, npc, player);
					}
				}
				break;
			}
			case "exit":
			{
				final Instance exitWorld = player.getInstanceWorld();
				if (isInInstance(exitWorld))
				{
					cancelAllHelperTimers(exitWorld.getNpc(HELPER_VAN_HALTER), exitWorld.getNpc(HELPER_FERIN), player);
				}
				startQuestTimer("finish", 3000, npc, player);
				player.sendPacket(new SystemMessage(SystemMessageId.THIS_DUNGEON_WILL_EXPIRE_IN_S1_MINUTE_S_YOU_WILL_BE_FORCED_OUT_OF_THE_DUNGEON_WHEN_THE_TIME_EXPIRES).addInt(1));
				final QuestState qs = player.getQuestState(Q11027_PathOfDestinyOvercome.class.getSimpleName());
				if (qs != null)
				{
					qs.setCond(23, true);
				}
				break;
			}
			case "finish":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.finishInstance(0);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the dialogue shown when a player talks to an {@code Npc} for the first time.<br>
	 * It checks the current quest state and returns the appropriate HTML file path based on the NPC ID.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} containing the name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState(Q11027_PathOfDestinyOvercome.class.getSimpleName());
		String htmltext = null;
		switch (npc.getId())
		{
			case KAIN_VAN_HALTER:
			{
				if ((qs != null) && qs.isCond(22))
				{
					htmltext = "31639.html";
				}
				break;
			}
			case GRAIL:
			{
				htmltext = "33996.html";
				break;
			}
			case MYSTERIOUS_WIZARD:
			{
				if ((qs != null) && qs.isCond(22))
				{
					htmltext = "33980.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code ChamberOfProphecies} class.<br>
	 * It initializes a new instance of the class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new ChamberOfProphecies();
	}
}
