/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Target Cancel effect implementation.
 * @author -Nemesiss-, Adry_85
 */
public class TargetCancel extends AbstractEffect
{
	private final int _chance;
	
	public TargetCancel(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !(effected.hasAbnormalType(AbnormalType.ABNORMAL_INVINCIBILITY) || effected.hasAbnormalType(AbnormalType.INVINCIBILITY_SPECIAL) || effected.hasAbnormalType(AbnormalType.INVINCIBILITY)) && Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || effected.isRaid())
		{
			return;
		}
		
		effected.setTarget(null);
		effected.abortAttack();
		effected.abortCast();
		effected.getAI().setIntentionIdle();
	}
}
