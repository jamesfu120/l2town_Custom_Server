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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.RankingCategory;
import org.l2jmobius.gameserver.entity.actor.enums.player.RankingScope;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Berezkin Nikolay
 */
public class ExPvpRankingList extends ServerPacket
{
	private final Player _player;
	private final int _season;
	private final int _tabId;
	private final int _type;
	private final int _race;
	private final int _class;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExPvpRankingList(Player player, int season, int tabId, int type, int race, int baseclass)
	{
		_player = player;
		_season = season;
		_tabId = tabId;
		_type = type;
		_race = race;
		_class = baseclass;
		_playerList = RankManager.getInstance().getPvpRankList();
		_snapshotList = RankManager.getInstance().getSnapshotPvpRankList();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PVP_RANKING_LIST.writeId(this, buffer);
		buffer.writeByte(_season);
		buffer.writeInt(_tabId); // Essence: writeByte?
		buffer.writeInt(_type); // Essence: writeByte?
		buffer.writeInt(_race);
		if (!_playerList.isEmpty() && (_type != 255) && (_race != 255))
		{
			// Group and race come from the client; an out-of-range one threw here.
			final RankingCategory category = (_tabId >= 0) && (_tabId < RankingCategory.values().length) ? RankingCategory.values()[_tabId] : null;
			final RankingScope scope = category == null ? null : category.getScopeByGroup(_type);
			if ((scope == null) || (_race < 0) || (_race >= Race.values().length))
			{
				buffer.writeInt(0);
				return;
			}
			
			writeFilteredRankingData(category, scope, Race.values()[_race], PlayerClass.getPlayerClass(_class), buffer);
		}
		else
		{
			buffer.writeInt(0);
		}
	}
	
	private void writeFilteredRankingData(RankingCategory category, RankingScope scope, Race race, PlayerClass baseclass, WriteBuffer buffer)
	{
		switch (category)
		{
			case SERVER:
			{
				writeScopeData(scope, new ArrayList<>(_playerList.entrySet()), new ArrayList<>(_snapshotList.entrySet()), buffer);
				break;
			}
			case RACE:
			{
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> it.getValue().getInt("race") == race.ordinal()).toList(), _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("race") == race.ordinal()).toList(), buffer);
				break;
			}
			case CLAN:
			{
				writeScopeData(scope, _player.getClan() == null ? Collections.emptyList() : _playerList.entrySet().stream().filter(it -> it.getValue().getString("clanName").equals(_player.getClan().getName())).toList(), _player.getClan() == null ? Collections.emptyList() : _snapshotList.entrySet().stream().filter(it -> it.getValue().getString("clanName").equals(_player.getClan().getName())).toList(), buffer);
				break;
			}
			case FRIEND:
			{
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> _player.getFriendList().contains(it.getValue().getInt("charId"))).toList(), _snapshotList.entrySet().stream().filter(it -> _player.getFriendList().contains(it.getValue().getInt("charId"))).toList(), buffer);
				break;
			}
			case CLASS:
			{
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> it.getValue().getInt("classId") == baseclass.getId()).toList(), _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("classId") == baseclass.getId()).toList(), buffer);
				break;
			}
		}
	}
	
	private void writeScopeData(RankingScope scope, List<Entry<Integer, StatSet>> list, List<Entry<Integer, StatSet>> snapshot, WriteBuffer buffer)
	{
		Entry<Integer, StatSet> playerData = list.stream().filter(it -> it.getValue().getInt("charId", 0) == _player.getObjectId()).findFirst().orElse(null);
		final int indexOf = list.indexOf(playerData);
		final List<Entry<Integer, StatSet>> limited;
		switch (scope)
		{
			case TOP_100:
			{
				limited = list.stream().limit(100).toList();
				break;
			}
			case ALL:
			{
				limited = list;
				break;
			}
			case TOP_150:
			{
				limited = list.stream().limit(150).toList();
				break;
			}
			case SELF:
			{
				limited = playerData == null ? Collections.emptyList() : list.subList(Math.max(0, indexOf - 10), Math.min(list.size(), indexOf + 10));
				break;
			}
			default:
			{
				limited = Collections.emptyList();
			}
		}
		
		// The client reads nRank, nPrevRank: the previous rank is the position in the snapshot of the same filter (its key in SELF), 0 = new.
		// It got the snapshot rank and the EXP race rank, and a player missing from the snapshot wrote nothing and shifted the packet.
		final Map<Integer, Integer> previousRanks = new HashMap<>();
		int position = 0;
		for (Entry<Integer, StatSet> ssData : snapshot.stream().sorted(Entry.comparingByKey()).toList())
		{
			position++;
			previousRanks.put(ssData.getValue().getInt("charId"), scope == RankingScope.SELF ? ssData.getKey() : position);
		}
		
		buffer.writeInt(limited.size());
		int rank = 1;
		for (Entry<Integer, StatSet> data : limited.stream().sorted(Entry.comparingByKey()).toList())
		{
			int curRank = rank++;
			final StatSet player = data.getValue();
			buffer.writeSizedString(player.getString("name"));
			buffer.writeSizedString(player.getString("clanName"));
			buffer.writeInt(player.getInt("level"));
			buffer.writeInt(player.getInt("race"));
			buffer.writeInt(player.getInt("classId"));
			buffer.writeLong(player.getInt("points")); // PvP points
			buffer.writeInt(scope == RankingScope.SELF ? data.getKey() : curRank); // rank
			buffer.writeInt(previousRanks.getOrDefault(player.getInt("charId"), 0)); // previous rank
			buffer.writeInt(player.getInt("kills"));
			buffer.writeInt(player.getInt("deaths"));
		}
	}
}
