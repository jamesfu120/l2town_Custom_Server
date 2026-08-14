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
package instances.FireAltar;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

import quests.Q10566_AncientWarSpirits.Q10566_AncientWarSpirits;

/**
 * @author CostyKiller
 */
public class FireAltar extends InstanceScript
{
	// NPCs
	private static final int TARKAI = 32684;
	private static final int BRAKKI = 32693;
	private static final int KIRUNA = 32694;
	private static final int TONAR = 32695;
	private static final int HERMODT = 32696;
	private static final int FIRE_ALTAR = 18030;
	
	private static final int[] ANCIENT_WAR_SPIRITS =
	{
		BRAKKI,
		KIRUNA,
		TONAR,
		HERMODT
	};
	
	// Monsters
	private static final int ENRAGED_WILD_DEFENDER = 27634;
	private static final int ENRAGED_WILD_KEEPER_TREE = 27635;
	private static final int ENRAGED_WILD_KEEPER_BUSH = 27636;
	private static final int ENRAGED_WILD_KEEPER = 27637;
	
	private static final int[] MONSTERS =
	{
		ENRAGED_WILD_DEFENDER,
		ENRAGED_WILD_KEEPER_TREE,
		ENRAGED_WILD_KEEPER_BUSH,
		ENRAGED_WILD_KEEPER
	};
	
	private static final int KEEPER_TREE_SPAWN_CHANCE = 10; // 10% chance
	
	// @formatter:off
	// Altar HP Threshold Configuration
	private static final int[] ALTAR_HP_THRESHOLDS =
	{
		80,	75,	60,	50,	30,	25
	};
	// @formatter:on
	private static final NpcStringId[] ALTAR_HP_MESSAGES =
	{
		NpcStringId.THE_FIRE_ALTAR_IS_IN_DANGER_WE_MUST_DEFEND_IT,
		NpcStringId.THE_FIRE_ALTAR_HAS_75_HP_LEFT,
		NpcStringId.THE_FIRE_ALTAR_IS_BADLY_DAMAGED_IF_YOU_DON_T_DEFEND_IT_THE_RITUAL_WILL_FAIL,
		NpcStringId.THE_FIRE_ALTAR_HAS_50_HP_LEFT,
		NpcStringId.THE_FIRE_ALTAR_IS_ALMOST_DESTROYED_KEEP_ENEMIES_AWAY_FROM_IT_UNTIL_WE_FINISH_THE_RITUAL,
		NpcStringId.THE_FIRE_ALTAR_HAS_25_HP_LEFT
	};
	
	// Misc
	private static final int TEMPLATE_ID = 349;
	private static final int INITIAL_STAGE = 0;
	private static final int STAGE1 = 1;
	private static final int STAGE2 = 2;
	private static final int STAGE3 = 3;
	private static final int STAGE4 = 4;
	
	// Timer delays in milliseconds
	private static final int DELAY_RITUAL_ANNOUNCE = 6000;
	private static final int DELAY_RITUAL_BEGIN = 6000;
	private static final int DELAY_PROGRESS_CHECK = 5000;
	private static final int DELAY_MISSION_END = 6000;
	private static final int DELAY_KEEP_ATTACKING = 2000;
	
	// Wave timings
	private static final int SPAWN_WAVE1_DELAY = 2000;
	private static final int PRAY_BRAKKI_DELAY = 7000;
	private static final int SPAWN_BRAKKI_DELAY = 15000;
	private static final int SPAWN_WAVE2_DELAY = 21000;
	private static final int PRAY_KIRUNA_DELAY = 27000;
	private static final int SPAWN_KIRUNA_DELAY = 33000;
	private static final int SPAWN_WAVE3_DELAY = 40000;
	private static final int PRAY_TONAR_DELAY = 46000;
	private static final int SPAWN_TONAR_DELAY = 51000;
	private static final int SPAWN_WAVE4_DELAY = 56000;
	private static final int PRAY_HERMODT_DELAY = 65000;
	private static final int SPAWN_HERMODT_DELAY = 72000;
	private static final int SPIRIT_MESSAGE_DELAY = 10000;
	
