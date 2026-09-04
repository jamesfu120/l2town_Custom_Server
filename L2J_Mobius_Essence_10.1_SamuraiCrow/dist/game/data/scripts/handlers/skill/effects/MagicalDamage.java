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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Magical damage effect implementation.
 * @author Adry_85, Mobius
 */
public class MagicalDamage extends AbstractEffect
{
	private final double _power;
	private final boolean _overHit;
	private final double _abnormalModifier;
	private final double _debuffModifier;
	private final double _raceModifier;
	private final int _repeatCount;
	private final int _chanceToRepeat;
	private final Set<Race> _races = EnumSet.noneOf(Race.class);
	private final Set<AbnormalType> _abnormals;
	
	public MagicalDamage(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_overHit = params.getBoolean("overHit", false);
		_abnormalModifier = params.getDouble("abnormalModifier", 1);
		_debuffModifier = params.getDouble("debuffModifier", 1);
		_raceModifier = params.getDouble("raceModifier", 1);
		_repeatCount = params.getInt("repeatCount", 1);
		_chanceToRepeat = params.getInt("chanceToRepeat", 0);
		if (params.contains("races"))
		{
			for (String race : params.getString("races", "").split(";"))
			{
				_races.add(Race.valueOf(race));
			}
		}
		final String abnormals = params.getString("abnormalType", null);
		if ((abnormals != null) && !abnormals.isEmpty())
		{
			_abnormals = new HashSet<>();
			for (String slot : abnormals.split(";"))
			{
				_abnormals.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_abnormals = Collections.<AbnormalType> emptySet();
		}
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
		
		if (params.contains("mode"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should not have mode.");
		}
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		if (_overHit && effected.isAttackable())
		{
			effected.asAttackable().overhitEnabled(true);
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		
		for (int i = 0; i < _repeatCount; i++)
		{
			if ((i > 0) && (_chanceToRepeat > 0) && (Rnd.get(100) >= _chanceToRepeat))
			{
				break;
			}
			
			double damage = Formulas.calcMagicDam(effector, effected, skill, effector.getMAtk(), _power, effected.getMDef(), sps, bss, mcrit);
			
			boolean hasAbnormalType = false;
			if (!_abnormals.isEmpty())
			{
				for (AbnormalType abnormal : _abnormals)
				{
					if (effected.hasAbnormalType(abnormal))
					{
						hasAbnormalType = true;
						break;
					}
				}
			}
			
			// Apply abnormal modifier.
			if (hasAbnormalType)
			{
				damage *= _abnormalModifier;
			}
			
			// Apply debuff modifier.
			if (effected.getEffectList().getDebuffCount() > 0)
			{
				damage *= _debuffModifier;
			}
			
			// Apply race modifier.
			if (_races.contains(effected.getRace()))
			{
				damage *= _raceModifier;
			}
			
			effector.doAttack(damage, effected, skill, false, false, mcrit, false);
		}
	}
}
