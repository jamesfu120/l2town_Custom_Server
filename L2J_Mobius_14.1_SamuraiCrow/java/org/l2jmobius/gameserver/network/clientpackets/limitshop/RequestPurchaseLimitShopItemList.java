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
 * @author Mobius, GM Fix
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
			
			// 👑 GM 終極完美解鎖補丁：利用父類別反射，精準強行清洗全商城商品等級限制！
			for (LimitShopProductHolder prod : productList)
			{
				try
				{
					// 先試著抓父類別的等級格子
					java.lang.reflect.Field minLvlField = prod.getClass().getSuperclass().getDeclaredField("_minLevel");
					minLvlField.setAccessible(true);
					minLvlField.setInt(prod, 1);
					
					java.lang.reflect.Field maxLvlField = prod.getClass().getSuperclass().getDeclaredField("_maxLevel");
					maxLvlField.setAccessible(true);
					maxLvlField.setInt(prod, 999);
				}
				catch (Exception e)
				{
					try
					{
						// 如果沒有父類別，直接抓本體
						java.lang.reflect.Field minLvlField = prod.getClass().getDeclaredField("_minLevel");
						minLvlField.setAccessible(true);
						minLvlField.setInt(prod, 1);
						
						java.lang.reflect.Field maxLvlField = prod.getClass().getDeclaredField("_maxLevel");
						maxLvlField.setAccessible(true);
						maxLvlField.setInt(prod, 999);
					}
					catch (Exception ex)
					{
						// 防禦報錯
					}
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

