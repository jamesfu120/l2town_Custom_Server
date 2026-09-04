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
package org.l2jmobius.gameserver.network.clientpackets.classchange;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClassChangeRequest;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Galagard
 */
public class ExClassChange extends ClientPacket
{
	private int _classId;
	private int _raceId;
	private int _sex;
	private int _jobGroup;
	private boolean _extractSkill;
	private int _commissionId;
	
	@Override
	protected void readImpl()
	{
		_classId = readInt();
		_raceId = readInt();
		_sex = readInt();
		_jobGroup = readInt();
		final int extractRaw = readByte() & 0xFF;
		_extractSkill = extractRaw != 0;
		_commissionId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLASS_CHANGE_REQUEST, player))
		{
			EventDispatcher.getInstance().notifyEvent(new OnPlayerClassChangeRequest(player, _classId, _raceId, _sex, _jobGroup, _extractSkill, _commissionId), player);
		}
	}
}