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
package org.l2jmobius.gameserver.config.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Loads and stores rebirth system configuration values from the custom rebirth configuration file.<br>
 * Provides feature toggles, limits, rewards, visual effects, announcements and configured skill lists.
 * @author BazookaRpm
 */
public class RebirthConfig
{
	// Logger.
	private static final Logger LOGGER = Logger.getLogger(RebirthConfig.class.getName());
	
	// Configuration file.
	private static final String REBIRTH_CONFIG_FILE = "config/Custom/Rebirth.ini";
	
	// Core settings.
	public static boolean REBIRTH_ALLOW_REBIRTH;
	public static int REBIRTH_MIN_LEVEL;
	public static int REBIRTH_MAX_COUNT;
	public static boolean REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES;
	
	// Skill removal settings.
	public static boolean REBIRTH_DELETE_ALL_SKILLS;
	public static Set<Integer> REBIRTH_SKILL_DELETE_WHITELIST = new HashSet<>();
	public static Set<String> REBIRTH_DELETE_PROTECTED_SKILL_GROUPS = new HashSet<>();
	public static boolean REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS;
	
	// Cost and reward settings.
	public static int REBIRTH_ITEM_ID;
	public static int REBIRTH_ITEM_AMOUNT;
	public static int REBIRTH_MAX_SELECTED_SKILLS;
	public static int REBIRTH_REWARD_ITEM_ID;
	public static int REBIRTH_REWARD_ITEM_AMOUNT;
	public static String REBIRTH_SKILL_REFUND_MODE;
	
	// Skill pools.
	public static List<String> REBIRTH_MAGE_SKILLS = new ArrayList<>();
	public static List<String> REBIRTH_FIGHTER_SKILLS = new ArrayList<>();
	
	// Finish visual settings.
	public static int REBIRTH_FINISH_SOCIAL_ID;
	public static int REBIRTH_FINISH_EFFECT_SKILL_ID;
	public static int REBIRTH_FINISH_EFFECT_SKILL_LVL;
	
	// Selection visual settings.
	public static int REBIRTH_SELECT_SOCIAL_ID;
	public static int REBIRTH_SELECT_EFFECT_SKILL_ID;
	public static int REBIRTH_SELECT_EFFECT_SKILL_LVL;
	
	// Screen message settings.
	public static boolean REBIRTH_SCREEN_MESSAGE_ENABLED;
	public static String REBIRTH_SCREEN_MESSAGE;
	public static String REBIRTH_SCREEN_MESSAGE_POSITION;
	public static int REBIRTH_SCREEN_MESSAGE_TIME;
	
	// Global announcement settings.
	public static boolean REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED;
	public static String REBIRTH_GLOBAL_ANNOUNCEMENT;
	public static boolean REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL;
	
	// 重生擴展（修仙稱號）
	public static boolean REBIRTH_TITLE_ENABLED = true;
	public static List<String> REBIRTH_TITLE_STAGES = new ArrayList<>();
	public static String REBIRTH_TITLE_COLOR = "FFFF00";
	public static boolean REBIRTH_AUTO_UPDATE_TITLE = true;

