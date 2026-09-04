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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

public class PlayerAI extends PlayableAI
{
	private boolean _thinking; // To prevent recursive thinking.
	
	/** The Intention saved when CAST interrupts another intention (to be replayed when the cast finishes). */
	private Intention _savedIntention = null;
	/** Saved ATTACK / FOLLOW target. */
	private Creature _savedAttackTarget = null;
	/** Saved CAST / PICK_UP / INTERACT target. */
	private WorldObject _savedWorldTarget = null;
	/** Saved MOVE_TO destination. */
	private ILocational _savedLocation = null;
	/** Saved CAST parameters. */
	private Skill _savedSkill = null;
	private Item _savedItem = null;
	private boolean _savedForceUse = false;
	private boolean _savedDontMove = false;
	
	public PlayerAI(Player player)
	{
		super(player);
	}
	
	@Override
	public Intention getNextIntention()
	{
		return _savedIntention;
	}
	
	private void clearSavedIntention()
	{
		_savedIntention = null;
		_savedAttackTarget = null;
		_savedWorldTarget = null;
		_savedLocation = null;
		_savedSkill = null;
		_savedItem = null;
		_savedForceUse = false;
		_savedDontMove = false;
	}
	
	/**
	 * Saves the current Intention (and its typed parameters) so it can be replayed after the active CAST finishes.<br>
	 * Mirrors the prior {@code changeIntention} override that bundled the args into an {@code IntentionCommand}.
	 * @param skill The skill of the incoming CAST intention (used to compare against the prior CAST)
	 * @param target The world target of the incoming CAST intention
	 */
	private synchronized void saveCurrentIntentionForCast(Skill skill, WorldObject target)
	{
		// Do nothing unless CAST intention.
		// However, forget interrupted actions when starting to use an offensive skill.
		if (skill.hasNegativeEffect())
		{
			clearSavedIntention();
			return;
		}
		
		// Do nothing if next intention is same as current one (same skill and same target).
		if ((_intention == Intention.CAST) && (_savedSkill == skill) && (_savedWorldTarget == target))
		{
			return;
		}
		
		// Save current intention so it can be used after cast.
		final Intention current = _intention;
		_savedIntention = current;
		switch (current)
		{
			case ATTACK:
			case FOLLOW:
			{
				final WorldObject currentTarget = getTarget();
				_savedAttackTarget = currentTarget == null ? null : currentTarget.asCreature();
				_savedWorldTarget = null;
				_savedLocation = null;
				break;
			}
			case PICK_UP:
			case INTERACT:
			{
				_savedAttackTarget = null;
				_savedWorldTarget = getTarget();
				_savedLocation = null;
				break;
			}
			case MOVE_TO:
			{
				_savedAttackTarget = null;
				_savedWorldTarget = null;
				
				// Location is not retained on the AI; without it MOVE_TO cannot be replayed.
				_savedLocation = null;
				break;
			}
			case CAST:
			{
				_savedAttackTarget = null;
				_savedWorldTarget = getCastTarget();
				_savedLocation = null;
				_savedSkill = _skill;
				_savedItem = _item;
				_savedForceUse = _forceUse;
				_savedDontMove = _dontMove;
				break;
			}
			default:
			{
				_savedAttackTarget = null;
				_savedWorldTarget = null;
				_savedLocation = null;
				break;
			}
		}
	}
	
	/**
	 * Replay the saved intention by calling the typed onIntention method directly.
	 */
	private void replaySavedIntention()
	{
		final Intention saved = _savedIntention;
		if (saved == null)
		{
			return;
		}
		
		final Creature attackTarget = _savedAttackTarget;
		final WorldObject worldTarget = _savedWorldTarget;
		final ILocational location = _savedLocation;
		final Skill skill = _savedSkill;
		final Item item = _savedItem;
		final boolean forceUse = _savedForceUse;
		final boolean dontMove = _savedDontMove;
		
		clearSavedIntention();
		
		switch (saved)
		{
			case IDLE:
			{
				setIntentionIdle();
				break;
			}
			case ACTIVE:
			{
				setIntentionActive();
				break;
			}
			case REST:
			{
				setIntentionRest();
				break;
			}
			case ATTACK:
			{
				if (attackTarget != null)
				{
					setIntentionAttack(attackTarget);
				}
				break;
			}
			case CAST:
			{
				if ((skill != null) && (worldTarget != null))
				{
					setIntentionCast(skill, worldTarget, item, forceUse, dontMove);
				}
				break;
			}
			case MOVE_TO:
			{
				if (location != null)
				{
					setIntentionMoveTo(location);
				}
				break;
			}
			case FOLLOW:
			{
				if (attackTarget != null)
				{
					setIntentionFollow(attackTarget);
				}
				break;
			}
			case PICK_UP:
			{
				if (worldTarget != null)
				{
					setIntentionPickUp(worldTarget);
				}
				break;
			}
			case INTERACT:
			{
				if (worldTarget != null)
				{
					setIntentionInteract(worldTarget);
				}
				break;
			}
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
		// Launch saved intention if any.
		if (_savedIntention != null)
		{
			replaySavedIntention();
		}
		
		super.notifyActionReadyToAct();
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
		clearSavedIntention();
		super.notifyActionCancel();
	}
	
	/**
	 * Finalize the casting of a skill. This method overrides CreatureAI method.<br>
	 * <b>What it does:</b><br>
	 * Check if actual intention is set to CAST and, if so, retrieves latest intention before the actual CAST and set it as the current intention for the player.
	 */
	@Override
	public void notifyActionFinishCasting()
	{
		if (getIntention() == Intention.CAST)
		{
			// Run interrupted or next intention.
			if (_savedIntention != null)
			{
				if (_savedIntention != Intention.CAST) // Previous state shouldn't be casting.
				{
					replaySavedIntention();
				}
				else
				{
					clearSavedIntention();
					setIntentionIdle();
				}
			}
			else
			{
				// Set intention to idle if skill doesn't change intention.
				setIntentionIdle();
			}
		}
	}
	
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		super.notifyActionAttacked(attacker);
		
		// Summons in defending mode defend its master when attacked.
		final Player player = _actor.asPlayer();
		if (player.hasServitors())
		{
			for (Summon summon : player.getServitors().values())
			{
				final SummonAI ai = (SummonAI) summon.getAI();
				if (ai.isDefending())
				{
					ai.defendAttack(attacker.asCreature());
				}
			}
		}
	}
	
