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
package org.l2jmobius.gameserver.data.holders;

import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * @author Brado, Galagard
 */
public final class CharacterStyleDataHolder
{
	public final int _styleId;
	public final String _name;
	public final WeaponType _weaponType;
	public final ArmorType _armorType;
	public final int _shiftWeaponId;
	private final SkillHolder _skillHolder;
	private final int _raceId;
	private final int _sexId;
	private final int _classId;
	public final List<ItemHolder> _activateCost;
	public final List<ItemHolder> _deactivateCost;
	public final List<ActivatableItem> _activatableItems;
	
	public CharacterStyleDataHolder(int styleId, String name, List<ItemHolder> activateCost, List<ItemHolder> deactivateCost)
	{
		_styleId = styleId;
		_name = name;
		_armorType = ArmorType.NONE;
		_shiftWeaponId = 0;
		_skillHolder = null;
		_weaponType = WeaponType.NONE;
		_sexId = 0;
		_raceId = 0;
		_classId = 0;
		_activateCost = List.copyOf(activateCost);
		_deactivateCost = List.copyOf(deactivateCost);
		_activatableItems = Collections.emptyList();
	}
	
	public CharacterStyleDataHolder(int styleId, String name, int shiftWeaponId, WeaponType weaponType, List<ItemHolder> activateCost, List<ItemHolder> deactivateCost)
	{
		_styleId = styleId;
		_name = name;
		_armorType = ArmorType.NONE;
		_shiftWeaponId = shiftWeaponId;
		_skillHolder = null;
		_weaponType = weaponType;
		_sexId = 0;
		_raceId = 0;
		_classId = 0;
		_activateCost = List.copyOf(activateCost);
		_deactivateCost = List.copyOf(deactivateCost);
		_activatableItems = Collections.emptyList();
	}
	
	public CharacterStyleDataHolder(int styleId, String name, SkillHolder skillHolder, List<ItemHolder> activateCost, List<ItemHolder> deactivateCost)
	{
		_styleId = styleId;
		_name = name;
		_shiftWeaponId = 0;
		_weaponType = WeaponType.NONE;
		_armorType = ArmorType.NONE;
		_skillHolder = skillHolder;
		_sexId = 0;
		_raceId = 0;
		_classId = 0;
		_activateCost = List.copyOf(activateCost);
		_deactivateCost = List.copyOf(deactivateCost);
		_activatableItems = Collections.emptyList();
	}
	
	public CharacterStyleDataHolder(int styleId, String name, int shiftWeaponId, List<ActivatableItem> activatableItems, List<ItemHolder> activateCost, List<ItemHolder> deactivateCost)
	{
		_styleId = styleId;
		_name = name;
		_shiftWeaponId = shiftWeaponId;
		_skillHolder = null;
		_weaponType = WeaponType.NONE;
		_armorType = ArmorType.NONE;
		_raceId = 0;
		_sexId = 0;
		_classId = 0;
		_activateCost = List.copyOf(activateCost);
		_deactivateCost = List.copyOf(deactivateCost);
		_activatableItems = List.copyOf(activatableItems);
	}
	
	public WeaponType getWeaponType()
	{
		return _weaponType;
	}
	
	public ArmorType getArmorType()
	{
		return _armorType;
	}
	
	public SkillHolder getSkillHolder()
	{
		return _skillHolder;
	}
	
	public int getShiftWeaponId()
	{
		return _shiftWeaponId;
	}
	
	public int getStyleId()
	{
		return _styleId;
	}
	
	public int raceId()
	{
		return _raceId;
	}
	
	public int sexId()
	{
		return _sexId;
	}
	
	public int classId()
	{
		return _classId;
	}
	
	public List<ItemHolder> getActivateCosts()
	{
		return _activateCost;
	}
	
	public List<ItemHolder> getDeactivateCosts()
	{
		return _deactivateCost;
	}
	
	public boolean canUse(Player player)
	{
		if (_activatableItems.isEmpty())
		{
			return true;
		}
		
		final int playerRaceId = player.getRace().ordinal();
		final int playerSexId = player.getAppearance().isFemale() ? 1 : 0;
		final int playerClassId = player.getPlayerClass().getId();
		
		for (ActivatableItem item : _activatableItems)
		{
			if ((item.raceId == playerRaceId) && (item.sexId == playerSexId) && (item.classId == playerClassId))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public static class ActivatableItem
	{
		public final int raceId;
		public final int sexId;
		public final int classId;
		
		public ActivatableItem(int raceId, int sexId, int classId)
		{
			this.raceId = raceId;
			this.sexId = sexId;
			this.classId = classId;
		}
	}
	
	@Override
	public String toString()
	{
		return "Style{id=" + _styleId + ", name='" + _name + '\'' + ", shiftWeaponId=" + _shiftWeaponId + ", activateCost=" + _activateCost + ", deactivateCost=" + _deactivateCost + ", activatableItems=" + _activatableItems + '}';
	}
}
