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
package org.l2jmobius.gameserver.network.serverpackets.classchange;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Galagard
 */
public class ExClassChangeUiOpen extends ServerPacket
{
	private final List<Integer> _prevClassList;
	private final int _changeClassType;
	private final Map<Integer, List<Skill>> _highGradeSkillHistory;
	private final int _extractType;
	
	public ExClassChangeUiOpen(List<Integer> prevClassList, int changeClassType, Map<Integer, List<Skill>> highGradeSkillHistory, int extractType)
	{
		_prevClassList = prevClassList;
		_changeClassType = changeClassType;
		_highGradeSkillHistory = highGradeSkillHistory;
		_extractType = extractType;
	}
	
	@Override
	protected void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CLASS_CHANGE_UI_OPEN.writeId(this, buffer);
		
		buffer.writeInt(_prevClassList.size());
		for (int classId : _prevClassList)
		{
			buffer.writeInt(classId);
		}
		
		buffer.writeInt(_changeClassType);
		
		buffer.writeInt(_highGradeSkillHistory.size());
		for (Entry<Integer, List<Skill>> entry : _highGradeSkillHistory.entrySet())
		{
			buffer.writeInt(entry.getKey()); // nClassType
			buffer.writeInt(entry.getValue().size());
			for (Skill skill : entry.getValue())
			{
				buffer.writeInt(skill.getId());
				buffer.writeInt(skill.getLevel());
				buffer.writeInt(skill.getSubLevel());
			}
		}
		
		buffer.writeByte(_extractType);
	}
}