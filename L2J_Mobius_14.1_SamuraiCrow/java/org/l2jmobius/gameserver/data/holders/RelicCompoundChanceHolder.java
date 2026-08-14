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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;

/**
 * Holds the compound chance configuration for a specific relic grade.<br>
 * Maps to the {@code <combineData>} XML element in RelicData.xml.
 * @author CostyKiller
 */
public class RelicCompoundChanceHolder
{
	private final RelicGrade _grade;
	private final int _failCommonChance;
	private final int _failShiningChance;
	private final int _successCommonChance;
	private final int _successShiningChance;
	
	public RelicCompoundChanceHolder(RelicGrade grade, int failCommonChance, int failShiningChance, int successCommonChance, int successShiningChance)
	{
		_grade = grade;
		_failCommonChance = failCommonChance;
		_failShiningChance = failShiningChance;
		_successCommonChance = successCommonChance;
		_successShiningChance = successShiningChance;
	}
	
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Chance to fail and receive a same-grade COMMON relic.
	 * @return
	 */
	public int getFailCommonChance()
	{
		return _failCommonChance;
	}
	
	/**
	 * Chance to fail and receive a same-grade SHINING relic.
	 * @return
	 */
	public int getFailShiningChance()
	{
		return _failShiningChance;
	}
	
	/**
	 * Chance to succeed and receive a higher-grade COMMON relic.
	 * @return
	 */
	public int getSuccessCommonChance()
	{
		return _successCommonChance;
	}
	
	/**
	 * Chance to succeed and receive a higher-grade SHINING relic.
	 * @return
	 */
	public int getSuccessShiningChance()
	{
		return _successShiningChance;
	}
	
	/**
	 * Total success chance (common + shining upgrade).
	 * @return
	 */
	public int getTotalSuccessChance()
	{
		return _successCommonChance + _successShiningChance;
	}
}
