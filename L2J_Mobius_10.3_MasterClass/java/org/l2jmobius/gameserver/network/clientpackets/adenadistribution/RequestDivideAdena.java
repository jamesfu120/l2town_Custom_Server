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

import java.util.List;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.AdenaDistributionRequest;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaCancel;
import org.l2jmobius.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaDone;

/**
 * @author Sdw, JankisLT
 */
public class RequestDivideAdena extends ClientPacket
{
	private int _adenaObjId;
	private long _adenaCount;
	
	@Override
	protected void readImpl()
	{
		_adenaObjId = readInt();
		_adenaCount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final AdenaDistributionRequest request = player.getRequest(AdenaDistributionRequest.class);
		if (request == null)
		{
			return;
		}
		else if (request.getDistributor() != player)
		{
			cancelDistribution(request);
			return;
		}
		else if (request.getAdenaObjectId() != _adenaObjId)
		{
			cancelDistribution(request);
			return;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISTRIBUTE_ADENA_IF_YOU_ARE_NOT_A_MEMBER_OF_AN_ALLIANCE_OR_A_COMMAND_CHANNEL);
			cancelDistribution(request);
			return;
		}
		
		final CommandChannel commandChannel = party.getCommandChannel();
		if ((commandChannel != null) && !commandChannel.isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PROCEED_AS_YOU_ARE_NOT_AN_ALLIANCE_LEADER_OR_PARTY_LEADER);
			cancelDistribution(request);
			return;
		}
		else if (!party.isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PROCEED_AS_YOU_ARE_NOT_A_PARTY_LEADER);
			cancelDistribution(request);
			return;
		}
		
		final List<Player> targets = commandChannel != null ? commandChannel.getMembers() : party.getMembers();
		if (player.getAdena() < targets.size())
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
			cancelDistribution(request);
			return;
		}
		
		if (player.getAdena() < request.getAdenaCount())
		{
			player.sendPacket(SystemMessageId.THE_ADENA_IN_POSSESSION_HAS_BEEN_DECREASED_ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
			cancelDistribution(request);
			return;
		}
		else if ((targets.size() != request.getPlayers().size()) || !targets.containsAll(request.getPlayers())) // Any leave or join, not only a smaller count.
		{
			player.sendPacket(SystemMessageId.THE_DISTRIBUTION_PARTICIPANTS_HAVE_CHANGED_ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
			cancelDistribution(request);
			return;
		}
		else if ((_adenaCount < 1) || (player.getAdena() < _adenaCount))
		{
			player.sendPacket(SystemMessageId.DISTRIBUTION_CANNOT_PROCEED_AS_THERE_IS_INSUFFICIENT_ADENA_FOR_DISTRIBUTION);
			cancelDistribution(request);
			return;
		}
		
		final long memberAdenaGet = _adenaCount / targets.size();
		
		// addAdena cuts a share at the adena limit, and the cut part was lost after the leader paid in full.
		for (Player target : targets)
		{
			if ((target != null) && (target.getAdena() > (PlayerConfig.MAX_ADENA - memberAdenaGet)))
			{
				player.sendPacket(SystemMessageId.DISTRIBUTION_CANNOT_PROCEED_AS_THERE_IS_INSUFFICIENT_ADENA_FOR_DISTRIBUTION);
				cancelDistribution(request);
				return;
			}
		}
		
		if (player.reduceAdena(ItemProcessType.TRANSFER, memberAdenaGet * targets.size(), player, false))
		{
			for (Player target : targets)
			{
				if (target == null)
				{
					// TODO : handle that case here + regive adena OR filter with Objects::nonNull on memberCount ?
					// Those sys msg exists and bother me ADENA_WAS_NOT_DISTRIBUTED_TO_S1 / YOU_DID_NOT_RECEIVE_ADENA_DISTRIBUTION.
					continue;
				}
				
				target.addAdena(ItemProcessType.TRANSFER, memberAdenaGet, player, false);
				target.sendPacket(new ExDivideAdenaDone(party.isLeader(target), (commandChannel != null) && commandChannel.isLeader(target), _adenaCount, memberAdenaGet, targets.size(), player.getName()));
				target.removeRequest(AdenaDistributionRequest.class);
			}
		}
		else
		{
			cancelDistribution(request);
		}
	}
	
	private void cancelDistribution(AdenaDistributionRequest request)
	{
		for (Player player : request.getPlayers())
		{
			if (player != null)
			{
				player.sendPacket(SystemMessageId.ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
				player.sendPacket(ExDivideAdenaCancel.STATIC_PACKET);
				player.removeRequest(AdenaDistributionRequest.class);
			}
		}
	}
}
