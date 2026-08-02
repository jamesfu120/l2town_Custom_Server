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
package org.l2jmobius.gameserver.model.spawns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.IParameterized;
import org.l2jmobius.gameserver.model.interfaces.ITerritorized;
import org.l2jmobius.gameserver.model.zone.type.BannedSpawnTerritory;
import org.l2jmobius.gameserver.model.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager;

/**
 * @author UnAfraid, Mobius
 */
public class SpawnGroup implements Cloneable, ITerritorized, IParameterized<StatSet>
{
	private static final Logger LOGGER = Logger.getLogger(SpawnGroup.class.getName());
	
	private final String _name;
	private final boolean _spawnByDefault;
	private final SpawnSelection _selection;
	private final int _maximumNpc;
	private List<SpawnTerritory> _territories;
	private List<BannedSpawnTerritory> _bannedTerritories;
	private final List<NpcSpawnTemplate> _spawns = new ArrayList<>();
	private StatSet _parameters;
	private int _aliveOrReserved = 0;
	private boolean _active = true;
	
	public SpawnGroup(StatSet set)
	{
		this(set.getString("name", null), set.getBoolean("spawnByDefault", true), parseSelection(set), set.getInt("maximumNpc", 0));
	}
	
	private SpawnGroup(String name, boolean spawnByDefault, SpawnSelection selection, int maximumNpc)
	{
		_name = name;
		_spawnByDefault = spawnByDefault;
		_selection = selection;
		_maximumNpc = maximumNpc;
	}
	
	/**
	 * Reads the entry selection policy of a group, defaulting to spawning every entry.
	 * @param set the parsed attributes of the group
	 * @return the selection policy declared by the group
	 */
	private static SpawnSelection parseSelection(StatSet set)
	{
		final String value = set.getString("selection", null);
		if (value == null)
		{
			return SpawnSelection.ALL;
		}
		
		if ("random".equalsIgnoreCase(value))
		{
			return SpawnSelection.RANDOM;
		}
		
		if ("randomFill".equalsIgnoreCase(value))
		{
			return SpawnSelection.RANDOM_FILL;
		}
		
		if (!"all".equalsIgnoreCase(value))
		{
			LOGGER.warning(SpawnGroup.class.getSimpleName() + ": Unknown selection \"" + value + "\" in group " + set.getString("name", "unnamed") + ", using all.");
		}
		
		return SpawnSelection.ALL;
	}
	
	public SpawnSelection getSelection()
	{
		return _selection;
	}
	
	public int getMaximumNpc()
	{
		return _maximumNpc;
	}
	
