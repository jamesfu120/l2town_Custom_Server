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

import java.util.List;

import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.holders.AttendanceItemHolder;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceList;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceReward;

/**
 * @author Serenitty, CostyKiller
 */
public class RequestVipAttendanceItemReward extends ClientPacket
{
	private int _day;
	
	@Override
	protected void readImpl()
	{
		_day = readByte();
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
		
		final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
		if (!attendanceInfo.isRewardAvailable())
		{
			player.sendMessage("Calendar abuse detected. GM has been informed!");
			PacketLogger.warning("Player " + player.getName() + " has tried to abuse calendar system!");
			return;
		}
		
		final int rewardIndex = attendanceInfo.getRewardIndex();
		final List<AttendanceItemHolder> rewards = AttendanceRewardData.getInstance().getRewards();
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_MATCH_REAL_DAYS)
		{
			// Real-days mode: only claim the first unclaimed day (free, after timer).
			// Missed days after this are handled by RequestVipAttendanceCheck (fee purchase).
			final int firstUnclaimedDay = rewardIndex + 1; // 1-based
			if ((firstUnclaimedDay > 0) && (firstUnclaimedDay <= rewards.size()))
			{
				final AttendanceItemHolder reward = rewards.get(firstUnclaimedDay - 1);
				player.addItem(ItemProcessType.REWARD, reward.getItemId(), reward.getItemCount(), player, true);
				player.setAttendanceInfo(firstUnclaimedDay);
				
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CAN_GET_S1_AS_A_VIP_REWARD_FOR_USING_PA_CLICK_ON_THE_REWARD_ICON);
				msg.addInt(firstUnclaimedDay);
				player.sendPacket(msg);
				player.sendPacket(new ExVipAttendanceReward());
				
				// Update other players on the same account.
				if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT && (!OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT || !OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT))
				{
					for (Player worldPlayer : World.getPlayers())
					{
						if (worldPlayer.getAccountName().equals(player.getAccountName()))
						{
							worldPlayer.setAttendanceInfo(firstUnclaimedDay);
							worldPlayer.sendPacket(new ExVipAttendanceList(worldPlayer));
						}
					}
				}
			}
			else
			{
				PacketLogger.warning(getClass().getSimpleName() + player + ": Invalid attendance day in real-days mode: " + firstUnclaimedDay);
			}
			return;
		}
		
		// Normal mode: claim requested day and auto-claim all skipped days before it.
		if ((_day > 0) && (_day <= rewards.size()))
		{
			// Claim all unclaimed rewards before the current day.
			for (int i = rewardIndex; i < (_day - 1); i++)
			{
				final AttendanceItemHolder unreclaimedReward = rewards.get(i);
				player.addItem(ItemProcessType.REWARD, unreclaimedReward.getItemId(), unreclaimedReward.getItemCount(), player, true);
			}
			
			// Claim the current day's reward.
			final AttendanceItemHolder reward = rewards.get(_day - 1); // Subtract 1 because the index is 0-based.
			player.addItem(ItemProcessType.REWARD, reward.getItemId(), reward.getItemCount(), player, true);
			player.setAttendanceInfo(_day); // Update reward index.
			
			// Send message.
			final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CAN_GET_S1_AS_A_VIP_REWARD_FOR_USING_PA_CLICK_ON_THE_REWARD_ICON);
			msg.addInt(_day);
			player.sendPacket(msg);
			
			// Send confirm packet.
			player.sendPacket(new ExVipAttendanceReward());
			
			// Update other players on the same account.
			if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT && (!OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT || !OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT))
			{
				for (Player worldPlayer : World.getPlayers())
				{
					if (worldPlayer.getAccountName().equals(player.getAccountName()))
					{
						worldPlayer.setAttendanceInfo(_day); // Update reward index.
						worldPlayer.sendPacket(new ExVipAttendanceList(worldPlayer));
					}
				}
			}
		}
		else
		{
			PacketLogger.warning(getClass().getSimpleName() + player + ": Invalid attendance day: " + _day);
		}
	}
}