	public FireAltar()
	{
		super(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addFirstTalkId(TARKAI);
		addFirstTalkId(ANCIENT_WAR_SPIRITS);
		addSpawnId(FIRE_ALTAR);
		addSpawnId(MONSTERS);
		addAttackId(FIRE_ALTAR);
		addKillId(MONSTERS);
		addKillId(FIRE_ALTAR);
	}
	
	@Override
	public void onInstanceCreated(Instance activeInstance, Player player)
	{
		activeInstance.setStatus(INITIAL_STAGE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				showOnScreenMsg(player, NpcStringId.TO_COMPLETE_THE_MISSION_TALK_TO_TARKAI_ZU_DUDA_MARA, ExShowScreenMessage.TOP_CENTER, 5000, true);
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
			case "startMission":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(player, NpcStringId.THE_SUMMONING_RITUAL_HAS_STARTED_PLEASE_DON_T_DISTURB_ME, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_RITUAL", DELAY_RITUAL_ANNOUNCE, null, player);
				}
				break;
			}
			case "finishMission":
			{
				final QuestState questState = player.getQuestState(Q10566_AncientWarSpirits.class.getSimpleName());
				final int currentCount = questState.getCount();
				questState.setCount(currentCount + 1);
				questState.set("FIRE_ALTAR_PASSED", 1);
				questState.setCond(QuestCondType.DONE);
				player.sendPacket(new ExQuestNotification(questState));
				finishInstance(player);
				break;
			}
			case "ANNOUNCE_RITUAL":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(player, NpcStringId.THE_SUMMONING_RITUAL_HAS_STARTED_PROTECT_THE_FIRE_ALTAR_UNTIL_ALL_ANCIENT_WAR_SPIRITS_ARE_SUMMONED, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("BEGIN_RITUAL", DELAY_RITUAL_BEGIN, null, player);
				}
				break;
			}
			case "BEGIN_RITUAL":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					break;
				}
				
				final Npc fireAltar = world.getNpc(FIRE_ALTAR);
				if ((fireAltar == null) || fireAltar.isDead())
				{
					break;
				}
				
				// Announce ritual begin.
				showOnScreenMsg(player, NpcStringId.THE_SUMMONING_RITUAL_HAS_BEGUN_KILL_THE_ENEMIES_THAT_ARE_TRYING_TO_INTERRUPT_IT, ExShowScreenMessage.TOP_CENTER, 5000, true);
				
				// Schedule all event timers.
				startQuestTimer("CHECK_PROGRESS", DELAY_PROGRESS_CHECK, null, player, true);
				startQuestTimer("SPAWN_WAVE1_MONSTERS", SPAWN_WAVE1_DELAY, null, player);
				
				startQuestTimer("PRAY_FOR_BRAKKI", PRAY_BRAKKI_DELAY, null, player);
				startQuestTimer("SPAWN_BRAKKI", SPAWN_BRAKKI_DELAY, null, player);
				
				startQuestTimer("SPAWN_WAVE2_MONSTERS", SPAWN_WAVE2_DELAY, null, player);
				
				startQuestTimer("PRAY_FOR_KIRUNA", PRAY_KIRUNA_DELAY, null, player);
				startQuestTimer("SPAWN_KIRUNA", SPAWN_KIRUNA_DELAY, null, player);
				
				startQuestTimer("SPAWN_WAVE3_MONSTERS", SPAWN_WAVE3_DELAY, null, player);
				
				startQuestTimer("PRAY_FOR_TONAR", PRAY_TONAR_DELAY, null, player);
				startQuestTimer("SPAWN_TONAR", SPAWN_TONAR_DELAY, null, player);
				
				startQuestTimer("SPAWN_WAVE4_MONSTERS", SPAWN_WAVE4_DELAY, null, player);
				
