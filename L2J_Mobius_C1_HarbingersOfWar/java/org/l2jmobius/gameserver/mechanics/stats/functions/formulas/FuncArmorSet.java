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
package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

/**
 * @author UnAfraid, Mobius
 */
public class FuncArmorSet extends AbstractFunction
{
	private static final Map<Stat, FuncArmorSet> _fh_instance = new EnumMap<>(Stat.class);
	
	public static AbstractFunction getInstance(Stat st)
	{
		_fh_instance.computeIfAbsent(st, k -> new FuncArmorSet(st));
		
		return _fh_instance.get(st);
	}
	
	private FuncArmorSet(Stat stat)
	{
		super(stat, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		
		// Should not apply armor set bonus to summons.
		if (effector.isPlayer())
		{
			final Player player = effector.asPlayer();
			final Item chest = player.getChestArmorInstance();
			if (chest != null)
			{
				final ArmorSet set = ArmorSetData.getInstance().getSet(chest.getId());
				if ((set != null) && set.containAll(player))
				{
					final Stat stat = getStat();
					
					// Legacy base-stat support: <str>, <con>, <dex>, <int>, <men>, <wit>.
					switch (stat)
					{
						case STAT_STR:
						{
							value += set.getSTR();
							break;
						}
						case STAT_DEX:
						{
							value += set.getDEX();
							break;
						}
						case STAT_INT:
						{
							value += set.getINT();
							break;
						}
						case STAT_MEN:
						{
							value += set.getMEN();
							break;
						}
						case STAT_CON:
						{
							value += set.getCON();
							break;
						}
						case STAT_WIT:
						{
							value += set.getWIT();
							break;
						}
					}
					
					// Retail-style numeric bonuses: apply add first, then mul.
					final Double add = set.getStatAdd(stat);
					if (add != null)
					{
						value += add;
					}
					
					final Double mul = set.getStatMul(stat);
					if (mul != null)
					{
						value *= mul;
					}
					
					// Shield-only bonuses, applied only when the set's shield is equipped.
					if (set.containShield(player))
					{
						final Double addShield = set.getStatAddShield(stat);
						if (addShield != null)
						{
							value += addShield;
						}
						
						final Double mulShield = set.getStatMulShield(stat);
						if (mulShield != null)
						{
							value *= mulShield;
						}
					}
				}
			}
		}
		
		return value;
	}
}