	@Override
	public void notifyActionEvaded(WorldObject attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		super.notifyActionEvaded(attacker);
		
		// Summons in defending mode defend its master when attacked.
		final Player player = _actor.asPlayer();
		if (player.hasServitors())
		{
			for (Summon summon : player.getServitors().values())
			{
				final SummonAI ai = (SummonAI) summon.getAI();
				if (ai.isDefending())
				{
					ai.defendAttack(attacker.asCreature());
				}
			}
		}
	}
	
	@Override
	public void setIntentionRest()
	{
		if (getIntention() != Intention.REST)
		{
			_intention = Intention.REST;
			setTarget(null);
			clientStopMoving(null);
		}
	}
	
	@Override
	public void setIntentionActive()
	{
		setIntentionIdle();
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
		// Stop the follow mode (MOVE_TO is neither FOLLOW nor ATTACK).
		stopFollow();
		
 		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			clientActionFailed();
			
			// Save MOVE_TO so it can be replayed after the current action ends.
			_savedIntention = Intention.MOVE_TO;
			_savedAttackTarget = null;
			_savedWorldTarget = null;
			_savedLocation = loc;
			_savedSkill = null;
			_savedItem = null;
			_savedForceUse = false;
			_savedDontMove = false;
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
	
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Save the prior intention so it can be replayed after the cast finishes.
		saveCurrentIntentionForCast(skill, target);
		
		super.setIntentionCast(skill, target, item, forceUse, dontMove);
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		super.clientNotifyDead();
	}
	
	private void thinkAttack()
	{
		final Player player = _actor.asPlayer();
		final SkillUseHolder queuedSkill = player.getQueuedSkill();
		if (queuedSkill != null)
		{
			// Remove the skill from queue.
			player.setQueuedSkill(null, null, false, false);
			
			// Check if player has the needed MP for the queued skill.
			if (player.getCurrentMp() >= player.getStat().getMpInitialConsume(queuedSkill.getSkill()))
			{
				// Abort attack.
				player.abortAttack();
				
				// Recharge shots.
				if (!player.isChargedShot(ShotType.SOULSHOTS) && !player.isChargedShot(ShotType.BLESSED_SOULSHOTS))
				{
					player.rechargeShots(true, false, false);
				}
				
				// Use queued skill.
				player.useMagic(queuedSkill.getSkill(), queuedSkill.getItem(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
				return;
			}
		}
		
		final WorldObject target = getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		if (checkTargetLostOrDead(target.asCreature()))
		{
			// Notify the target
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		_actor.doAutoAttack(target.asCreature());
	}
	
	private void thinkCast()
	{
		final WorldObject target = getCastTarget();
		if ((_skill.getTargetType() == TargetType.GROUND) && _actor.isPlayer())
		{
			if (maybeMoveToPosition(_actor.asPlayer().getCurrentSkillWorldPosition(), _actor.getMagicalAttackRange(_skill)))
			{
				return;
			}
		}
		else
		{
			if (checkTargetLost(target))
			{
				if (_skill.hasNegativeEffect() && (target != null))
				{
					// Notify the target
					setCastTarget(null);
					setTarget(null);
				}
				return;
			}
			
			if ((target != null) && maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			{
				return;
			}
		}
		
		// Check if target has changed.
		final WorldObject currentTarget = _actor.getTarget();
		if ((currentTarget != target) && (currentTarget != null) && (target != null))
		{
			_actor.setTarget(target);
			_actor.doCast(_skill, _item, _forceUse, _dontMove);
			_actor.setTarget(currentTarget);
			return;
		}
		
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntentionIdle();
		getActor().doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		if (!(target instanceof StaticObject))
		{
			getActor().doInteract(target.asCreature());
		}
		
		setIntentionIdle();
	}
	
	@Override
	public void notifyActionThink()
	{
		if (_thinking && (getIntention() != Intention.CAST))
		{
			return;
		}
		
		_thinking = true;
		try
		{
			if (getIntention() == Intention.ATTACK)
			{
				thinkAttack();
			}
			else if (getIntention() == Intention.CAST)
			{
				thinkCast();
			}
			else if (getIntention() == Intention.PICK_UP)
			{
				thinkPickUp();
			}
			else if (getIntention() == Intention.INTERACT)
			{
				thinkInteract();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	public Player getActor()
	{
		return super.getActor().asPlayer();
	}
}
