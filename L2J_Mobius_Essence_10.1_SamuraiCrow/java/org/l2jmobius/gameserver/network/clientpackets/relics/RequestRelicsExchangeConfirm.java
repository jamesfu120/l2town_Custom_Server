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
package org.l2jmobius.gameserver.network.clientpackets.relics;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsExchangeConfirm;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsPointInfo;

/**
 * @author Galagard
 */
public class RequestRelicsExchangeConfirm extends ClientPacket
{
	private int _index;
	private int _relicsId;
	
	@Override
	protected void readImpl()
	{
		_index = readInt(); // nIndex
		_relicsId = readInt(); // nRelicsID
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final String pityKey = "RELIC_EXCHANGE_REMAIN_" + _index;
		final int currentPity = player.getVariables().getInt(pityKey, 0);
		
		// Validate if pity was reached before confirming.
		if (currentPity < 165)
		{
			player.sendPacket(new ExRelicsExchangeConfirm(_index, false, _relicsId));
			return;
		}
		
		// TODO: Deliver the guaranteed doll/relic from the _index slot.
		// RelicGrade grade = RelicGrade.values()[_index];
		// RelicData.getInstance().getGuaranteedRelicByGrade(grade); // It depends on how the data is structured.
		
		// Reset pity progress.
		player.getVariables().set(pityKey, 0);
		player.getVariables().storeMe();
		
		player.sendPacket(new ExRelicsPointInfo(player));
		player.sendPacket(new ExRelicsExchangeConfirm(_index, true, _relicsId));
	}
}