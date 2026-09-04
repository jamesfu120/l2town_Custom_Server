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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Holds the replacement attempt configuration for a specific relic grade.<br>
 * Maps to the {@code <replaceCategory>} XML element in RelicData.xml.
 * @author CostyKiller
 */
public class RelicReplaceCategoryHolder
{
	private final RelicGrade _grade;
	private final int _replaceAttempts;
	private final List<ItemHolder> _fees;
	
	public RelicReplaceCategoryHolder(RelicGrade grade, int replaceAttempts, List<ItemHolder> fees)
	{
		_grade = grade;
		_replaceAttempts = replaceAttempts;
		_fees = fees;
	}
	
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Maximum number of replacement attempts allowed per reset cycle.
	 * @return
	 */
	public int getReplaceAttempts()
	{
		return _replaceAttempts;
	}
	
	/**
	 * Ordered fee list - index 0 is the fee for the first use, last index is for the final attempt.
	 * @return
	 */
	public List<ItemHolder> getFees()
	{
		return _fees;
	}
}
