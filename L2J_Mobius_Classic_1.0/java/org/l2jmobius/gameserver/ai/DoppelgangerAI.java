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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Doppelganger;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class DoppelgangerAI extends CreatureAI
{
	private volatile boolean _thinking; // To prevent recursive thinking.
	private volatile boolean _startFollow;
	private Creature _lastAttack = null;
	
	public DoppelgangerAI(Doppelganger clone)
	{
		super(clone);
	}
	
	@Override
	public void setIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		setIntentionActive();
	}
	
	@Override
	public void setIntentionActive()
	{
		if (_startFollow)
		{
			setIntentionFollow(getActor().getSummoner());
		}
		else
		{
			super.setIntentionActive();
		}
	}
	
	private void thinkAttack()
	{
		final WorldObject target = getTarget();
		final Creature attackTarget = target == null ? null : target.asCreature();
		if (checkTargetLostOrDead(attackTarget))
		{
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		_actor.doAutoAttack(attackTarget);
	}
	
	private void thinkCast()
	{
		if (_actor.isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			setTarget(null);
			return;
		}
		
		final boolean val = _startFollow;
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		getActor().followSummoner(false);
		setIntentionIdle();
		_startFollow = val;
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	private void thinkInteract()
	{
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
	}
	
	@Override
	public void notifyActionThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
				case INTERACT:
				{
					thinkInteract();
					break;
				}
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	public void notifyActionFinishCasting()
	{
		if (_lastAttack == null)
		{
			getActor().followSummoner(_startFollow);
		}
		else
		{
			setIntentionAttack(_lastAttack);
			_lastAttack = null;
		}
	}
	
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case ACTIVE:
			case FOLLOW:
			case IDLE:
			case MOVE_TO:
			case PICK_UP:
			{
				getActor().followSummoner(_startFollow);
			}
		}
	}
	
	public void setStartFollowController(boolean value)
	{
		_startFollow = value;
	}
	
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		if (getIntention() == Intention.ATTACK)
		{
			_lastAttack = (getTarget() != null) && getTarget().isCreature() ? getTarget().asCreature() : null;
		}
		else
		{
			_lastAttack = null;
		}
		
		super.setIntentionCast(skill, target, item, forceUse, dontMove);
	}
	
	@Override
	public void moveToPawn(WorldObject pawn, int offsetValue)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled() && (_actor.getMoveSpeed() > 0))
		{
			int offset = offsetValue;
			if (offset < 10)
			{
				offset = 10;
			}
			
			// Prevent possible extra calls to this function (there is none?), also don't send movetopawn packets too often.
			boolean sendPacket = true;
			if (_actor.isMoving() && (getTarget() == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (GameTimeTaskManager.getInstance().getGameTicks() < _moveToPawnTimeout)
					{
						return;
					}
					
					sendPacket = false;
				}
				else if (_actor.isOnGeodataPath())
				{
					// Minimum time to calculate new route is 2 seconds.
					if (GameTimeTaskManager.getInstance().getGameTicks() < (_moveToPawnTimeout + 10))
					{
						return;
					}
				}
			}
			
			// Set AI movement data.
			_clientMovingToPawnOffset = offset;
			setTarget(pawn);
			_moveToPawnTimeout = GameTimeTaskManager.getInstance().getGameTicks();
			_moveToPawnTimeout += 1000 / GameTimeTaskManager.MILLIS_IN_TICK;
			if (pawn == null)
			{
				return;
			}
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager.
			// _actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			final Location loc = new Location(pawn.getX() + Rnd.get(-offset, offset), pawn.getY() + Rnd.get(-offset, offset), pawn.getZ());
			_actor.moveToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
			if (!_actor.isMoving())
			{
				clientActionFailed();
				return;
			}
			
			// Doppelgangers always send MoveToLocation packet.
			if (sendPacket)
			{
				_actor.broadcastMoveToLocation();
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	@Override
	public Doppelganger getActor()
	{
		return (Doppelganger) super.getActor();
	}
}
