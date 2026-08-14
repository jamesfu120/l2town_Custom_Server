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
package handlers.items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeFail;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeUiOpen;

import ai.others.ClassChange.ClassChangeManager;

/**
 * @author Galagard
 */
public class ClassChangeCoupon implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}
		
		final Player player = playable.asPlayer();
		final ClassChangeManager classChange = ClassChangeManager.getInstance();
		if (!classChange.canCheckAction(player))
		{
			player.sendPacket(ExClassChangeFail.STATIC_PACKET);
			return false;
		}
		
		final Map<Integer, List<Skill>> highGradeSkillHistory = new LinkedHashMap<>();
		final List<Integer> prevClassList = new ArrayList<>();
		
		// Loads history of previous trades.
		for (String varName : player.getVariables().getSet().keySet())
		{
			if (varName.startsWith(PlayerVariables.CLASS_CHANGE_HISTORY))
			{
				try
				{
					final int pastClassId = Integer.parseInt(varName.substring(PlayerVariables.CLASS_CHANGE_HISTORY.length()));
					prevClassList.add(pastClassId);
					
					final List<Skill> skillList = new ArrayList<>();
					final String data = player.getVariables().getString(varName);
					if ((data != null) && !data.isEmpty())
					{
						for (String s : data.split("\\|"))
						{
							final String[] p = s.split(",");
							if (p.length >= 3)
							{
								final Skill sk = SkillData.getInstance().getSkill(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
								if (sk != null)
								{
									skillList.add(sk);
								}
							}
						}
					}
					
					highGradeSkillHistory.put(pastClassId, skillList);
				}
				catch (Exception e)
				{
				}
			}
		}
		
		final int currentClassId = player.getActiveClass();
		if (!prevClassList.contains(currentClassId))
		{
			prevClassList.add(currentClassId);
		}
		
		// Assembles skills for the current class.
		final List<Skill> currentHighGrade = classChange.getCurrentHighGradeSkills(player);
		
		highGradeSkillHistory.put(currentClassId, currentHighGrade);
		
		// changeClassType:
		// 1 = extraction of the current class (origin has skills)
		// 2 = extraction of history (some previous class has saved skills)
		int changeClassType = 1;
		for (Entry<Integer, List<Skill>> entry : highGradeSkillHistory.entrySet())
		{
			if (entry.getKey() == currentClassId)
			{
				continue;
			}
			if (!entry.getValue().isEmpty())
			{
				changeClassType = 2;
				break;
			}
		}
		
		// extract Type: extraction cost tier
		// 0 = sem skills, 1 = heroic, 2 = legendary
		int extractType = 0;
		
		for (Entry<Integer, List<Skill>> entry : highGradeSkillHistory.entrySet())
		{
			if (entry.getKey() == currentClassId)
			{
				continue;
			}
			for (Skill skill : entry.getValue())
			{
				if (skill.getLevel() >= 2)
				{
					extractType = 2;
					break;
				}
				extractType = Math.max(extractType, 1);
			}
			if (extractType == 2)
			{
				break;
			}
		}
		
		if ((extractType == 0) && !currentHighGrade.isEmpty())
		{
			for (Skill skill : currentHighGrade)
			{
				if (skill.getLevel() >= 2)
				{
					extractType = 2;
					break;
				}
				extractType = Math.max(extractType, 1);
			}
		}
		
		player.getVariables().set(PlayerVariables.CLASS_CHANGE_COUPON_ACTIVE, true);
		player.getVariables().set(PlayerVariables.CLASS_CHANGE_COUPON_ITEM_ID, item.getId());
		player.getVariables().storeMe();
		
		player.sendPacket(new ExClassChangeUiOpen(prevClassList, changeClassType, highGradeSkillHistory, extractType));
		return true;
	}
}