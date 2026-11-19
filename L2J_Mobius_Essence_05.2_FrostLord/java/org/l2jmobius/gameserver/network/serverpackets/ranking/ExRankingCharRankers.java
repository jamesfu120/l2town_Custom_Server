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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author NviX, Mobius
 */
public class ExRankingCharRankers extends ServerPacket
{
	private final Player _player;
	private final int _group;
	private final int _scope;
	private final int _race;
	private final int _class;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExRankingCharRankers(Player player, int group, int scope, int race, int baseclass)
	{
		_player = player;
		_group = group;
		_scope = scope;
		_race = race;
		_class = baseclass;
		_playerList = RankManager.getInstance().getRankList();
		_snapshotList = RankManager.getInstance().getSnapshotList();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RANKING_CHAR_RANKERS.writeId(this, buffer);
		buffer.writeByte(_group);
		buffer.writeByte(_scope);
		buffer.writeInt(_race);
		buffer.writeInt(_player.getPlayerClass().getId());
		if (!_playerList.isEmpty())
		{
			switch (_group)
			{
				case 0: // all
				{
					if (_scope == 0) // all
					{
						final int count = _playerList.size() > 150 ? 150 : _playerList.size();
						buffer.writeInt(count);
						for (Entry<Integer, StatSet> idEntry : _playerList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet player = idEntry.getValue();
							buffer.writeSizedString(player.getString("name"));
							buffer.writeSizedString(player.getString("clanName"));
							buffer.writeInt(player.getInt("level"));
							buffer.writeInt(player.getInt("classId"));
							buffer.writeInt(player.getInt("race"));
							buffer.writeInt(id); // server rank
							writeSnapshot(buffer, _snapshotList, player.getInt("charId"), id, 0, 0);
						}
					}
					else
					{
						boolean found = false;
						for (Entry<Integer, StatSet> idEntry : _playerList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet player = idEntry.getValue();
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = _playerList.size() >= (id + 10) ? id + 10 : id + (_playerList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = _playerList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(plr.getInt("level"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("race"));
									buffer.writeInt(id2); // server rank
									writeSnapshot(buffer, _snapshotList, plr.getInt("charId"), id2, 0, 0); // Was the requester's row for every line.
								}
							}
						}
						
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
				case 1: // race
				{
					if (_scope == 0) // all
					{
						int count = 0;
						for (int i = 1; i <= _playerList.size(); i++)
						{
							final StatSet player = _playerList.get(i);
							if (_race == player.getInt("race"))
							{
								count++;
							}
						}
						
						buffer.writeInt(count > 100 ? 100 : count);
						int i = 1;
						for (StatSet player : _playerList.values())
						{
							if (_race == player.getInt("race"))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(player.getInt("level"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("race"));
								buffer.writeInt(i); // server rank
								final Map<Integer, StatSet> snapshotRaceList = new ConcurrentHashMap<>();
								int j = 1;
								for (Entry<Integer, StatSet> id2Entry : _snapshotList.entrySet())
								{
									final Integer id2 = id2Entry.getKey();
									final StatSet snapshot = id2Entry.getValue();
									if (_race == snapshot.getInt("race"))
									{
										snapshotRaceList.put(j, _snapshotList.get(id2));
										j++;
									}
								}
								
								writeSnapshot(buffer, snapshotRaceList, player.getInt("charId"), i, i, i);
								
								i++;
							}
						}
					}
					else
					{
						boolean found = false;
						final Map<Integer, StatSet> raceList = new ConcurrentHashMap<>();
						int i = 1;
						for (Entry<Integer, StatSet> idEntry : _playerList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet set = idEntry.getValue();
							if (_player.getRace().ordinal() == set.getInt("race"))
							{
								raceList.put(i, _playerList.get(id));
								i++;
							}
						}
						
						for (Entry<Integer, StatSet> idEntry : raceList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet player = idEntry.getValue();
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = raceList.size() >= (id + 10) ? id + 10 : id + (raceList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = raceList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(plr.getInt("level"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("race"));
									buffer.writeInt(id2); // server rank
									buffer.writeInt(id2);
									buffer.writeInt(id2);
									buffer.writeInt(id2); // nClassRank_Snapshot
								}
							}
						}
						
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
				case 2: // clan
				{
					final Clan clan = _player.getClan();
					if (clan != null)
					{
						final Map<Integer, StatSet> clanList = new ConcurrentHashMap<>();
						int i = 1;
						for (Entry<Integer, StatSet> idEntry : _playerList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet set = idEntry.getValue();
							if (clan.getName().equals(set.getString("clanName")))
							{
								clanList.put(i, _playerList.get(id));
								i++;
							}
						}
						
						buffer.writeInt(clanList.size());
						for (Entry<Integer, StatSet> idEntry : clanList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet player = idEntry.getValue();
							buffer.writeSizedString(player.getString("name"));
							buffer.writeSizedString(player.getString("clanName"));
							buffer.writeInt(player.getInt("level"));
							buffer.writeInt(player.getInt("classId"));
							buffer.writeInt(player.getInt("race"));
							buffer.writeInt(id); // clan rank
							writeSnapshot(buffer, _snapshotList, player.getInt("charId"), id, 0, 0);
						}
					}
					else
					{
						buffer.writeInt(0);
					}
					break;
				}
				case 3: // friend
				{
					if (!_player.getFriendList().isEmpty())
					{
						final Set<Integer> friendList = ConcurrentHashMap.newKeySet();
						int count = 1;
						for (int id : _player.getFriendList())
						{
							for (StatSet temp : _playerList.values())
							{
								if (temp.getInt("charId") == id)
								{
									friendList.add(temp.getInt("charId"));
									count++;
								}
							}
						}
						
						friendList.add(_player.getObjectId());
						buffer.writeInt(count);
						for (Entry<Integer, StatSet> idEntry : _playerList.entrySet())
						{
							final int id = idEntry.getKey();
							final StatSet player = idEntry.getValue();
							if (friendList.contains(player.getInt("charId")))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(player.getInt("level"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("race"));
								buffer.writeInt(id); // friend rank
								writeSnapshot(buffer, _snapshotList, player.getInt("charId"), id, 0, 0);
							}
						}
					}
					else
					{
						buffer.writeInt(1);
						buffer.writeSizedString(_player.getName());
						final Clan clan = _player.getClan();
						if (clan != null)
						{
							buffer.writeSizedString(clan.getName());
						}
						else
						{
							buffer.writeSizedString("");
						}
						
						buffer.writeInt(_player.getStat().getBaseLevel());
						buffer.writeInt(_player.getBaseClass());
						buffer.writeInt(_player.getRace().ordinal());
						buffer.writeInt(1); // clan rank
						writeSnapshot(buffer, _snapshotList, _player.getObjectId(), 0, 0, 0);
					}
					break;
				}
				case 4: // class
				{
					if (_scope == 0) // all
					{
						int count = 0;
						for (int i = 1; i <= _playerList.size(); i++)
						{
							final StatSet player = _playerList.get(i);
							if (_class == player.getInt("classId"))
							{
								count++;
							}
						}
						
						buffer.writeInt(count > 100 ? 100 : count);
						int i = 1;
						for (StatSet player : _playerList.values())
						{
							if (_class == player.getInt("classId"))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(player.getInt("level"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("race"));
								buffer.writeInt(i); // server rank
								final Map<Integer, StatSet> snapshotClassList = new ConcurrentHashMap<>();
								int j = 1;
								for (Entry<Integer, StatSet> id2Entry : _snapshotList.entrySet())
								{
									final Integer id2 = id2Entry.getKey();
									final StatSet snapshot = id2Entry.getValue();
									if (_class == snapshot.getInt("classId"))
									{
										snapshotClassList.put(j, _snapshotList.get(id2));
										j++;
									}
								}
								
								writeSnapshot(buffer, snapshotClassList, player.getInt("charId"), i, i, i);
								
								i++;
							}
						}
					}
					else
					{
						boolean found = false;
						
						final Map<Integer, StatSet> classList = new ConcurrentHashMap<>();
						int i = 1;
						for (Entry<Integer, StatSet> idEntry : _playerList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet set = idEntry.getValue();
							if (_player.getBaseClass() == set.getInt("classId"))
							{
								classList.put(i, _playerList.get(id));
								i++;
							}
						}
						
						for (Entry<Integer, StatSet> idEntry : classList.entrySet())
						{
							final Integer id = idEntry.getKey();
							final StatSet player = idEntry.getValue();
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = classList.size() >= (id + 10) ? id + 10 : id + (classList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = classList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(plr.getInt("level"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("race"));
									buffer.writeInt(id2); // server rank
									buffer.writeInt(id2);
									buffer.writeInt(id2);
									buffer.writeInt(id2); // nClassRank_Snapshot?
								}
							}
						}
						
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
			}
		}
		else
		{
			buffer.writeInt(0);
		}
	}
	
	/**
	 * Writes the three snapshot ranks of {@code charId}, or the fallback values when it is not in {@code snapshot}.
	 * A row missing from the snapshot wrote nothing and shifted every following row.
	 * @param buffer the packet buffer
	 * @param snapshot the snapshot list to search
	 * @param charId the character to look up
	 * @param rank fallback server rank
	 * @param raceRank fallback race rank
	 * @param classRank fallback class rank
	 */
	private static void writeSnapshot(WriteBuffer buffer, Map<Integer, StatSet> snapshot, int charId, int rank, int raceRank, int classRank)
	{
		for (Entry<Integer, StatSet> entry : snapshot.entrySet())
		{
			final StatSet data = entry.getValue();
			if (data.getInt("charId") == charId)
			{
				buffer.writeInt(entry.getKey()); // server rank snapshot
				buffer.writeInt(data.getInt("raceRank", 0)); // race rank snapshot
				buffer.writeInt(data.getInt("classRank", 0)); // nClassRank_Snapshot
				return;
			}
		}
		
		buffer.writeInt(rank);
		buffer.writeInt(raceRank);
		buffer.writeInt(classRank);
	}
}
