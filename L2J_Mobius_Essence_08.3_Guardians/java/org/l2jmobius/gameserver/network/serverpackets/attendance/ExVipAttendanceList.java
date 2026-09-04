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
package org.l2jmobius.gameserver.network.serverpackets.attendance;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.data.holders.AttendanceItemHolder;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius, Serenitty, Lonely, CostyKiller
 */
public class ExVipAttendanceList extends ServerPacket
{
	private final int _index;
	private final int _delayreward;
	private final boolean _available;
	private final boolean _loopReset;
	private final int _currentCycleDay;
	private final List<AttendanceItemHolder> _rewardItems;
	
	public ExVipAttendanceList(Player player)
	{
		final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
		_index = attendanceInfo.getRewardIndex();
		_delayreward = player.getAttendanceDelay();
		_available = attendanceInfo.isRewardAvailable();
		_currentCycleDay = attendanceInfo.getCurrentCycleDay();
		_rewardItems = AttendanceRewardData.getInstance().getRewards();
		
		// Loop reset flag: when true, all days should appear as unclaimed (RewardDay = 0).
		_loopReset = AttendanceRewardsConfig.ATTENDANCE_REWARDS_LOOP && (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT ? player.getAccountVariables().getBoolean(PlayerVariables.ATTENDANCE_LOOP_RESET, false) : player.getVariables().getBoolean(PlayerVariables.ATTENDANCE_LOOP_RESET, false));
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VIP_ATTENDANCE_LIST.writeId(this, buffer);
		
		buffer.writeInt(_rewardItems.size());
		for (AttendanceItemHolder reward : _rewardItems)
		{
			buffer.writeInt(reward.getItemId());
			buffer.writeLong(reward.getItemCount());
			buffer.writeByte(reward.getHighlight());
		}
		
		buffer.writeInt(AttendanceRewardsConfig.ATTENDANCE_REWARD_LCOIN_CHECK); // MinimumLevel / coin check threshold sent to client
		buffer.writeInt(_delayreward); // RemainCheckTime
		
		// RewardDay controls which days show the red "claimed" mark.
		// When loop just reset, send 0 so all days appear as unclaimed.
		final int rewardDay = _loopReset ? 0 : _index;
		
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_MATCH_REAL_DAYS)
		{
			// Real-days mode:
			// RollBookDay = today's cycle day (where the calendar highlights "today").
			// AttendanceDay = only the first unclaimed day (_index + 1) when available,
			// so only that one day gets the free "Reward" button.
			// Missed days between AttendanceDay and RollBookDay are
			// expected to show the purchase (coin) button on the client.
			// RewardDay = how many days claimed so far (red marks up to here).
			if (_available)
			{
				buffer.writeByte(_currentCycleDay); // RollBookDay - today's position on calendar
				buffer.writeByte(_index + 1); // AttendanceDay - ONLY first unclaimed is free
				buffer.writeByte(rewardDay); // RewardDay - claimed so far
				buffer.writeByte(0); // FollowBaseDay
				buffer.writeByte(0); // FollowBaseDay
			}
			else
			{
				buffer.writeByte(_currentCycleDay); // RollBookDay
				buffer.writeByte(_index); // AttendanceDay - nothing free to claim
				buffer.writeByte(rewardDay); // RewardDay
				buffer.writeByte(0); // FollowBaseDay
				buffer.writeByte(1); // FollowBaseDay
			}
		}
		else
		{
			// Normal mode: original logic unchanged.
			if (_available)
			{
				buffer.writeByte(_index + 1); // RollBookDay
				if ((_delayreward == 0) && (_available))
				{
					buffer.writeByte(_index + 1); // AttendanceDay
				}
				else
				{
					buffer.writeByte(_index); // AttendanceDay
				}
				buffer.writeByte(rewardDay); // RewardDay
				buffer.writeByte(0); // FollowBaseDay
				buffer.writeByte(0); // FollowBaseDay
			}
			else
			{
				buffer.writeByte(_index); // RollBookDay
				if ((_delayreward == 0) && (_available))
				{
					buffer.writeByte(_index + 1); // AttendanceDay
				}
				else
				{
					buffer.writeByte(_index); // AttendanceDay
				}
				buffer.writeByte(rewardDay); // RewardDay
				buffer.writeByte(0); // FollowBaseDay
				buffer.writeByte(1); // FollowBaseDay
			}
		}
	}
}