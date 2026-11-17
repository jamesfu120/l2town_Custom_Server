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

import static org.l2jmobius.gameserver.entity.itemcontainer.Inventory.MAX_ADENA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author l3x, JankisLT
 */
public class RequestBuySeed extends ClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	private int _manorId;
	private List<ItemHolder> _items = null;
	
	@Override
	protected void readImpl()
	{
		_manorId = readInt();
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = readInt();
			final long cnt = readLong();
			if ((cnt < 1) || (itemId < 1))
			{
				_items = null;
				return;
			}
			
			_items.add(new ItemHolder(itemId, cnt));
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
		else if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are buying seeds too fast!");
			return;
		}
		else if (_items == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (manor.isUnderMaintenance())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_manorId);
		if (castle == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Npc manager = player.getLastFolkNPC();
		if (!(manager instanceof Merchant) || !manager.canInteract(player) || (manager.getParameters().getInt("manor_id", -1) != _manorId))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		long totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;
		
		final Map<Integer, SeedProduction> productInfo = new HashMap<>();
		for (ItemHolder ih : _items)
		{
			final SeedProduction sp = manor.getSeedProduct(_manorId, ih.getId(), false);
			if ((sp == null) || (sp.getPrice() <= 0) || (sp.getAmount() < ih.getCount()) || ((MAX_ADENA / ih.getCount()) < sp.getPrice()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Calculate price
			totalPrice += (sp.getPrice() * ih.getCount());
			if (totalPrice > MAX_ADENA)
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.", GeneralConfig.DEFAULT_PUNISH);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Calculate weight
			final ItemTemplate template = ItemData.getInstance().getTemplate(ih.getId());
			totalWeight += ih.getCount() * template.getWeight();
			
			// Calculate slots
			if (!template.isStackable())
			{
				slots += ih.getCount();
			}
			else if (player.getInventory().getItemByItemId(ih.getId()) == null)
			{
				slots++;
			}
			
			productInfo.put(ih.getId(), sp);
		}
		
		if (!player.getInventory().validateWeight(totalWeight))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}
		else if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			return;
		}
		else if ((totalPrice < 0) || (player.getAdena() < totalPrice))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}
		
		// Proceed the purchase
		for (ItemHolder i : _items)
		{
			final SeedProduction sp = productInfo.get(i.getId());
			final long price = sp.getPrice() * i.getCount();
			
			// Take Adena and decrease seed amount.
			if (!player.reduceAdena(ItemProcessType.BUY, price, player, false))
			{
				// Failed buy, reduce total price.
				totalPrice -= price;
				continue;
			}
			
			// Decreased first, a failed Adena take left the seeds out of stock; now the Adena is given back.
			if (!sp.decreaseAmount(i.getCount()))
			{
				player.addAdena(ItemProcessType.REFUND, price, manager, false);
				totalPrice -= price;
				continue;
			}
			
			// Add item to player's inventory.
			player.addItem(ItemProcessType.BUY, i.getId(), i.getCount(), manager, true);
		}
		
		// Adding to treasury for Manor Castle.
		if (totalPrice > 0)
		{
			castle.addToTreasuryNoTax(totalPrice);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
			sm.addLong(totalPrice);
			player.sendPacket(sm);
			
			if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				manor.updateCurrentProduction(_manorId, productInfo.values());
			}
		}
	}
}
