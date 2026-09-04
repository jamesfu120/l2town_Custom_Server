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
package org.l2jmobius.gameserver.util;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerState;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionCategoryType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionChangeWeapon;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionGameChance;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionGameTime;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionLogicAnd;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionLogicNot;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionLogicOr;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionMinDistance;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionMinimumVitalityPoints;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerActiveEffectId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerActiveSkillId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerAgathionId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCallPc;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanCreateBase;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanEscape;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanRefuelAirship;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanResurrect;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSummonPet;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSummonServitor;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSummonSiegeGolem;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSweep;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSwitchSubclass;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanTakeCastle;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanTakeFort;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanTransform;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanUntransform;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCharges;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCheckAbnormal;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerClassIdRestriction;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCloakStatus;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCp;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerDualclass;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerFlyMounted;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasCastle;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasClanHall;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasFort;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasFreeSummonPoints;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasFreeTeleportBookmarkSlots;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasPet;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasSummon;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHp;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerImmobile;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInInstance;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInsideZoneId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInstanceId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInvSize;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsClanLeader;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsHero;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsInCombat;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsOnSide;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsPvpFlagged;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerLandingZone;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerLevel;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerLevelRange;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerMp;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerPkCount;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerPledgeClass;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerRace;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerRangeFromNpc;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerRangeFromSummonedNpc;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerServitorNpcId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSex;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSiegeSide;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSouls;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerState;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSubclass;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerTransformationId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerVehicleMounted;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerWeight;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionSiegeZone;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionSlotItemId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetAbnormalType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetActiveEffectId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetActiveSkillId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetAggro;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetCheckCrtEffect;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetClassIdRestriction;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetInvSize;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetLevel;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetLevelRange;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetMyPartyExceptMe;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetNpcId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetNpcType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetPlayable;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetPlayer;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetRace;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetUsesWeaponKind;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetWeight;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingItemType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingSkill;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingSlotType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionWithSkill;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;

/**
 * @author mkizub, Mobius
 */
public abstract class DocumentBase
{
	protected final Logger LOGGER = Logger.getLogger(getClass().getName());
	
	private final File _file;
	protected final Map<String, String[]> _tables = new HashMap<>();
	
	protected DocumentBase(File pFile)
	{
		_file = pFile;
	}
	
	public Document parse()
	{
		Document document = null;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			document = factory.newDocumentBuilder().parse(_file);
			parseDocument(document);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error loading file " + _file, e);
		}
		
