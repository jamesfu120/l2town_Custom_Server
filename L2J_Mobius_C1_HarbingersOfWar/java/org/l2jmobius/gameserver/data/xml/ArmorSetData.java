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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * Loads armor set bonuses.
 * @author godson, Luno, UnAfraid, Mobius
 */
public class ArmorSetData implements IXmlReader
{
	private ArmorSet[] _armorSets;
	private final Map<Integer, ArmorSet> _armorSetMap = new ConcurrentHashMap<>();
	
	/**
	 * Instantiates a new armor sets data.
	 */
	protected ArmorSetData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackDirectory("data/stats/armorsets", false);
		
		_armorSets = new ArmorSet[Collections.max(_armorSetMap.keySet()) + 1];
		for (Entry<Integer, ArmorSet> armorSet : _armorSetMap.entrySet())
		{
			_armorSets[armorSet.getKey()] = armorSet.getValue();
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _armorSetMap.size() + " armor sets.");
		_armorSetMap.clear();
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("set".equalsIgnoreCase(d.getNodeName()))
					{
						final ArmorSet set = new ArmorSet();
						for (Node a = d.getFirstChild(); a != null; a = a.getNextSibling())
						{
							final NamedNodeMap attrs = a.getAttributes();
							switch (a.getNodeName())
							{
								case "chest":
								{
									set.addChest(parseInteger(attrs, "id"));
									break;
								}
								case "feet":
								{
									set.addFeet(parseInteger(attrs, "id"));
									break;
								}
								case "gloves":
								{
									set.addGloves(parseInteger(attrs, "id"));
									break;
								}
								case "head":
								{
									set.addHead(parseInteger(attrs, "id"));
									break;
								}
								case "legs":
								{
									set.addLegs(parseInteger(attrs, "id"));
									break;
								}
								case "shield":
								{
									set.addShield(parseInteger(attrs, "id"));
									break;
								}
								case "skill":
								{
									final int skillId = parseInteger(attrs, "id");
									final int skillLevel = parseInteger(attrs, "level");
									set.addSkill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "shield_skill":
								{
									final int skillId = parseInteger(attrs, "id");
									final int skillLevel = parseInteger(attrs, "level");
									set.addShieldSkill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "enchant6skill":
								{
									final int skillId = parseInteger(attrs, "id");
									final int skillLevel = parseInteger(attrs, "level");
									set.addEnchant6Skill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "con":
								{
									set.addCon(parseInteger(attrs, "val"));
									break;
								}
								case "dex":
								{
									set.addDex(parseInteger(attrs, "val"));
									break;
								}
								case "str":
								{
									set.addStr(parseInteger(attrs, "val"));
									break;
								}
								case "men":
								{
									set.addMen(parseInteger(attrs, "val"));
									break;
								}
								case "wit":
								{
									set.addWit(parseInteger(attrs, "val"));
									break;
								}
								case "int":
								{
									set.addInt(parseInteger(attrs, "val"));
									break;
								}
								case "add":
								case "mul":
								{
									final Stat stat = Stat.valueOfXml(parseString(attrs, "stat"));
									final double val = parseDouble(attrs, "val");
									final boolean shield = parseBoolean(attrs, "shield", Boolean.FALSE);
									if ("add".equals(a.getNodeName()))
									{
										set.addStatAdd(stat, val, shield);
									}
									else
									{
										set.addStatMul(stat, val, shield);
									}
									break;
								}
							}
						}
						
						_armorSetMap.put(set.getChestId(), set);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if is armor set.
	 * @param chestId the chest Id to verify.
	 * @return {@code true} if the chest Id belongs to a registered armor set, {@code false} otherwise.
	 */
	public boolean isArmorSet(int chestId)
	{
		return (_armorSets.length > chestId) && (_armorSets[chestId] != null);
	}
	
	/**
	 * Gets the sets the.
	 * @param chestId the chest Id identifying the armor set.
	 * @return the armor set associated to the give chest Id.
	 */
	public ArmorSet getSet(int chestId)
	{
		if (_armorSets.length > chestId)
		{
			return _armorSets[chestId];
		}
		
		return null;
	}
	
	/**
	 * Gets the single instance of ArmorSetsData.
	 * @return single instance of ArmorSetsData
	 */
	public static ArmorSetData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ArmorSetData INSTANCE = new ArmorSetData();
	}
}
