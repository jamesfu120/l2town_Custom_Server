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
package org.l2jmobius.gameserver.network.clientpackets.characterstyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.data.holders.CharacterStyleDataHolder;
import org.l2jmobius.gameserver.data.xml.CharacterStylesData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleList;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleRegister;

/**
 * @author Brado, Galagard
 */
public class ExRequestCharacterStyleRegister extends ClientPacket
{
	private int _styleType;
	private int _styleId;
	private int _costItemId;
	
	@Override
	protected void readImpl()
	{
		_styleType = readInt();
		_styleId = readInt();
		_costItemId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CharacterStyleCategoryType category = CharacterStyleCategoryType.getByClientId(_styleType);
		final CharacterStyleDataHolder style = CharacterStylesData.getInstance().getSpecificStyleByCategoryAndId(category, _styleId);
		if ((category == null) || (style == null))
		{
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}
		
		// 1. Requirement check.
		if ((category == CharacterStyleCategoryType.APPEARANCE_ARMOR) && !style.canUse(player))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM));
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}
		
		// 2. Find matching price.
		ItemHolder selectedCost = null;
		for (ItemHolder price : style.getActivateCosts())
		{
			if (price.getId() == _costItemId)
			{
				selectedCost = price;
				break;
			}
		}
		
		// 3. Check costItemId - new 542.
		if ((selectedCost == null) || (player.getInventory().getInventoryItemCount(_costItemId, -1) < selectedCost.getCount()))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}
		
		Item costItem = player.getInventory().getItemByItemId(_costItemId);
		
		// 4. Consume items.
		if (player.getInventory().destroyItemByItemId(ItemProcessType.DESTROY, _costItemId, selectedCost.getCount(), player, null) == null)
		{
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}
		
		// 5. Update Inventory.
		InventoryUpdate iu = new InventoryUpdate();
		if ((costItem != null) && (costItem.getCount() > 0))
		{
			iu.addModifiedItem(costItem);
		}
		else if (costItem != null)
		{
			iu.addRemovedItem(costItem);
		}
		player.sendInventoryUpdate(iu);
		
		// 6. Unlock style (Update DB/Variables).
		player.modifyCharacterStyle(category, _styleId, false, true);
		
		// 7. Send success confirmation (This closes the purchase dialog).
		player.sendPacket(ExCharacterStyleRegister.success(_styleId));
		
		final Map<Integer, Integer> activeMap = new HashMap<>();
		if (category == CharacterStyleCategoryType.APPEARANCE_WEAPON)
		{
			activeMap.put(0, player.getActiveCharacterStyleId(category, 0));
			activeMap.put(1, player.getActiveCharacterStyleId(category, 1));
		}
		else
		{
			activeMap.put(0, player.getActiveCharacterStyleId(category));
		}
		
		final List<Integer> availableStyles = player.getVariables().getIntegerList(PlayerVariables.AVAILABLE_CHARACTER_STYLES + category);
		
		// Ensuring the new style is in the list sent to client.
		if (!availableStyles.contains(_styleId))
		{
			availableStyles.add(_styleId);
		}
		
		final List<Integer> favoriteStyles = player.getVariables().getIntegerList(PlayerVariables.FAVORITE_CHARACTER_STYLES + category);
		final ItemHolder swapCosts = CharacterStylesData.getInstance().getSwapCostItemByCategory(category);
		
		// Force refresh.
		player.sendPacket(new ExCharacterStyleList(category, swapCosts, availableStyles, favoriteStyles, activeMap));
	}
}
