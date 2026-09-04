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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.data.holders.CharacterStyleDataHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * @author Brado, Galagard
 */
public class CharacterStylesData implements IXmlReader
{
	private final Map<CharacterStyleCategoryType, List<CharacterStyleDataHolder>> STYLES = new ConcurrentHashMap<>();
	private final Map<CharacterStyleCategoryType, ItemHolder> SWAP_COST_HOLDER = new HashMap<>();
	
	private final ConcurrentMap<Integer, Integer> _cachedWeaponMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, SkillHolder> _cachedKillEffectMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, Integer> _cachedArmorMap = new ConcurrentHashMap<>();
	
	public CharacterStylesData()
	{
		load();
		buildCacheMaps();
		
		// for (CharacterStyleDataHolder style : getStylesByCategory(CharacterStyleCategoryType.APPEARANCE_ARMOR))
		// {
		// LOGGER.info("Style " + style._styleId + " (" + style._name + ") has " + style._activatableItems.size() + " activatable items");
		// if (!style._activatableItems.isEmpty())
		// {
		// for (int i = 0; i < Math.min(3, style._activatableItems.size()); i++)
		// {
		// CharacterStyleDataHolder.ActivatableItem item = style._activatableItems.get(i);
		// LOGGER.info(" -> raceId=" + item.raceId + ", sexId=" + item.sexId + ", classId=" + item.classId);
		// }
		// }
		// }
	}
	
