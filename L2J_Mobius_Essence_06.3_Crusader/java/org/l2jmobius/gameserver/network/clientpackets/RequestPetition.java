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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * <p>
 * Format: (c) Sd
 * <ul>
 * <li>S: content</li>
 * <li>d: type</li>
 * </ul>
 * </p>
 * @author -Wooden-, TempyIncursion, JankisLT
 */
public class RequestPetition extends ClientPacket
{
	private String _content;
	private int _type; // 1 = on : 0 = off;
	
	@Override
	protected void readImpl()
	{
		_content = readString();
		_type = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_type <= 0) || (_type >= 10))
		{
			return;
		}
		
		if (!AdminData.getInstance().isGmOnline(false))
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_GMS_CURRENTLY_VISIBLE_IN_THE_PUBLIC_LIST_AS_THEY_MAY_BE_PERFORMING_OTHER_FUNCTIONS_AT_THE_MOMENT);
			return;
		}
		
		if (!PetitionManager.getInstance().isPetitioningAllowed())
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_CONNECT_TO_THE_GLOBAL_SUPPORT_SERVER);
			return;
		}
		
		if (PetitionManager.getInstance().isPlayerPetitionPending(player))
		{
			player.sendPacket(SystemMessageId.YOUR_GLOBAL_SUPPORT_REQUEST_WAS_RECEIVED);
			return;
		}
		
		if (PetitionManager.getInstance().getPendingPetitionCount() >= PlayerConfig.MAX_PETITIONS_PENDING)
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_SEND_YOUR_REQUEST_TO_THE_GLOBAL_SUPPORT_PLEASE_TRY_AGAIN_LATER);
			return;
		}
		
		final int totalPetitions = PetitionManager.getInstance().getPlayerTotalPetitionCount(player) + 1;
		if (totalPetitions > PlayerConfig.MAX_PETITIONS_PER_PLAYER)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUBMITTED_MAXIMUM_NUMBER_OF_S1_GLOBAL_SUPPORT_REQUESTS_TODAY_YOU_CANNOT_SUBMIT_MORE_REQUESTS);
			sm.addInt(PlayerConfig.MAX_PETITIONS_PER_PLAYER);
			player.sendPacket(sm);
			return;
		}
		
		// The client allows up to 799 characters and the message says 800; 255 refused most real petitions.
		if (_content.length() >= 800)
		{
			player.sendPacket(SystemMessageId.YOUR_GLOBAL_SUPPORT_REQUEST_CAN_CONTAIN_UP_TO_800_CHARACTERS);
			return;
		}
		
		final int petitionId = PetitionManager.getInstance().submitPetition(player, _content, _type);
		SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_GLOBAL_SUPPORT_REQUEST_WAS_RECEIVED_REQUEST_NO_S1);
		sm.addInt(petitionId);
		player.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.SUPPORT_RECEIVED_S1_TIME_S_GLOBAL_SUPPORT_REQUESTS_LEFT_FOR_TODAY_S2);
		sm.addInt(totalPetitions);
		sm.addInt(PlayerConfig.MAX_PETITIONS_PER_PLAYER - totalPetitions);
		player.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.S1_USERS_ARE_IN_LINE_TO_GET_THE_GLOBAL_SUPPORT);
		sm.addInt(PetitionManager.getInstance().getPendingPetitionCount());
		player.sendPacket(sm);
	}
}
