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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicSummonCategoryHolder;
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.RelicsProbList;

/**
 * @author Mobius, Brado
 */
public class RequestRelicsProbList extends ClientPacket
{
	// type value is relics ui section ( summon - 0, compound - 1, guaranteed compound - 3, confirmation replace - 2)
	// private final int _type;
	// key value
	// summon section is coupon order from 1 to 10
	// compound section is relic grade(no, d, c, b) from 1 to 4
	// guaranteed compound is relic grade ( C, B ) 3, 4
	// confirmation replace is relic id
	// private final int _key;
	
	private static final int TYPE_COUPON = 4;
	private static final int TYPE_SUMMON = 0;
	private static final int TYPE_COMBINE = 1;
	private static final int TYPE_REPLACE = 2;
	private static final int TYPE_GUARANTEED_COMBINE = 3;
	
	private int _type;
	private int _value;
	
	@Override
	protected void readImpl()
	{
		_type = readInt();
		_value = readInt(); // RelicGrade or ItemId
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
		{
			player.sendMessage("Type: " + _type);
			player.sendMessage("Value: " + _value);
		}
		
		final Map<Integer, Long> relics = new LinkedHashMap<>();
		switch (_type)
		{
			case TYPE_COUPON:
			{
				final RelicCouponHolder coupon = RelicCouponData.getInstance().getCouponFromCouponItemId(_value);
				if (coupon == null)
				{
					return;
				}
				
				final Map<Integer, Long> possibleEntries = RelicCouponData.getInstance().getCachedChances(coupon.getItemId());
				for (Entry<Integer, Long> entry : possibleEntries.entrySet())
				{
					relics.put(entry.getKey(), entry.getValue());
				}
				break;
			}
			case TYPE_SUMMON:
			{
				final RelicSummonCategoryHolder category = RelicData.getInstance().getRelicSummonCategory(_value);
				if (category == null)
				{
					return;
				}
				
				final Map<Integer, Long> possibleEntries = RelicCouponData.getInstance().getCachedChances(category.getPriceId());
				if (possibleEntries == null)
				{
					return;
				}
				
				relics.putAll(possibleEntries);
				break;
			}
			case TYPE_COMBINE:
			{
				final RelicGrade grade = RelicGrade.values()[_value];
				final RelicGrade upgradeGrade = (_value + 1) < RelicGrade.values().length ? RelicGrade.values()[_value + 1] : null;
				
				// Use grade ticket disabled list to exclude special/event relics.
				final int[] ticketIds =
				{
					83007,
					83008,
					83009,
					83010
				};
				final int ticketIdx = _value - 1;
				final int ticketItemId = ((ticketIdx >= 0) && (ticketIdx < ticketIds.length)) ? ticketIds[ticketIdx] : 0;
				final RelicCouponHolder compoundCoupon = ticketItemId > 0 ? RelicCouponData.getInstance().getCouponFromCouponItemId(ticketItemId) : null;
				final Set<Integer> disabledIds = compoundCoupon != null ? (Set<Integer>) compoundCoupon.getDisabledIds() : Set.of();
				
				// Use combineData chances from XML for correct probability display.
				final org.l2jmobius.gameserver.data.holders.RelicCompoundChanceHolder combineChances = RelicData.getInstance().getCompoundChances(grade);
				final int failTotal = combineChances != null ? (combineChances.getFailCommonChance() + combineChances.getFailShiningChance()) : 80;
				final int successTotal = combineChances != null ? combineChances.getTotalSuccessChance() : 20;
				
				// Same-grade (fail) pool
				final Collection<RelicDataHolder> sameGradePool = RelicData.getInstance().getRelicsByGrade(grade);
				if (sameGradePool != null)
				{
					final long totalSameGradeSC = sameGradePool.stream().filter(r -> (r.getCompoundChanceModifier() != 0) && !disabledIds.contains(r.getRelicId())).mapToLong(RelicDataHolder::getSummonChance).sum();
					if (totalSameGradeSC > 0)
					{
						for (RelicDataHolder rh : sameGradePool)
						{
							if ((rh.getCompoundChanceModifier() == 0) || disabledIds.contains(rh.getRelicId()))
							{
								continue;
							}
							final long chance = (rh.getSummonChance() * failTotal * (10_000_000_000L / 100L)) / totalSameGradeSC;
							relics.put(rh.getRelicId(), chance);
						}
					}
				}
				
				// Upgrade-grade (success) pool
				if (upgradeGrade != null)
				{
					final Collection<RelicDataHolder> upgradeGradePool = RelicData.getInstance().getRelicsByGrade(upgradeGrade);
					if (upgradeGradePool != null)
					{
						final long totalUpgradeGradeSC = upgradeGradePool.stream().filter(r -> (r.getCompoundUpGradeChanceModifier() != 0) && !disabledIds.contains(r.getRelicId())).mapToLong(RelicDataHolder::getSummonChance).sum();
						if (totalUpgradeGradeSC > 0)
						{
							for (RelicDataHolder rh : upgradeGradePool)
							{
								if ((rh.getCompoundUpGradeChanceModifier() == 0) || disabledIds.contains(rh.getRelicId()))
								{
									continue;
								}
								final long chance = (rh.getSummonChance() * successTotal * (10_000_000_000L / 100L)) / totalUpgradeGradeSC;
								relics.put(rh.getRelicId(), chance);
							}
						}
					}
				}
				break;
			}
			case TYPE_REPLACE:
			{
				final RelicDataHolder sourceRelic = RelicData.getInstance().getRelic(_value);
				if (sourceRelic == null)
				{
					return;
				}
				
				final RelicGrade currentGrade = sourceRelic.getGrade();
				final int ticketItemId = currentGrade == RelicGrade.BGRADE ? 83009 : 83010;
				final RelicCouponHolder coupon = RelicCouponData.getInstance().getCouponFromCouponItemId(ticketItemId);
				final Set<Integer> disabledIds = coupon != null ? (Set<Integer>) coupon.getDisabledIds() : Set.of();
				
				final List<RelicDataHolder> replacePool = new ArrayList<>();
				for (RelicDataHolder relicHolder : RelicData.getInstance().getRelicsByGrade(currentGrade))
				{
					if ((relicHolder.getRelicId() == _value) || disabledIds.contains(relicHolder.getRelicId()))
					{
						continue;
					}
					replacePool.add(relicHolder);
				}
				
				final long totalReplaceSC = replacePool.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
				if (totalReplaceSC > 0)
				{
					for (RelicDataHolder rh : replacePool)
					{
						final long chance = (rh.getSummonChance() * 10_000_000_000L) / totalReplaceSC;
						relics.put(rh.getRelicId(), chance);
					}
				}
				break;
			}
			case TYPE_GUARANTEED_COMBINE:
			{
				if ((_value + 1) >= RelicGrade.values().length)
				{
					break;
				}
				
				final RelicGrade upgradeGrade = RelicGrade.values()[_value + 1];
				final Collection<RelicDataHolder> pool = RelicData.getInstance().getRelicsByGrade(upgradeGrade);
				if (pool == null)
				{
					break;
				}
				
				final int[] ticketIds =
				{
					83007,
					83008,
					83009,
					83010
				};
				final int ticketIdx = _value - 1;
				final int ticketItemId = ((ticketIdx >= 0) && (ticketIdx < ticketIds.length)) ? ticketIds[ticketIdx] : 0;
				final RelicCouponHolder guaranteedCoupon = ticketItemId > 0 ? RelicCouponData.getInstance().getCouponFromCouponItemId(ticketItemId) : null;
				final Set<Integer> guaranteedDisabledIds = guaranteedCoupon != null ? (Set<Integer>) guaranteedCoupon.getDisabledIds() : Set.of();
				
				final List<RelicDataHolder> filtered = pool.stream().filter(r -> (r.getCompoundUpGradeChanceModifier() != 0) && !guaranteedDisabledIds.contains(r.getRelicId())).toList();
				final long totalSummon = filtered.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
				if (totalSummon <= 0)
				{
					break;
				}
				
				final long SCALE = 10_000_000_000L;
				for (RelicDataHolder rh : filtered)
				{
					final long chance = (rh.getSummonChance() * SCALE) / totalSummon;
					relics.put(rh.getRelicId(), Math.max(1L, chance));
				}
				break;
			}
			default:
			{
				
				break;
			}
		}
		
		if (!relics.isEmpty())
		{
			player.sendPacket(new RelicsProbList(_type, _value, relics));
		}
	}
}