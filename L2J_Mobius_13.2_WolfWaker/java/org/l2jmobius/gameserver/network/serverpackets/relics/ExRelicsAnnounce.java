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
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsAnnounce extends ServerPacket
{
	public static final int TYPE_SUMMON = 0;
	public static final int TYPE_COMPOUND = 1;
	
	private final String _announceName;
	private final int _relicId;
	private final boolean _shouldSend;
	
	public ExRelicsAnnounce(Player player, int relicId)
	{
		this(player, relicId, TYPE_COMPOUND);
	}
	
	public ExRelicsAnnounce(Player player, int relicId, int announceType)
	{
		_relicId = relicId;
		
		// Check announce config based on type.
		final boolean announceEnabled = announceType == TYPE_SUMMON ? RelicSystemConfig.RELIC_SUMMON_ANNOUNCE : RelicSystemConfig.RELIC_COMPOUND_ANNOUNCE;
		if (!announceEnabled)
		{
			_announceName = "";
			_shouldSend = false;
			return;
		}
		
		// If only A/B grade should be announced, check the relic grade.
		if (RelicSystemConfig.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
		{
			final var holder = RelicData.getInstance().getRelic(relicId);
			if ((holder == null) || ((holder.getGrade() != RelicGrade.BGRADE) && (holder.getGrade() != RelicGrade.AGRADE)))
			{
				_announceName = "";
				_shouldSend = false;
				return;
			}
		}
		
		_shouldSend = true;
		if (!player.getClientSettings().isAnnounceDisabled())
		{
			_announceName = player.getName();
		}
		else if ("ru".equals(player.getLang()))
		{
			_announceName = "Некто";
		}
		else
		{
			_announceName = "Someone";
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		if (!_shouldSend)
		{
			return;
		}
		ServerPackets.EX_RELICS_ANNOUNCE.writeId(this, buffer);
		buffer.writeSizedString(_announceName);
		buffer.writeInt(_relicId);
	}
}
