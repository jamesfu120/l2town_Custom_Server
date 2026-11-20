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
package handlers.skill.effects;

import java.util.Collection;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Shadow;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Liamxroy
 */
public class SummonShadow extends AbstractEffect
{
	private final int _despawnDelay;
	private final int _npcId;
	
	public SummonShadow(StatSet params)
	{
		_despawnDelay = params.getInt("despawnDelay", 10000);
		_npcId = params.getInt("npcId", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_NPC;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// The summoner, not the effected: Clone Dance calls this on its enemy target.
		final Player player = effector.asPlayer();
		if ((player == null) || player.isAlikeDead())
		{
			return;
		}
		
		if (_npcId <= 0)
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Invalid NPC ID or count skill ID: " + skill.getId());
			return;
		}
		
		if (player.isMounted())
		{
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Spawn of the nonexisting NPC ID: " + _npcId + ", skill ID:" + skill.getId());
			return;
		}
		
		// Up to 3 shadows: at the limit the oldest one gives way. Bounded, so a shadow that stays listed cannot spin it.
		for (int i = 0; (i < 3) && (player.getSummonedNpcCount() >= 3); i++)
		{
			Shadow oldest = null;
			final Collection<Npc> summoned = player.getSummonedNpcs();
			for (Npc npc : summoned)
			{
				if ((npc.getId() == _npcId) && (npc instanceof Shadow) && ((oldest == null) || (((Shadow) npc).getSpawnTime() < oldest.getSpawnTime())))
				{
					oldest = (Shadow) npc;
				}
			}
			if (oldest == null)
			{
				return; // The limit is held by other summoned NPCs.
			}
			oldest.deleteMe();
		}
		if (player.getSummonedNpcCount() >= 3)
		{
			return;
		}
		
		final int x = player.getX();
		final int y = player.getY();
		final int z = player.getZ();
		final Shadow shadow = new Shadow(npcTemplate, player);
		shadow.fullRestore();
		shadow.setHeading(player.getHeading());
		shadow.setInstance(player.getInstanceWorld());
		shadow.setShowSummonAnimation(true);
		// Summoner before spawnMe: Shadow.onSpawn copies the summoner's buffs and passives.
		player.addSummonedNpc(shadow);
		shadow.spawnMe(x, y, z);
		shadow.scheduleDespawn(_despawnDelay);
	}
}
