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
package org.l2jmobius.gameserver.network.clientpackets.newhenna;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.HennaCombinationData;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.henna.CombinationHenna;
import org.l2jmobius.gameserver.entity.item.henna.CombinationHennaReward;
import org.l2jmobius.gameserver.entity.item.henna.Henna;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaList;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenCompose;

/**
 * @author Index, Serenitty
 */
public class RequestNewHennaCompose extends ClientPacket
{
	private int _slotOneIndex;
	private int _slotOneItemId;
	private int _slotTwoItemId;
	
	@Override
	protected void readImpl()
	{
		_slotOneIndex = readInt();
		_slotOneItemId = readInt();
		_slotTwoItemId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Inventory inventory = player.getInventory();
		if ((player.getHenna(_slotOneIndex) == null) || ((_slotOneItemId != -1) && (inventory.getItemByObjectId(_slotOneItemId) == null)) || ((_slotTwoItemId != -1) && (inventory.getItemByObjectId(_slotTwoItemId) == null)))
		{
			return;
		}
		
		final Henna henna = player.getHenna(_slotOneIndex);
		final CombinationHenna combinationHennas = getCombination(henna.getDyeId(), inventory);
		if (combinationHennas == null)
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}
		
		// Validate both outcomes up front so a misconfigured recipe fails before anything is consumed.
		final CombinationHennaReward successReward = combinationHennas.getReward(CombinationItemType.ON_SUCCESS);
		final CombinationHennaReward failureReward = combinationHennas.getReward(CombinationItemType.ON_FAILURE);
		if ((successReward == null) || (failureReward == null) || (HennaData.getInstance().getHenna(successReward.getHennaId()) == null) || (HennaData.getInstance().getHenna(failureReward.getHennaId()) == null))
		{
			PacketLogger.info(getClass().getSimpleName() + ": " + player + " attempted to compose henna with incomplete reward data!" + System.lineSeparator() + "Henna DyeId: " + henna.getDyeId());
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}
		
		final long commission = combinationHennas.getCommission();
		if (commission > player.getAdena())
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}
		
		final ItemHolder one = new ItemHolder(combinationHennas.getItemOne(), combinationHennas.getCountOne());
		final ItemHolder two = new ItemHolder(combinationHennas.getItemTwo(), combinationHennas.getCountTwo());
		final boolean consumeItemOne = one.getId() > 0;
		final boolean consumeItemTwo = two.getId() > 0;
		if ((_slotOneItemId != -1) && (_slotTwoItemId != -1) && !isValidCombinationItem(inventory.getItemByObjectId(_slotOneItemId).getId(), one.getId(), two.getId()) && !isValidCombinationItem(inventory.getItemByObjectId(_slotTwoItemId).getId(), one.getId(), two.getId()))
		{
			PacketLogger.info(getClass().getSimpleName() + ": " + player + " has modified client or combination data is outdated!" + System.lineSeparator() + "Henna DyeId: " + henna.getDyeId() + " ItemOne: " + combinationHennas.getItemOne() + " ItemTwo: " + combinationHennas.getItemTwo());
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}
		
		if ((consumeItemOne && ((inventory.getItemByItemId(one.getId()) == null) || (inventory.getItemByItemId(one.getId()).getCount() < one.getCount()))) || (consumeItemTwo && ((inventory.getItemByItemId(two.getId()) == null) || (inventory.getItemByItemId(two.getId()).getCount() < two.getCount()))))
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}
		
		if (((commission > 0) && !player.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, commission, player, true)) || (consumeItemOne && !player.destroyItemByItemId(ItemProcessType.FEE, one.getId(), one.getCount(), player, true)) || (consumeItemTwo && !player.destroyItemByItemId(ItemProcessType.FEE, two.getId(), two.getCount(), player, true)))
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}
		
		if (Rnd.get(0, 100) <= combinationHennas.getChance())
		{
			player.removeHenna(_slotOneIndex, false);
			player.addHenna(_slotOneIndex, HennaData.getInstance().getHenna(successReward.getHennaId()));
			if ((successReward.getId() > 0) && (successReward.getCount() > 0))
			{
				player.addItem(ItemProcessType.REWARD, successReward.getId(), successReward.getCount(), null, false);
			}
			player.sendPacket(new NewHennaPotenCompose(successReward.getHennaId(), successReward.getId() > 0 ? successReward.getId() : -1, true));
		}
		else
		{
			if (henna.getDyeId() != failureReward.getHennaId())
			{
				player.removeHenna(_slotOneIndex, false);
				player.addHenna(_slotOneIndex, HennaData.getInstance().getHenna(failureReward.getHennaId()));
			}
			
			if ((failureReward.getId() > 0) && (failureReward.getCount() > 0))
			{
				player.addItem(ItemProcessType.REWARD, failureReward.getId(), failureReward.getCount(), null, false);
			}
			player.sendPacket(new NewHennaPotenCompose(failureReward.getHennaId(), failureReward.getId() > 0 ? failureReward.getId() : -1, false));
		}
		
		player.sendPacket(new NewHennaList(player));
	}
	
	private boolean isValidCombinationItem(int itemId, int itemOneId, int itemTwoId)
	{
		return (itemId > 0) && ((itemId == itemOneId) || (itemId == itemTwoId));
	}
	
	private CombinationHenna getCombination(int hennaId, Inventory inventory)
	{
		CombinationHenna fallback = null;
		for (CombinationHenna combination : HennaCombinationData.getInstance().getHenna())
		{
			if (combination.getHenna() != hennaId)
			{
				continue;
			}
			
			if (matchesClientItems(combination, inventory))
			{
				return combination;
			}
			
			if ((fallback == null) && hasRequiredItems(combination, inventory))
			{
				fallback = combination;
			}
		}
		
		return fallback;
	}
	
	private boolean matchesClientItems(CombinationHenna combination, Inventory inventory)
	{
		boolean hasClientItem = false;
		if (_slotOneItemId != -1)
		{
			hasClientItem = true;
			if (!isValidCombinationItem(inventory.getItemByObjectId(_slotOneItemId).getId(), combination.getItemOne(), combination.getItemTwo()))
			{
				return false;
			}
		}
		
		if (_slotTwoItemId != -1)
		{
			hasClientItem = true;
			if (!isValidCombinationItem(inventory.getItemByObjectId(_slotTwoItemId).getId(), combination.getItemOne(), combination.getItemTwo()))
			{
				return false;
			}
		}
		
		return hasClientItem && hasRequiredItems(combination, inventory);
	}
	
	private boolean hasRequiredItems(CombinationHenna combination, Inventory inventory)
	{
		return hasItem(inventory, combination.getItemOne(), combination.getCountOne()) && hasItem(inventory, combination.getItemTwo(), combination.getCountTwo());
	}
	
	private boolean hasItem(Inventory inventory, int itemId, long count)
	{
		return (itemId <= 0) || ((inventory.getItemByItemId(itemId) != null) && (inventory.getItemByItemId(itemId).getCount() >= count));
	}
}
