/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.entity.teleporter.TeleportLocation;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * Player teleport request listner - called from {@link TeleportHolder#doTeleport(Player, Npc, int)}
 * @author malyelfik
 */
public class OnNpcTeleportRequest implements IBaseEvent
{
	private final Player _player;
	private final Npc _npc;
	private final TeleportLocation _loc;
	
	public OnNpcTeleportRequest(Player player, Npc npc, TeleportLocation loc)
	{
		_player = player;
		_npc = npc;
		_loc = loc;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public Npc getNpc()
	{
		return _npc;
	}
	
	public TeleportLocation getLocation()
	{
		return _loc;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_TELEPORT_REQUEST;
	}
}
