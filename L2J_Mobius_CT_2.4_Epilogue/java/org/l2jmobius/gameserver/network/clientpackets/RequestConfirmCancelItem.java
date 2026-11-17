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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;

/**
 * Format(ch) d
 * @author -Wooden-, JankisLT
 */
public class RequestConfirmCancelItem extends ClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || !AbstractRefinePacket.isValid(player))
		{
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		
		if (item.getOwnerId() != player.getObjectId())
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tryied to destroy augment on item that doesn't own.", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if (!item.isAugmented())
		{
			player.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}
		
		if (item.isPvp() && !PlayerConfig.ALT_ALLOW_AUGMENT_PVP_ITEMS)
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}
		
		int price = 0;
		switch (item.getTemplate().getCrystalType())
		{
			case C:
			{
				if (item.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if (item.getCrystalCount() < 2452)
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
				if (item.getCrystalCount() < 1746)
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
				if (item.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if (item.getCrystalCount() < 2824)
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
			case S80:
			case S84:
			{
				price = 920000;
				break;
			}
			
			// TODO: S84 TOP price 3.2M
			// any other item type is not augmentable
			default:
			{
				return;
			}
		}
		
		player.sendPacket(new ExPutItemResultForVariationCancel(item, price));
	}
}
