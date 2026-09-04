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
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Mobius
 */
public class LimitShopData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(LimitShopData.class.getName());
	
	private final List<LimitShopProductHolder> _products = new ArrayList<>();
	
	protected LimitShopData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_products.clear();
		parseDatapackFile("data/LimitShop.xml");
		
		if (!_products.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _products.size() + " items.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (!"list".equalsIgnoreCase(n.getNodeName()))
			{
				continue;
			}
			
			final NamedNodeMap at = n.getAttributes();
			final Node attribute = at.getNamedItem("enabled");
			if ((attribute == null) || !Boolean.parseBoolean(attribute.getNodeValue()))
			{
				continue;
			}
			
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (!"product".equalsIgnoreCase(d.getNodeName()))
				{
					continue;
				}
				
				NamedNodeMap attrs = d.getAttributes();
				final StatSet set = new StatSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					Node att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				
				final int id = parseInteger(attrs, "id");
				final int category = parseInteger(attrs, "category");
				final int minLevel = parseInteger(attrs, "minLevel", 1);
				final int maxLevel = parseInteger(attrs, "maxLevel", 999);
				
				// Java automatically initializes arrays with 0/null values.
				final int[] ingredientIds = new int[8];
				final long[] ingredientQuantities = new long[8];
				final int[] ingredientEnchants = new int[8];
				
				int productionId = 0;
				int productionCount = 0;
				int productionEnchant2 = 0;
				int accountDailyLimit = 0;
				int accountWeeklyLimit = 0;
				int accountMonthlyLimit = 0;
				int accountBuyLimit = 0;
				
				int ingredientIdx = 0;
				
				for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
				{
					attrs = b.getAttributes();
					final String nodeName = b.getNodeName();
					
					if ("ingredient".equalsIgnoreCase(nodeName))
					{
						final int ingredientId = parseInteger(attrs, "id");
						final long ingredientQuantity = parseLong(attrs, "count", 1L);
						final int ingredientEnchant = parseInteger(attrs, "enchant", 0);
						
						if (ingredientId > 0)
						{
							final ItemTemplate template = ItemData.getInstance().getTemplate(ingredientId);
							if (template == null)
							{
								LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + ingredientId + " in productId: " + id);
								continue;
							}
							
							if ((ingredientQuantity > 1) && !template.isStackable() && !template.isEquipable())
							{
								LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + ingredientId + " should be stackable!");
							}
							
							// Correctly fill the arrays using an index pointer.
							if (ingredientIdx < 8)
							{
								ingredientIds[ingredientIdx] = ingredientId;
								ingredientQuantities[ingredientIdx] = ingredientQuantity;
								ingredientEnchants[ingredientIdx] = ingredientEnchant;
								ingredientIdx++;
							}
						}
					}
					else if ("production".equalsIgnoreCase(nodeName))
					{
						productionId = parseInteger(attrs, "id");
						productionCount = parseInteger(attrs, "count", 1);
						productionEnchant2 = parseInteger(attrs, "enchant2", 0);
						accountDailyLimit = parseInteger(attrs, "accountDailyLimit", 0);
						accountWeeklyLimit = parseInteger(attrs, "accountWeeklyLimit", 0);
						accountMonthlyLimit = parseInteger(attrs, "accountMonthlyLimit", 0);
						accountBuyLimit = parseInteger(attrs, "accountBuyLimit", 0);
						
						if (ItemData.getInstance().getTemplate(productionId) == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Production item template null for itemId: " + productionId + " productId: " + id);
						}
					}
				}
				
				_products.add(new LimitShopProductHolder(id, category, minLevel, maxLevel, ingredientIds, ingredientQuantities, ingredientEnchants, productionId, productionCount, 100, false, 0, 0, productionEnchant2, 0, 0, false, 0, 0, 0, 0, false, 0, 0, 0, 0, false, 0, 0, 0, 0f, false, accountDailyLimit, accountWeeklyLimit, accountMonthlyLimit, accountBuyLimit, false, 0L, null, null));
			}
		}
	}
	
	public LimitShopProductHolder getProduct(int id)
	{
		for (LimitShopProductHolder product : _products)
		{
			if (product.getId() == id)
			{
				return product;
			}
		}
		
		return null;
	}
	
	public List<LimitShopProductHolder> getProducts()
	{
		return _products;
	}
	
	public static LimitShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LimitShopData INSTANCE = new LimitShopData();
	}
}