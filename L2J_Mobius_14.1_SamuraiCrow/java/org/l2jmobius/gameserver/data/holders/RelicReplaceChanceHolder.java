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
 * Holds the replacement chance configuration for a specific relic grade.<br>
 * Maps to the {@code <replaceData>} XML element in RelicData.xml.
 * @author CostyKiller
 */
public class RelicReplaceChanceHolder
{
	private final RelicGrade _grade;
	private final int _failChance;
	private final int _successCommonChance;
	private final int _successShiningChance;
	
	public RelicReplaceChanceHolder(RelicGrade grade, int failChance, int successCommonChance, int successShiningChance)
	{
		_grade = grade;
		_failChance = failChance;
		_successCommonChance = successCommonChance;
		_successShiningChance = successShiningChance;
	}
	
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Chance the replacement attempt fails entirely.
	 * @return
	 */
	public int getFailChance()
	{
		return _failChance;
	}
	
	/**
	 * Chance to receive a COMMON relic of the same grade on success.
	 * @return
	 */
	public int getSuccessCommonChance()
	{
		return _successCommonChance;
	}
	
	/**
	 * Chance to receive a SHINING relic of the same grade on success.
	 * @return
	 */
	public int getSuccessShiningChance()
	{
		return _successShiningChance;
	}
	
	/**
	 * Total success chance (common + shining).
	 * @return
	 */
	public int getTotalSuccessChance()
	{
		return _successCommonChance + _successShiningChance;
	}
}
