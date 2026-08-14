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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * @author Galagard
 */
public class OnPlayerClassChangeRequest implements IBaseEvent
{
	private final Player _player;
	private final int _classId;
	private final int _raceId;
	private final int _sex;
	private final int _jobGroup;
	private final boolean _extractSkill;
	private final int _commissionId;
	
	public OnPlayerClassChangeRequest(Player player, int classId, int raceId, int sex, int jobGroup, boolean extractSkill, int commissionId)
	{
		_player = player;
		_classId = classId;
		_raceId = raceId;
		_sex = sex;
		_jobGroup = jobGroup;
		_extractSkill = extractSkill;
		_commissionId = commissionId;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public int getClassId()
	{
		return _classId;
	}
	
	public int getRaceId()
	{
		return _raceId;
	}
	
	public int getSex()
	{
		return _sex;
	}
	
	public int getJobGroup()
	{
		return _jobGroup;
	}
	
	public boolean isExtractSkill()
	{
		return _extractSkill;
	}
	
	public int getCommissionId()
	{
		return _commissionId;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLASS_CHANGE_REQUEST;
	}
}