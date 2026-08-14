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

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Sdw
 */
public class Feed extends AbstractEffect
{
	private final int _normal;
	private final int _ride;
	private final int _wyvern;
	
	public Feed(StatSet params)
	{
		_normal = params.getInt("normal", 0);
		_ride = params.getInt("ride", 0);
		_wyvern = params.getInt("wyvern", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPet())
		{
			final Pet pet = effected.asPet();
			final int feedEffect = (int) pet.getStat().getValue(Stat.FEED_MODIFY, 0);
			pet.setCurrentFed(pet.getCurrentFed() + (_normal * RatesConfig.PET_FOOD_RATE) + (feedEffect * (_normal / 100)));
		}
		else if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			if (player.getMountType() == MountType.WYVERN)
			{
				player.setCurrentFeed(player.getCurrentFeed() + _wyvern);
			}
			else
			{
				player.setCurrentFeed(player.getCurrentFeed() + _ride);
			}
		}
	}
}
