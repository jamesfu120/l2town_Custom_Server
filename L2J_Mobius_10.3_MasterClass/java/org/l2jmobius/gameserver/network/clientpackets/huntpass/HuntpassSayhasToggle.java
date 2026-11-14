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
package org.l2jmobius.gameserver.network.clientpackets.huntpass;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.HuntPass;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassSayhasSupportInfo;

/**
 * @author Serenitty
 */
public class HuntpassSayhasToggle extends ClientPacket
{
	private boolean _sayhaToggle;
	
	@Override
	protected void readImpl()
	{
		_sayhaToggle = readByte() != 0;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final HuntPass huntPass = player.getHuntPass();
		if (huntPass == null)
		{
			return;
		}
		
		// Only a change of state: activating again restarted the span without counting the time used so far.
		if (_sayhaToggle == huntPass.toggleSayha())
		{
			player.sendPacket(new HuntPassSayhasSupportInfo(player));
			return;
		}
		
		if (!_sayhaToggle)
		{
			huntPass.setSayhasSustention(false);
		}
		else if (player.getVitalityPoints() < 35000) // Required to activate only, deactivating is always allowed.
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_ACTIVATE_YOU_CAN_USE_VITALITY_SUSTENTION_EFFECT_OF_THE_SEASON_PASS_ONLY_IF_YOU_HAVE_AT_LEAST_35_000_VITALITY_POINTS);
			return;
		}
		else if ((huntPass.getAvailableSayhaTime() > 0) && (huntPass.getAvailableSayhaTime() > huntPass.getUsedSayhaTime()))
		{
			huntPass.setSayhasSustention(true);
		}
		
		player.sendPacket(new HuntPassSayhasSupportInfo(player));
	}
}
