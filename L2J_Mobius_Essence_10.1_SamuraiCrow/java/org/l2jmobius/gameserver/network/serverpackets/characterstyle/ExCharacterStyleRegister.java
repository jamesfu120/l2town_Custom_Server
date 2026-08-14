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
package org.l2jmobius.gameserver.network.serverpackets.characterstyle;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Brado
 */
public class ExCharacterStyleRegister extends ServerPacket
{
	public static final ExCharacterStyleRegister STATIC_PACKET_FAIL = new ExCharacterStyleRegister(0, 0);
	
	private final int _result;
	private final int _styleInfo;
	
	/**
	 * Private constructor to force use of STATIC_PACKET_FAIL or success(id) method.
	 * @param result
	 * @param styleInfo
	 */
	private ExCharacterStyleRegister(int result, int styleInfo)
	{
		_result = result;
		_styleInfo = styleInfo;
	}
	
	/**
	 * Elegant way to send success with the dynamic style ID.
	 * @param styleInfo The ID from Request packet.
	 * @return A new instance of ExCharacterStyleRegister.
	 */
	public static ExCharacterStyleRegister success(int styleInfo)
	{
		return new ExCharacterStyleRegister(1, styleInfo);
	}
	
	@Override
	protected void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHARACTER_STYLE_REGIST.writeId(this, buffer);
		
		buffer.writeInt(_result); // cResult
		
		buffer.writeInt(_styleInfo); // 542 - nStyleInfo;
		// buffer.writeInt(0); // 541 - nCommissionClassID; - not needed in 542
		// buffer.writeLong(0); // 541 - nCommissionAmount; - not needed in 542
	}
}