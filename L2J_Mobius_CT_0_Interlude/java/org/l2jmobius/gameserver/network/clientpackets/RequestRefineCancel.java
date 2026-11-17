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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExVariationCancelResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Format(ch) d
 * @author -Wooden-, JankisLT
 */
public class RequestRefineCancel extends ClientPacket
{
	private int _targetItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			player.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		
		if (targetItem.getOwnerId() != player.getObjectId())
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tryied to augment item that doesn't own.", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		// Cannot remove augmentation from a not augmented item.
		if (!targetItem.isAugmented())
		{
			player.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			player.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		
		// Same state gate as augmenting, otherwise an item offered in a trade or private store loses its augment before the buyer receives it.
		if (!AbstractRefinePacket.isValid(player) || (targetItem.isPvp() && !PlayerConfig.ALT_ALLOW_AUGMENT_PVP_ITEMS))
		{
			player.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		
		// get the price
		int price = 0;
		switch (targetItem.getTemplate().getCrystalType())
		{
			case C:
			{
				if (targetItem.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if (targetItem.getCrystalCount() < 2452)
				{
					price = 150000;
				}
				else
				{
					price = 210000;
				}
				break;
			}
			case B:
			{
				if (targetItem.getCrystalCount() < 1746)
				{
					price = 240000;
				}
				else
				{
					price = 270000;
				}
				break;
			}
			case A:
			{
				if (targetItem.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if (targetItem.getCrystalCount() < 2824)
				{
					price = 390000;
				}
				else
				{
					price = 420000;
				}
				break;
			}
			case S:
			{
				price = 480000;
				break;
			}
			
			// Any other item type is not augmentable.
			default:
			{
				player.sendPacket(new ExVariationCancelResult(0));
				return;
			}
		}
		
		// try to reduce the players adena
		if (!player.reduceAdena(ItemProcessType.FEE, price, null, true))
		{
			player.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		
		// unequip item
		final InventoryUpdate iu = new InventoryUpdate();
		if (targetItem.isEquipped())
		{
			for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(targetItem.getLocationSlot()))
			{
				iu.addModifiedItem(itm);
			}
		}
		
		// remove the augmentation
		targetItem.removeAugmentation();
		
		// send ExVariationCancelResult
		player.sendPacket(new ExVariationCancelResult(1));
		
		// send inventory update
		iu.addModifiedItem(targetItem);
		player.sendInventoryUpdate(iu);
	}
}
