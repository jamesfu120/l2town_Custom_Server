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
package org.l2jmobius.gameserver.network.clientpackets.adenadistribution;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.AdenaDistributionRequest;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaStart;

/**
 * @author Sdw, JankisLT
 */
public class RequestDivideAdenaStart extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISTRIBUTE_ADENA_IF_YOU_ARE_NOT_A_MEMBER_OF_AN_ALLIANCE_OR_A_COMMAND_CHANNEL);
			return;
		}
		
		final CommandChannel commandChannel = party.getCommandChannel();
		if ((commandChannel != null) && !commandChannel.isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PROCEED_AS_YOU_ARE_NOT_AN_ALLIANCE_LEADER_OR_PARTY_LEADER);
			return;
		}
		else if (!party.isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PROCEED_AS_YOU_ARE_NOT_A_PARTY_LEADER);
			return;
		}
		
		// A copy: the party list is live, so the request could never see a member leave or join.
		final List<Player> targets = new ArrayList<>(commandChannel != null ? commandChannel.getMembers() : party.getMembers());
		if (player.getAdena() < targets.size())
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
			return;
		}
		
		if (targets.stream().anyMatch(t -> t.hasRequest(AdenaDistributionRequest.class)))
		{
			// Handle that case ?
			return;
		}
		
		final int adenaObjectId = player.getInventory().getAdenaInstance().getObjectId();
		targets.forEach(t ->
		{
			t.sendPacket(SystemMessageId.ADENA_DISTRIBUTION_HAS_STARTED);
			t.addRequest(new AdenaDistributionRequest(t, player, targets, adenaObjectId, player.getAdena()));
		});
		
		player.sendPacket(ExDivideAdenaStart.STATIC_PACKET);
	}
}
