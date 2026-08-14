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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * An effect that pulls effected target back to the effector.
 * @author Nik, Atronic
 */
public class PullBack extends AbstractEffect
{
	private final int _speed;
	private final int _delay;
	private final int _animationSpeed;
	private final FlyType _type;
	
	public PullBack(StatSet params)
	{
		_speed = params.getInt("speed", 0);
		_delay = params.getInt("delay", _speed);
		_animationSpeed = params.getInt("animationSpeed", 0);
		_type = params.getEnum("type", FlyType.class, FlyType.WARP_FORWARD); // type 9
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(100, effector, effected, skill);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Prevent pulling raids and town NPCs.
		if ((effected == null) || effected.isRaid() || (effected.isNpc() && !effected.isAttackable()))
		{
			return;
		}
		
		// Prevent pulling debuff blocked characters.
		if ((effected.isDebuffBlocked()))
		{
			return;
		}
		
		// In retail, you get debuff, but you are not even moved if there is obstacle. You are still disabled from using skills and moving though.
		if (GeoEngine.getInstance().canMoveToTarget(effected.getX(), effected.getY(), effected.getZ(), effector.getX(), effector.getY(), effector.getZ(), effected.getInstanceWorld()))
		{
			final Location dest = GeoEngine.getInstance().getValidLocation(effected.getX(), effected.getY(), effected.getZ(), effector.getX(), effector.getY(), effector.getZ() + 20, effected.getInstanceWorld());
			effected.broadcastPacket(new FlyToLocation(effected, dest, _type, _speed, _delay, _animationSpeed));
			effected.setXYZ(dest);
			effected.broadcastPacket(new ValidateLocation(effected), false);
			effected.revalidateZone(true);
		}
	}
}
