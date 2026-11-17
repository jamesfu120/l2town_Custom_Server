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
package org.l2jmobius.gameserver.network.clientpackets.castlewar;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * @author Mobius
 */
public class ExCastleWarObserverStart extends ClientPacket
{
	private int _castleId;
	
	@Override
	protected void readImpl()
	{
		_castleId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// A second start overwrote the return point (free teleport into the siege); observing also made a fighter invulnerable and left instances.
		if (player.inObserverMode())
		{
			player.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			return;
		}
		
		if (player.isDead() || AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isInOlympiadMode() || player.isInInstance() || player.isInStoreMode() || player.isCursedWeaponEquipped() || player.isJailed() || player.isTeleporting())
		{
			return;
		}
		
		if (player.hasSummon())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_OBSERVE_A_SIEGE_WITH_A_SERVITOR_SUMMONED);
			return;
		}
		
		if (player.isOnEvent())
		{
			player.sendMessage("Cannot use while on an event.");
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle == null)
		{
			return;
		}
		
		if (!castle.getSiege().isInProgress())
		{
			return;
		}
		
		final Player random = castle.getSiege().getPlayersInZone().stream().findAny().orElse(null);
		if (random == null)
		{
			return;
		}
		
		player.enterObserverMode(random.getLocation());
	}
}