	public synchronized int getAliveOrReserved()
	{
		return _aliveOrReserved;
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	/**
	 * Enables or disables the replacement of dead units.
	 * @param active {@code false} to stop replacing dead units
	 */
	public void setActive(boolean active)
	{
		_active = active;
	}
	
	/**
	 * Reserves the given amount of units on an entry, honoring the entry quota and the group cap.<br>
	 * The group cap is deliberately not checked for RANDOM selection, retail random_spawn never reads it.
	 * @param entry the entry to reserve on
	 * @param count the amount of units to reserve
	 * @return {@code true} when the reservation was made, {@code false} when quota or cap would be exceeded
	 */
	public synchronized boolean tryReserve(NpcSpawnTemplate entry, int count)
	{
		if ((_selection != SpawnSelection.RANDOM) && (_maximumNpc > 0) && ((_aliveOrReserved + count) > _maximumNpc))
		{
			return false;
		}
		
		if ((entry.getAliveOrReserved() + count) > entry.getCount())
		{
			return false;
		}
		
		entry.setAliveOrReserved(entry.getAliveOrReserved() + count);
		_aliveOrReserved += count;
		return true;
	}
	
	/**
	 * Releases previously reserved units, used when the actual spawn could not be created.
	 * @param entry the entry to release from
	 * @param count the amount of units to release
	 */
	public synchronized void release(NpcSpawnTemplate entry, int count)
	{
		entry.setAliveOrReserved(entry.getAliveOrReserved() - count);
		_aliveOrReserved -= count;
	}
	
	/**
	 * Handles the death of a unit of this group.<br>
	 * A new entry is rolled ignoring which one died; when the rolled entry is at its quota nothing is replaced this cycle.
	 * @param deadEntry the entry the dead unit belonged to
	 * @param instance the instance the dead unit belonged to, {@code null} in the open world
	 */
	public void onUnitDeath(NpcSpawnTemplate deadEntry, Instance instance)
	{
		if ((_selection == SpawnSelection.ALL) || (deadEntry == null))
		{
			// ALL entries respawn themselves with their fixed species.
			return;
		}
		
		synchronized (this)
		{
			deadEntry.setAliveOrReserved(deadEntry.getAliveOrReserved() - 1);
			_aliveOrReserved--;
		}
		
		if (!_active)
		{
			// Deactivated groups keep their census truthful but stop replacing dead units.
			return;
		}
		
		final NpcSpawnTemplate rolled = _spawns.get(Rnd.get(_spawns.size()));
		if (tryReserve(rolled, 1))
		{
			// Reserve now, materialize later. The delay comes from the rolled entry, not from the dead one.
			final int respawnDelay = rolled.getRespawnTime() != null ? (int) rolled.getRespawnTime().getSeconds() : 0;
			final int respawnRandom = rolled.getRespawnTimeRandom() != null ? (int) rolled.getRespawnTimeRandom().getSeconds() : 0;
			final int minDelay = Math.max(1, respawnDelay - respawnRandom) * 1000;
			final int maxDelay = Math.max(1, respawnDelay + respawnRandom) * 1000;
			SpawnGroupTaskManager.getInstance().add(this, rolled, instance, System.currentTimeMillis() + (respawnRandom > 0 ? Rnd.get(minDelay, maxDelay) : minDelay));
		}
	}
	
	/**
	 * Materializes the given amount of already reserved units of an entry.
	 * @param entry the entry to spawn from
	 * @param count the amount of units to spawn
	 * @param instance the instance to spawn in, {@code null} in the open world
	 */
	public void spawnUnits(NpcSpawnTemplate entry, int count, Instance instance)
	{
		if (!_active)
		{
			// A pending replacement dequeued while the group was being stopped must not materialize.
			release(entry, count);
			return;
		}
		
		for (int i = 0; i < count; i++)
		{
			if (!entry.spawnOne(instance))
			{
				release(entry, 1);
			}
		}
	}
	
	public String getName()
	{
		return _name;
	}
	
	public boolean isSpawningByDefault()
	{
		return _spawnByDefault;
	}
	
	public void addSpawn(NpcSpawnTemplate template)
	{
		_spawns.add(template);
	}
	
	public List<NpcSpawnTemplate> getSpawns()
	{
		return _spawns;
	}
	
	@Override
	public void addTerritory(SpawnTerritory territory)
	{
		if (_territories == null)
		{
			_territories = new ArrayList<>();
		}
		
		_territories.add(territory);
	}
	
	@Override
	public List<SpawnTerritory> getTerritories()
	{
		return _territories != null ? _territories : Collections.emptyList();
	}
	
	@Override
	public void addBannedTerritory(BannedSpawnTerritory territory)
	{
		if (_bannedTerritories == null)
		{
			_bannedTerritories = new ArrayList<>();
		}
		
		_bannedTerritories.add(territory);
	}
	
	@Override
	public List<BannedSpawnTerritory> getBannedTerritories()
	{
		return _bannedTerritories != null ? _bannedTerritories : Collections.emptyList();
	}
	
	@Override
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	@Override
	public void setParameters(StatSet parameters)
	{
		_parameters = parameters;
	}
	
	public List<NpcSpawnTemplate> getSpawnsById(int id)
	{
		List<NpcSpawnTemplate> result = new ArrayList<>();
		for (NpcSpawnTemplate spawn : _spawns)
		{
			if (spawn.getId() == id)
			{
				result.add(spawn);
			}
		}
		
		return result;
	}
	
	public void spawnAll()
	{
		spawnAll(null);
	}
	
	public void spawnAll(Instance instance)
	{
		if (_selection == SpawnSelection.ALL)
		{
			for (NpcSpawnTemplate template : _spawns)
			{
				template.spawn(instance);
			}
			
			return;
		}
		
		if (_spawns.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + _name + " has no entries to spawn.");
			return;
		}
		
		if (_selection == SpawnSelection.RANDOM)
		{
			// A single roll over the entries, the whole quota of the rolled entry spawns. Nothing is filled up and the cap is not read.
			final NpcSpawnTemplate entry = _spawns.get(Rnd.get(_spawns.size()));
			if (tryReserve(entry, entry.getCount()))
			{
				spawnUnits(entry, entry.getCount(), instance);
			}
			
			return;
		}
		
		// The cap is the roll counter. Rolls use replacement and no retry, a roll into a full entry is simply lost.
		for (int i = 0; i < _maximumNpc; i++)
		{
			final NpcSpawnTemplate entry = _spawns.get(Rnd.get(_spawns.size()));
			if (tryReserve(entry, 1))
			{
				spawnUnits(entry, 1, instance);
			}
		}
	}
	
	public void despawnAll()
	{
		_spawns.forEach(NpcSpawnTemplate::despawn);
	}
	
	@Override
	public SpawnGroup clone()
	{
		final SpawnGroup group = new SpawnGroup(_name, _spawnByDefault, _selection, _maximumNpc);
		
		// Clone banned territories
		for (BannedSpawnTerritory territory : getBannedTerritories())
		{
			group.addBannedTerritory(territory);
		}
		
		// Clone territories
		for (SpawnTerritory territory : getTerritories())
		{
			group.addTerritory(territory);
		}
		
		// Clone spawns
		for (NpcSpawnTemplate spawn : _spawns)
		{
			// The clone keeps its own census, its entries must point at it and not at the source group.
			group.addSpawn(spawn.clone(group));
		}
		
		return group;
	}
}
