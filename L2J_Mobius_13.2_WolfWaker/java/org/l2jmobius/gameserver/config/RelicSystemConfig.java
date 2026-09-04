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
package org.l2jmobius.gameserver.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * This class loads all the relic system related configurations.
 * @author Mobius
 */
public class RelicSystemConfig
{
	private static final Logger LOGGER = Logger.getLogger(RelicSystemConfig.class.getName());
	
	// File
	private static final String RELIC_SYSTEM_CONFIG_FILE = "./config/RelicSystem.ini";
	
	// Constants
	public static boolean RELIC_SYSTEM_ENABLED;
	public static boolean RELIC_SYSTEM_DEBUG_ENABLED;
	public static boolean RELIC_SUMMON_ANNOUNCE;
	public static boolean RELIC_COMPOUND_ANNOUNCE;
	public static boolean RELIC_ANNOUNCE_ONLY_A_B_GRADE;
	public static int RELIC_UNCONFIRMED_LIST_LIMIT;
	public static int RELIC_UNCONFIRMED_TIME_LIMIT;
	public static boolean RELIC_COMPOUND_ADDITIONAL_FAILURE_ITEM_ENABLED;
	public static int RELIC_COMPOUND_FAILURE_ITEM_CHANCE;
	public static List<ItemHolder> RELIC_COMPOUND_FAILURE_ITEM_C_GRADE;
	public static List<ItemHolder> RELIC_COMPOUND_FAILURE_ITEM_B_GRADE;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(RELIC_SYSTEM_CONFIG_FILE);
		RELIC_SYSTEM_ENABLED = config.getBoolean("RelicSystemEnabled", true);
		RELIC_SYSTEM_DEBUG_ENABLED = config.getBoolean("RelicSystemDebugEnabled", false);
		RELIC_SUMMON_ANNOUNCE = config.getBoolean("RelicSummonAnnounce", true);
		RELIC_COMPOUND_ANNOUNCE = config.getBoolean("RelicCompoundAnnounce", true);
		RELIC_ANNOUNCE_ONLY_A_B_GRADE = config.getBoolean("RelicAnnounceOnlyABGrade", true);
		RELIC_UNCONFIRMED_LIST_LIMIT = config.getInt("RelicUnconfirmedListLimit", 100);
		RELIC_UNCONFIRMED_TIME_LIMIT = config.getInt("RelicUnconfirmedTimeLimit", 7);
		RELIC_COMPOUND_ADDITIONAL_FAILURE_ITEM_ENABLED = config.getBoolean("RelicCompoundAdditionalFailureItemEnabled", false);
		RELIC_COMPOUND_FAILURE_ITEM_CHANCE = config.getInt("RelicCompoundFailureItemChance", 10);
		RELIC_COMPOUND_FAILURE_ITEM_C_GRADE = parseItemsList(config.getString("RelicCompoundFailureItemCGrade", "83011,1"));
		RELIC_COMPOUND_FAILURE_ITEM_B_GRADE = parseItemsList(config.getString("RelicCompoundFailureItemBGrade", "83012,1"));
	}
	
	/**
	 * Parse a config value from its string representation to a two-dimensional int array.<br>
	 * The format of the value to be parsed should be as follows: "item1Id,item1Amount;item2Id,item2Amount;...itemNId,itemNAmount".
	 * @param line the value of the parameter to parse
	 * @return the parsed list or {@code null} if nothing was parsed
	 */
	private static List<ItemHolder> parseItemsList(String line)
	{
		if (line.isEmpty())
		{
			return Collections.emptyList();
		}
		
		final String[] propertySplit = line.split(";");
		if (line.equalsIgnoreCase("none") || (propertySplit.length == 0))
		{
			return Collections.emptyList();
		}
		
		String[] valueSplit;
		final List<ItemHolder> result = new ArrayList<>(propertySplit.length);
		for (String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				LOGGER.warning("parseItemsList[RelicSystemConfig.load()]: invalid entry -> " + valueSplit[0] + ", should be itemId,itemNumber. Skipping to the next entry in the list.");
				continue;
			}
			
			int itemId = -1;
			try
			{
				itemId = Integer.parseInt(valueSplit[0]);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("parseItemsList[RelicSystemConfig.load()]: invalid itemId -> " + valueSplit[0] + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			
			int count = -1;
			try
			{
				count = Integer.parseInt(valueSplit[1]);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("parseItemsList[RelicSystemConfig.load()]: invalid item number -> " + valueSplit[1] + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			
			if ((itemId > 0) && (count > 0))
			{
				result.add(new ItemHolder(itemId, count));
			}
		}
		
		return result;
	}
}