	/**
	 * Loads all rebirth configuration values from the rebirth configuration file.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(REBIRTH_CONFIG_FILE);
		REBIRTH_ALLOW_REBIRTH = config.getBoolean("RebirthAllow", true);
		
		// 修正：使用 config 物件讀取自訂的稱號擴展設定
		REBIRTH_TITLE_ENABLED = config.getBoolean("RebirthTitleEnabled", true);
		REBIRTH_TITLE_COLOR = config.getString("RebirthTitleColor", "FFFF00");
		REBIRTH_AUTO_UPDATE_TITLE = config.getBoolean("RebirthAutoUpdateTitle", true);
		
		// 新增：安全讀取並以逗號分割稱號列表
		final String stagesRaw = config.getString("RebirthTitleStages", "人仙,地仙,天仙,金仙,玄仙,真仙,大罗真仙,准圣,亚圣,圣人");
		REBIRTH_TITLE_STAGES.clear();
		if ((stagesRaw != null) && !stagesRaw.trim().isEmpty())
		{
			for (String stage : stagesRaw.split(","))
			{
				if (!stage.trim().isEmpty())
				{
					REBIRTH_TITLE_STAGES.add(stage.trim());
				}
			}
		}
		
		REBIRTH_MIN_LEVEL = config.getInt("RebirthMin", 80);
		REBIRTH_MAX_COUNT = config.getInt("RebirthMaxCount", 3);
		REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES = config.getBoolean("RebirthInheritSkillsToSubclasses", true);
		REBIRTH_DELETE_ALL_SKILLS = config.getBoolean("RebirthDeleteAllSkills", true);
		REBIRTH_SKILL_DELETE_WHITELIST = parseIntegerSet(config.getString("RebirthSkillDeleteWhitelist", ""));
		REBIRTH_DELETE_PROTECTED_SKILL_GROUPS = parseStringSet(config.getString("RebirthDeleteProtectedSkillGroups", ""));
		REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS = config.getBoolean("RebirthDeleteRestoreTemporarySkills", true);
		REBIRTH_ITEM_ID = config.getInt("RebirthItemId", 57);
		REBIRTH_ITEM_AMOUNT = config.getInt("RebirthItemAmount", 3000);
		REBIRTH_MAX_SELECTED_SKILLS = config.getInt("RebirthMaxSelectedSkills", 3);
		REBIRTH_REWARD_ITEM_ID = config.getInt("RebirthRewardItemId", 57);
		REBIRTH_REWARD_ITEM_AMOUNT = config.getInt("RebirthRewardItemAmount", 1);
		REBIRTH_SKILL_REFUND_MODE = config.getString("RebirthSkillRefundMode", "MID").trim().toUpperCase();
		REBIRTH_MAGE_SKILLS = parseSkillList(config.getString("RebirthMageSkills", ""));
		REBIRTH_FIGHTER_SKILLS = parseSkillList(config.getString("RebirthFighterSkills", ""));
		REBIRTH_FINISH_SOCIAL_ID = config.getInt("RebirthFinishSocialId", 3);
		REBIRTH_FINISH_EFFECT_SKILL_ID = config.getInt("RebirthFinishEffectSkillId", 2024);
		REBIRTH_FINISH_EFFECT_SKILL_LVL = config.getInt("RebirthFinishEffectSkillLevel", 1);
		REBIRTH_SELECT_SOCIAL_ID = config.getInt("RebirthSelectSocialId", 3);
		REBIRTH_SELECT_EFFECT_SKILL_ID = config.getInt("RebirthSelectEffectSkillId", 2024);
		REBIRTH_SELECT_EFFECT_SKILL_LVL = config.getInt("RebirthSelectEffectSkillLevel", 1);
		REBIRTH_SCREEN_MESSAGE_ENABLED = config.getBoolean("RebirthScreenMessageEnabled", true);
		REBIRTH_SCREEN_MESSAGE = config.getString("RebirthScreenMessage", "Congratulations %player%! Rebirth status acquired. [%rebirth_count%/%max_rebirth%]");
		REBIRTH_SCREEN_MESSAGE_POSITION = config.getString("RebirthScreenMessagePosition", "TOP_CENTER").trim().toUpperCase();
		REBIRTH_SCREEN_MESSAGE_TIME = config.getInt("RebirthScreenMessageTime", 5000);
		REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED = config.getBoolean("RebirthGlobalAnnouncementEnabled", true);
		REBIRTH_GLOBAL_ANNOUNCEMENT = config.getString("RebirthGlobalAnnouncement", "Player %player% has reached Rebirth %rebirth_count% of %max_rebirth%!");
		REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL = config.getBoolean("RebirthGlobalAnnouncementCritical", false);
	}
	
	/**
	 * Parses a semicolon-separated skill list string into a list of entries.
	 * @param value
	 * @return The parsed skill list, or an empty list if the value is blank.
	 */
	private static List<String> parseSkillList(String value)
	{
		if ((value == null) || value.trim().isEmpty())
		{
			return new ArrayList<>();
		}
		
		return Arrays.asList(value.split(";"));
	}
	
	/**
	 * Parses a comma-separated integer list into a skill id set.
	 * @param value
	 * @return The parsed skill id set.
	 */
	private static Set<Integer> parseIntegerSet(String value)
	{
		final Set<Integer> skillIds = new HashSet<>();
		if ((value == null) || value.trim().isEmpty())
		{
			return skillIds;
		}
		
		for (String token : value.split(","))
		{
			final String trimmedToken = token.trim();
			if (trimmedToken.isEmpty())
			{
				continue;
			}
			
			try
			{
				skillIds.add(Integer.valueOf(Integer.parseInt(trimmedToken)));
			}
			catch (Exception e)
			{
				// Ignore parsing failures.
			}
		}
		return skillIds;
	}

	/**
	 * Parses a comma-separated string list into a set.
	 * @param value
	 * @return The parsed string set.
	 */
	private static Set<String> parseStringSet(String value)
	{
		final Set<String> set = new HashSet<>();
		if ((value == null) || value.trim().isEmpty())
		{
			return set;
		}
		
		for (String token : value.split(","))
		{
			final String trimmedToken = token.trim();
			if (!trimmedToken.isEmpty())
			{
				set.add(trimmedToken);
			}
		}
		return set;
	}
}
