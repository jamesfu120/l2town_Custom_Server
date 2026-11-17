package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.actor.holders.player.ClientSettings;
import org.l2jmobius.gameserver.entity.actor.request.PartyRequest;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinParty;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestJoinParty extends ClientPacket
{
	private String _name;
	private int _partyDistributionTypeId;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
		_partyDistributionTypeId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player requestor = getPlayer();
		if (requestor == null)
		{
			return;
		}
		
		final ClientSettings clientSettings = requestor.getClientSettings();
		if (clientSettings.getPartyContributionType() != _partyDistributionTypeId)
		{
			requestor.getClientSettings().setPartyContributionType(_partyDistributionTypeId);
		}
		
		final Player target;
		if (requestor.isInsideZone(ZoneId.CONQUEST))
		{
			final WorldObject requestorTarget = requestor.getTarget();
			if ((requestorTarget != null) && requestorTarget.isPlayer())
			{
				target = requestorTarget.asPlayer();
			}
			else
			{
				Player found = null;
				for (Player player : World.getPlayers())
				{
					final String conquestName = player.getConquestName();
					if ((conquestName != null) && _name.equalsIgnoreCase(conquestName.substring(1)))
					{
						found = player;
						break;
					}
				}
				target = found;
			}
			
			if ((target == null) || !target.isInsideZone(ZoneId.CONQUEST))
			{
				requestor.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
				return;
			}
		}
		else
		{
			target = World.getPlayer(_name);
		}
		
		if (target == null)
		{
			requestor.sendPacket(SystemMessageId.SELECT_A_PLAYER_YOU_WANT_TO_INVITE_TO_YOUR_PARTY);
			return;
		}
		
				if (target.isFakePlayer())
		{
			// 情況 A：如果假人已經有隊伍了 -> 只要你目前沒有隊伍且對方未滿員，直接讓你加入他們的隊伍！
			if (target.isInParty())
			{
				final Party targetParty = target.getParty();
				if (!requestor.isInParty() && (targetParty.getMemberCount() < PlayerConfig.ALT_PARTY_MAX_MEMBERS))
				{
					requestor.joinParty(targetParty);
					requestor.sendMessage("你已成功加入假人「" + target.getName() + "」的練功隊伍！");
					return;
				}
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED);
				sm.addString(target.getAppearance().getVisibleName());
				requestor.sendPacket(sm);
				return;
			}
			
			// 情況 B：如果假人目前沒有隊伍 -> 維持原本的秒同意加入你的隊伍
			final PartyDistributionType distType = PartyDistributionType.findById(_partyDistributionTypeId);
			Party party = requestor.getParty();
			if (party == null)
			{
				party = new Party(requestor, distType != null ? distType : PartyDistributionType.FINDERS_KEEPERS);
				requestor.setParty(party);
			}
			else if (!party.isLeader(requestor))
			{
				requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
				return;
			}
			
			target.joinParty(party);
			requestor.sendMessage("假人 " + target.getName() + " 已秒速同意並加入你的隊伍！");
			return;
		}

		if ((target.getClient() == null) || target.getClient().isDetached())
		{
			requestor.sendMessage("Player is in offline mode.");
			return;
		}
		
		if (requestor.isPartyBanned())
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_SO_PARTICIPATING_IN_A_PARTY_IS_NOT_ALLOWED);
			requestor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isPartyBanned())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_CANNOT_JOIN_A_PARTY);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (requestor.isRegisteredOnEvent() || target.isRegisteredOnEvent())
		{
			if (GeneralConfig.ALLOW_PARTY_IN_SAME_EVENT)
			{
				if (!((requestor.getInstanceId() == target.getInstanceId()) && requestor.isRegisteredOnEvent() && target.isRegisteredOnEvent()))
				{
					requestor.sendMessage("Event paticipants cannot be invited to parties.");
					return;
				}
				
				if (!requestor.getTeam().equals(target.getTeam()))
				{
					requestor.sendMessage("You cannot be invited to a party of another team.");
					return;
				}
			}
			else
			{
				requestor.sendMessage("Event paticipants cannot be invited to parties.");
				return;
			}
		}
		
		SystemMessage sm;
		if (target.isInParty())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (BlockList.isBlocked(target, requestor))
		{
			sm = new SystemMessage(SystemMessageId.C1_HAS_ADDED_YOU_TO_THEIR_IGNORE_LIST);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (target == requestor)
		{
			requestor.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
			return;
		}
		
		if (checkInviteByIgnoredSettings(target, requestor))
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_PARTY_REQUESTS_AND_CANNOT_RECEIVE_A_PARTY_REQUEST).addPcName(target));
			target.sendPacket(new SystemMessage(SystemMessageId.PARTY_INVITATION_IS_SET_UP_TO_BE_REJECTED_AT_PREFERENCES_THE_PARTY_INVITATION_OF_C1_IS_AUTOMATICALLY_REJECTED).addPcName(requestor));
			return;
		}
		
		if (target.isJailed() || requestor.isJailed())
		{
			requestor.sendMessage("You cannot invite a player while is in Jail.");
			return;
		}
		
		if ((target.isInOlympiadMode() || requestor.isInOlympiadMode()) && ((target.isInOlympiadMode() != requestor.isInOlympiadMode()) || (target.getOlympiadGameId() != requestor.getOlympiadGameId()) || (target.getOlympiadSide() != requestor.getOlympiadSide())))
		{
			requestor.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return;
		}
		
		if (requestor.isProcessingRequest())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		
		if (target.isProcessingRequest())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
			return;
		}
		
		final Party party = requestor.getParty();
		if ((party != null) && !party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}
		
		sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_INVITED_TO_THE_PARTY);
		sm.addString(target.getAppearance().getVisibleName());
		requestor.sendPacket(sm);
		
		if (!requestor.isInParty())
		{
			createNewParty(target, requestor);
		}
		else
		{
			addTargetToParty(target, requestor);
		}
	}
	
	private void addTargetToParty(Player target, Player requestor)
	{
		final Party party = requestor.getParty();
		if (!party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
		}
		else if (party.getMemberCount() >= PlayerConfig.ALT_PARTY_MAX_MEMBERS)
		{
			requestor.sendPacket(SystemMessageId.THE_PARTY_IS_FULL);
		}
		else if (party.getPendingInvitation() && !party.isInvitationRequestExpired())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
		}
		else if (!target.hasRequest(PartyRequest.class))
		{
			final PartyRequest request = new PartyRequest(requestor, target, party);
			request.scheduleTimeout(30 * 1000);
			requestor.addRequest(request);
			target.addRequest(request);
			target.sendPacket(new AskJoinParty(requestor.getAppearance().getVisibleName(), party.getDistributionType()));
			party.setPendingInvitation(true);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
		}
	}
	
	private void createNewParty(Player target, Player requestor)
	{
		final PartyDistributionType partyDistributionType = PartyDistributionType.findById(_partyDistributionTypeId);
		if (partyDistributionType == null)
		{
			return;
		}
		
		if (!target.hasRequest(PartyRequest.class))
		{
			final Party party = new Party(requestor, partyDistributionType);
			party.setPendingInvitation(true);
			final PartyRequest request = new PartyRequest(requestor, target, party);
			request.scheduleTimeout(30 * 1000);
			requestor.addRequest(request);
			target.addRequest(request);
			target.sendPacket(new AskJoinParty(requestor.getAppearance().getVisibleName(), partyDistributionType));
		}
		else
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
		}
	}
	
	private boolean checkInviteByIgnoredSettings(Player target, Player requestor)
	{
		final ClientSettings targetClientSettings = target.getClientSettings();
		boolean condition = targetClientSettings.isPartyRequestRestrictedFromOthers();
		final boolean clanCheck = (target.getClan() != null) && (requestor.getClan() != null) && (target.getClan() == requestor.getClan());
		if (condition && ((!targetClientSettings.isPartyRequestRestrictedFromFriends() && target.getFriendList().contains(requestor.getObjectId())) || (!targetClientSettings.isPartyRequestRestrictedFromClan() && clanCheck)))
		{
			condition = false;
		}
		
		return condition;
	}
}