				startQuestTimer("PRAY_FOR_HERMODT", PRAY_HERMODT_DELAY, null, player);
				startQuestTimer("SPAWN_HERMODT", SPAWN_HERMODT_DELAY, null, player);
				break;
			}
			case "PRAY_FOR_BRAKKI":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.HEAR_US_THE_WISE_BRAKKI_SHARE_YOUR_WISDOM_WITH_THE_HIGH_ORCS, ExShowScreenMessage.TOP_CENTER, 6000, true);
				}
				break;
			}
			case "SPAWN_BRAKKI":
			{
				if (!validateAltarState(player))
				{
					break;
				}
				
				final Instance world = player.getInstanceWorld();
				
				world.spawnGroup("BRAKKI");
				world.setStatus(STAGE1);
				showOnScreenMsg(player, NpcStringId.THE_WISE_BRAKKI_IS_SUMMONED_THE_RITUAL_CONTINUES, ExShowScreenMessage.TOP_CENTER, 5000, true);
				startQuestTimer("BRAKKI_MESSAGE", SPIRIT_MESSAGE_DELAY, null, player);
				break;
			}
			case "BRAKKI_MESSAGE":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.HOW_DID_YOU_MANAGE_TO_SUMMON_ME_ALL_RIGHT_I_LL_HELP_YOU_THIS_TIME, ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "SPAWN_WAVE1_MONSTERS":
			case "SPAWN_WAVE2_MONSTERS":
			case "SPAWN_WAVE3_MONSTERS":
			case "SPAWN_WAVE4_MONSTERS":
			{
				if (!validateAltarState(player))
				{
					break;
				}
				
				final int waveNumber = Character.getNumericValue(event.charAt(10));
				showOnScreenMsg(player, NpcStringId.THE_MONSTER_WAVE_S1_HAS_APPEARED, ExShowScreenMessage.TOP_CENTER, 3000, true, String.valueOf(waveNumber));
				player.getInstanceWorld().spawnGroup("WAVE" + waveNumber + "_MONSTERS");
				break;
			}
			case "PRAY_FOR_KIRUNA":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.THE_VALIANT_KIRUNA_WE_SUMMON_THEE_GIVE_US_STRENGTH_TO_DEFEAT_OUR_ENEMIES, ExShowScreenMessage.TOP_CENTER, 6000, true);
				}
				break;
			}
			case "SPAWN_KIRUNA":
			{
				if (!validateAltarState(player))
				{
					break;
				}
				
				final Instance world = player.getInstanceWorld();
				
				world.spawnGroup("KIRUNA");
				world.setStatus(STAGE2);
				showOnScreenMsg(player, NpcStringId.THE_VALIANT_KIRUNA_IS_SUMMONED_THE_RITUAL_CONTINUES, ExShowScreenMessage.TOP_CENTER, 5000, true);
				startQuestTimer("KIRUNA_MESSAGE", SPIRIT_MESSAGE_DELAY, null, player);
				break;
			}
			case "KIRUNA_MESSAGE":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.WELL_I_CAN_T_REFUSE_YOUR_SINCERE_REQUEST, ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "PRAY_FOR_TONAR":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.THE_DREADFUL_TONAR_I_SUMMON_THEE_COME_AND_FULFILL_YOUR_DESTINY, ExShowScreenMessage.TOP_CENTER, 6000, true);
				}
				break;
			}
			case "SPAWN_TONAR":
			{
				if (!validateAltarState(player))
				{
					break;
				}
				
				final Instance world = player.getInstanceWorld();
				
				world.spawnGroup("TONAR");
				world.setStatus(STAGE3);
				showOnScreenMsg(player, NpcStringId.THE_DREADFUL_TONAR_IS_SUMMONED_THE_RITUAL_CONTINUES, ExShowScreenMessage.TOP_CENTER, 5000, true);
				startQuestTimer("TONAR_MESSAGE", SPIRIT_MESSAGE_DELAY, null, player);
				break;
			}
			case "TONAR_MESSAGE":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.I_THE_DREADFUL_TONAR_HAVE_COME_IN_ANSWER_TO_YOUR_PLEAS, ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "PRAY_FOR_HERMODT":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.THE_INDOMITABLE_HERMODT_RETURN_AND_RID_US_OF_OUR_WEAKNESSES, ExShowScreenMessage.TOP_CENTER, 6000, true);
				}
				break;
			}
			case "SPAWN_HERMODT":
			{
				if (!validateAltarState(player))
				{
					break;
				}
				
				final Instance world = player.getInstanceWorld();
				
				world.spawnGroup("HERMODT");
				world.setStatus(STAGE4);
				showOnScreenMsg(player, NpcStringId.THE_INDOMITABLE_HERMODT_IS_SUMMONED_ALL_OF_THE_ANCIENT_WAR_SPIRITS_ARE_SUMMONED, ExShowScreenMessage.TOP_CENTER, 5000, true);
				startQuestTimer("HERMODT_MESSAGE", SPIRIT_MESSAGE_DELAY, null, player);
				break;
			}
			case "HERMODT_MESSAGE":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.DO_NOT_FEAR_ANYMORE_FOR_HERMODT_IS_HERE, ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "ENRAGED_WILD_KEEPER_TREE_SPAWN_MESSAGE":
			{
				if (validateAltarState(player))
				{
					showOnScreenMsg(player, NpcStringId.ENRAGED_WILD_KEEPER_TREE_HAS_APPEARED_DESTROY_IT_BEFORE_IT_SELF_DESTRUCTS, ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "KEEP_ATTACKING":
			{
				final Instance world = npc.getInstanceWorld();
				final Npc fireAltar = world.getNpc(FIRE_ALTAR);
				if (isInInstance(world) && (fireAltar != null) && !fireAltar.isDead())
				{
					addAttackDesire(npc, fireAltar.asCreature());
					npc.getAI().setIntentionAttack(fireAltar);
				}
				else
				{
					cancelQuestTimer("KEEP_ATTACKING", npc, null);
				}
				break;
			}
			case "CHECK_PROGRESS":
			{
				final Instance world = player.getInstanceWorld();
				if (world == null)
				{
					break;
				}
				
				final Npc fireAltar = world.getNpc(FIRE_ALTAR);
				if (isInInstance(world) && (fireAltar != null) && !fireAltar.isDead())
				{
					if ((world.getStatus() == STAGE4) && (world.getAliveNpcCount(MONSTERS) == 0))
					{
						showOnScreenMsg(player, NpcStringId.THE_SUMMONING_RITUAL_IS_OVER_THANK_YOU_FOR_DEFENDING_THE_FIRE_ALTAR, ExShowScreenMessage.TOP_CENTER, 5000, true);
						startQuestTimer("END_MISSION", DELAY_MISSION_END, npc, player);
					}
				}
				break;
			}
			case "END_MISSION":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc fireAltar = world.getNpc(FIRE_ALTAR);
					if ((fireAltar != null) && !fireAltar.isDead())
					{
						showOnScreenMsg(player, NpcStringId.THE_SUMMONING_RITUAL_IS_OVER_TO_COMPLETE_THE_MISSION_TALK_TO_TARKAI_ZU_DUDA_MARA, ExShowScreenMessage.TOP_CENTER, 5000, true);
					}
				}
				else
				{
					showOnScreenMsg(player, NpcStringId.THE_FIRE_ALTAR_IS_DESTROYED_YOU_HAVE_FAILED_THE_MISSION, ExShowScreenMessage.TOP_CENTER, 5000, true);
					finishInstance(player);
				}
				
				// Remove progress timer.
				cancelQuestTimer("CHECK_PROGRESS", npc, player);
				break;
			}
			case "32684.html":
			case "32693.html":
			case "32694.html":
			case "32695.html":
			case "32696.html":
			{
				return event;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Helper method to validate altar state and reduce code duplication
	 * @param player The player to get the instance from
	 * @return true if the instance and altar are valid, false otherwise
	 */
	private boolean validateAltarState(Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (!isInInstance(world))
		{
			return false;
		}
		
		final Npc fireAltar = world.getNpc(FIRE_ALTAR);
		return (fireAltar != null) && !fireAltar.isDead();
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			npc.showChatWindow(player);
			return null;
		}
		
		switch (npc.getId())
		{
			case BRAKKI:
			{
				return "32693.html";
			}
			case KIRUNA:
			{
				return "32694.html";
			}
			case TONAR:
			{
				return "32695.html";
			}
			case HERMODT:
			{
				return "32696.html";
			}
			case TARKAI:
			{
				switch (world.getStatus())
				{
					case INITIAL_STAGE:
					{
						if (npc.getScriptValue() != 0)
						{
							return "32684-started.html";
						}
						
						npc.setScriptValue(1);
						return "32684.html";
					}
					case STAGE1:
					case STAGE2:
					case STAGE3:
					{
						return "32684-started.html";
					}
					case STAGE4:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							return "32684-finished.html";
						}
						
						return "32684-started.html";
					}
				}
				break;
			}
		}
		
		npc.showChatWindow(player);
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case ENRAGED_WILD_DEFENDER:
			case ENRAGED_WILD_KEEPER:
			case ENRAGED_WILD_KEEPER_BUSH:
			{
				setupMonsterAttack(npc, world);
				break;
			}
			case ENRAGED_WILD_KEEPER_TREE:
			{
				final Npc fireAltar = world.getNpc(FIRE_ALTAR);
				if ((fireAltar != null) && !fireAltar.isDead())
				{
					addAttackDesire(npc, fireAltar.asCreature());
					npc.getAI().moveTo(fireAltar.getLocation());
				}
				break;
			}
			case FIRE_ALTAR:
			{
				npc.setDisplayEffect(1);
				npc.disableCoreAI(true);
				break;
			}
		}
	}
	
	/**
	 * Helper method to setup monster attack behavior
	 * @param npc The monster to setup
	 * @param world The instance world
	 */
	private void setupMonsterAttack(Npc npc, Instance world)
	{
		final Npc fireAltar = world.getNpc(FIRE_ALTAR);
		if ((fireAltar == null) || fireAltar.isDead())
		{
			return;
		}
		
		npc.setRandomWalking(false);
		((Attackable) npc).addDamageHate(fireAltar.asCreature(), 0, 999999);
		addAttackDesire(npc, fireAltar.asCreature());
		npc.setTarget(fireAltar);
		npc.getAI().setIntentionAttack(fireAltar);
		
		// Repeating timer needed to keep monsters attacking.
		startQuestTimer("KEEP_ATTACKING", DELAY_KEEP_ATTACKING, npc, null, true);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final int hpPercent = npc.getCurrentHpPercent();
		final int currentMark = npc.getScriptValue();
		
		// Check HP thresholds.
		for (int i = 0; i < ALTAR_HP_THRESHOLDS.length; i++)
		{
			if ((hpPercent <= ALTAR_HP_THRESHOLDS[i]) && (currentMark == i))
			{
				showOnScreenMsg(world, ALTAR_HP_MESSAGES[i], ExShowScreenMessage.TOP_CENTER, 5000, true);
				npc.setScriptValue(i + 1);
				npc.setDisplayEffect(i + 1);
				break; // Only trigger one threshold per attack.
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case ENRAGED_WILD_DEFENDER:
			case ENRAGED_WILD_KEEPER:
			case ENRAGED_WILD_KEEPER_BUSH:
			{
				// Chance to spawn a keeper tree.
				if (getRandom(100) < KEEPER_TREE_SPAWN_CHANCE)
				{
					addSpawn(ENRAGED_WILD_KEEPER_TREE, killer, true, 60000, false, killer.getInstanceId());
					startQuestTimer("ENRAGED_WILD_KEEPER_TREE_SPAWN_MESSAGE", 1000, npc, killer);
				}
				break;
			}
			case FIRE_ALTAR:
			{
				// Despawn all monster waves.
				for (int i = 1; i <= 4; i++)
				{
					world.despawnGroup("WAVE" + i + "_MONSTERS");
				}
				
				// Despawn the keeper trees.
				world.getAliveNpcs(ENRAGED_WILD_KEEPER_TREE).forEach(Npc::decayMe);
				
				// Get first player since killer of altar is not player.
				final Player player = world.getFirstPlayer();
				if (player != null)
				{
					showOnScreenMsg(player, NpcStringId.DESPITE_OUR_BEST_EFFORTS_THE_FIRE_ALTAR_HAS_BEEN_DESTROYED, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("END_MISSION", DELAY_MISSION_END, npc, player);
				}
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new FireAltar();
	}
}
