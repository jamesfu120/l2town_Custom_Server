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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.ChatBroadcastType;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExRequestNewInvitePartyInquiry;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Serenitty
 */
public class RequestNewInvitePartyInquiry extends ClientPacket
{
	private int _reqType;
	private ChatType _sayType;
	
	@Override
	protected void readImpl()
	{
		_reqType = readByte();
		
		final int chatTypeValue = readByte();
		
		switch (chatTypeValue)
		{
			case 0:
			{
				_sayType = ChatType.GENERAL;
				break;
			}
			case 1:
			{
				_sayType = ChatType.SHOUT;
				break;
			}
			case 3:
			{
				_sayType = ChatType.PARTY;
				break;
			}
			case 4:
			{
				_sayType = ChatType.CLAN;
				break;
			}
			case 8:
			{
				_sayType = ChatType.TRADE;
				break;
			}
			case 9: // The alliance tab sends 9; without it the ALLIANCE branch was unreachable.
			{
				_sayType = ChatType.ALLIANCE;
				break;
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isChatBanned())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_CHAT_WITH_A_CONTACT_WHILE_A_CHATTING_BLOCK_IS_IMPOSED);
			return;
		}
		
		// Ten second delay.
		// TODO: Create another flood protection for this?
		if (!getClient().getFloodProtectors().canSendMail())
		{
			return;
		}
		
		if (GeneralConfig.JAIL_DISABLE_CHAT && player.isJailed() && !player.isGM())
		{
			player.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CHAT_WHILE_PARTICIPATING_IN_THE_OLYMPIAD);
			return;
		}
		
		if ((_sayType != ChatType.GENERAL) && (_sayType != ChatType.TRADE) && (_sayType != ChatType.SHOUT) && (_sayType != ChatType.CLAN) && (_sayType != ChatType.ALLIANCE))
		{
			return;
		}
		
		switch (_reqType)
		{
			case 0: // Party
			{
				if (player.isInParty())
				{
					return;
				}
				break;
			}
			case 1: // Command Channel
			{
				final Party party = player.getParty();
				if ((party == null) || !party.isLeader(player) || (party.getCommandChannel() != null))
				{
					return;
				}
				break;
			}
			default:
			{
				return;
			}
		}
		
		switch (_sayType)
		{
			case SHOUT:
			{
				if (player.inObserverMode())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CHAT_WHILE_IN_THE_SPECTATOR_MODE);
					return;
				}
				
				if ((player.getLevel() < GeneralConfig.MINIMUM_CHAT_LEVEL) && !player.isGM())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SHOUT_CANNOT_BE_USED_BY_CHARACTERS_LV_S1_OR_LOWER).addInt(GeneralConfig.MINIMUM_CHAT_LEVEL));
					return;
				}
				
				broadcastAsChat(player, new ExRequestNewInvitePartyInquiry(player, _reqType, _sayType), GeneralConfig.DEFAULT_GLOBAL_CHAT);
				break;
			}
			case TRADE:
			{
				if (player.inObserverMode())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CHAT_WHILE_IN_THE_SPECTATOR_MODE);
					return;
				}
				
				if (player.getLevel() < 20)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.TRADE_CHAT_CANNOT_BE_USED_BY_CHARACTERS_LV_S1_OR_LOWER).addInt(20));
					return;
				}
				
				broadcastAsChat(player, new ExRequestNewInvitePartyInquiry(player, _reqType, _sayType), GeneralConfig.DEFAULT_TRADE_CHAT);
				break;
			}
			case GENERAL:
			{
				if (player.inObserverMode())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_CHAT_WHILE_IN_THE_SPECTATOR_MODE);
					return;
				}
				
				final ExRequestNewInvitePartyInquiry msg = new ExRequestNewInvitePartyInquiry(player, _reqType, _sayType);
				player.sendPacket(msg);
				World.forEachVisibleObjectInRange(player, Player.class, PlayerConfig.ALT_PARTY_RANGE, nearby -> nearby.sendPacket(msg));
				break;
			}
			case CLAN:
			{
				final Clan clan = player.getClan();
				if (clan == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_CLAN);
					return;
				}
				
				clan.broadcastToOnlineMembers(new ExRequestNewInvitePartyInquiry(player, _reqType, _sayType));
				break;
			}
			case ALLIANCE:
			{
				if ((player.getClan() == null) || ((player.getClan() != null) && (player.getClan().getAllyId() == 0)))
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_AN_ALLIANCE);
					return;
				}
				
				player.getClan().broadcastToOnlineAllyMembers(new ExRequestNewInvitePartyInquiry(player, _reqType, _sayType));
				break;
			}
		}
	}
	
	/**
	 * Sends like shout or trade chat: same region and instance for ON (GM only for GM), everyone for GLOBAL, nobody for OFF.
	 * It went to every online player, past the chat mode and the receivers' block lists.
	 * @param player the sender
	 * @param msg the inquiry packet
	 * @param mode the chat broadcast mode of that channel
	 */
	private static void broadcastAsChat(Player player, ExRequestNewInvitePartyInquiry msg, ChatBroadcastType mode)
	{
		final boolean regional = (mode == ChatBroadcastType.ON) || ((mode == ChatBroadcastType.GM) && player.isGM());
		if (!regional && (mode != ChatBroadcastType.GLOBAL))
		{
			return;
		}
		
		final int region = MapRegionData.getInstance().getMapRegionLocId(player);
		for (Player target : World.getPlayers())
		{
			if (!BlockList.isBlocked(target, player) && (!regional || ((target.getInstanceId() == player.getInstanceId()) && (MapRegionData.getInstance().getMapRegionLocId(target) == region))))
			{
				target.sendPacket(msg);
			}
		}
	}
}
