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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulFee;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulStone;

/**
 * @author UnAfraid, BazookaRpm
 */
public class EnsoulData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnsoulData.class.getName());
	private final Map<CrystalType, EnsoulFee> _ensoulFees = new ConcurrentHashMap<>();
	private final Map<Integer, EnsoulOption> _ensoulOptions = new ConcurrentHashMap<>();
	private final Map<Integer, EnsoulStone> _ensoulStones = new ConcurrentHashMap<>();
	
	protected EnsoulData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackDirectory("data/stats/ensoul", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ensoulFees.size() + " fees.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ensoulOptions.size() + " options.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ensoulStones.size() + " stones.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, IXmlReader::isNode, ensoulNode ->
		{
			switch (ensoulNode.getNodeName())
			{
				case "fee":
				{
					parseFees(ensoulNode);
					break;
				}
				case "option":
				{
					parseOptions(ensoulNode);
					break;
				}
				case "stone":
				{
					parseStones(ensoulNode);
					break;
				}
			}
		}));
	}
	
	private void parseFees(Node ensoulNode)
	{
		final CrystalType type = parseEnum(ensoulNode.getAttributes(), CrystalType.class, "crystalType");
		final EnsoulFee fee = new EnsoulFee(type);
		forEach(ensoulNode, IXmlReader::isNode, feeNode ->
		{
			switch (feeNode.getNodeName())
			{
				case "first":
				{
					parseFee(feeNode, fee, 0);
					break;
				}
				case "secondary":
				{
					parseFee(feeNode, fee, 1);
					break;
				}
				case "third":
				{
					parseFee(feeNode, fee, 2);
					break;
				}
				case "reNormal":
				{
					parseReFee(feeNode, fee, 0);
					break;
				}
				case "reSecondary":
				{
					parseReFee(feeNode, fee, 1);
					break;
				}
				case "reThird":
				{
					parseReFee(feeNode, fee, 2);
					break;
				}
			}
		});
	}
	
	private void parseFee(Node ensoulNode, EnsoulFee fee, int index)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.setEnsoul(index, new ItemHolder(id, count));
		_ensoulFees.put(fee.getCrystalType(), fee);
	}
	
	private void parseReFee(Node ensoulNode, EnsoulFee fee, int index)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.setResoul(index, new ItemHolder(id, count));
	}
	
	private void parseOptions(Node ensoulNode)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "id");
		final String name = parseString(attrs, "name");
		final String desc = parseString(attrs, "desc");
		final int skillId = parseInteger(attrs, "skillId");
		final int skillLevel = parseInteger(attrs, "skillLevel");
		final EnsoulOption option = new EnsoulOption(id, name, desc, skillId, skillLevel);
		_ensoulOptions.put(option.getId(), option);
	}
	
	private void parseStones(Node ensoulNode)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "id");
		final int slotType = parseInteger(attrs, "slotType");
		final EnsoulStone stone = new EnsoulStone(id, slotType);
		forEach(ensoulNode, "option", optionNode -> stone.addOption(parseInteger(optionNode.getAttributes(), "id")));
		_ensoulStones.put(stone.getId(), stone);
	}
	
	public ItemHolder getEnsoulFee(CrystalType type, int index)
	{
		final EnsoulFee fee = _ensoulFees.get(type);
		return fee != null ? fee.getEnsoul(index) : null;
	}
	
	public ItemHolder getResoulFee(CrystalType type, int index)
	{
		final EnsoulFee fee = _ensoulFees.get(type);
		return fee != null ? fee.getResoul(index) : null;
	}
	
	public EnsoulOption getOption(int id)
	{
		return _ensoulOptions.get(id);
	}
	
	public EnsoulStone getStone(int id)
	{
		return _ensoulStones.get(id);
	}
	
	/**
	 * Gets the single instance of EnsoulData.
	 * @return single instance of EnsoulData
	 */
	public static EnsoulData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnsoulData INSTANCE = new EnsoulData();
	}
}
