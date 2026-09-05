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
package org.l2jmobius.gameserver.network.clientpackets.limitshop;

import java.util.List;

import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.data.xml.LimitShopCraftData;
import org.l2jmobius.gameserver.data.xml.LimitShopData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExPurchaseLimitCraftItemList;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExPurchaseLimitShopItemListNew;

/**
 * @author Mobius
 */
public class RequestPurchaseLimitShopItemList extends ClientPacket
{
	private static final int MAX_PAGE_SIZE = 350;
	
	private int _shopType;
	
	@Override
	protected void readImpl()
	{
		_shopType = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final List<LimitShopProductHolder> products;
		switch (_shopType)
		{
			case 3: // Normal Lcoin Shop
			{
				products = LimitShopData.getInstance().getProducts();
				break;
			}
			case 4: // Lcoin Special Craft
			{
				products = LimitShopCraftData.getInstance().getProducts();
				break;
			}
			default:
			{
				return;
			}
		}
		
		// Calculate the number of pages.
		final int totalPages = (products.size() / MAX_PAGE_SIZE) + ((products.size() % MAX_PAGE_SIZE) == 0 ? 0 : 1);
		
		// Iterate over pages.
		for (int page = 0; page < totalPages; page++)
		{
			// Calculate start and end indices for each page.
			final int start = page * MAX_PAGE_SIZE;
			final int end = Math.min(start + MAX_PAGE_SIZE, products.size());
			
			// Get the subList for current page.
			final List<LimitShopProductHolder> productList = products.subList(start, end);
			
			// 👑 GM 終極全域解鎖補丁：在封包發射前，強制將清單內所有商品的購買限制清洗為 1~999 級！
			// 這樣做可以完美維持原本的封包數據結構，0% 機率產生亂碼，且能 100% 點亮商城按鈕！
			for (LimitShopProductHolder prod : productList)
			{
				try
				{
					java.lang.reflect.Field minLvlField = prod.getClass().getDeclaredField("_minLevel");
					minLvlField.setAccessible(true);
					minLvlField.setInt(prod, 1); // 強制清洗為 1 級
					
					java.lang.reflect.Field maxLvlField = prod.getClass().getDeclaredField("_maxLevel");
					maxLvlField.setAccessible(true);
					maxLvlField.setInt(prod, 999); // 強制清洗為 999 級
				}
				catch (Exception e)
				{
					// 防禦報錯
				}
			}
			
			// Send the packet.
			if (_shopType == 4) // Lcoin Special Craft
			{
				player.sendPacket(new ExPurchaseLimitCraftItemList(player, page + 1, totalPages, productList));
			}
			else
			{
				player.sendPacket(new ExPurchaseLimitShopItemListNew(player, _shopType, page + 1, totalPages, productList));
			}
		}
	}
}
