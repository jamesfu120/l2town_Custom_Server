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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Galagard
 */
public class ExRelicsExchange extends ServerPacket
{
	private final int _index;
	private final boolean _cResult;
	private final int _remainCount;
	private final int _maxCount;
	private final int _relicsId;
	
	public ExRelicsExchange(int index, boolean success, int remainCount, int maxCount, int relicsId)
	{
		_index = index;
		_cResult = success;
		_remainCount = remainCount;
		_maxCount = maxCount;
		_relicsId = relicsId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE.writeId(this, buffer);
		buffer.writeInt(_index); // nIndex
		buffer.writeByte(_cResult ? 1 : 0); // cResult
		buffer.writeInt(_remainCount); // nRemainCount
		buffer.writeInt(_maxCount); // nMaxCount
		buffer.writeInt(_relicsId); // nRelicsID
	}
}