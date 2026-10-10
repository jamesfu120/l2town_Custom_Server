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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.RebirthConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public final class RebirthManager
{
	private static final Logger LOGGER = Logger.getLogger(RebirthManager.class.getName());
	private static final int SKILLS_PER_ROW = 3;
	private static final int SKILL_ICON_PADDING_THRESHOLD = 1000;
	
	private static final String REBIRTH_BUSY_MESSAGE = "A rebirth operation is already in progress.";
	private static final String SELECT_REBIRTH_COUNT = "SELECT rebirthCount FROM rebirth_system WHERE charId=?";
	private static final String INSERT_FIRST_REBIRTH = "INSERT INTO rebirth_system (charId, rebirthCount) VALUES (?,1)";
	private static final String UPDATE_REBIRTH_COUNT = "UPDATE rebirth_system SET rebirthCount=? WHERE charId=?";
	private static final String SELECT_SELECTED_SKILLS = "SELECT selectedSkills FROM rebirth_system WHERE charId=?";
	private static final String UPDATE_SELECTED_SKILLS = "UPDATE rebirth_system SET selectedSkills=? WHERE charId=?";
	
	private static final String CLASS_LIST_FILE = "data/stats/players/classList.xml";
	
	private static final Map<Integer, Integer> ROOT_CLASS_IDS = new ConcurrentHashMap<>();
	private static volatile boolean ROOT_CLASS_IDS_LOADED = false;
	
	private static final Set<Integer> REBIRTH_OPERATIONS_IN_PROGRESS = ConcurrentHashMap.newKeySet();
	private static final Set<Integer> REBIRTH_SKILL_OPERATIONS_IN_PROGRESS = ConcurrentHashMap.newKeySet();

	protected RebirthManager()
	{
	}
	public void displaySkillSelectionWindow(Player player, int objectId)
	{
		final int rebirthCount = getRebirthLevel(player);
		if (rebirthCount <= 0)
		{
			player.sendMessage("You must perform at least one Rebirth before selecting skills.");
			return;
		}
		final List<String> skillDefinitions = getSkillPool(player);
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(skillDefinitions, player);
		if (skillDefinitionMap.isEmpty())
		{
			player.sendMessage("There are no skills configured for your class. Contact an administrator.");
			return;
		}
		final List<Integer> selectedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
		final Set<Integer> selectedSkillSet = createSkillSet(selectedSkills);
		final int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<html><body><center><title>Select your Rebirth Skills</title>");
		htmlBuilder.append("<br><font color=LEVEL>Maximum allowed: ").append(allowedSkills).append(" skill(s)</font><br1>");
		htmlBuilder.append("<font color=99FF99>Tokens available: ").append(getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID)).append("</font><br><br>");
		htmlBuilder.append("<table width=256 cellpadding=1 cellspacing=2><tr>");
		int column = 0;
		for (Entry<Integer, String> entry : skillDefinitionMap.entrySet())
		{
			final String definition = entry.getValue();
			final int skillLevel = getSkillLevel(definition);
			if (skillLevel <= 0) continue;
			final int skillId = entry.getKey().intValue();
			final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill == null) continue;
			final int itemCost = getSkillCost(definition);
			if (column == SKILLS_PER_ROW)
			{
				htmlBuilder.append("</tr><tr>");
				column = 0;
			}
			final String icon = (skillId < SKILL_ICON_PADDING_THRESHOLD) ? ("0" + skillId) : String.valueOf(skillId);
			htmlBuilder.append("<td width=90 align=center><img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
			htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
			htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
			htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font><br1>");
			if (selectedSkillSet.contains(Integer.valueOf(skillId)))
			{
				htmlBuilder.append("<font color=AAAAAA>Owned</font>");
			}
			else if (selectedSkills.size() >= allowedSkills)
			{
				htmlBuilder.append("<font color=AAAAAA>Limit reached</font>");
			}
			else
			{
				htmlBuilder.append("<button value=\"Choose\" action=\"bypass -h rebirth_previewSkill ").append(skillId).append("\" width=55 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
			htmlBuilder.append("</td>");
			column++;
		}
		htmlBuilder.append("</tr></table><br><button value=\"Return\" action=\"bypass -h rebirth_openmenu\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>");
		html.setHtml(htmlBuilder.toString());
		player.sendPacket(html);
	}
	public void displaySkillPreviewWindow(Player player, int objectId, int skillId)
	{
		final List<String> skillDefinitions = getSkillPool(player);
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(skillDefinitions, player);
		final List<Integer> savedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
		final Set<Integer> savedSkillSet = createSkillSet(savedSkills);
		final int rebirthCount = getRebirthLevel(player);
		final String definition = getSkillDefinition(skillDefinitionMap, skillId);
		if (definition == null) { player.sendMessage("Invalid skill."); return; }
		final int targetLevel = getSkillLevel(definition);
		final Skill skill = SkillData.getInstance().getSkill(skillId, targetLevel);
		if (skill == null) { player.sendMessage("Invalid skill."); return; }
		final int itemCost = getSkillCost(definition);
		final int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
		final long availableTokens = getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID);
		final boolean alreadySelected = savedSkillSet.contains(Integer.valueOf(skillId));
		final boolean limitReached = savedSkills.size() >= allowedSkills;
		final boolean hasEnoughTokens = availableTokens >= itemCost;
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<html><body><center><br><font color=\"LEVEL\">Rebirth Manager</font><br><table width=256 border=0 cellpadding=1 cellspacing=2>");
		htmlBuilder.append("<tr><td width=95 align=center><font color=\"66CC66\">Your Rebirths</font><br1><font color=\"FFFF99\">").append(rebirthCount).append('/').append(RebirthConfig.REBIRTH_MAX_COUNT).append("</font><br1><font color=\"99FF99\">Tokens: ").append(availableTokens).append("</font></td>");
		final String icon = (skillId < SKILL_ICON_PADDING_THRESHOLD) ? ("0" + skillId) : String.valueOf(skillId);
		htmlBuilder.append("<td width=95 align=center><img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1><font color=\"FFDD99\">").append(skill.getName()).append("</font><br1><font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1><font color=\"99FF99\">Cost: ").append(itemCost).append("</font></td></tr></table><br1>");
		htmlBuilder.append("<font color=\"LEVEL\">This purchase consumes ").append(itemCost).append(" token(s).</font><br1><font color=\"LEVEL\">Refund: ").append(RebirthConfig.REBIRTH_SKILL_REFUND_MODE).append("</font><br1><font color=\"66CC66\">Rebirth Skills Obtained</font><br1>");
		htmlBuilder.append(buildAcquiredSkillsHtml(player, savedSkills, skillDefinitionMap, false)).append("<br1>");
		if (!alreadySelected && !limitReached && hasEnoughTokens) { htmlBuilder.append("<button value=\"Confirm\" action=\"bypass -h rebirth_confirmSkill ").append(skillId).append("\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>"); }
		else {
			if (alreadySelected) htmlBuilder.append("<font color=AAAAAA>Already selected.</font><br1>");
			if (limitReached) htmlBuilder.append("<font color=AAAAAA>Skill limit reached.</font><br1>");
			if (!hasEnoughTokens) htmlBuilder.append("<font color=FF6666>Not enough tokens.</font><br1>");
		}
		htmlBuilder.append("<button value=\"Return\" action=\"bypass -h rebirth_selectskills\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>");
		html.setHtml(htmlBuilder.toString());
		player.sendPacket(html);
	}

	public void displayMainWindow(Player player, int objectId)
	{
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
		final List<Integer> selectedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
		final int currentRebirthCount = getRebirthLevel(player);
		final long tokenCount = getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID);
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, "data/html/default/70001_dynamic.htm");
		html.replace("%rebirth_count%", currentRebirthCount + "/" + RebirthConfig.REBIRTH_MAX_COUNT);
		html.replace("%rebirth_icons%", buildAcquiredSkillsHtml(player, selectedSkills, skillDefinitionMap, true));
		html.replace("%next_skill_icon%", "<img src=\"icon.etc_coins_gold_i00\" width=32 height=32>");
		html.replace("%next_skill_name%", "Rebirth Tokens");
		html.replace("%next_skill_lvl%", String.valueOf(tokenCount));
		html.replace("%next_skill_desc%", "Refund mode: " + RebirthConfig.REBIRTH_SKILL_REFUND_MODE);
		if (currentRebirthCount < RebirthConfig.REBIRTH_MAX_COUNT) { html.replace("%rebirth_button%", "<button value=\"Request Rebirth\" action=\"bypass -h rebirth_confirmrequest\" width=130 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">"); }
		else { html.replace("%rebirth_button%", "<font color=AAAAAA>Max Rebirth reached.</font>"); }
		player.sendPacket(html);
	}
	public void acquireRebirthSkill(Player player, int objectId, int skillId)
	{
		final int playerId = player.getObjectId();
		if (!REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.add(Integer.valueOf(playerId))) { player.sendMessage(REBIRTH_BUSY_MESSAGE); return; }
		try {
			final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
			final List<Integer> savedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
			final int rebirthCount = getRebirthLevel(player);
			if (rebirthCount <= 0) { player.sendMessage("You must perform at least one Rebirth before selecting skills."); displayMainWindow(player, objectId); return; }
			final Set<Integer> savedSkillSet = createSkillSet(savedSkills);
			if (savedSkillSet.contains(Integer.valueOf(skillId))) { player.sendMessage("You already own this rebirth skill."); displayMainWindow(player, objectId); return; }
			final int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
			if (savedSkills.size() >= allowedSkills) { player.sendMessage("You have reached the maximum number of skills allowed."); displayMainWindow(player, objectId); return; }
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null) { player.sendMessage("This skill is not configured for your class."); displayMainWindow(player, objectId); return; }
			final int itemCost = getSkillCost(definition);
			if (!consumeItem(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID, itemCost, ItemProcessType.DESTROY, "rebirth token")) { displaySkillPreviewWindow(player, objectId, skillId); return; }
			savedSkills.add(Integer.valueOf(skillId));
			saveSelectedSkills(playerId, savedSkills);
			grantRebirthSkills(player, skillDefinitionMap, savedSkills);
			playRebirthSelectFx(player);
			player.sendMessage("Rebirth skill purchased successfully.");
			displayMainWindow(player, objectId);
		} finally { REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.remove(Integer.valueOf(playerId)); }
	}

	public void resetRebirthSkill(Player player, int objectId, int skillId)
	{
		final int playerId = player.getObjectId();
		if (!REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.add(Integer.valueOf(playerId))) { player.sendMessage(REBIRTH_BUSY_MESSAGE); return; }
		try {
			final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
			final List<Integer> savedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
			final Set<Integer> savedSkillSet = createSkillSet(savedSkills);
			final int rebirthCount = getRebirthLevel(player);
			if (!savedSkillSet.contains(Integer.valueOf(skillId))) { player.sendMessage("You do not own this rebirth skill."); displayMainWindow(player, objectId); return; }
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null) return;
			final int skillLevel = getSkillLevel(definition);
			final int itemCost = getSkillCost(definition);
			final int refundAmount = getRefundAmount(itemCost);
			savedSkills.remove(Integer.valueOf(skillId));
			saveSelectedSkills(playerId, savedSkills);
			final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill != null) player.removeSkill(skill, true);
			player.sendSkillList();
			if (refundAmount > 0) { player.addItem(ItemProcessType.REFUND, RebirthConfig.REBIRTH_REWARD_ITEM_ID, refundAmount, player, true); player.sendMessage("Refunded " + refundAmount + " rebirth token(s)."); }
			displayMainWindow(player, objectId);
		} finally { REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.remove(Integer.valueOf(playerId)); }
	}
	private String buildAcquiredSkillsHtml(Player player, List<Integer> selectedSkills, Map<Integer, String> skillDefinitionMap, boolean showResetButton)
	{
		if (selectedSkills.isEmpty()) return "<center><font color=\"AAAAAA\">You don't have any skills selected yet.</font></center>";
		final StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<table width=270 border=0 cellpadding=2 cellspacing=2><tr>");
		int column = 0;
		for (int skillId : selectedSkills)
		{
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null) continue;
			final int skillLevel = getSkillLevel(definition);
			final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill == null) continue;
			final int itemCost = getSkillCost(definition);
			if (column == SKILLS_PER_ROW) { htmlBuilder.append("</tr><tr>"); column = 0; }
			final String icon = (skillId < SKILL_ICON_PADDING_THRESHOLD) ? ("0" + skillId) : String.valueOf(skillId);
			htmlBuilder.append("<td width=90 align=center><img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1><font color=\"FFDD99\">").append(skill.getName()).append("</font><br1><font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1><font color=\"99FF99\">Cost: ").append(itemCost).append("</font><br1>");
			if (showResetButton) htmlBuilder.append("<button value=\"Reset\" action=\"bypass -h rebirth_resetSkill ").append(skillId).append("\" width=65 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
			htmlBuilder.append("</td>");
			column++;
		}
		htmlBuilder.append("</tr></table>");
		return htmlBuilder.toString();
	}

	private List<String> getSkillPool(Player player) { return player.getPlayerClass().isMage() ? RebirthConfig.REBIRTH_MAGE_SKILLS : RebirthConfig.REBIRTH_FIGHTER_SKILLS; }
	
	private Map<Integer, String> createSkillDefinitionMap(List<String> skillDefinitions, Player player)
	{
		final Map<Integer, String> skillDefinitionMap = new HashMap<>();
		for (String definition : skillDefinitions) {
			final int skillId = getSkillId(definition);
			if (skillId <= 0) continue;
			skillDefinitionMap.put(Integer.valueOf(skillId), definition);
		}
		return skillDefinitionMap;
	}
	
	private Set<Integer> createSkillSet(List<Integer> skills) { return new HashSet<>(skills); }
	
	private List<Integer> loadValidatedSelectedSkills(Player player, Map<Integer, String> skillDefinitionMap)
	{
		final int playerId = player.getObjectId();
		final List<Integer> storedSkills = getSelectedSkills(playerId);
		if (storedSkills.isEmpty()) return storedSkills;
		final List<Integer> validSkills = new ArrayList<>(storedSkills.size());
		boolean changed = false;
		for (int skillId : storedSkills) {
			if (skillDefinitionMap.containsKey(Integer.valueOf(skillId))) validSkills.add(Integer.valueOf(skillId));
			else changed = true;
		}
		if (changed) saveSelectedSkills(playerId, validSkills);
		return validSkills;
	}
	
	private String getSkillDefinition(Map<Integer, String> skillDefinitionMap, int skillId) { return skillDefinitionMap.get(Integer.valueOf(skillId)); }
	private int getSkillId(String definition) { final String[] parts = definition.split(","); return (parts.length < 2) ? 0 : Integer.parseInt(parts[0].trim()); }
	private int getSkillLevel(String definition) { final String[] parts = definition.split(","); return (parts.length < 2) ? 1 : Integer.parseInt(parts[1].trim()); }
	private int getSkillCost(String definition) { final String[] parts = definition.split(","); return (parts.length < 3) ? 1 : Math.max(1, Integer.parseInt(parts[2].trim())); }
	private int getRefundAmount(int itemCost) { return RebirthConfig.REBIRTH_SKILL_REFUND_MODE.equals("FULL") ? itemCost : (RebirthConfig.REBIRTH_SKILL_REFUND_MODE.equals("MID") ? itemCost / 2 : 0); }
	private synchronized long getItemCount(Player player, int itemId) { final Item item = player.getInventory().getItemByItemId(itemId); return item != null ? item.getCount() : 0; }
	public void requestRebirth(Player player)
	{
		final int playerId = player.getObjectId();
		if (!REBIRTH_OPERATIONS_IN_PROGRESS.add(Integer.valueOf(playerId))) { player.sendMessage(REBIRTH_BUSY_MESSAGE); return; }
		try {
			if (!RebirthConfig.REBIRTH_ALLOW_REBIRTH || player.getLevel() < RebirthConfig.REBIRTH_MIN_LEVEL) return;
			if (player.isSubClassActive() || player.isAlikeDead() || player.isInDuel() || player.isCastingNow() || player.isAttackingNow()) return;
			final int currentRebirthCount = getRebirthLevel(player);
			if (currentRebirthCount >= RebirthConfig.REBIRTH_MAX_COUNT) return;
			if (!consumeItem(player, RebirthConfig.REBIRTH_ITEM_ID, RebirthConfig.REBIRTH_ITEM_AMOUNT, ItemProcessType.DESTROY, "rebirth request")) return;
			grantRebirth(player, currentRebirthCount + 1, currentRebirthCount == 0);
		} finally { REBIRTH_OPERATIONS_IN_PROGRESS.remove(Integer.valueOf(playerId)); }
	}

	private void grantRebirth(Player player, int newCount, boolean firstBirth)
	{
		final int playerId = player.getObjectId();
		try {
			final boolean updated = firstBirth ? insertFirst(playerId) : updateCount(playerId, newCount);
			if (!updated) return;
			final long targetExp = ExperienceData.getInstance().getExpForLevel(1);
			if (player.getExp() > targetExp) player.removeExpAndSp(player.getExp() - targetExp, 0);
			player.setPlayerClass(player.getBaseClass());
			if (RebirthConfig.REBIRTH_DELETE_ALL_SKILLS) {
				for (Skill skill : player.getAllSkills()) {
					if (skill != null && !RebirthConfig.REBIRTH_SKILL_DELETE_WHITELIST.contains(Integer.valueOf(skill.getId())) && !isProtectedByGroup(player, skill)) { player.removeSkill(skill, true); }
				}
			}
			player.giveAvailableSkills(true, true, true, false);
			if (RebirthConfig.REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS) player.regiveTemporarySkills();
			if (RebirthConfig.REBIRTH_TITLE_ENABLED && RebirthConfig.REBIRTH_AUTO_UPDATE_TITLE) {
				String titleName = (newCount <= RebirthConfig.REBIRTH_TITLE_STAGES.size()) ? RebirthConfig.REBIRTH_TITLE_STAGES.get(newCount - 1) : "轉生強者 Lv." + newCount;
				player.setTitle(titleName);
			}
			player.storeMe();
		} catch (Exception e) { return; }
		try {
			final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
			final List<Integer> selectedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
			grantRewardTokens(player);
			if (!RebirthConfig.REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS) grantRebirthSkills(player, skillDefinitionMap, selectedSkills);
			player.sendSkillList();
			player.broadcastUserInfo();
			player.broadcastStatusUpdate();
			displayCongrats(player, newCount);
			routePostRebirthUi(player, selectedSkills, newCount, skillDefinitionMap);
		} catch (Exception e) {}
	}
	public void displayTitleSelectionWindow(Player player, int objectId)
	{
		if (!RebirthConfig.REBIRTH_TITLE_ENABLED) { player.sendMessage("轉生稱號系統目前未啟用。"); return; }
		final int rebirthCount = getRebirthLevel(player);
		if (rebirthCount <= 0) { player.sendMessage("你必須至少完成一次轉生才能選擇稱號。"); return; }
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<html><body><center><title>選擇你的轉生稱號</title><br><font color=LEVEL>目前的轉生次數: ").append(rebirthCount).append(" 次</font><br><br><table width=256 cellpadding=1 cellspacing=2>");
		final int maxConfigStages = RebirthConfig.REBIRTH_TITLE_STAGES.size();
		for (int i = 1; i <= rebirthCount; i++) {
			String titleName = (i <= maxConfigStages) ? RebirthConfig.REBIRTH_TITLE_STAGES.get(i - 1) : "轉生強者 Lv." + i;
			htmlBuilder.append("<tr><td width=150 align=center><font color=\"").append(RebirthConfig.REBIRTH_TITLE_COLOR).append("\">").append(titleName).append("</font></td>");
			htmlBuilder.append("<td width=100 align=center><button value=\"配戴\" action=\"bypass -h rebirth_selectTitle ").append(i).append("\" width=55 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
		}
		htmlBuilder.append("</table><br><button value=\"返回\" action=\"bypass -h rebirth_openmenu\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>");
		html.setHtml(htmlBuilder.toString());
		player.sendPacket(html);
	}

	public void selectRebirthTitle(Player player, int objectId, int titleIndex)
	{
		if (!RebirthConfig.REBIRTH_TITLE_ENABLED) return;
		final int rebirthCount = getRebirthLevel(player);
		if (titleIndex > rebirthCount || titleIndex <= 0) { player.sendMessage("無效的稱號索引或你的轉生次數不足。"); return; }
		String titleName = (titleIndex <= RebirthConfig.REBIRTH_TITLE_STAGES.size()) ? RebirthConfig.REBIRTH_TITLE_STAGES.get(titleIndex - 1) : "轉生強者 Lv." + titleIndex;
		player.setTitle(titleName);
		player.broadcastUserInfo();
		player.sendMessage("成功更換修仙稱號為：【" + titleName + "】！");
		displayTitleSelectionWindow(player, objectId);
	}

	private void grantRebirthSkills(Player player, Map<Integer, String> skillDefinitionMap, List<Integer> selectedSkills)
	{
		if (!RebirthConfig.REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES && (player.getClassIndex() > 0)) return;
		for (int skillId : selectedSkills) {
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null) continue;
			final Skill skill = SkillData.getInstance().getSkill(skillId, getSkillLevel(definition));
			if (skill != null) player.addSkill(skill, true);
		}
		player.sendSkillList();
	}

	public void playRebirthSelectFx(Player player)
	{
		if (RebirthConfig.REBIRTH_SELECT_SOCIAL_ID > 0) player.broadcastPacket(new SocialAction(player.getObjectId(), RebirthConfig.REBIRTH_SELECT_SOCIAL_ID));
		if (RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_ID > 0) player.broadcastPacket(new MagicSkillUse(player, player, RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_ID, RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_LVL, 1, 0));
	}
	private boolean isProtectedByGroup(Player player, Skill skill)
	{
		if ((player == null) || (skill == null) || RebirthConfig.REBIRTH_DELETE_PROTECTED_SKILL_GROUPS.isEmpty()) return false;
		if (isProtectedSkillGroupEnabled("STARTING_CLASS") && isStartingClassSkill(player, skill)) return true;
		if (isProtectedSkillGroupEnabled("COMMON") && isSkillInTree(skill, SkillTreeData.getInstance().getCommonSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("FISHING") && isSkillInTree(skill, SkillTreeData.getInstance().getFishingSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("NOBLE") && isSkillInList(skill, SkillTreeData.getInstance().getNobleSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("HERO") && isSkillInList(skill, SkillTreeData.getInstance().getHeroSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("CLAN") && isSkillInTree(skill, SkillTreeData.getInstance().getPledgeSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("SUBPLEDGE") && isSkillInTree(skill, SkillTreeData.getInstance().getSubPledgeSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("COLLECT") && isSkillInTree(skill, SkillTreeData.getInstance().getCollectSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("SUBCLASS") && isSkillInTree(skill, SkillTreeData.getInstance().getSubClassSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("TRANSFORM") && isSkillInTree(skill, SkillTreeData.getInstance().getTransformSkillTree())) return true;
		if (isProtectedSkillGroupEnabled("TRANSFER") && isTransferSkill(player, skill)) return true;
		if (isProtectedSkillGroupEnabled("RACE") && isRaceSkill(player, skill)) return true;
		if (isProtectedSkillGroupEnabled("REVELATION") && isRevelationSkill(player, skill)) return true;
		if (isProtectedSkillGroupEnabled("ABILITY") && isSkillInTree(skill, SkillTreeData.getInstance().getAbilitySkillTree())) return true;
		if (isProtectedSkillGroupEnabled("ALCHEMY") && isSkillInTree(skill, SkillTreeData.getInstance().getAlchemySkillTree())) return true;
		if (isProtectedSkillGroupEnabled("DUAL_CLASS") && isDualClassSkill(skill)) return true;
		return false;
	}
	private boolean isProtectedSkillGroupEnabled(String groupName) { return RebirthConfig.REBIRTH_DELETE_PROTECTED_SKILL_GROUPS.contains(groupName); }
	private boolean isSkillInTree(Skill skill, Map<Long, ?> tree) { if ((skill == null) || (tree == null) || tree.isEmpty()) return false; return tree.containsKey(Long.valueOf(SkillData.getSkillHashCode(skill.getId(), skill.getLevel()))); }
	private boolean isSkillInList(Skill skill, List<Skill> tree) { if ((skill == null) || (tree == null) || tree.isEmpty()) return false; for (Skill t : tree) { if (t != null && t.getId() == skill.getId() && t.getLevel() == skill.getLevel()) return true; } return false; }
	private boolean isStartingClassSkill(Player player, Skill skill) { if (player == null || skill == null) return false; int r = getRootClassId(player.getBaseClass()); PlayerClass rc = PlayerClass.getPlayerClass(r); return rc != null && isSkillInTree(skill, SkillTreeData.getInstance().getCompleteClassSkillTree(rc)); }
	private boolean isRaceSkill(Player player, Skill skill) { if (player == null || skill == null) return false; return SkillTreeData.getInstance().getRaceSkillTree(player.getRace()).stream().anyMatch(sl -> sl != null && sl.getSkillId() == skill.getId() && sl.getSkillLevel() == skill.getLevel()); }
	private boolean isRevelationSkill(Player player, Skill skill) { if (player == null || skill == null) return false; return SkillTreeData.getInstance().getRevelationSkill(org.l2jmobius.gameserver.entity.actor.enums.player.SubclassType.BASECLASS, skill.getId(), skill.getLevel()) != null || SkillTreeData.getInstance().getRevelationSkill(org.l2jmobius.gameserver.entity.actor.enums.player.SubclassType.DUALCLASS, skill.getId(), skill.getLevel()) != null; }
	private boolean isDualClassSkill(Skill skill) { return skill != null && SkillTreeData.getInstance().getDualClassSkill(skill.getId(), skill.getLevel()) != null; }
	private boolean isTransferSkill(Player player, Skill skill) { if (player == null || skill == null) return false; PlayerClass pc = PlayerClass.getPlayerClass(player.getBaseClass()); return pc != null && isSkillInTree(skill, SkillTreeData.getInstance().getTransferSkillTree(pc)); }
	private int getRootClassId(int classId) { ensureRootClassIdsLoaded(); Integer r = ROOT_CLASS_IDS.get(Integer.valueOf(classId)); return r != null ? r.intValue() : (PlayerClass.getPlayerClass(classId) != null ? PlayerClass.getPlayerClass(classId).getRootClass().getId() : classId); }
	private void ensureRootClassIdsLoaded() { if (!ROOT_CLASS_IDS_LOADED) { synchronized (ROOT_CLASS_IDS) { if (!ROOT_CLASS_IDS_LOADED) { loadRootClassIds(); ROOT_CLASS_IDS_LOADED = true; } } } }
	private void loadRootClassIds() { File f = new File(CLASS_LIST_FILE); if (!f.exists()) return; Map<Integer, Integer> p = new HashMap<>(); try { DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); factory.setFeature("http://apache.org", true); NodeList classNodes = factory.newDocumentBuilder().parse(f).getElementsByTagName("class"); for (int i = 0; i < classNodes.getLength(); i++) { Node classNode = classNodes.item(i); int classId = parseIntAttribute(classNode, "classId", -1); if (classId >= 0) p.put(Integer.valueOf(classId), Integer.valueOf(parseIntAttribute(classNode, "parentClassId", -1))); } for (Integer classId : p.keySet()) ROOT_CLASS_IDS.put(classId, Integer.valueOf(resolveRootClassId(classId.intValue(), p))); } catch (Exception e) {} }
	private int resolveRootClassId(int classId, Map<Integer, Integer> p) { int c = classId; Set<Integer> v = new HashSet<>(); while (v.add(Integer.valueOf(c))) { Integer parent = p.get(Integer.valueOf(c)); if (parent == null || parent.intValue() < 0) return c; c = parent.intValue(); } return classId; }
	private int parseIntAttribute(Node n, String name, int def) { if (n == null || name == null || !n.hasAttributes() || n.getAttributes().getNamedItem(name) == null) return def; try { return Integer.parseInt(n.getAttributes().getNamedItem(name).getNodeValue()); } catch (NumberFormatException e) { return def; } }
	public int getRebirthLevel(Player player) { return getCount(player.getObjectId()); }
	public int getCount(int charId) { int count = 0; try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_REBIRTH_COUNT)) { statement.setInt(1, charId); try (ResultSet resultSet = statement.executeQuery()) { if (resultSet.next()) count = resultSet.getInt(1); } } catch (Exception e) {} return count; }
	public boolean insertFirst(int charId) { try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(INSERT_FIRST_REBIRTH)) { statement.setInt(1, charId); return statement.executeUpdate() > 0; } catch (Exception e) { return false; } }
	public boolean updateCount(int charId, int newCount) { try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(UPDATE_REBIRTH_COUNT)) { statement.setInt(1, newCount); statement.setInt(2, charId); return statement.executeUpdate() > 0; } catch (Exception e) { return false; } }
	public List<Integer> getSelectedSkills(int charId) { final List<Integer> skills = new ArrayList<>(); try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_SELECTED_SKILLS)) { statement.setInt(1, charId); try (ResultSet resultSet = statement.executeQuery()) { if (resultSet.next()) { final String data = resultSet.getString(1); if (data != null && !data.isEmpty()) { for (String skillValue : data.split(",")) skills.add(Integer.valueOf(Integer.parseInt(skillValue.trim()))); } } } } catch (Exception e) {} return skills; }
	public void saveSelectedSkills(int charId, List<Integer> skills) { try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(UPDATE_SELECTED_SKILLS)) { final StringBuilder skillBuilder = new StringBuilder(); for (int i = 0; i < skills.size(); i++) { if (i > 0) skillBuilder.append(','); skillBuilder.append(skills.get(i)); } statement.setString(1, skillBuilder.toString()); statement.setInt(2, charId); statement.executeUpdate(); } catch (Exception e) {} }
	private boolean consumeItem(Player player, int itemId, long amount, ItemProcessType processType, String reason) { if ((player == null) || (itemId <= 0) || (amount <= 0) || (amount > Integer.MAX_VALUE)) return false; final Item item = player.getInventory().getItemByItemId(itemId); if ((item == null) || (item.getCount() < amount)) { final String itemName = ((item != null) && (item.getTemplate() != null)) ? item.getTemplate().getName() : ("Item " + itemId); player.sendMessage("You need at least " + amount + " [ " + itemName + " ] for " + reason + "."); return false; } player.getInventory().destroyItem(processType, item, (int) amount, player, null); return true; }
	private void routePostRebirthUi(Player player, List<Integer> selectedSkills, int rebirthCount, Map<Integer, String> skillDefinitionMap) { displayMainWindow(player, 0); player.sendMessage("Use your rebirth tokens to purchase skills."); }
	private void grantRewardTokens(Player player) { if ((RebirthConfig.REBIRTH_REWARD_ITEM_ID <= 0) || (RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT <= 0)) return; player.addItem(ItemProcessType.REWARD, RebirthConfig.REBIRTH_REWARD_ITEM_ID, RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT, player, true); player.sendMessage("You received " + RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT + " rebirth token(s)."); }
	public void grantRebirthSkills(Player player) { final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player); if (!RebirthConfig.REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES && (player.getClassIndex() > 0)) return; for (int skillId : loadValidatedSelectedSkills(player, skillDefinitionMap)) { final String definition = getSkillDefinition(skillDefinitionMap, skillId); if (definition == null) continue; final Skill skill = SkillData.getInstance().getSkill(skillId, getSkillLevel(definition)); if (skill != null) player.addSkill(skill, true); } player.sendSkillList(); }
	public void displayCongrats(Player player, int rebirthCount) { if (RebirthConfig.REBIRTH_FINISH_SOCIAL_ID > 0) player.broadcastPacket(new SocialAction(player.getObjectId(), RebirthConfig.REBIRTH_FINISH_SOCIAL_ID)); if (RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_ID > 0) player.broadcastPacket(new MagicSkillUse(player, player, RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_ID, RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_LVL, 1, 0)); final String screenMessage = RebirthConfig.REBIRTH_SCREEN_MESSAGE.replace("%player%", player.getName()).replace("%rebirth_count%", String.valueOf(rebirthCount)).replace("%max_rebirth%", String.valueOf(RebirthConfig.REBIRTH_MAX_COUNT)); if (RebirthConfig.REBIRTH_SCREEN_MESSAGE_ENABLED) player.sendPacket(new ExShowScreenMessage(screenMessage, ExShowScreenMessage.TOP_CENTER, RebirthConfig.REBIRTH_SCREEN_MESSAGE_TIME)); if (RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED) World.broadcastToAllOnlinePlayers(RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT.replace("%player%", player.getName()).replace("%rebirth_count%", String.valueOf(rebirthCount)).replace("%max_rebirth%", String.valueOf(RebirthConfig.REBIRTH_MAX_COUNT)), RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL); }
	public static RebirthManager getInstance() { return SingletonHolder.INSTANCE; }
	private static class SingletonHolder { protected static final RebirthManager INSTANCE = new RebirthManager(); }
}
