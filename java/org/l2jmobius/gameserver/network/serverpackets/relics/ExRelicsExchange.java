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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicReplaceChanceHolder;
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsExchange extends ServerPacket
{
	private final Player _player;
	private final int _index;
	private int _relicId;
	private boolean _success;
	
	public ExRelicsExchange(Player player, int index, int relicId)
	{
		_player = player;
		_index = index;
		_relicId = relicId;
		_success = false;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE.writeId(this, buffer);
		
		final RelicGrade unconfirmedRelicGrade = RelicData.getInstance().getRelic(_relicId).getGrade();
		
		// Read max attempts and current remaining from XML data + account variables.
		final int maxAttempts = RelicData.getInstance().getReplaceAttempts(unconfirmedRelicGrade);
		final String attemptsVar = unconfirmedRelicGrade == RelicGrade.BGRADE ? AccountVariables.B_GRADE_RELIC_ATEMPTS : AccountVariables.A_GRADE_RELIC_ATEMPTS;
		int attemptsRemaining = _player.getAccountVariables().getInt(attemptsVar, maxAttempts);
		
		final int obtainedRelicId = getRandomRelicId(_relicId, unconfirmedRelicGrade);
		_success = obtainedRelicId != 0;
		
		final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 1, 300, System.currentTimeMillis());
		final PlayerRelicData unconfirmedRelic = _player.getRelics().stream().filter(r -> (r.getRelicId() == _relicId) && (r.getRelicIndex() >= 300) && (r.getRelicCount() != 0)).findFirst().orElse(null);
		
		if (unconfirmedRelic != null)
		{
			// Swap out the old unconfirmed relic for the new one.
			unconfirmedRelic.setRelicCount(unconfirmedRelic.getRelicCount() - 1);
			_relicId = newRelic.getRelicId();
			_player.getRelics().add(newRelic);
			
			// Decrement remaining attempts.
			_player.getAccountVariables().set(attemptsVar, attemptsRemaining - 1);
			attemptsRemaining = attemptsRemaining - 1;
			_player.getAccountVariables().storeMe();
			_player.storeRelics();
			
			// Determine fee for this attempt from the XML fee list.
			// The fee list is ordered 1st-use → last-use.
			// After decrement, currentRemaining tells us how many are left.
			// Fee index = maxAttempts - currentRemaining - 1 (0-based, capped to list size).
			final int currentRemaining = _player.getAccountVariables().getInt(attemptsVar, maxAttempts);
			final List<ItemHolder> feeList = RelicData.getInstance().getReplaceFees(unconfirmedRelicGrade);
			if (!feeList.isEmpty())
			{
				final int feeIndex = Math.min(maxAttempts - currentRemaining - 1, feeList.size() - 1);
				if (feeIndex >= 0)
				{
					final ItemHolder fee = feeList.get(feeIndex);
					if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("Replace fee list size: " + feeList.size());
						_player.sendMessage("currentRemaining: " + currentRemaining + ", feeIndex: " + feeIndex);
						_player.sendMessage("Fee item: " + fee.getId() + " x" + fee.getCount());
					}
					_player.destroyItemByItemId(ItemProcessType.FEE, fee.getId(), fee.getCount(), _player, true);
					_player.getAccountVariables().storeMe();
				}
			}
		}
		
		buffer.writeInt(_index);
		buffer.writeByte(_success);
		buffer.writeInt(attemptsRemaining); // Exchange attempts remaining.
		buffer.writeInt(maxAttempts); // Exchange attempts max.
		buffer.writeInt(_relicId);
		_player.sendPacket(new ExRelicsExchangeList(_player));
	}
	
	/**
	 * Picks a replacement relic for the given grade using the configured replaceData chances. Rolls against failChance / successCommonChance / successShiningChance to determine which type pool to draw from, then does a weighted pick by summonChance.
	 * @param currentRelicId
	 * @param grade
	 * @return
	 */
	private int getRandomRelicId(int currentRelicId, RelicGrade grade)
	{
		// Use disabled list from the grade ticket coupons:
		// 83009 = B-grade ticket, 83010 = A-grade ticket
		final int ticketItemId = grade == RelicGrade.BGRADE ? 83009 : 83010;
		final RelicCouponHolder coupon = RelicCouponData.getInstance().getCouponFromCouponItemId(ticketItemId);
		final Set<Integer> disabledIds = coupon != null ? (Set<Integer>) coupon.getDisabledIds() : Set.of();
		
		// Determine result type using XML replace chances
		final RelicReplaceChanceHolder replaceChances = RelicData.getInstance().getReplaceChances(grade);
		boolean shining = false;
		if (replaceChances != null)
		{
			final int roll = Rnd.get(100);
			final int failTop = replaceChances.getFailChance();
			final int commonTop = failTop + replaceChances.getSuccessCommonChance();
			// roll < failTop → failed replace, still give common (fallback)
			// roll < commonTop → COMMON result
			// else → SHINING result
			shining = roll >= commonTop;
		}
		
		// Build pool filtered by type and disabled list
		final List<RelicDataHolder> pool;
		if (shining)
		{
			final Collection<RelicDataHolder> shiningPool = RelicData.getInstance().getShiningRelicsByGrade(grade);
			pool = (shiningPool != null ? shiningPool.stream() : RelicData.getInstance().getRelicsByGrade(grade).stream()).filter(r -> (r.getRelicId() != currentRelicId) && !disabledIds.contains(r.getRelicId())).toList();
		}
		else
		{
			final Collection<RelicDataHolder> commonPool = RelicData.getInstance().getCommonRelicsByGrade(grade);
			pool = (commonPool != null ? commonPool.stream() : RelicData.getInstance().getRelicsByGrade(grade).stream()).filter(r -> (r.getRelicId() != currentRelicId) && !disabledIds.contains(r.getRelicId())).toList();
		}
		
		if (pool.isEmpty())
		{
			// Fall back to full grade pool if the type pool is empty after filtering
			final List<RelicDataHolder> fallback = RelicData.getInstance().getRelicsByGrade(grade).stream().filter(r -> (r.getRelicId() != currentRelicId) && !disabledIds.contains(r.getRelicId())).toList();
			if (fallback.isEmpty())
			{
				return currentRelicId;
			}
			final long totalFallback = fallback.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
			long rng = Rnd.get(totalFallback);
			for (RelicDataHolder relic : fallback)
			{
				rng -= relic.getSummonChance();
				if (rng < 0)
				{
					return relic.getRelicId();
				}
			}
			return fallback.get(fallback.size() - 1).getRelicId();
		}
		
		// Weighted pick by summonChance within the selected pool
		final long totalWeight = pool.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
		long rng = Rnd.get(totalWeight);
		for (RelicDataHolder relic : pool)
		{
			rng -= relic.getSummonChance();
			if (rng < 0)
			{
				return relic.getRelicId();
			}
		}
		
		return pool.get(pool.size() - 1).getRelicId();
	}
}
