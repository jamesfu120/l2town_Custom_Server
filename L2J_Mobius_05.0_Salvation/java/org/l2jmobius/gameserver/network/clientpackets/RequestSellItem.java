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
package org.l2jmobius.gameserver.network.clientpackets;

import static org.l2jmobius.gameserver.entity.actor.Npc.INTERACTION_DISTANCE;
import static org.l2jmobius.gameserver.entity.itemcontainer.Inventory.MAX_ADENA;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.MerchantZeroSellPriceConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.UniqueItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.buylist.ProductList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoInvenWeight;

/**
 * RequestSellItem client packet class.
 */
public class RequestSellItem extends ClientPacket
{
	private static final int BATCH_LENGTH = 16;
	private static final int CUSTOM_CB_SELL_LIST = 423;
	
	private int _listId;
	private List<UniqueItemHolder> _items = null;
	
	@Override
	protected void readImpl()
	{
		_listId = readInt();
		final int size = readInt();
		if ((size <= 0) || (size > PlayerConfig.MAX_ITEM_IN_PACKET) || ((size * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			final int objectId = readInt();
			final int itemId = readInt();
			final long count = readLong();
			if ((objectId < 1) || (itemId < 1) || (count < 1))
			{
				_items = null;
				return;
			}
			
			_items.add(new UniqueItemHolder(itemId, objectId, count));
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are selling too fast.");
			return;
		}
		
		if (_items == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Alt game - Karma punishment.
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (player.getReputation() < 0))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final WorldObject target = player.getTarget();
		Merchant merchant = null;
		if (!player.isGM() && (_listId != CUSTOM_CB_SELL_LIST))
		{
			if ((target == null) || !player.isInsideRadius3D(target, INTERACTION_DISTANCE) || (player.getInstanceId() != target.getInstanceId()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (target instanceof Merchant)
			{
				merchant = (Merchant) target;
			}
			else
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if ((merchant == null) && !player.isGM() && (_listId != CUSTOM_CB_SELL_LIST))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final ProductList buyList = BuyListData.getInstance().getBuyList(_listId);
		if (buyList == null)
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId, GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if ((merchant != null) && !buyList.isNpcAllowed(merchant.getId()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		long totalPrice = 0;
		
		// The limit is checked for the whole list before anything is sold, against the room left under MAX_ADENA:
		// checked inside the loop it returned after earlier items were gone, and addAdena cut what passed the limit.
		long listPrice = 0;
		for (UniqueItemHolder i : _items)
		{
			final Item item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "sell");
			if ((item == null) || (!item.isSellable()))
			{
				continue;
			}
			
			final long price = item.getReferencePrice() / 2;
			listPrice += price * i.getCount();
			if (((MAX_ADENA / i.getCount()) < price) || (listPrice > MAX_ADENA) || (!MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE && (listPrice > (MAX_ADENA - player.getAdena()))))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
				return;
			}
		}
		
		// Proceed the sell
		for (UniqueItemHolder i : _items)
		{
			final Item item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "sell");
			if ((item == null) || (!item.isSellable()))
			{
				continue;
			}
			
			final long price = item.getReferencePrice() / 2;
			final Item removedItem;
			if (GeneralConfig.ALLOW_REFUND)
			{
				removedItem = player.getInventory().transferItem(ItemProcessType.TRANSFER, i.getObjectId(), i.getCount(), player.getRefund(), player, merchant);
			}
			else
			{
				removedItem = player.getInventory().destroyItem(ItemProcessType.SELL, i.getObjectId(), i.getCount(), player, merchant);
			}
			
			// Paid only for what was removed.
			if (removedItem != null)
			{
				totalPrice += price * i.getCount();
			}
		}
		
		if (!MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE)
		{
			player.addAdena(ItemProcessType.SELL, totalPrice, merchant, false);
		}
		
		// Update current load as well.
		player.sendPacket(new ExUserInfoInvenWeight(player));
		player.sendPacket(new ExBuySellList(player, true));
		player.sendPacket(SystemMessageId.THE_TRADE_WAS_SUCCESSFUL);
	}
}
