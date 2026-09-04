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
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleList;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleUnregister;

/**
 * @author Brado, Galagard
 */
public class ExRequestCharacterStyleUnregister extends ClientPacket
{
	private int _styleType;
	private int _styleId;
	
	@Override
	protected void readImpl()
	{
		_styleType = readInt();
		_styleId = readInt();
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
			player.sendPacket(ExCharacterStyleUnregister.STATIC_PACKET_FAIL);
			return;
		}
		
		// 1. Verify styles.
		final List<Integer> availableStyles = player.getVariables().getIntegerList(PlayerVariables.AVAILABLE_CHARACTER_STYLES + category);
		if (!availableStyles.contains(_styleId))
		{
			player.sendPacket(ExCharacterStyleUnregister.STATIC_PACKET_FAIL);
			return;
		}
		
		// 2. Extraction costs.
		final List<ItemHolder> deactivateCosts = style.getDeactivateCosts();
		if ((deactivateCosts != null) && !deactivateCosts.isEmpty())
		{
			// 3. First Item validation loop.
			for (ItemHolder cost : deactivateCosts)
			{
				if (player.getInventory().getInventoryItemCount(cost.getId(), -1) < cost.getCount())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
					player.sendPacket(ExCharacterStyleUnregister.STATIC_PACKET_FAIL);
					return;
				}
			}
			
			// 4. Second Item validation loop.
			for (ItemHolder cost : deactivateCosts)
			{
				player.getInventory().destroyItemByItemId(ItemProcessType.DESTROY, cost.getId(), cost.getCount(), player, null);
			}
		}
		
		// 5. if equiped remove the active visual style.
		if (player.getActiveCharacterStyleId(category) == _styleId)
		{
			player.setActiveCharacterStyle(category, 0);
		}
		
		// 6. Remove item from collection.
		player.modifyCharacterStyle(category, _styleId, false, false);
		player.sendPacket(ExCharacterStyleUnregister.STATIC_PACKET_SUCCESS);
		
		// 7. Send updated list to update interface.
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
		
		final List<Integer> favoriteStyles = player.getVariables().getIntegerList(PlayerVariables.FAVORITE_CHARACTER_STYLES + category);
		final ItemHolder swapCosts = CharacterStylesData.getInstance().getSwapCostItemByCategory(category);
		
		player.sendPacket(new ExCharacterStyleList(category, swapCosts, player.getAvailableCharacterStyles(category), favoriteStyles, activeMap));
		
		// Visual update.
		player.broadcastUserInfo();
	}
}
