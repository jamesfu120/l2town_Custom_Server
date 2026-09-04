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
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * @author Sdw, Mobius
 */
public class FriendlyNpcAI extends AttackableAI
{
	public FriendlyNpcAI(Attackable attackable)
	{
		super(attackable);
	}
	
	@Override
	public void notifyActionAttacked(WorldObject attacker)
	{
	}
	
	@Override
	public void notifyActionAggression(WorldObject target, int aggro)
	{
	}
	
	@Override
	public synchronized void setIntentionAttack(WorldObject target)
	{
		if (target == null)
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == Intention.REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isControlBlocked())
		{
			clientActionFailed();
			return;
		}
		
		// Set the Intention of this AbstractAI to ATTACK.
		_intention = Intention.ATTACK;
		
		// Set the AI attack target.
		setTarget(target);
		
		stopFollow();
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
		
		// Launch the Think Action.
		notifyActionThink();
	}
	
	@Override
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if (npc.isCastingNow() || npc.isCoreAIDisabled())
		{
			return;
		}
		
		// Check if target is dead or if timeout is expired to stop this attack.
		final WorldObject target = getTarget();
		final Creature originalAttackTarget = target == null ? null : target.asCreature();
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead())
		{
			// Stop hating this target after the attack timeout or if target is dead.
			if (originalAttackTarget != null)
			{
				npc.stopHating(originalAttackTarget);
			}
			
			setIntentionActive();
			npc.setWalking();
			return;
		}
		
		final int collision = npc.getTemplate().getCollisionRadius();
		setTarget(originalAttackTarget);
		
		final int combinedCollision = collision + originalAttackTarget.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			World.forFirstVisibleObject(npc, Attackable.class, nearby -> npc.isInsideRadius2D(nearby, collision) && (nearby != originalAttackTarget), nearby ->
			{
				int newX = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newX += originalAttackTarget.getX();
				}
				else
				{
					newX = originalAttackTarget.getX() - newX;
				}
				
				int newY = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newY += originalAttackTarget.getY();
				}
				else
				{
					newY = originalAttackTarget.getY() - newY;
				}
				
				if (!npc.isInsideRadius2D(newX, newY, 0, collision))
				{
					final int newZ = npc.getZ() + 30;
					if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceWorld()))
					{
						moveTo(newX, newY, newZ);
					}
				}
			});
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance = npc.calculateDistance2D(originalAttackTarget);
			if (distance <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (originalAttackTarget.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (originalAttackTarget.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceWorld()))
				{
					setIntentionMoveTo(new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		final double dist = npc.calculateDistance2D(originalAttackTarget);
		final int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (originalAttackTarget.isMoving())
		{
			range += 50;
			if (npc.isMoving())
			{
				range += 50;
			}
		}
		
		if ((dist2 > range) || !GeoEngine.getInstance().canSeeTarget(npc, originalAttackTarget))
		{
			if (originalAttackTarget.isMoving())
			{
				range -= 100;
			}
			
			if (range < 5)
			{
				range = 5;
			}
			
			moveToPawn(originalAttackTarget, range);
			return;
		}
		
		_actor.doAutoAttack(originalAttackTarget);
	}
	
	@Override
	protected void thinkCast()
	{
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
}
