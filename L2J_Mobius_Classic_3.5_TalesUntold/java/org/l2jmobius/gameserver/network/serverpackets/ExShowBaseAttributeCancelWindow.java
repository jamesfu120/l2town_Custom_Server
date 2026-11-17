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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowBaseAttributeCancelWindow extends ServerPacket
{
	private final List<Item> _items = new ArrayList<>();
	
	public ExShowBaseAttributeCancelWindow(Player player)
	{
		for (Item item : player.getInventory().getItems())
		{
			if (item.hasAttributes())
			{
				_items.add(item);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_BASE_ATTRIBUTE_CANCEL_WINDOW.writeId(this, buffer);
		buffer.writeInt(_items.size());
		for (Item item : _items)
		{
			buffer.writeInt(item.getObjectId());
			buffer.writeLong(getPrice(item));
		}
	}
	
	/**
	 * Attribute removal fee, shared with {@code RequestExRemoveItemAttribute} so the shown price is the charged price.
	 * @param item the item to clear
	 * @return the fee in adena, 0 for grades without a fee
	 */
	public static long getPrice(Item item)
	{
		final boolean weapon = item.isWeapon();
		switch (item.getTemplate().getCrystalType())
		{
			case S:
			{
				return weapon ? 50000 : 40000;
			}
			case S80:
			{
				return weapon ? 100000 : 80000;
			}
			case S84:
			{
				return weapon ? 200000 : 160000;
			}
			case R:
			{
				return weapon ? 400000 : 320000;
			}
			case R95:
			{
				return weapon ? 800000 : 640000;
			}
			case R99:
			{
				return weapon ? 3200000 : 2560000;
			}
			default:
			{
				return 0;
			}
		}
	}
}
