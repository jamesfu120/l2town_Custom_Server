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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsExchangeList extends ServerPacket
{
	private final Player _player;
	
	public ExRelicsExchangeList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE_LIST.writeId(this, buffer);
		
		// Max attempts are now loaded from RelicData.xml instead of config.
		final int maxBGradeAttempts = RelicData.getInstance().getReplaceAttempts(RelicGrade.BGRADE);
		final int maxAGradeAttempts = RelicData.getInstance().getReplaceAttempts(RelicGrade.AGRADE);
		
		final int attemptsRemainingCountBgrade = Math.max(0, _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, maxBGradeAttempts));
		final int attemptsRemainingCountAgrade = Math.max(0, _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, maxAGradeAttempts));
		final int maxRelicsOnConfirmationList = RelicSystemConfig.RELIC_UNCONFIRMED_LIST_LIMIT;
		final long defaultReplacementTime = RelicSystemConfig.RELIC_UNCONFIRMED_TIME_LIMIT * 86400000L; // 1 day in ms
		
		long relicSummonTime = 0;
		final List<Integer> unconfirmedRelics = new ArrayList<>();
		for (PlayerRelicData relic : _player.getRelics())
		{
			if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() == 1)) // Unconfirmed relics are set on summon to index 300 and up.
			{
				unconfirmedRelics.add(relic.getRelicId());
				relicSummonTime = relic.getRelicSummonTime();
			}
		}
		
		buffer.writeInt(maxRelicsOnConfirmationList);
		buffer.writeInt(unconfirmedRelics.size()); // Confirmation relics array size.
		for (int i = 0; i < unconfirmedRelics.size(); i++)
		{
			final RelicGrade unconfirmedRelicGrade = RelicData.getInstance().getRelic(unconfirmedRelics.get(i)).getGrade();
			final int maxAttemptsForGrade = RelicData.getInstance().getReplaceAttempts(unconfirmedRelicGrade);
			buffer.writeInt(i); // List position.
			buffer.writeInt(unconfirmedRelics.get(i)); // Relic Id.
			buffer.writeInt(unconfirmedRelicGrade == RelicGrade.BGRADE ? attemptsRemainingCountBgrade : attemptsRemainingCountAgrade); // Exchange attempts remaining.
			buffer.writeInt(maxAttemptsForGrade); // Exchange attempts max (from XML).
			buffer.writeInt((int) ((relicSummonTime + defaultReplacementTime) / 1000)); // Replacement Time.
		}
	}
}
