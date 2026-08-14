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

import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;

public class SummonAI extends PlayableAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;
	
	private volatile boolean _thinking; // To prevent recursive thinking.
	private volatile boolean _startFollow = _actor.asSummon().getFollowStatus();
	private Creature _lastAttack = null;
	
	private volatile boolean _startAvoid;
	private volatile boolean _isDefending;
	private Future<?> _avoidTask = null;
	
	// Fix: Infinite Atk. Spd. exploit.
	private Creature _savedAttackTarget = null;
	
	@Override
	public Intention getNextIntention()
	{
		return _savedAttackTarget != null ? Intention.ATTACK : null;
	}
	
	public SummonAI(Summon summon)
	{
		super(summon);
	}
	
	@Override
	public void setIntentionIdle()
	{
		stopAvoidTask();
		stopFollow();
		_startFollow = false;
		setIntentionActive();
	}
	
	@Override
	public synchronized void setIntentionActive()
	{
		startAvoidTask();
		final Summon summon = _actor.asSummon();
		if (_startFollow)
		{
			setIntentionFollow(summon.getOwner());
		}
		else
		{
			super.setIntentionActive();
		}
	}
	
	@Override
	public synchronized void setIntentionFollow(WorldObject target)
	{
		startAvoidTask();
		super.setIntentionFollow(target);
	}
	
	@Override
	public synchronized void setIntentionAttack(WorldObject target)
	{
		stopAvoidTask();
		super.setIntentionAttack(target);
	}
	
	@Override
	public synchronized void setIntentionMoveTo(ILocational destination)
	{
		stopAvoidTask();
		super.setIntentionMoveTo(destination);
	}
	
	@Override
	public synchronized void setIntentionRest()
	{
		stopAvoidTask();
		super.setIntentionRest();
	}
	
	@Override
	public synchronized void setIntentionPickUp(WorldObject item)
	{
		stopAvoidTask();
		super.setIntentionPickUp(item);
	}
	
	@Override
	public synchronized void setIntentionInteract(WorldObject object)
	{
		stopAvoidTask();
		super.setIntentionInteract(object);
	}
	
	private void thinkAttack()
	{
		final WorldObject target = getTarget();
		final Creature attackTarget = target == null ? null : target.asCreature();
		if (checkTargetLostOrDead(attackTarget))
		{
			setTarget(null);
			if (_startFollow)
			{
				_actor.asSummon().setFollowStatus(true);
			}
			return;
		}
		
		if (maybeMoveToPawn(attackTarget, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		
		// Fix: Infinite Atk. Spd. exploit.
		if (_actor.isAttackingNow())
		{
			_savedAttackTarget = attackTarget;
			return;
		}
		
		_actor.doAutoAttack(attackTarget);
	}
	
	private void thinkCast()
	{
		final Summon summon = _actor.asSummon();
		if (summon.isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setTarget(null);
			setCastTarget(null);
			summon.setFollowStatus(true);
			return;
		}
		
		final boolean val = _startFollow;
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		summon.setFollowStatus(false);
		setIntentionIdle();
		_startFollow = val;
		_actor.doCast(_skill, _item, _skill.hasNegativeEffect(), _dontMove);
	}
	
	private void thinkPickUp()
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
		getActor().doPickupItem(target);
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
				case PICK_UP:
				{
					thinkPickUp();
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
			_actor.asSummon().setFollowStatus(_startFollow);
		}
		else
		{
			setIntentionAttack(_lastAttack);
			_lastAttack = null;
		}
	}
	
	@Override
	public void notifyActionAttacked(WorldObject attackerObj)
	{
		if (attackerObj == null)
		{
			return;
		}
		
		final Creature attacker = attackerObj.asCreature();
		if (attacker == null)
		{
			return;
		}
		
		super.notifyActionAttacked(attackerObj);
		
		if (_isDefending)
		{
			allServitorsDefend(attacker);
		}
		else
		{
			avoidAttack(attacker);
		}
	}
	
	@Override
	public void notifyActionEvaded(WorldObject attackerObj)
	{
		if (attackerObj == null)
		{
			return;
		}
		
		final Creature attacker = attackerObj.asCreature();
		if (attacker == null)
		{
			return;
		}
		
		super.notifyActionEvaded(attackerObj);
		
		if (_isDefending)
		{
			allServitorsDefend(attacker);
		}
		else
		{
			avoidAttack(attacker);
		}
	}
	
	private void allServitorsDefend(Creature attacker)
	{
		final Creature owner = getActor().getOwner();
		if ((owner != null) && owner.asPlayer().hasServitors())
		{
			for (Summon summon : owner.asPlayer().getServitors().values())
			{
				final SummonAI ai = (SummonAI) summon.getAI();
				if (ai.isDefending())
				{
					ai.defendAttack(attacker);
				}
			}
		}
		else
		{
			defendAttack(attacker);
		}
	}
	
	private void avoidAttack(Creature attacker)
	{
		// Don't move while casting. It breaks casting animation, but still casts the skill... looks so bugged.
		if (_actor.isCastingNow())
		{
			return;
		}
		
		final Creature owner = getActor().getOwner();
		
		// Trying to avoid if summon near owner.
		if ((owner != null) && (owner != attacker) && owner.isInsideRadius3D(_actor, 2 * AVOID_RADIUS))
		{
			_startAvoid = true;
		}
	}
	
	public void defendAttack(Creature attacker)
	{
		// Cannot defend while attacking or casting.
		if (_actor.isAttackingNow() || _actor.isCastingNow())
		{
			return;
		}
		
		final Summon summon = getActor();
		final Player owner = summon.getOwner();
		if (owner != null)
		{
			if (summon.calculateDistance3D(owner) > 3000)
			{
				summon.getAI().setIntentionFollow(owner);
			}
			else if ((owner != attacker) && !summon.isMoving() && summon.canAttack(attacker, false))
			{
				summon.doAttack(attacker);
			}
		}
	}
	
	@Override
	public void run()
	{
		if (_startAvoid)
		{
			_startAvoid = false;
			if (!_actor.isMoving() && !_actor.isDead() && !_actor.isMovementDisabled() && (_actor.getMoveSpeed() > 0))
			{
				final int ownerX = _actor.asSummon().getOwner().getX();
				final int ownerY = _actor.asSummon().getOwner().getY();
				final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - _actor.getY(), ownerX - _actor.getX());
				final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
				final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
				if (GeoEngine.getInstance().canMoveToTarget(_actor.getX(), _actor.getY(), _actor.getZ(), targetX, targetY, _actor.getZ(), _actor.getInstanceWorld()))
				{
					moveTo(targetX, targetY, _actor.getZ());
				}
			}
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
				_actor.asSummon().setFollowStatus(_startFollow);
			}
		}
	}
	
	public void setStartFollowController(boolean value)
	{
		_startFollow = value;
	}
	
	@Override
	public synchronized void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		stopAvoidTask();
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
	
	private void startAvoidTask()
	{
		if (_avoidTask == null)
		{
			_avoidTask = ThreadPool.scheduleAtFixedRate(this, 100, 100);
		}
	}
	
	private void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}
	
	@Override
	public void stopAITask()
	{
		stopAvoidTask();
		super.stopAITask();
	}
	
	@Override
	public Summon getActor()
	{
		return super.getActor().asSummon();
	}
	
	/**
	 * @return if the summon is defending itself or master.
	 */
	public boolean isDefending()
	{
		return _isDefending;
	}
	
	/**
	 * @param isDefending set the summon to defend itself and master, or be passive and avoid while being attacked.
	 */
	public void setDefending(boolean isDefending)
	{
		_isDefending = isDefending;
	}
}