		return document;
	}
	
	protected abstract void parseDocument(Document document);
	
	protected abstract StatSet getStatSet();
	
	protected abstract String getTableValue(String name);
	
	protected abstract String getTableValue(String name, int idx);
	
	protected void resetTable()
	{
		_tables.clear();
	}
	
	protected void setTable(String name, String[] table)
	{
		_tables.put(name, table);
	}
	
	protected void parseTemplate(Node node, Object template)
	{
		parseTemplate(node, template, null);
	}
	
	protected void parseTemplate(Node node, Object template, EffectScope effectScope)
	{
		Condition condition = null;
		Node n = node.getFirstChild();
		if (n == null)
		{
			return;
		}
		
		if ("conditions".equalsIgnoreCase(n.getNodeName()))
		{
			condition = parseCondition(n.getFirstChild(), template);
			final Node msg = n.getAttributes().getNamedItem("msg");
			final Node msgId = n.getAttributes().getNamedItem("msgId");
			if ((condition != null) && (msg != null))
			{
				condition.setMessage(msg.getNodeValue());
			}
			else if ((condition != null) && (msgId != null))
			{
				condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
				final Node addName = n.getAttributes().getNamedItem("addName");
				if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
				{
					condition.addName();
				}
			}
			
			n = n.getNextSibling();
		}
		
		for (; n != null; n = n.getNextSibling())
		{
			final String name = n.getNodeName().toLowerCase();
			
			switch (name)
			{
				case "add":
				case "sub":
				case "mul":
				case "div":
				case "set":
				case "enchant":
				case "enchanthp":
				{
					// Check if we need to handle an alternative format for the "enchant" or "enchanthp" tag.
					if ((n.getAttributes().getNamedItem("stat") == null) && (n.getAttributes().getNamedItem("val") == null))
					{
						// This is the alternative format with element-based stats.
						processAlternativeFuncFormat(n, template, name, condition);
					}
					else
					{
						// Standard attribute-based format.
						attachFunc(n, template, name, condition);
					}
					break;
				}
			}
		}
	}
	
	protected void attachFunc(Node n, Object template, String functionName, Condition attachCond)
	{
		final Stat stat = Stat.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		int order = -1;
		final Node orderNode = n.getAttributes().getNamedItem("order");
		if (orderNode != null)
		{
			order = Integer.parseInt(orderNode.getNodeValue());
		}
		
		final String valueString = n.getAttributes().getNamedItem("val").getNodeValue();
		double value;
		if (valueString.charAt(0) == '#')
		{
			value = Double.parseDouble(getTableValue(valueString));
		}
		else
		{
			value = Double.parseDouble(valueString);
		}
		
		final Condition applyCond = parseCondition(n.getFirstChild(), template);
		final FuncTemplate ft = new FuncTemplate(attachCond, applyCond, functionName, order, stat, value);
		if (template instanceof ItemTemplate)
		{
			((ItemTemplate) template).addFunctionTemplate(ft);
		}
		else
		{
			throw new RuntimeException("Attaching stat to a non-effect template [" + template + "]!!!");
		}
	}
	
	/**
	 * Process the alternative stats format for "share", "enchant", and "enchanthp" tags where stats are defined as element nodes.<br>
	 * For example: &lt;enchant&gt;&lt;pDef&gt;0&lt;/pDef&gt;&lt;/enchant&gt; instead of &lt;enchant stat="pDef" val="0" /&gt;
	 * @param n the node containing stat elements
	 * @param template the template to attach stats to
	 * @param functionName the function name ("share", "enchant" or "enchanthp")
	 * @param attachCond the condition for attachment
	 */
	protected void processAlternativeFuncFormat(Node n, Object template, String functionName, Condition attachCond)
	{
		// Process each child element as a stat.
		Node statNode = n.getFirstChild();
		while (statNode != null)
		{
			// Skip non-element nodes (like whitespace text nodes).
			if (statNode.getNodeType() == Node.ELEMENT_NODE)
			{
				// The element name is the stat name.
				String statName = statNode.getNodeName();
				// The element content is the value.
				String valueString = statNode.getTextContent().trim();
				
				// Convert the stat name to a Stat enum value.
				try
				{
					final Stat stat = Stat.valueOfXml(statName);
					
					// Parse the value.
					double value;
					if (!valueString.isEmpty() && (valueString.charAt(0) == '#'))
					{
						value = Double.parseDouble(getTableValue(valueString));
					}
					else
					{
						value = Double.parseDouble(valueString);
					}
					
					// Use default order (-1).
					final int order = -1;
					
					// Parse any conditions that might be inside the stat element.
					final Condition applyCond = parseCondition(statNode.getFirstChild(), template);
					
					// Create and attach the function template with the original function name (enchant or enchanthp).
					final FuncTemplate ft = new FuncTemplate(attachCond, applyCond, functionName, order, stat, value);
					if (template instanceof ItemTemplate)
					{
						((ItemTemplate) template).addFunctionTemplate(ft);
					}
					else
					{
						throw new RuntimeException("Attaching stat to a non-effect template [" + template + "]!!!");
					}
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("Invalid numeric value: '" + valueString + "' for stat: " + statName + ": " + e.getMessage());
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.warning("Unknown stat name: " + statName + " in alternative " + functionName + " format: " + e.getMessage());
				}
				catch (Exception e)
				{
					LOGGER.warning("Error processing alternative " + functionName + " format for " + statName + ": " + e.getMessage());
				}
			}
			
			// Move to the next stat element.
			statNode = statNode.getNextSibling();
		}
	}
	
	protected Condition parseCondition(Node node, Object template)
	{
		Node n = node;
		while ((n != null) && (n.getNodeType() != Node.ELEMENT_NODE))
		{
			n = n.getNextSibling();
		}
		
		Condition condition = null;
		if (n != null)
		{
			switch (n.getNodeName().toLowerCase())
			{
				case "and":
				{
					condition = parseLogicAnd(n, template);
					break;
				}
				case "or":
				{
					condition = parseLogicOr(n, template);
					break;
				}
				case "not":
				{
					condition = parseLogicNot(n, template);
					break;
				}
				case "player":
				{
					condition = parsePlayerCondition(n, template);
					break;
				}
				case "target":
				{
					condition = parseTargetCondition(n, template);
					break;
				}
				case "using":
				{
					condition = parseUsingCondition(n);
					break;
				}
				case "game":
				{
					condition = parseGameCondition(n);
					break;
				}
			}
		}
		
		return condition;
	}
	
	protected Condition parseLogicAnd(Node node, Object template)
	{
		final ConditionLogicAnd cond = new ConditionLogicAnd();
		Node n = node;
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		
		if ((cond.conditions == null) || (cond.conditions.length == 0))
		{
			LOGGER.severe("Empty <and> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseLogicOr(Node node, Object template)
	{
		final ConditionLogicOr cond = new ConditionLogicOr();
		Node n = node;
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		
		if ((cond.conditions == null) || (cond.conditions.length == 0))
		{
			LOGGER.severe("Empty <or> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseLogicNot(Node node, Object template)
	{
		Node n = node;
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				return new ConditionLogicNot(parseCondition(n, template));
			}
		}
		
		LOGGER.severe("Empty <not> condition in " + _file);
		return null;
	}
	
	protected Condition parsePlayerCondition(Node n, Object template)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			switch (a.getNodeName().toLowerCase())
			{
				case "races":
				{
					final String[] racesVal = a.getNodeValue().split(",");
					final Set<Race> races = EnumSet.noneOf(Race.class);
					for (int r = 0; r < racesVal.length; r++)
					{
						if (racesVal[r] != null)
						{
							races.add(Race.valueOf(racesVal[r]));
						}
					}
					
					cond = joinAnd(cond, new ConditionPlayerRace(races));
					break;
				}
				case "level":
				{
					final int lvl = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerLevel(lvl));
					break;
				}
				case "levelrange":
				{
					final String[] range = getValue(a.getNodeValue(), template).split(";");
					if (range.length == 2)
					{
						final int[] lvlRange = new int[2];
						lvlRange[0] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[0]);
						lvlRange[1] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[1]);
						cond = joinAnd(cond, new ConditionPlayerLevelRange(lvlRange));
					}
					break;
				}
				case "resting":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RESTING, val));
					break;
				}
				case "flying":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FLYING, val));
					break;
				}
				case "moving":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.MOVING, val));
					break;
				}
				case "running":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RUNNING, val));
					break;
				}
				case "standing":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.STANDING, val));
					break;
				}
				case "behind":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.BEHIND, val));
					break;
				}
				case "front":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FRONT, val));
					break;
				}
				case "chaotic":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.CHAOTIC, val));
					break;
				}
				case "olympiad":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerState(PlayerState.OLYMPIAD, val));
					break;
				}
				case "ishero":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerIsHero(val));
					break;
				}
				case "ispvpflagged":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerIsPvpFlagged(val));
					break;
				}
				case "transformationid":
				{
					final int id = Integer.parseInt(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerTransformationId(id));
					break;
				}
				case "hp":
				{
					final int hp = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerHp(hp));
					break;
				}
				case "mp":
				{
					final int mp = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerMp(mp));
					break;
				}
				case "cp":
				{
					final int cp = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerCp(cp));
					break;
				}
				case "pkcount":
				{
					final int expIndex = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerPkCount(expIndex));
					break;
				}
				case "siegezone":
				{
					final int value = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionSiegeZone(value, true));
					break;
				}
				case "siegeside":
				{
					final int value = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerSiegeSide(value));
					break;
				}
				case "charges":
				{
					final int value = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerCharges(value));
					break;
				}
				case "souls":
				{
					final int value = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerSouls(value));
					break;
				}
				case "weight":
				{
					final int weight = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerWeight(weight));
					break;
				}
				case "invsize":
				{
					final int size = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerInvSize(size));
					break;
				}
				case "isclanleader":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerIsClanLeader(val));
					break;
				}
				case "pledgeclass":
				{
					final int pledgeClass = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerPledgeClass(pledgeClass));
					break;
				}
				case "clanhall":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final List<Integer> array = new ArrayList<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						array.add(Integer.decode(getValue(item, template)));
					}
					
					cond = joinAnd(cond, new ConditionPlayerHasClanHall(array));
					break;
				}
				case "fort":
				{
					final int fort = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerHasFort(fort));
					break;
				}
				case "castle":
				{
					final int castle = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerHasCastle(castle));
					break;
				}
				case "sex":
				{
					final int sex = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionPlayerSex(sex));
					break;
				}
				case "flymounted":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerFlyMounted(val));
					break;
				}
				case "vehiclemounted":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerVehicleMounted(val));
					break;
				}
				case "landingzone":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerLandingZone(val));
					break;
				}
				case "active_effect_id":
				{
					final int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id));
					break;
				}
				case "active_effect_id_lvl":
				{
					final String val = getValue(a.getNodeValue(), template);
					final int effect_id = Integer.decode(getValue(val.split(",")[0], template));
					final int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
					cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id, effect_lvl));
					break;
				}
				case "active_skill_id":
				{
					final int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id));
					break;
				}
				case "active_skill_id_lvl":
				{
					final String val = getValue(a.getNodeValue(), template);
					final int skill_id = Integer.decode(getValue(val.split(",")[0], template));
					final int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
					cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id, skill_lvl));
					break;
				}
				case "class_id_restriction":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final Set<Integer> array = new HashSet<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						array.add(Integer.decode(getValue(item, template)));
					}
					
					cond = joinAnd(cond, new ConditionPlayerClassIdRestriction(array));
					break;
				}
				case "subclass":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerSubclass(val));
					break;
				}
				case "dualclass":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerDualclass(val));
					break;
				}
				case "canswitchsubclass":
				{
					cond = joinAnd(cond, new ConditionPlayerCanSwitchSubclass(Integer.decode(a.getNodeValue())));
					break;
				}
				case "instanceid":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final Set<Integer> set = new HashSet<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						set.add(Integer.decode(getValue(item, template)));
					}
					
					cond = joinAnd(cond, new ConditionPlayerInstanceId(set));
					break;
				}
				case "agathionid":
				{
					final int agathionId = Integer.decode(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerAgathionId(agathionId));
					break;
				}
				case "cloakstatus":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerCloakStatus(val));
					break;
				}
				case "hassummon":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionPlayerHasSummon(val));
					break;
				}
				case "haspet":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final List<Integer> array = new ArrayList<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						array.add(Integer.decode(getValue(item, template)));
					}
					
					cond = joinAnd(cond, new ConditionPlayerHasPet(array));
					break;
				}
				case "servitornpcid":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final List<Integer> array = new ArrayList<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						array.add(Integer.decode(getValue(item, null)));
					}
					
					cond = joinAnd(cond, new ConditionPlayerServitorNpcId(array));
					break;
				}
				case "npcidradius":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					if (st.countTokens() == 3)
					{
						final String[] ids = st.nextToken().split(";");
						final Set<Integer> npcIds = new HashSet<>(ids.length);
						for (int index = 0; index < ids.length; index++)
						{
							npcIds.add(Integer.parseInt(getValue(ids[index], template)));
						}
						
						final int radius = Integer.parseInt(st.nextToken());
						final boolean val = Boolean.parseBoolean(st.nextToken());
						cond = joinAnd(cond, new ConditionPlayerRangeFromNpc(npcIds, radius, val));
					}
					break;
				}
				case "summonednpcidradius":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					if (st.countTokens() == 3)
					{
						final String[] ids = st.nextToken().split(";");
						final Set<Integer> npcIds = new HashSet<>(ids.length);
						for (int index = 0; index < ids.length; index++)
						{
							npcIds.add(Integer.parseInt(getValue(ids[index], template)));
						}
						
						final int radius = Integer.parseInt(st.nextToken());
						final boolean val = Boolean.parseBoolean(st.nextToken());
						cond = joinAnd(cond, new ConditionPlayerRangeFromSummonedNpc(npcIds, radius, val));
					}
					break;
				}
				case "callpc":
				{
					cond = joinAnd(cond, new ConditionPlayerCallPc(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cancreatebase":
				{
					cond = joinAnd(cond, new ConditionPlayerCanCreateBase(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "canescape":
				{
					cond = joinAnd(cond, new ConditionPlayerCanEscape(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "canrefuelairship":
				{
					cond = joinAnd(cond, new ConditionPlayerCanRefuelAirship(Integer.parseInt(a.getNodeValue())));
					break;
				}
				case "canresurrect":
				{
					cond = joinAnd(cond, new ConditionPlayerCanResurrect(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cansummonpet":
				{
					cond = joinAnd(cond, new ConditionPlayerCanSummonPet(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cansummonservitor":
				{
					cond = joinAnd(cond, new ConditionPlayerCanSummonServitor(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "hasfreesummonpoints":
				{
					cond = joinAnd(cond, new ConditionPlayerHasFreeSummonPoints(Integer.parseInt(a.getNodeValue())));
					break;
				}
				case "hasfreeteleportbookmarkslots":
				{
					cond = joinAnd(cond, new ConditionPlayerHasFreeTeleportBookmarkSlots(Integer.parseInt(a.getNodeValue())));
					break;
				}
				case "cansummonsiegegolem":
				{
					cond = joinAnd(cond, new ConditionPlayerCanSummonSiegeGolem(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cansweep":
				{
					cond = joinAnd(cond, new ConditionPlayerCanSweep(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cantakecastle":
				{
					cond = joinAnd(cond, new ConditionPlayerCanTakeCastle(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cantakefort":
				{
					cond = joinAnd(cond, new ConditionPlayerCanTakeFort(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "cantransform":
				{
					cond = joinAnd(cond, new ConditionPlayerCanTransform(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "canuntransform":
				{
					cond = joinAnd(cond, new ConditionPlayerCanUntransform(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "insidezoneid":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final Set<Integer> set = new HashSet<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						set.add(Integer.decode(getValue(item, template)));
					}
					
					cond = joinAnd(cond, new ConditionPlayerInsideZoneId(set));
					break;
				}
				case "checkabnormal":
				{
					final String value = a.getNodeValue();
					if (value.contains(","))
					{
						final String[] values = value.split(",");
						cond = joinAnd(cond, new ConditionPlayerCheckAbnormal(AbnormalType.valueOf(values[0]), Integer.decode(getValue(values[1], template))));
					}
					else
					{
						cond = joinAnd(cond, new ConditionPlayerCheckAbnormal(AbnormalType.valueOf(value)));
					}
					break;
				}
				case "categorytype":
				{
					final String[] values = a.getNodeValue().split(",");
					final Set<CategoryType> array = new HashSet<>(values.length);
					for (String value : values)
					{
						array.add(CategoryType.valueOf(getValue(value, template)));
					}
					
					cond = joinAnd(cond, new ConditionCategoryType(array));
					break;
				}
				case "immobile":
				{
					cond = joinAnd(cond, new ConditionPlayerImmobile(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "incombat":
				{
					cond = joinAnd(cond, new ConditionPlayerIsInCombat(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "isonside":
				{
					cond = joinAnd(cond, new ConditionPlayerIsOnSide(Enum.valueOf(CastleSide.class, a.getNodeValue())));
					break;
				}
				case "ininstance":
				{
					cond = joinAnd(cond, new ConditionPlayerInInstance(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "minimumvitalitypoints":
				{
					final int count = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionMinimumVitalityPoints(count));
					break;
				}
			}
		}
		
		if (cond == null)
		{
			LOGGER.severe("Unrecognized <player> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseTargetCondition(Node n, Object template)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			switch (a.getNodeName().toLowerCase())
			{
				case "aggro":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionTargetAggro(val));
					break;
				}
				case "siegezone":
				{
					final int value = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionSiegeZone(value, false));
					break;
				}
				case "level":
				{
					final int lvl = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionTargetLevel(lvl));
					break;
				}
				case "levelrange":
				{
					final String[] range = getValue(a.getNodeValue(), template).split(";");
					if (range.length == 2)
					{
						final int[] lvlRange = new int[2];
						lvlRange[0] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[0]);
						lvlRange[1] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[1]);
						cond = joinAnd(cond, new ConditionTargetLevelRange(lvlRange));
					}
					break;
				}
				case "mypartyexceptme":
				{
					cond = joinAnd(cond, new ConditionTargetMyPartyExceptMe(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
				case "playable":
				{
					cond = joinAnd(cond, new ConditionTargetPlayable());
					break;
				}
				case "player":
				{
					cond = joinAnd(cond, new ConditionTargetPlayer());
					break;
				}
				case "class_id_restriction":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final Set<Integer> set = new HashSet<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						set.add(Integer.decode(getValue(item, null)));
					}
					
					cond = joinAnd(cond, new ConditionTargetClassIdRestriction(set));
					break;
				}
				case "active_effect_id":
				{
					final int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id));
					break;
				}
				case "active_effect_id_lvl":
				{
					final String val = getValue(a.getNodeValue(), template);
					final int effect_id = Integer.decode(getValue(val.split(",")[0], template));
					final int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
					cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id, effect_lvl));
					break;
				}
				case "active_skill_id":
				{
					final int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id));
					break;
				}
				case "active_skill_id_lvl":
				{
					final String val = getValue(a.getNodeValue(), template);
					final int skill_id = Integer.decode(getValue(val.split(",")[0], template));
					final int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
					cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id, skill_lvl));
					break;
				}
				case "abnormaltype":
				{
					final AbnormalType abnormalType = AbnormalType.getAbnormalType(getValue(a.getNodeValue(), template));
					cond = joinAnd(cond, new ConditionTargetAbnormalType(abnormalType));
					break;
				}
				case "mindistance":
				{
					final int distance = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionMinDistance(distance));
					break;
				}
				case "race":
				{
					cond = joinAnd(cond, new ConditionTargetRace(Race.valueOf(a.getNodeValue())));
					break;
				}
				case "using":
				{
					int mask = 0;
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						for (WeaponType wt : WeaponType.values())
						{
							if (wt.name().equals(item))
							{
								mask |= wt.mask();
								break;
							}
						}
						
						for (ArmorType at : ArmorType.values())
						{
							if (at.name().equals(item))
							{
								mask |= at.mask();
								break;
							}
						}
					}
					
					cond = joinAnd(cond, new ConditionTargetUsesWeaponKind(mask));
					break;
				}
				case "npcid":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					final Set<Integer> set = new HashSet<>(st.countTokens());
					while (st.hasMoreTokens())
					{
						final String item = st.nextToken().trim();
						set.add(Integer.decode(getValue(item, null)));
					}
					
					cond = joinAnd(cond, new ConditionTargetNpcId(set));
					break;
				}
				case "npctype":
				{
					final String values = getValue(a.getNodeValue(), template).trim();
					final String[] valuesSplit = values.split(",");
					final InstanceType[] types = new InstanceType[valuesSplit.length];
					InstanceType type;
					for (int j = 0; j < valuesSplit.length; j++)
					{
						type = Enum.valueOf(InstanceType.class, valuesSplit[j]);
						if (type == null)
						{
							throw new IllegalArgumentException("Instance type not recognized: " + valuesSplit[j]);
						}
						
						types[j] = type;
					}
					
					cond = joinAnd(cond, new ConditionTargetNpcType(types));
					break;
				}
				case "weight":
				{
					final int weight = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionTargetWeight(weight));
					break;
				}
				case "invsize":
				{
					final int size = Integer.decode(getValue(a.getNodeValue(), null));
					cond = joinAnd(cond, new ConditionTargetInvSize(size));
					break;
				}
				case "checkcrteffect":
				{
					cond = joinAnd(cond, new ConditionTargetCheckCrtEffect(Boolean.parseBoolean(a.getNodeValue())));
					break;
				}
			}
		}
		
		if (cond == null)
		{
			LOGGER.severe("Unrecognized <target> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseUsingCondition(Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			switch (a.getNodeName().toLowerCase())
			{
				case "kind":
				{
					int mask = 0;
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					while (st.hasMoreTokens())
					{
						final int old = mask;
						final String item = st.nextToken().trim();
						for (WeaponType wt : WeaponType.values())
						{
							if (wt.name().equals(item))
							{
								mask |= wt.mask();
							}
						}
						
						for (ArmorType at : ArmorType.values())
						{
							if (at.name().equals(item))
							{
								mask |= at.mask();
							}
						}
						
						if (old == mask)
						{
							LOGGER.info("[parseUsingCondition=\"kind\"] Unknown item type name: " + item);
						}
					}
					
					cond = joinAnd(cond, new ConditionUsingItemType(mask));
					break;
				}
				case "slot":
				{
					int mask = 0;
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
					while (st.hasMoreTokens())
					{
						final int old = mask;
						final String item = st.nextToken().trim();
						final BodyPart bodyPart = BodyPart.fromName(item);
						if (bodyPart != null)
						{
							mask |= bodyPart.getMask();
						}
						
						if (old == mask)
						{
							LOGGER.info("[parseUsingCondition=\"slot\"] Unknown item slot name: " + item);
						}
					}
					
					cond = joinAnd(cond, new ConditionUsingSlotType(mask));
					break;
				}
				case "skill":
				{
					final int id = Integer.parseInt(a.getNodeValue());
					cond = joinAnd(cond, new ConditionUsingSkill(id));
					break;
				}
				case "slotitem":
				{
					final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
					final int id = Integer.parseInt(st.nextToken().trim());
					final int slot = Integer.parseInt(st.nextToken().trim());
					int enchant = 0;
					if (st.hasMoreTokens())
					{
						enchant = Integer.parseInt(st.nextToken().trim());
					}
					
					cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
					break;
				}
				case "weaponchange":
				{
					final boolean val = Boolean.parseBoolean(a.getNodeValue());
					cond = joinAnd(cond, new ConditionChangeWeapon(val));
					break;
				}
			}
		}
		
		if (cond == null)
		{
			LOGGER.severe("Unrecognized <using> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseGameCondition(Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionWithSkill(val));
			}
			
			if ("night".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(val));
			}
			
			if ("chance".equalsIgnoreCase(a.getNodeName()))
			{
				final int val = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionGameChance(val));
			}
		}
		
		if (cond == null)
		{
			LOGGER.severe("Unrecognized <game> condition in " + _file);
		}
		
		return cond;
	}
	
	protected void parseTable(Node n)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		if (name.charAt(0) != '#')
		{
			throw new IllegalArgumentException("Table name must start with #");
		}
		
		final StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
		final List<String> array = new ArrayList<>(data.countTokens());
		while (data.hasMoreTokens())
		{
			array.add(data.nextToken());
		}
		
		setTable(name, array.toArray(new String[0]));
	}
	
	protected void parseBeanSet(Node n, StatSet set, Integer level)
	{
		final String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		final String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		final char ch = value.isEmpty() ? ' ' : value.charAt(0);
		if ((ch == '#') || (ch == '-') || Character.isDigit(ch))
		{
			set.set(name, getValue(value, level));
		}
		else
		{
			set.set(name, value);
		}
	}
	
	/**
	 * Parse an XML element with its value directly inside the element Example: <reuseDelay>3000</reuseDelay>
	 * @param n the XML node to parse
	 * @param set the StatSet to store the data into
	 * @param level the current level
	 */
	protected void parseElementValue(Node n, StatSet set, Integer level)
	{
		final String name = n.getNodeName().trim();
		
		// The value is the text content of the node.
		final String value = n.getTextContent().trim();
		final char ch = value.isEmpty() ? ' ' : value.charAt(0);
		if ((ch == '#') || (ch == '-') || Character.isDigit(ch))
		{
			set.set(name, getValue(value, level));
		}
		else
		{
			set.set(name, value);
		}
	}
	
	protected String getValue(String value, Object template)
	{
		// Is it a table?
		if ((value != null) && !value.isEmpty() && (value.charAt(0) == '#'))
		{
			if (template instanceof Skill)
			{
				return getTableValue(value);
			}
			else if (template instanceof Integer)
			{
				return getTableValue(value, ((Integer) template).intValue());
			}
			else
			{
				throw new IllegalStateException();
			}
		}
		
		return value;
	}
	
	protected Condition joinAnd(Condition cond, Condition c)
	{
		if (cond == null)
		{
			return c;
		}
		
		if (cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		
		final ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}