	@Override
	public void load()
	{
		STYLES.clear();
		SWAP_COST_HOLDER.clear();
		parseDatapackFile("data/CharacterStylesData.xml");
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + STYLES.size() + " Character Styles.");
		for (CharacterStyleCategoryType type : STYLES.keySet())
		{
			LOGGER.info(getClass().getSimpleName() + ": " + type + " -> " + STYLES.get(type).size() + " styles.");
		}
	}
	
	public void loadDatapack(String relativePath)
	{
		parseDatapackFile(relativePath);
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final Node root = document.getDocumentElement();
		if (root == null)
		{
			return;
		}
		
		forEach(root, "category", categoryNode ->
		{
			final NamedNodeMap catAttr = categoryNode.getAttributes();
			final String typeStr = parseString(catAttr, "type", null);
			if ((typeStr == null) || typeStr.isEmpty())
			{
				return;
			}
			
			final CharacterStyleCategoryType type = CharacterStyleCategoryType.from(typeStr);
			final List<CharacterStyleDataHolder> stylesList = STYLES.computeIfAbsent(type, k -> new ArrayList<>());
			
			final Integer styleCostId = parseInteger(catAttr, "swapCostId", 0);
			final Long styleCostCount = parseLong(catAttr, "swapCostCount", 0L);
			if ((styleCostId != null) && (styleCostCount != null) && (styleCostId > 0) && (styleCostCount > 0))
			{
				SWAP_COST_HOLDER.put(type, new ItemHolder(styleCostId, styleCostCount));
			}
			
			forEach(categoryNode, "style", styleNode ->
			{
				final NamedNodeMap sAttr = styleNode.getAttributes();
				final Integer styleId = parseInteger(sAttr, "styleId");
				if (styleId == null)
				{
					return;
				}
				
				final String name = parseString(sAttr, "name", "");
				final Integer shiftWeaponId = parseInteger(sAttr, "shiftWeaponId", 0);
				final WeaponType weaponType = parseEnum(sAttr, WeaponType.class, "weaponType", WeaponType.NONE);
				final Integer skillId = parseInteger(sAttr, "skillId", 0);
				final Integer skillLevel = parseInteger(sAttr, "skillLevel", 0);
				
				final List<ItemHolder> activateCost = new ArrayList<>();
				final List<ItemHolder> deactivateCost = new ArrayList<>();
				parseCost(styleNode, "activateCost", activateCost);
				parseCost(styleNode, "deactivateCost", deactivateCost);
				
				final List<CharacterStyleDataHolder.ActivatableItem> activatableItems = new ArrayList<>();
				if (type == CharacterStyleCategoryType.APPEARANCE_ARMOR)
				{
					forEach(styleNode, "activatableItems", aiNode -> forEach(aiNode, "item", itemNode ->
					{
						final NamedNodeMap aAttr = itemNode.getAttributes();
						final int rId = parseInteger(aAttr, "raceId", 0);
						final int sId = parseInteger(aAttr, "sexId", 0);
						final int cId = parseInteger(aAttr, "classId", 0);
						activatableItems.add(new CharacterStyleDataHolder.ActivatableItem(rId, sId, cId));
					}));
				}
				
				CharacterStyleDataHolder holder;
				switch (type)
				{
					case APPEARANCE_WEAPON:
					{
						holder = new CharacterStyleDataHolder(styleId, name, shiftWeaponId, weaponType, activateCost, deactivateCost);
						break;
					}
					case KILL_EFFECT:
					{
						holder = new CharacterStyleDataHolder(styleId, name, new SkillHolder(skillId, skillLevel), activateCost, deactivateCost);
						break;
					}
					case APPEARANCE_ARMOR:
					{
						holder = new CharacterStyleDataHolder(styleId, name, shiftWeaponId, activatableItems, activateCost, deactivateCost);
						break;
					}
					default:
					{
						holder = new CharacterStyleDataHolder(styleId, name, activateCost, deactivateCost);
						break;
					}
				}
				stylesList.add(holder);
			});
		});
	}
	
	private void parseCost(Node parentNode, String costTag, List<ItemHolder> targetList)
	{
		forEach(parentNode, costTag, costNode -> forEach(costNode, "item", itemNode ->
		{
			final NamedNodeMap iAttr = itemNode.getAttributes();
			final Integer itemId = parseInteger(iAttr, "id");
			final Long count = parseLong(iAttr, "count", 1L);
			if (itemId != null)
			{
				targetList.add(new ItemHolder(itemId, count));
			}
		}));
	}
	
	public void buildCacheMaps()
	{
		_cachedWeaponMap.clear();
		_cachedArmorMap.clear();
		_cachedKillEffectMap.clear();
		
		for (Entry<CharacterStyleCategoryType, List<CharacterStyleDataHolder>> entry : STYLES.entrySet())
		{
			final CharacterStyleCategoryType type = entry.getKey();
			for (CharacterStyleDataHolder holder : entry.getValue())
			{
				final int visualId = holder.getShiftWeaponId();
				if (visualId > 0)
				{
					if (type == CharacterStyleCategoryType.APPEARANCE_ARMOR) // Categoria 3
					{
						_cachedArmorMap.put(holder.getStyleId(), visualId);
					}
					else if (type == CharacterStyleCategoryType.APPEARANCE_WEAPON) // Categoria 0
					{
						_cachedWeaponMap.put(holder.getStyleId(), visualId);
					}
				}
				
				if ((type == CharacterStyleCategoryType.KILL_EFFECT) && (holder.getSkillHolder() != null))
				{
					_cachedKillEffectMap.put(holder.getStyleId(), holder.getSkillHolder());
				}
			}
		}
	}
	
	public int getWeaponVisualId(int styleId)
	{
		return _cachedWeaponMap.getOrDefault(styleId, 0);
	}
	
	public int getArmorVisualId(int styleId)
	{
		return _cachedArmorMap.getOrDefault(styleId, 0);
	}
	
	public int getWeaponStyleByStyleId(int styleId)
	{
		return _cachedWeaponMap.getOrDefault(styleId, 0);
	}
	
	public SkillHolder getKillEffectStyleByStyleId(int styleId)
	{
		return _cachedKillEffectMap.get(styleId);
	}
	
	public int getArmorStyleByStyleId(int styleId)
	{
		return _cachedArmorMap.getOrDefault(styleId, 0);
	}
	
	public List<CharacterStyleDataHolder> getStylesByCategory(CharacterStyleCategoryType category)
	{
		return STYLES.getOrDefault(category, Collections.emptyList());
	}
	
	public CharacterStyleDataHolder getSpecificStyleByCategoryAndId(CharacterStyleCategoryType category, int styleId)
	{
		for (CharacterStyleDataHolder holder : getStylesByCategory(category))
		{
			if (holder._styleId == styleId)
			{
				return holder;
			}
		}
		return null;
	}
	
	public ItemHolder getSwapCostItemByCategory(CharacterStyleCategoryType category)
	{
		return SWAP_COST_HOLDER.get(category);
	}
	
	public static CharacterStylesData getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final CharacterStylesData INSTANCE = new CharacterStylesData();
	}
}
