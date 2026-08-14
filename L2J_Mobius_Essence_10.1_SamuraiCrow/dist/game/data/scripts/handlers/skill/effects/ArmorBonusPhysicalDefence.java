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

import org.l2jmobius.gameserver.data.enums.ArmorPhysicalDefenceType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Norvox
 */
public class ArmorBonusPhysicalDefence extends AbstractStatEffect
{
	private final int _paperdollSlot;
	private final ArmorPhysicalDefenceType _defenceType;
	
	public ArmorBonusPhysicalDefence(StatSet params)
	{
		super(params, Stat.ARMOR_BONUS_PHYSICAL_DEFENCE);
		
		final String slotName = params.getString("slot", "");
		switch (slotName.toLowerCase())
		{
			case "chest":
			{
				_defenceType = ArmorPhysicalDefenceType.CHEST;
				_paperdollSlot = Inventory.PAPERDOLL_CHEST;
				break;
			}
			case "legs":
			{
				_defenceType = ArmorPhysicalDefenceType.LEGS;
				_paperdollSlot = Inventory.PAPERDOLL_LEGS;
				break;
			}
			case "head":
			{
				_defenceType = ArmorPhysicalDefenceType.HEAD;
				_paperdollSlot = Inventory.PAPERDOLL_HEAD;
				break;
			}
			case "feet":
			{
				_defenceType = ArmorPhysicalDefenceType.FEET;
				_paperdollSlot = Inventory.PAPERDOLL_FEET;
				break;
			}
			case "gloves":
			{
				_defenceType = ArmorPhysicalDefenceType.GLOVES;
				_paperdollSlot = Inventory.PAPERDOLL_GLOVES;
				break;
			}
			case "underwear":
			{
				_defenceType = ArmorPhysicalDefenceType.UNDER;
				_paperdollSlot = Inventory.PAPERDOLL_UNDER;
				break;
			}
			case "cloak":
			{
				_defenceType = ArmorPhysicalDefenceType.CLOAK;
				_paperdollSlot = Inventory.PAPERDOLL_CLOAK;
				break;
			}
			case "hair":
			{
				_defenceType = ArmorPhysicalDefenceType.HAIR1;
				_paperdollSlot = Inventory.PAPERDOLL_HAIR;
				break;
			}
			case "hair2":
			{
				_defenceType = ArmorPhysicalDefenceType.HAIR2;
				_paperdollSlot = Inventory.PAPERDOLL_HAIR2;
				break;
			}
			case "hairall":
			{
				_defenceType = ArmorPhysicalDefenceType.HAIR1_AND_HAIR2;
				_paperdollSlot = Inventory.PAPERDOLL_HAIR;
				break;
			}
			case "belt":
			{
				_defenceType = ArmorPhysicalDefenceType.BELT;
				_paperdollSlot = Inventory.PAPERDOLL_BELT;
				break;
			}
			case "sigel":
			{
				_defenceType = ArmorPhysicalDefenceType.SIGEL;
				_paperdollSlot = Inventory.PAPERDOLL_LHAND;
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Unknown slot ArmorBonusPhysicalDefence: " + slotName);
			}
		}
	}
	
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return effected.asPlayer().getInventory().getPaperdollItem(_paperdollSlot) != null;
	}
	
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().addPhysicalDefenceBonus(_defenceType, (int) _amount);
		super.pump(effected, skill);
	}
}
