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
package org.l2jmobius.gameserver.entity.actor.holders.player;

/**
 * @author Mobius, CostyKiller
 */
public class AttendanceInfoHolder
{
	private final int _rewardIndex;
	private final boolean _rewardAvailable;
	private final int _missedDays;
	private final int _currentCycleDay;
	
	/**
	 * Constructor for normal (non real-days) mode.
	 * @param rewardIndex
	 * @param rewardAvailable
	 */
	public AttendanceInfoHolder(int rewardIndex, boolean rewardAvailable)
	{
		_rewardIndex = rewardIndex;
		_rewardAvailable = rewardAvailable;
		_missedDays = 0;
		_currentCycleDay = rewardIndex;
	}
	
	/**
	 * Constructor for real-days mode.
	 * @param rewardIndex how many days have been claimed/purchased so far
	 * @param rewardAvailable whether the first unclaimed day is claimable (timer done)
	 * @param missedDays how many days between last claimed and today (purchasable with fee)
	 * @param currentCycleDay which day of the cycle today is (1-based)
	 */
	public AttendanceInfoHolder(int rewardIndex, boolean rewardAvailable, int missedDays, int currentCycleDay)
	{
		_rewardIndex = rewardIndex;
		_rewardAvailable = rewardAvailable;
		_missedDays = missedDays;
		_currentCycleDay = currentCycleDay;
	}
	
	public int getRewardIndex()
	{
		return _rewardIndex;
	}
	
	public boolean isRewardAvailable()
	{
		return _rewardAvailable;
	}
	
	/**
	 * Number of days that can be purchased with fee (days between last claimed and today, excluding the first unclaimed). Only relevant when AttendanceRewardsMatchRealDays = True.
	 * @return
	 */
	public int getMissedDays()
	{
		return _missedDays;
	}
	
	/**
	 * The current day of the attendance cycle (1-based, e.g. if today is the 5th day of the cycle, returns 5). Only relevant when AttendanceRewardsMatchRealDays = True.
	 * @return
	 */
	public int getCurrentCycleDay()
	{
		return _currentCycleDay;
	}
	
	/**
	 * Whether there are missed days available to purchase with fee. Only relevant when AttendanceRewardsMatchRealDays = True and first unclaimed day already claimed.
	 * @return
	 */
	public boolean hasMissedDays()
	{
		return _missedDays > 0;
	}
}