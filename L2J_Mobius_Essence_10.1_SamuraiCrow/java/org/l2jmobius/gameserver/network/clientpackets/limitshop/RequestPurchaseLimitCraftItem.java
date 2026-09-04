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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.VipSystemConfig;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.data.holders.LimitShopRandomCraftReward;
import org.l2jmobius.gameserver.data.xml.LimitShopCraftData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.PrimeShopRequest;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.ExBrProductReplyType;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExPurchaseLimitShopItemResult;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRBuyProduct;

/**
 * @author Galagard
 */
public class RequestPurchaseLimitCraftItem extends ClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestPurchaseLimitCraftItem.class.getName());
	
	private int _productId;
	private int _amount;
	private int _successionItemSID;
	private int _materialItemSID;
	private int _cost1SID;
	private int _cost2SID;
	private int _cost3SID;
	private int _cost4SID;
	private int _cost5SID;
	private LimitShopProductHolder _product;
	
	private int getSIDForSlot(int slot)
	{
		switch (slot)
		{
			case 0:
			{
				return _cost1SID;
			}
			case 1:
			{
				return _cost2SID;
			}
			case 2:
			{
				return _cost3SID;
			}
			case 3:
			{
				return _cost4SID;
			}
			case 4:
			{
				return _cost5SID;
			}
			default:
			{
				return 0;
			}
		}
	}
	
	@Override
	protected void readImpl()
	{
		_productId = readInt();
		_amount = readInt();
		_successionItemSID = readInt();
		_materialItemSID = readInt();
		_cost1SID = readInt();
		_cost2SID = readInt();
		_cost3SID = readInt();
		_cost4SID = readInt();
		_cost5SID = readInt();
		_product = LimitShopCraftData.getInstance().getProduct(_productId);
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_product == null)
		{
			LOGGER.warning("LimitShopCraft: product not found id=" + _productId);
			return;
		}
		
		if ((_amount < 1) || (_amount > 10000))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
			return;
		}
		
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
			return;
		}
		
		if ((player.getLevel() < _product.getMinLevel()) || (player.getLevel() > _product.getMaxLevel()))
		{
			player.sendPacket(SystemMessageId.YOUR_LEVEL_CANNOT_PURCHASE_THIS_ITEM);
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
			return;
		}
		
		if (player.hasItemRequest() || player.hasRequest(PrimeShopRequest.class))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
			return;
		}
		
		player.addRequest(new PrimeShopRequest(player));
		
		// Check limits.
		if (_product.getAccountDailyLimit() > 0)
		{
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _productId, 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountDailyLimit())
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_NO_LIMITED_CRAFT_ATTEMPTS_LEFT);
				player.sendMessage("You have reached your daily limit.");
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, Math.max(_product.getAccountDailyLimit() - currentPurchaseCount, 0), Collections.emptyList()));
				return;
			}
		}
		else if (_product.getAccountWeeklyLimit() > 0)
		{
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _productId, 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountWeeklyLimit())
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_NO_LIMITED_CRAFT_ATTEMPTS_LEFT);
				player.sendMessage("You have reached your weekly limit.");
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, Math.max(_product.getAccountWeeklyLimit() - currentPurchaseCount, 0), Collections.emptyList()));
				return;
			}
		}
		else if (_product.getAccountMonthlyLimit() > 0)
		{
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _productId, 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountMonthlyLimit())
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_NO_LIMITED_CRAFT_ATTEMPTS_LEFT);
				player.sendMessage("You have reached your monthly limit.");
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, Math.max(_product.getAccountMonthlyLimit() - currentPurchaseCount, 0), Collections.emptyList()));
				return;
			}
		}
		else if (_product.getAccountBuyLimit() > 0)
		{
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _productId, 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountBuyLimit())
			{
				player.sendPacket(SystemMessageId.FAILED_TO_BUY_PLEASE_REFRESH_AND_TRY_AGAIN);
				player.sendMessage("You cannot buy any more of this item.");
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, Math.max(_product.getAccountBuyLimit() - currentPurchaseCount, 0), Collections.emptyList()));
				return;
			}
		}
		
		// Check existing items.
		final boolean[] validatedSlots = new boolean[5];
		for (int i = 0; i < _product.getIngredientIds().length; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			
			final int ingredientSlot = _product.getIngredientSlot(i);
			if ((ingredientSlot >= 0) && validatedSlots[ingredientSlot])
			{
				continue;
			}
			
			final int ingredientId = _product.getIngredientIds()[i];
			final long requiredAmount = _product.getIngredientQuantities()[i] * _amount;
			final int requiredEnchant = _product.getIngredientEnchants()[i];
			final int sid = (ingredientSlot >= 0) ? getSIDForSlot(ingredientSlot) : 0;
			
			if (ingredientId == Inventory.ADENA_ID)
			{
				if (player.getAdena() < requiredAmount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
			}
			else if (ingredientId == SpecialItemType.HONOR_COINS.getClientId())
			{
				if (player.getHonorCoins() < requiredAmount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
			}
			else if (ingredientId == SpecialItemType.PC_CAFE_POINTS.getClientId())
			{
				if (player.getPcCafePoints() < requiredAmount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
			}
			else if ((ingredientSlot >= 0) && (sid > 0))
			{
				final Item item = player.getInventory().getItemByObjectId(sid);
				if (item == null)
				{
					player.sendMessage("Selected item not found in your inventory!");
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
				
				boolean isValidItem = false;
				for (int validId : _product.getIngredientIds())
				{
					if ((validId > 0) && (item.getId() == validId))
					{
						isValidItem = true;
						break;
					}
				}
				
				if (!isValidItem)
				{
					player.sendPacket(SystemMessageId.THIS_IS_NOT_A_VALID_COMBINATION);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
				
				if ((requiredEnchant > 0) && (item.getEnchantLevel() < requiredEnchant))
				{
					player.sendPacket(SystemMessageId.THE_ITEM_ENCHANTMENT_ERROR_PLEASE_TRY_AGAIN);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
				
				if (item.getCount() < requiredAmount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
				
				validatedSlots[ingredientSlot] = true;
			}
			else
			{
				final long inventoryCount = player.getInventory().getInventoryItemCount(ingredientId, requiredEnchant == 0 ? -1 : requiredEnchant, false);
				if (inventoryCount < requiredAmount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
					return;
				}
			}
		}
		
		// KeepOption: succession item transfers its augment to the result.
		// Fee is charged only when succession item has augmentation.
		// If only material has augmentation, it is deleted with no fee.
		Item augmentedItem = null;
		int successionEnchant = _product.getEnchant();
		if (_product.hasKeepOption() && (_successionItemSID > 0))
		{
			final Item successionItem = player.getInventory().getItemByObjectId(_successionItemSID);
			if (successionItem != null)
			{
				successionEnchant = successionItem.getEnchantLevel();
				if (successionItem.getAugmentation() != null)
				{
					augmentedItem = successionItem;
					if (_product.getKeepOptionFeeAmount() > 0)
					{
						if (!player.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, _product.getKeepOptionFeeAmount(), player, true))
						{
							player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
							player.sendMessage("Not enough Adena to pay augmentation transfer fee!");
							player.removeRequest(PrimeShopRequest.class);
							player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
							return;
						}
					}
				}
			}
		}
		else if (_product.hasKeepOption() && (_materialItemSID > 0))
		{
			final Item materialItem = player.getInventory().getItemByObjectId(_materialItemSID);
			if ((materialItem != null) && (materialItem.getAugmentation() != null))
			{
				augmentedItem = materialItem;
				if (_product.getKeepOptionFeeAmount() > 0)
				{
					if (!player.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, _product.getKeepOptionFeeAmount(), player, true))
					{
						player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
						player.sendMessage("Not enough Adena to pay augmentation transfer fee!");
						player.removeRequest(PrimeShopRequest.class);
						player.sendPacket(new ExPurchaseLimitShopItemResult(false, 4, _productId, 0, Collections.emptyList()));
						return;
					}
				}
			}
		}
		
		// Remove items.
		final boolean[] destroyedSlots = new boolean[5];
		for (int i = 0; i < _product.getIngredientIds().length; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			
			final int ingredientSlot = _product.getIngredientSlot(i);
			if ((ingredientSlot >= 0) && destroyedSlots[ingredientSlot])
			{
				continue;
			}
			
			final int ingredientId = _product.getIngredientIds()[i];
			final long ingredientQuantity = _product.getIngredientQuantities()[i];
			final int sid = (ingredientSlot >= 0) ? getSIDForSlot(ingredientSlot) : 0;
			
			if (ingredientId == Inventory.ADENA_ID)
			{
				player.reduceAdena(ItemProcessType.FEE, ingredientQuantity * _amount, player, true);
			}
			else if (ingredientId == SpecialItemType.HONOR_COINS.getClientId())
			{
				player.setHonorCoins(player.getHonorCoins() - (ingredientQuantity * _amount));
			}
			else if (ingredientId == SpecialItemType.PC_CAFE_POINTS.getClientId())
			{
				final int newPoints = (int) (player.getPcCafePoints() - (ingredientQuantity * _amount));
				player.setPcCafePoints(newPoints);
				player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), (int) (-(ingredientQuantity * _amount)), 1));
			}
			else if ((ingredientSlot >= 0) && (sid > 0))
			{
				final Item item = player.getInventory().getItemByObjectId(sid);
				player.destroyItem(ItemProcessType.FEE, item, ingredientQuantity * _amount, player, true);
				destroyedSlots[ingredientSlot] = true;
			}
			else
			{
				if (_product.getIngredientEnchants()[i] > 0)
				{
					long remaining = ingredientQuantity * _amount;
					final Collection<Item> items = player.getInventory().getAllItemsByItemId(ingredientId, _product.getIngredientEnchants()[i]);
					for (Item item : items)
					{
						if (remaining <= 0)
						{
							break;
						}
						final long toDestroy = Math.min(item.getCount(), remaining);
						player.destroyItem(ItemProcessType.FEE, item, toDestroy, player, true);
						remaining -= toDestroy;
					}
				}
				else
				{
					player.destroyItemByItemId(ItemProcessType.FEE, ingredientId, ingredientQuantity * _amount, player, true);
				}
			}
			
			if (VipSystemConfig.VIP_SYSTEM_L_SHOP_AFFECT)
			{
				player.updateVipPoints(_amount);
			}
		}
		
		// Succession system: destroy both succession and material items separately.
		if (_product.hasKeepOption() && (_successionItemSID > 0))
		{
			final Item successionItem = player.getInventory().getItemByObjectId(_successionItemSID);
			if (successionItem != null)
			{
				player.destroyItem(ItemProcessType.FEE, successionItem, 1, player, true);
			}
			final Item materialItem = player.getInventory().getItemByObjectId(_materialItemSID);
			if (materialItem != null)
			{
				player.destroyItem(ItemProcessType.FEE, materialItem, 1, player, true);
			}
		}
		
		// Reward.
		final Map<Integer, LimitShopRandomCraftReward> rewards = new HashMap<>();
		for (int i = 0; i < _amount; i++)
		{
			final double chance = Rnd.get(100f);
			
			float accumulatedChance = 0;
			final float accumulatedChance1 = accumulatedChance += _product.getChance();
			final float accumulatedChance2 = (_product.getProductionId2() > 0) ? (accumulatedChance += _product.getChance2()) : accumulatedChance;
			final float accumulatedChance3 = (_product.getProductionId3() > 0) ? (accumulatedChance += _product.getChance3()) : accumulatedChance;
			final float accumulatedChance4 = (_product.getProductionId4() > 0) ? (accumulatedChance += _product.getChance4()) : accumulatedChance;
			final float accumulatedChance5 = (_product.getProductionId5() > 0) ? (accumulatedChance += _product.getChance5()) : accumulatedChance;
			
			final float totalChance = _product.getChance() //
				+ (_product.getProductionId2() > 0 ? _product.getChance2() : 0) //
				+ (_product.getProductionId3() > 0 ? _product.getChance3() : 0) //
				+ (_product.getProductionId4() > 0 ? _product.getChance4() : 0) //
				+ (_product.getProductionId5() > 0 ? _product.getChance5() : 0);
			if (Math.abs(totalChance - 100f) > 0.1f)
			{
				LOGGER.warning("LimitShopCraft: product " + _productId + " chances don't sum to 100, total=" + totalChance);
			}
			
			if (chance < accumulatedChance1)
			{
				rewards.put(i, new LimitShopRandomCraftReward(_product.getProductionId(), (int) _product.getCount(), 0));
				final Item item = player.addItem(ItemProcessType.CRAFT, _product.getProductionId(), _product.getCount(), successionEnchant, player, true);
				if ((augmentedItem != null) && (augmentedItem.getAugmentation() != null))
				{
					item.setAugmentation(augmentedItem.getAugmentation(), true);
				}
				if (_product.isAnnounce())
				{
					World.broadcastToAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
				}
			}
			else if (chance < accumulatedChance2)
			{
				rewards.put(i, new LimitShopRandomCraftReward(_product.getProductionId2(), (int) _product.getCount2(), 1));
				final Item item = player.addItem(ItemProcessType.CRAFT, _product.getProductionId2(), _product.getCount2(), _product.getProductionEnchant2(), player, true);
				if (_product.isAnnounce2())
				{
					World.broadcastToAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
				}
			}
			else if (chance < accumulatedChance3)
			{
				rewards.put(i, new LimitShopRandomCraftReward(_product.getProductionId3(), (int) _product.getCount3(), 2));
				final Item item = player.addItem(ItemProcessType.CRAFT, _product.getProductionId3(), _product.getCount3(), _product.getProductionEnchant3(), player, true);
				if (_product.isAnnounce3())
				{
					World.broadcastToAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
				}
			}
			else if ((_product.getProductionId4() > 0) && (chance < accumulatedChance4))
			{
				rewards.put(i, new LimitShopRandomCraftReward(_product.getProductionId4(), (int) _product.getCount4(), 3));
				final Item item = player.addItem(ItemProcessType.CRAFT, _product.getProductionId4(), _product.getCount4(), _product.getProductionEnchant4(), player, true);
				if (_product.isAnnounce4())
				{
					World.broadcastToAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
				}
			}
			else if ((_product.getProductionId5() > 0) && (chance < accumulatedChance5))
			{
				rewards.put(i, new LimitShopRandomCraftReward(_product.getProductionId5(), (int) _product.getCount5(), 4));
				final Item item = player.addItem(ItemProcessType.CRAFT, _product.getProductionId5(), _product.getCount5(), _product.getProductionEnchant5(), player, true);
				if (_product.isAnnounce5())
				{
					World.broadcastToAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
				}
			}
			else
			{
				LOGGER.warning("LimitShopCraft: product " + _productId + " fell through all reward chances.");
			}
		}
		
		player.sendItemList();
		
		// Update account variables.
		int finalRemainingInfo = 0;
		if (_product.getAccountDailyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _product.getProductionId(), 0) + _amount);
			finalRemainingInfo = _product.getAccountDailyLimit() - player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _productId, 0);
		}
		if (_product.getAccountWeeklyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _product.getProductionId(), 0) + _amount);
			finalRemainingInfo = _product.getAccountWeeklyLimit() - player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _productId, 0);
		}
		if (_product.getAccountMonthlyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _product.getProductionId(), 0) + _amount);
			finalRemainingInfo = _product.getAccountMonthlyLimit() - player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _productId, 0);
		}
		if (_product.getAccountBuyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _product.getProductionId(), 0) + _amount);
			finalRemainingInfo = _product.getAccountBuyLimit() - player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _productId, 0);
		}
		
		player.sendPacket(new ExPurchaseLimitShopItemResult(true, 4, _productId, Math.max(finalRemainingInfo, 0), rewards.values()));
		
		// Remove request.
		ThreadPool.schedule(() -> player.removeRequest(PrimeShopRequest.class), 1000);
	}
}