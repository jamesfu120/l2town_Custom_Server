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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Norvox
 */
public class ExEnemyKillLog extends ServerPacket
{
	private final boolean _isEnemy;
	private final String _killerName;
	private final int _killerClass;
	private final int _killerRace;
	private final int _killerSex;
	private final int _killerKills;
	private final String _targetName;
	private final int _targetClass;
	private final int _targetRace;
	private final int _targetSex;
	private final int _targetKills;
	
	public ExEnemyKillLog(Player killer, Player victim, boolean isEnemy)
	{
		_isEnemy = isEnemy;
		_killerName = killer.getName();
		_killerClass = killer.getPlayerClass().getId();
		_killerRace = killer.getRace().ordinal();
		_killerSex = killer.getAppearance().getSexType().ordinal();
		_killerKills = killer.getPvpStreak();
		_targetName = victim.getName();
		_targetClass = victim.getPlayerClass().getId();
		_targetRace = victim.getRace().ordinal();
		_targetSex = victim.getAppearance().getSexType().ordinal();
		_targetKills = victim.getPvpStreak();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENEMY_KILL_LOG.writeId(this, buffer);
		buffer.writeByte(_isEnemy);
		buffer.writeShort(100);
		buffer.writeSizedString(_killerName);
		buffer.writeInt(_killerClass);
		buffer.writeInt(_killerRace);
		buffer.writeInt(_killerSex);
		buffer.writeInt(_killerKills);
		buffer.writeShort(100);
		buffer.writeSizedString(_targetName);
		buffer.writeInt(_targetClass);
		buffer.writeInt(_targetRace);
		buffer.writeInt(_targetSex);
		buffer.writeInt(_targetKills);
	}
}