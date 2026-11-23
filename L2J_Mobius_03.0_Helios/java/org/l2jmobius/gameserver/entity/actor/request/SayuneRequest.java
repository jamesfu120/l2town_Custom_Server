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
package org.l2jmobius.gameserver.entity.actor.request;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.l2jmobius.gameserver.data.holders.SayuneEntry;
import org.l2jmobius.gameserver.data.xml.SayuneData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.SayuneType;
import org.l2jmobius.gameserver.network.serverpackets.sayune.ExFlyMove;
import org.l2jmobius.gameserver.network.serverpackets.sayune.ExFlyMoveBroadcast;

/**
 * @author UnAfraid, Notorion
 */
public class SayuneRequest extends AbstractRequest
{
	private final int _mapId;
	private final Location _startLocation;
	private boolean _isSelecting;
	private final Deque<SayuneEntry> _possibleEntries = new ArrayDeque<>();
	
	public SayuneRequest(Player player, int mapId)
	{
		super(player);
		_mapId = mapId;
		_startLocation = new Location(player.getX(), player.getY(), player.getZ());
		
		final SayuneEntry map = SayuneData.getInstance().getMap(_mapId);
		Objects.requireNonNull(map);
		
		_possibleEntries.addAll(map.getInnerEntries());
	}
	
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
	
	private SayuneEntry findEntry(int pos)
	{
		if (_possibleEntries.isEmpty())
		{
			return null;
		}
		else if (_isSelecting)
		{
			for (SayuneEntry entry : _possibleEntries)
			{
				if (entry.getId() == pos)
				{
					return entry;
				}
			}
			
			return null;
		}
		
		return _possibleEntries.removeFirst();
	}
	
	public synchronized void move(Player player, int pos)
	{
		final SayuneEntry map = SayuneData.getInstance().getMap(_mapId);
		if ((map == null) || map.getInnerEntries().isEmpty())
		{
			player.sendMessage("MapId: " + _mapId + " was not found in the map!");
			return;
		}
		
		final SayuneEntry nextEntry = findEntry(pos);
		if (nextEntry == null)
		{
			player.removeRequest(getClass());
			return;
		}
		
		// If player was selecting unset and set his possible path.
		if (_isSelecting)
		{
			_isSelecting = false;
			
			// Set next possible path.
			if (!nextEntry.isSelector())
			{
				_possibleEntries.clear();
				_possibleEntries.addAll(nextEntry.getInnerEntries());
			}
		}
		
		final SayuneType type = (pos == 0) && nextEntry.isSelector() ? SayuneType.START_LOC : nextEntry.isSelector() ? SayuneType.MULTI_WAY_LOC : SayuneType.ONE_WAY_LOC;
		final List<SayuneEntry> locations = nextEntry.isSelector() ? nextEntry.getInnerEntries() : Arrays.asList(nextEntry);
		if (nextEntry.isSelector())
		{
			_possibleEntries.clear();
			_possibleEntries.addAll(locations);
			_isSelecting = true;
		}
		
		player.sendPacket(new ExFlyMove(player, type, _mapId, locations));
		
		final SayuneEntry activeEntry = locations.get(0);
		World.broadcastToVisiblePlayersInRange(player, new ExFlyMoveBroadcast(player, type, map.getId(), activeEntry), 1000);
		player.setXYZ(activeEntry);
	}
	
	public void onLogout()
	{
		getPlayer().setXYZ(_startLocation);
	}
}
