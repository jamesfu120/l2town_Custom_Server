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
package org.l2jmobius.gameserver.ai;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * This class manages AI of Creature.<br>
 * CreatureAI:
 * <ul>
 * <li>AttackableAI</li>
 * <li>DoorAI</li>
 * <li>PlayerAI</li>
 * <li>SummonAI</li>
 * </ul>
 */
public class CreatureAI extends AbstractAI
{
	private OnNpcMoveFinished _onNpcMoveFinished = null;
	
	public CreatureAI(Creature creature)
	{
		super(creature);
	}
	
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		clientStartAutoAttack();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ATTACKED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Manage the Idle Intention : Stop Attack, Movement and Stand Up the actor.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType (broadcast)</li>
	 * </ul>
	 */
	@Override
	public void setIntentionIdle()
	{
		// Stop the follow mode (IDLE is neither FOLLOW nor ATTACK).
		stopFollow();
		
		// Set the AI Intention to IDLE.
		_intention = Intention.IDLE;
		
		// Init cast target.
		setCastTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
		clientStopAutoAttack();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isRemovedBy(Intention.IDLE))
		{
			setNextAction(null);
		}
	}
	
	/**
	 * Manage the Active Intention : Stop Attack, Movement and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>if the Intention is not already Active</i></b>
	 * <ul>
	 * <li>Set the AI Intention to ACTIVE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 */
	@Override
	public void setIntentionActive()
	{
		try
		{
			// Stop the follow mode (ACTIVE is neither FOLLOW nor ATTACK).
			stopFollow();
			
			// Check if the Intention is not already Active.
			if (getIntention() == Intention.ACTIVE)
			{
				return;
			}
			
			// Set the AI Intention to ACTIVE.
			_intention = Intention.ACTIVE;
			
			// Check if region and its neighbors are active.
			final WorldRegion region = _actor.getWorldRegion();
			if ((region == null) || !region.areNeighborsActive())
			{
				return;
			}
			
			// Init cast target.
			setCastTarget(null);
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
			clientStopMoving(null);
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			// Launch the Think Action.
			notifyActionThink();
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.ACTIVE))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Manage the Rest Intention.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * </ul>
	 */
	@Override
	public void setIntentionRest()
	{
		// Set the AI Intention to IDLE.
		setIntentionIdle();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isRemovedBy(Intention.REST))
		{
			setNextAction(null);
		}
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to ATTACK</li>
	 * <li>Set or change the AI attack target</li>
	 * <li>Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart (broadcast)</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <ul>
	 * <li>AttackableAI : Calculate attack timeout</li>
	 * </ul>
	 */
	@Override
	public void setIntentionAttack(WorldObject target)
	{
		try
		{
			if ((target == null) || !target.isTargetable())
			{
				clientActionFailed();
				return;
			}
			
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isControlBlocked())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Check if the Intention is already ATTACK.
			if (getIntention() == Intention.ATTACK)
			{
				// Check if the AI already targets the Creature.
				if (getTarget() != target)
				{
					// Set the AI attack target (change target).
					setTarget(target);
					
					// Launch the Think Action.
					notifyActionThink();
				}
				else
				{
					// A repeated attack on the same target revalidates a move that ended without arriving.
					//  Sending only ActionFailed leaves the intention waiting on an event that never comes.
					stopFollow();
					notifyActionThink();
				}
			}
			else
			{
				// Set the Intention of this AbstractAI to ATTACK.
				_intention = Intention.ATTACK;
				
				// Set the AI attack target.
				setTarget(target);
				
				// Launch the Think Action.
				notifyActionThink();
			}
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.ATTACK))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Manage the Cast Intention : Stop current Attack, Init the AI in order to cast and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI cast target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Cancel action client side by sending Server->Client packet ActionFailed to the Player actor</li>
	 * <li>Set the AI skill used by INTENTION_CAST</li>
	 * <li>Set the Intention of this AI to CAST</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 */
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		try
		{
			// Stop the follow mode (CAST is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if ((getIntention() == Intention.REST) && skill.isMagic())
			{
				clientActionFailed();
				return;
			}
			
			final long currentTime = System.nanoTime();
			final long attackEndTime = _actor.getAttackEndTime();
			if (attackEndTime > currentTime)
			{
				ThreadPool.schedule(() ->
				{
					if (_actor.isAttackingNow())
					{
						_actor.abortAttack();
					}
					
					changeIntentionToCast(skill, target, item, forceUse, dontMove);
				}, TimeUnit.NANOSECONDS.toMillis(attackEndTime - currentTime));
			}
			else
			{
				changeIntentionToCast(skill, target, item, forceUse, dontMove);
			}
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.CAST))
			{
				setNextAction(null);
			}
		}
	}
	
	protected void changeIntentionToCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Set the AI cast target.
		setCastTarget(target);
		
		// Set the AI skill used by INTENTION_CAST.
		_skill = skill;
		
		// Set the AI item that triggered this skill.
		_item = item;
		
		// Set the ctrl/shift pressed parameters.
		_forceUse = forceUse;
		_dontMove = dontMove;
		
		// Change the Intention of this AbstractAI to CAST.
		_intention = Intention.CAST;
		
		// Launch the Think Action.
		notifyActionThink();
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to MOVE_TO</li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)</li>
	 * </ul>
	 */
	@Override
	public void setIntentionMoveTo(ILocational loc)
	{
		try
		{
			// Stop the follow mode (MOVE_TO is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Set the Intention of this AbstractAI to MOVE_TO.
			_intention = Intention.MOVE_TO;
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			// Abort the attack of the Creature and send Server->Client ActionFailed packet.
			_actor.abortAttack();
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
			moveTo(loc.getX(), loc.getY(), loc.getZ());
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.MOVE_TO))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Manage the Follow Intention : Stop current Attack and Launch a Follow Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to FOLLOW</li>
	 * <li>Create and Launch an AI Follow Task to execute every 1s</li>
	 * </ul>
	 */
	@Override
	public void setIntentionFollow(WorldObject target)
	{
		try
		{
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Dead actors can't follow.
			if (_actor.isDead())
			{
				clientActionFailed();
				return;
			}
			
			// Do not follow yourself.
			if (_actor == target)
			{
				clientActionFailed();
				return;
			}
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			// Set the Intention of this AbstractAI to FOLLOW.
			_intention = Intention.FOLLOW;
			
			// Create and Launch an AI Follow Task to execute every 1s.
			startFollow(target.asCreature());
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.FOLLOW))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Manage the PickUp Intention : Set the pick up target and Launch a Move To Pawn Task (offset=20).<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI pick up target</li>
	 * <li>Set the Intention of this AI to PICK_UP</li>
	 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * </ul>
	 */
	@Override
	public void setIntentionPickUp(WorldObject object)
	{
		try
		{
			// Stop the follow mode (PICK_UP is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			if (object.isItem() && (((Item) object).getItemLocation() != ItemLocation.VOID))
			{
				return;
			}
			
			// Set the Intention of this AbstractAI to PICK_UP.
			_intention = Intention.PICK_UP;
			
			// Set the AI pick up target.
			setTarget(object);
			
			if ((object.getX() == 0) && (object.getY() == 0))
			{
				// LOGGER.warning("Object in coords 0,0 - using a temporary fix");
				object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
			}
			
			// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast).
			moveToPawn(object, 20);
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.PICK_UP))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Manage the Interact Intention : Set the interact target and Launch a Move To Pawn Task (offset=60).<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the AI interact target</li>
	 * <li>Set the Intention of this AI to INTERACT</li>
	 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * </ul>
	 */
	@Override
	public void setIntentionInteract(WorldObject object)
	{
		try
		{
			// Stop the follow mode (INTERACT is neither FOLLOW nor ATTACK).
			stopFollow();
			
			if (getIntention() == Intention.REST)
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			{
				// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
				clientActionFailed();
				return;
			}
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
			clientStopAutoAttack();
			
			if (getIntention() != Intention.INTERACT)
			{
				// Set the Intention of this AbstractAI to INTERACT.
				_intention = Intention.INTERACT;
				
				// Set the AI interact target.
				setTarget(object);
				
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast).
				moveToPawn(object, 60);
			}
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isRemovedBy(Intention.INTERACT))
			{
				setNextAction(null);
			}
		}
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	public void notifyActionThink()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.THINK))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.AGGRESSION))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Stunned then onAttacked Action.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Launch actions corresponding to the Action onAttacked (only for AttackableAI after the stunning periode)</li>
	 * </ul>
	 */
	@Override
	public void notifyActionBlocked()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast).
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also.
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.BLOCKED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Rooted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Action onAttacked</li>
	 * </ul>
	 */
	@Override
	public void notifyActionRooted()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ROOTED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Confused.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Action onAttacked</li>
	 * </ul>
	 */
	@Override
	public void notifyActionConfused()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.CONFUSED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Muted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * </ul>
	 */
	@Override
	public void notifyActionMuted()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.MUTED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	public void notifyActionEvaded(WorldObject attacker)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.EVADED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action ReadyToAct.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	public void notifyActionReadyToAct()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Skip if the actor is casting (matches the prior dispatcher gate).
		if (!_actor.isCastingNow())
		{
			// Launch actions corresponding to the Action Think.
			notifyActionThink();
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.READY_TO_ACT))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Arrived.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	public void notifyActionArrived()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		try
		{
			// Skip if the actor is casting (matches the prior dispatcher gate, e.g. from stopmove during cast).
			if (_actor.isCastingNow())
			{
				return;
			}
			
			getActor().revalidateZone(true);
			
			if (getActor().moveToNextRoutePoint())
			{
				return;
			}
			
			clientStoppedMoving();
			
			// If the Intention was MOVE_TO, set the Intention to ACTIVE.
			if (getIntention() == Intention.MOVE_TO)
			{
				setIntentionActive();
			}
			
			if (_actor.isNpc())
			{
				final Npc npc = _actor.asNpc();
				WalkingManager.getInstance().onArrived(npc); // Walking Manager support.
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_FINISHED, npc))
				{
					if (_onNpcMoveFinished == null)
					{
						_onNpcMoveFinished = new OnNpcMoveFinished(npc);
					}
					
					EventDispatcher.getInstance().notifyEventAsync(_onNpcMoveFinished, npc);
				}
			}
			
			// Launch actions corresponding to the Action Think.
			notifyActionThink();
		}
		finally
		{
			final NextAction nextAction = getNextAction();
			if ((nextAction != null) && nextAction.isTriggeredBy(Action.ARRIVED))
			{
				setNextAction(null);
				nextAction.doAction();
			}
		}
	}
	
	/**
	 * Launch actions corresponding to the Action ArrivedRevalidate.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	public void notifyActionArrivedRevalidate()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Skip if the actor is not moving any more (matches the prior dispatcher gate).
		if (_actor.isMoving())
		{
			// Launch actions corresponding to the Action Think.
			notifyActionThink();
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ARRIVED_REVALIDATE))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action ArrivedBlocked.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	public void notifyActionArrivedBlocked(Location location)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// If the Intention was MOVE_TO, set the Intention to ACTIVE.
		if ((getIntention() == Intention.MOVE_TO) || (getIntention() == Intention.CAST))
		{
			setIntentionActive();
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(location);
		
		// Launch actions corresponding to the Action Think.
		notifyActionThink();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.ARRIVED_BLOCKED))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action ForgetObject.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to attack, stop the auto-attack, cancel target and set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to cast, cancel target and set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to follow, stop the movement, cancel AI Follow Task and set the Intention to ACTIVE</li>
	 * <li>If the targeted object was the actor , cancel AI target, stop AI Follow Task, stop the movement and set the Intention to IDLE</li>
	 * </ul>
	 */
	@Override
	public void notifyActionForgetObject(WorldObject object)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Remove the object from the seen creatures list (matches the prior dispatcher side effect).
		_actor.removeSeenCreature(object);
		
		final WorldObject target = getTarget();
		
		// Stop any casting pointing to this object.
		getActor().abortCast(sc -> sc.getTarget() == object);
		
		// If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE.
		if (target == object)
		{
			setTarget(null);
			
			if (isFollowing())
			{
				// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
				clientStopMoving(null);
				
				// Stop an AI Follow Task.
				stopFollow();
			}
			
			// Stop any intention that has target we want to forget.
			if (getIntention() != Intention.MOVE_TO)
			{
				setIntentionActive();
			}
		}
		
		// Check if the targeted object was the actor.
		if (_actor == object)
		{
			// Cancel AI target.
			setTarget(null);
			
			// Init cast target.
			setCastTarget(null);
			
			// Stop an AI Follow Task.
			stopFollow();
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
			clientStopMoving(null);
			
			// Set the Intention of this AbstractAI to IDLE.
			_intention = Intention.IDLE;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.FORGET_OBJECT))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Cancel.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	public void notifyActionCancel()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		_actor.abortCast();
		
		// Stop an AI Follow Task.
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		// Launch actions corresponding to the Action Think.
		notifyActionThink();
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.CANCEL))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Dead.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)</li>
	 * </ul>
	 */
	@Override
	public void notifyActionDeath()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop an AI Tasks.
		stopAITask();
		
		if (_actor.isNpc())
		{
			_actor.asNpc().setDisplayEffect(0);
		}
		
		// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast).
		clientNotifyDead();
		
		if (!_actor.isPlayable() && !_actor.isFakePlayer())
		{
			_actor.setWalking();
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.DEATH))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Fake Death.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * </ul>
	 */
	@Override
	public void notifyActionFakeDeath()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		// Stop an AI Follow Task.
		stopFollow();
		
		// Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast).
		clientStopMoving(null);
		
		// Init AI
		_intention = Intention.IDLE;
		setTarget(null);
		setCastTarget(null);
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.FAKE_DEATH))
		{
			nextAction.doAction();
		}
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		final NextAction nextAction = getNextAction();
		if ((nextAction != null) && nextAction.isTriggeredBy(Action.FINISH_CASTING))
		{
			nextAction.doAction();
		}
	}
	
	protected boolean maybeMoveToPosition(ILocational worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			// LOGGER.warning("maybeMoveToPosition: worldPosition == NULL!");
			return false;
		}
		
		if (offset < 0)
		{
			return false; // skill radius -1
		}
		
		if (!_actor.isInsideRadius2D(worldPosition, offset + _actor.getTemplate().getCollisionRadius()))
		{
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				return true;
			}
			
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			
			int x = _actor.getX();
			int y = _actor.getY();
			
			final double dx = worldPosition.getX() - x;
			final double dy = worldPosition.getY() - y;
			double dist = Math.sqrt((dx * dx) + (dy * dy));
			
			final double sin = dy / dist;
			final double cos = dx / dist;
			dist -= offset - 5;
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			moveTo(x, y, worldPosition.getZ());
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		return false;
	}
	
	/**
	 * Manage the Move to Pawn action in function of the distance and of the Interact area.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get the distance between the current position of the Creature and the target (x,y)</li>
	 * <li>If the distance > offset+20, move the actor (by running) to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * <li>If the distance <= offset+20, Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PLayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @param offsetValue The Interact area radius
	 * @return True if a movement must be done
	 */
	protected boolean maybeMoveToPawn(WorldObject target, int offsetValue)
	{
		// Get the distance between the current position of the Creature and the target (x,y).
		if (target == null)
		{
			// LOGGER.warning("maybeMoveToPawn: target == NULL!");
			return false;
		}
		
		if (offsetValue < 0)
		{
			return false; // skill radius -1
		}
		
		int offsetWithCollision = offsetValue + _actor.getTemplate().getCollisionRadius();
		if (target.isCreature())
		{
			offsetWithCollision += target.asCreature().getTemplate().getCollisionRadius();
		}
		
		if (!_actor.isInsideRadius2D(target, offsetWithCollision))
		{
			// Caller should be Playable and thinkAttack/thinkCast/thinkInteract/thinkPickUp.
			if (isFollowing())
			{
				// Allow larger hit range when the target is moving (check is run only once per second).
				if (!_actor.isInsideRadius2D(target, offsetWithCollision + 100))
				{
					return true;
				}
				
				stopFollow();
				return false;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				// If player is trying attack target but he cannot move to attack target.
				// Change his intention to idle.
				if (_actor.getAI().getIntention() == Intention.ATTACK)
				{
					_actor.getAI().setIntentionIdle();
				}
				
				return true;
			}
			
			// While flying there is no move to cast.
			if (_actor.isPlayer() && (_actor.getAI().getIntention() == Intention.CAST))
			{
				final Transform transform = _actor.getTransformation();
				if ((transform != null) && !transform.canUseWeaponStats())
				{
					_actor.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
					_actor.sendPacket(ActionFailed.STATIC_PACKET);
					return true;
				}
			}
			
			// If not running, set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			int offset = offsetValue;
			if (target.isCreature() && !target.isDoor())
			{
				if (target.asCreature().isMoving())
				{
					offset -= 100;
				}
				
				if (offset < 5)
				{
					offset = 5;
				}
				
				startFollow(target.asCreature(), offset);
			}
			else
			{
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast).
				moveToPawn(target, offset);
			}
			
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast).
		// clientStopMoving(null);
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost or dead.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>If the target is lost or dead</i></b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to ACTIVE</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PLayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is lost or dead (false if fakedeath)
	 */
	protected boolean checkTargetLostOrDead(Creature target)
	{
		if ((target == null) || target.isDead())
		{
			setIntentionActive();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>If the target is lost</i></b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to ACTIVE</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PlayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is lost
	 */
	protected boolean checkTargetLost(WorldObject target)
	{
		if (target == null)
		{
			setIntentionActive();
			return true;
		}
		
		if (_actor != null)
		{
			if ((_skill != null) && _skill.hasNegativeEffect() && (_skill.getAffectRange() > 0))
			{
				if (_actor.isPlayer() && _actor.isMoving())
				{
					if (!GeoEngine.getInstance().canMoveToTarget(_actor, target))
					{
						setIntentionActive();
						return true;
					}
				}
				else
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
					{
						setIntentionActive();
						return true;
					}
				}
			}
			
			if (_actor.isSummon())
			{
				if (GeoEngine.getInstance().canMoveToTarget(_actor, target))
				{
					return false;
				}
				
				setIntentionActive();
				return true;
			}
		}
		
		return false;
	}
	
	protected class SelfAnalysis
	{
		public boolean isMage = false;
		public boolean isBalanced;
		public boolean isArcher = false;
		public boolean isHealer = false;
		public boolean isFighter = false;
		public boolean cannotMoveOnLand = false;
		public Set<Skill> generalSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> buffSkills = ConcurrentHashMap.newKeySet();
		public int lastBuffTick = 0;
		public Set<Skill> debuffSkills = ConcurrentHashMap.newKeySet();
		public int lastDebuffTick = 0;
		public Set<Skill> cancelSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> healSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> generalDisablers = ConcurrentHashMap.newKeySet();
		public Set<Skill> sleepSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> rootSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> muteSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> resurrectSkills = ConcurrentHashMap.newKeySet();
		public boolean hasHealOrResurrect = false;
		public boolean hasLongRangeSkills = false;
		public boolean hasLongRangeDamageSkills = false;
		public int maxCastRange = 0;
		
		public SelfAnalysis()
		{
		}
		
		public void init()
		{
			switch (((NpcTemplate) _actor.getTemplate()).getAIType())
			{
				case FIGHTER:
				{
					isFighter = true;
					break;
				}
				case MAGE:
				{
					isMage = true;
					break;
				}
				case CORPSE:
				case BALANCED:
				{
					isBalanced = true;
					break;
				}
				case ARCHER:
				{
					isArcher = true;
					break;
				}
				case HEALER:
				{
					isHealer = true;
					break;
				}
				default:
				{
					isFighter = true;
					break;
				}
			}
			
			// Water movement analysis.
			if (_actor.isNpc())
			{
				switch (_actor.getId())
				{
					case 20314: // Great White Shark
					case 20849: // Light Worm
					{
						cannotMoveOnLand = true;
						break;
					}
					default:
					{
						cannotMoveOnLand = false;
						break;
					}
				}
			}
			
			// Skill analysis.
			for (Skill sk : _actor.getAllSkills())
			{
				if (sk.isPassive())
				{
					continue;
				}
				
				final int castRange = sk.getCastRange();
				boolean hasLongRangeDamageSkill = false;
				if (sk.isContinuous())
				{
					if (!sk.isDebuff())
					{
						buffSkills.add(sk);
					}
					else
					{
						debuffSkills.add(sk);
					}
					continue;
				}
				
				if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
				{
					cancelSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.HEAL))
				{
					healSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else if (sk.hasEffectType(EffectType.SLEEP))
				{
					sleepSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.BLOCK_ACTIONS))
				{
					// Hardcoding petrification until improvements are made to EffectTemplate... petrification is totally different for AI than paralyze.
					switch (sk.getId())
					{
						case 367:
						case 4111:
						case 4383:
						case 4616:
						case 4578:
						{
							sleepSkills.add(sk);
							break;
						}
						default:
						{
							generalDisablers.add(sk);
							break;
						}
					}
				}
				else if (sk.hasEffectType(EffectType.ROOT))
				{
					rootSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.BLOCK_CONTROL))
				{
					debuffSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.MUTE))
				{
					muteSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.RESURRECTION))
				{
					resurrectSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else
				{
					generalSkills.add(sk);
					hasLongRangeDamageSkill = true;
				}
				
				if (castRange > 150)
				{
					hasLongRangeSkills = true;
					if (hasLongRangeDamageSkill)
					{
						hasLongRangeDamageSkills = true;
					}
				}
				
				if (castRange > maxCastRange)
				{
					maxCastRange = castRange;
				}
			}
			
			// Because of missing skills, some mages/balanced cannot play like mages.
			if (!hasLongRangeDamageSkills && isMage)
			{
				isBalanced = true;
				isMage = false;
				isFighter = false;
			}
			
			if (!hasLongRangeSkills && (isMage || isBalanced))
			{
				isBalanced = false;
				isMage = false;
				isFighter = true;
			}
			
			if (generalSkills.isEmpty() && isMage)
			{
				isBalanced = true;
				isMage = false;
			}
		}
	}
}
