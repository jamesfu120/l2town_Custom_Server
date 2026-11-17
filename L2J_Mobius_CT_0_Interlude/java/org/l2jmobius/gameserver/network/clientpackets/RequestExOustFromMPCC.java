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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * D0 0F 00 5A 00 77 00 65 00 72 00 67 00 00 00
 * @author -Wooden-, JankisLT
 */
public class RequestExOustFromMPCC extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Player target = World.getPlayer(_name);
		if ((target != null) && target.isInParty() && player.isInParty() && player.getParty().isInCommandChannel() && target.getParty().isInCommandChannel() && player.getParty().getCommandChannel().getLeader().equals(player) && player.getParty().getCommandChannel().equals(target.getParty().getCommandChannel()))
		{
			// Naming a member of the leader's own party ousted that party instead of the leader using leave.
			if (target.getParty() == player.getParty())
			{
				return;
			}
			
			target.getParty().getCommandChannel().removeParty(target.getParty());
			
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WERE_DISMISSED_FROM_THE_COMMAND_CHANNEL);
			target.getParty().broadcastPacket(sm);
			
			// Check if CC has not been canceled.
			if (player.getParty().isInCommandChannel())
			{
				sm = new SystemMessage(SystemMessageId.S1_S_PARTY_HAS_BEEN_DISMISSED_FROM_THE_COMMAND_CHANNEL);
				sm.addString(target.getParty().getLeader().getName());
				player.getParty().getCommandChannel().broadcastPacket(sm);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
		}
	}
}
