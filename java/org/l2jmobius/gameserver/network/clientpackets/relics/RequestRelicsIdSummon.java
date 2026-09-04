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
package org.l2jmobius.gameserver.network.clientpackets.relics;

import java.util.List;

import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicSummonCategoryHolder;
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.RelicSummonRequest;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsSummonResult;

/**
 * @author Brado
 */
public class RequestRelicsIdSummon extends ClientPacket
{
	private int _summonCategoryId;
	private int _itemId;
	
	@Override
	protected void readImpl()
	{
		_summonCategoryId = readInt();
		_itemId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// If you have 100 relics in your confirmation list, restrictions are applied to the relic summoning and compounding functions.
		if (player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) >= RelicSystemConfig.RELIC_UNCONFIRMED_LIST_LIMIT)
		{
			player.sendPacket(SystemMessageId.SUMMON_COMPOUND_IS_UNAVAILABLE_AS_YOU_HAVE_MORE_THAN_100_UNCONFIRMED_RELICS);
			return;
		}
		
		if (_itemId == Inventory.EINHASAD_COIN_ID)
		{
			if (!player.destroyItemByItemId(ItemProcessType.DESTROY, Inventory.EINHASAD_COIN_ID, _summonCategoryId == 1 ? 3000 : 300, player, true))
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS_AVAILABLE_FOR_PURCHASE);
				return;
			}
			
			if (_summonCategoryId == 1)
			{
				_itemId = 83004; // Shining Relic Summon Coupon - 11 times
			}
			else
			{
				_itemId = 83005; // Shining Relic Summon Coupon - 1 time
			}
			
			player.addItem(ItemProcessType.REWARD, _itemId, 1, player, false);
		}
		
		final RelicSummonCategoryHolder category = RelicData.getInstance().getRelicSummonCategory(_summonCategoryId);
		if ((category == null) || !player.destroyItemByItemId(ItemProcessType.DESTROY, category.getPriceId(), category.getPriceCount(), player, false))
		{
			return;
		}
		
		final RelicCouponHolder relicCoupon = RelicCouponData.getInstance().getCouponFromCouponItemId(category.getPriceId());
		if (relicCoupon == null)
		{
			player.sendPacket(SystemMessageId.AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER);
			PacketLogger.finer("Summon Category: " + category.getCategoryId() + " has no matching coupon in RelicCouponData for priceId " + category.getPriceId());
			return;
		}
		
		final List<Integer> obtainedRelics = RelicCouponData.getInstance().generateSummonRelics(relicCoupon);
		if (obtainedRelics.isEmpty())
		{
			player.sendPacket(SystemMessageId.AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER);
			PacketLogger.finer("Summon Category: " + category.getCategoryId() + " generated 0 relics!");
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Summon Category: " + category.getCategoryId() + " generated 0 relics!");
			}
			return;
		}
		
		for (int relicId : obtainedRelics)
		{
			player.handleRelicAcquisition(relicId);
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Summoned relic ID: " + relicId);
			}
		}
		
		player.addRequest(new RelicSummonRequest(player));
		player.sendPacket(new ExRelicsSummonResult(category, obtainedRelics));
	}
}