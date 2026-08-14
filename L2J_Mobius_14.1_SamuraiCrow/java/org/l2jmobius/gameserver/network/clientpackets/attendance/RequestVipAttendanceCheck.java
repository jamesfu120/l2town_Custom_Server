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
package org.l2jmobius.gameserver.network.clientpackets.attendance;

import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.holders.AttendanceItemHolder;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceCheck;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceList;

/**
 * @author Mobius, Serenitty, CostyKiller
 */
public class RequestVipAttendanceCheck extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!AttendanceRewardsConfig.ENABLE_ATTENDANCE_REWARDS)
		{
			player.sendPacket(SystemMessageId.DUE_TO_A_SYSTEM_ERROR_THE_ATTENDANCE_REWARD_CANNOT_BE_RECEIVED_PLEASE_TRY_AGAIN_LATER_BY_GOING_TO_MENU_ATTENDANCE_CHECK);
			return;
		}
		
		if (AttendanceRewardsConfig.PREMIUM_ONLY_ATTENDANCE_REWARDS && !player.hasPremiumStatus())
		{
			player.sendPacket(SystemMessageId.YOUR_VIP_RANK_IS_TOO_LOW_TO_RECEIVE_THE_REWARD);
			return;
		}
		
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_MATCH_REAL_DAYS)
		{
			handleMissedDayPurchase(player);
		}
		else
		{
			handleNormalCheck(player);
		}
	}
	
	/**
	 * Normal mode: deduct coin fee and confirm attendance check.
	 * @param player
	 */
	private void handleNormalCheck(Player player)
	{
		// Item ID 48472 (Einhasad Coin) x100 hardcoded by client.
		if (!player.destroyItemByItemId(ItemProcessType.FEE, Inventory.EINHASAD_COIN_ID, 100, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		player.sendPacket(new ExVipAttendanceCheck(true));
	}
	
	/**
	 * Real-days mode: purchase the next missed day in order, earliest first. Available as soon as the timer is done. The client controls the coin item ID (48472) and count (100) - hardcoded in client dat files and cannot be changed server-side.<br>
	 * Config.ATTENDANCE_REWARD_EINHASAD_COIN_CHECK controls the threshold sent to the client in ExVipAttendanceList (MinimumLevel field) which drives the coin cost UI display.
	 * @param player
	 */
	private void handleMissedDayPurchase(Player player)
	{
		// Timer still running - reward not yet available.
		if (player.getAttendanceDelay() > 0)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
		
		// Free claim still pending - player must claim the first unclaimed day before purchasing missed days.
		if (attendanceInfo.isRewardAvailable())
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		// No missed days to purchase - all days up to today already claimed.
		if (!attendanceInfo.hasMissedDays())
		{
			return;
		}
		
		// Deduct coins - item ID 48472 (Einhasad Coin), count 100, hardcoded by client.
		if (!player.destroyItemByItemId(ItemProcessType.FEE, Inventory.EINHASAD_COIN_ID, 100, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		// Give reward for next missed day in order (earliest first).
		final int nextMissedDay = attendanceInfo.getRewardIndex() + 1; // 1-based
		final AttendanceItemHolder reward = AttendanceRewardData.getInstance().getRewards().get(nextMissedDay - 1);
		player.addItem(ItemProcessType.REWARD, reward.getItemId(), reward.getItemCount(), player, true);
		
		// Advance index.
		player.setAttendanceInfo(nextMissedDay);
		
		// Update other players on the same account.
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT && (!OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT || !OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT))
		{
			for (Player worldPlayer : World.getPlayers())
			{
				if (worldPlayer.getAccountName().equals(player.getAccountName()))
				{
					worldPlayer.setAttendanceInfo(nextMissedDay);
					worldPlayer.sendPacket(new ExVipAttendanceList(worldPlayer));
				}
			}
		}
		
		// Send updated packets.
		player.sendPacket(new ExVipAttendanceCheck(true));
		player.sendPacket(new ExVipAttendanceList(player));
	}
}