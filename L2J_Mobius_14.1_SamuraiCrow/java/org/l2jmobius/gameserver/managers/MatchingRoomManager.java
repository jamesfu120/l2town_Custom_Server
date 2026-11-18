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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoomType;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchingRoomLevelType;

/**
 * @author Sdw, JankisLT
 */
public class MatchingRoomManager
{
	private Set<Player> _waitingList;
	
	private static final Map<MatchingRoomType, Map<Integer, MatchingRoom>> _rooms = new ConcurrentHashMap<>(2);
	
	private final AtomicInteger _id = new AtomicInteger(0);
	
	public void addToWaitingList(Player player)
	{
		if (_waitingList == null)
		{
			synchronized (this)
			{
				if (_waitingList == null)
				{
					_waitingList = ConcurrentHashMap.newKeySet(1);
				}
			}
		}
		
		_waitingList.add(player);
	}
	
	public void removeFromWaitingList(Player player)
	{
		getPlayerInWaitingList().remove(player);
	}
	
	public Set<Player> getPlayerInWaitingList()
	{
		return _waitingList == null ? Collections.emptySet() : _waitingList;
	}
	
	public List<Player> getPlayerInWaitingList(int minLevel, int maxLevel, List<PlayerClass> classIds, String query)
	{
		if (_waitingList == null)
		{
			return Collections.emptyList();
		}
		
		// The name is lowered, so the typed query must be too; any capital letter matched nobody.
		final String search = query == null ? null : query.toLowerCase();
		final List<Player> players = new ArrayList<>();
		for (Player player : _waitingList)
		{
			if ((player != null) && (player.getLevel() >= minLevel) && (player.getLevel() <= maxLevel) && ((classIds == null) || classIds.contains(player.getPlayerClass())) && ((search == null) || search.isEmpty() || player.getName().toLowerCase().contains(search)))
			{
				players.add(player);
			}
		}
		
		return players;
	}
	
	public int addMatchingRoom(MatchingRoom room)
	{
		final int roomId = _id.incrementAndGet();
		_rooms.computeIfAbsent(room.getRoomType(), _ -> new ConcurrentHashMap<>()).put(roomId, room);
		return roomId;
	}
	
	public void removeMatchingRoom(MatchingRoom room)
	{
		_rooms.getOrDefault(room.getRoomType(), Collections.emptyMap()).remove(room.getId());
	}
	
	public Map<Integer, MatchingRoom> getPartyMathchingRooms()
	{
		return _rooms.get(MatchingRoomType.PARTY);
	}
	
	public List<MatchingRoom> getPartyMathchingRooms(int location, PartyMatchingRoomLevelType type, int requestorLevel)
	{
		final List<MatchingRoom> result = new ArrayList<>();
		final Map<Integer, MatchingRoom> rooms = _rooms.get(MatchingRoomType.PARTY);
		if (rooms != null)
		{
			for (MatchingRoom room : rooms.values())
			{
				if (((location < 0) || (room.getLocation() == location)) 
					&& ((type == PartyMatchingRoomLevelType.ALL) || ((room.getMinLevel() <= requestorLevel) && (room.getMaxLevel() >= requestorLevel))))
				{
					result.add(room);
				}
			}
		}
		
		return result;
	}
	
	public Map<Integer, MatchingRoom> getCCMathchingRooms()
	{
		return _rooms.get(MatchingRoomType.COMMAND_CHANNEL);
	}
	
	public List<MatchingRoom> getCCMathchingRooms(int location, int level)
	{
		final List<MatchingRoom> result = new ArrayList<>();
		final Map<Integer, MatchingRoom> rooms = _rooms.get(MatchingRoomType.COMMAND_CHANNEL);
		if (rooms != null)
		{
			for (MatchingRoom room : rooms.values())
			{
				if ((room.getLocation() == location) //
					&& ((room.getMinLevel() <= level) && (room.getMaxLevel() >= level)))
				{
					result.add(room);
				}
			}
		}
		
		return result;
	}
	
	public MatchingRoom getCCMatchingRoom(int roomId)
	{
		return _rooms.getOrDefault(MatchingRoomType.COMMAND_CHANNEL, Collections.emptyMap()).get(roomId);
	}
	
	public MatchingRoom getPartyMathchingRoom(int location, int level)
	{
		final Map<Integer, MatchingRoom> rooms = _rooms.get(MatchingRoomType.PARTY);
		if (rooms != null)
		{
			for (MatchingRoom room : rooms.values())
			{
				if ((room.getLocation() == location) //
					&& ((room.getMinLevel() <= level) && (room.getMaxLevel() >= level)))
				{
					return room;
				}
			}
		}
		
		return null;
	}
	
	public MatchingRoom getPartyMathchingRoom(int roomId)
	{
		return _rooms.getOrDefault(MatchingRoomType.PARTY, Collections.emptyMap()).get(roomId);
	}
	
	public static MatchingRoomManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MatchingRoomManager INSTANCE = new MatchingRoomManager();
	}
}
