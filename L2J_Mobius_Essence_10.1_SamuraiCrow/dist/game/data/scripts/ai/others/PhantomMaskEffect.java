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
package ai.others;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Phantom Mask special effect handler.
 * <p>
 * When a player equipped with a Phantom Mask kills a titled monster of level 76 or higher, there is a chance to spawn an additional Ghost Knight NPC depending on the mask enchant level.
 * </p>
 * <p>
 * The original monster always drops normally. The spawned monster is an additional enemy.
 * </p>
 * @author Galagard
 * @since 21/12/2025
 */
public class PhantomMaskEffect extends Script
{
	/** Script value marker for eligible NPCs */
	private static final String PHANTOM_MARK_VAR = "PHANTOM_MARK";
	
	/** Phantom Mask item IDs */
	private static final int PHANTOM_MASK = 100545; // Phantom Mask
	private static final int BLESSED_PHANTOM_MASK = 100545; // Blessed Phantom Mask
	
	/** Mask enchant level -> Ghost Knight NPC ID */
	private static final Map<Integer, Integer> MASK_LEVEL_TO_NPC = new HashMap<>();
	static
	{
		MASK_LEVEL_TO_NPC.put(7, 22899); // Soldier
		MASK_LEVEL_TO_NPC.put(8, 22900); // Fighter
		MASK_LEVEL_TO_NPC.put(9, 22901); // Berserker
		MASK_LEVEL_TO_NPC.put(10, 22902); // Commander
	}
	
	/** Chance out of 1000 */
	private static final int SPAWN_CHANCE = 1000;
	
	private PhantomMaskEffect()
	{
		NpcData.getInstance().getTemplates(tpl -> (tpl.getLevel() >= 76) && (tpl.getTitle() != null) && !tpl.getTitle().isEmpty() && !MASK_LEVEL_TO_NPC.containsValue(tpl.getId())).forEach(tpl ->
		{
			addAttackId(tpl.getId());
			addKillId(tpl.getId());
		});
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Item item = attacker.getInventory().getPaperdollItemByBodyPart(BodyPart.HAIR2);
		if (item == null)
		{
			return;
		}
		
		final int itemId = item.getId();
		if ((itemId != PHANTOM_MASK) && (itemId != BLESSED_PHANTOM_MASK))
		{
			return;
		}
		
		if (item.getEnchantLevel() < 7)
		{
			return;
		}
		
		if (npc.getVariables().hasVariable(PHANTOM_MARK_VAR))
		{
			return;
		}
		
		npc.getVariables().set(PHANTOM_MARK_VAR, true);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((killer == null) || !npc.getVariables().hasVariable(PHANTOM_MARK_VAR))
		{
			return;
		}
		
		final Item item = killer.getInventory().getPaperdollItemByBodyPart(BodyPart.HAIR2);
		if (item == null)
		{
			return;
		}
		
		final int itemId = item.getId();
		if ((itemId != PHANTOM_MASK) && (itemId != BLESSED_PHANTOM_MASK))
		{
			return;
		}
		
		if (item.getEnchantLevel() < 7)
		{
			return;
		}
		
		final Integer ghostNpcId = MASK_LEVEL_TO_NPC.get(item.getEnchantLevel());
		if (ghostNpcId == null)
		{
			return;
		}
		
		if (getRandom(1000) >= SPAWN_CHANCE)
		{
			return;
		}
		
		final Creature attacker = isSummon ? killer.asSummon() : killer;
		addSpawn(ghostNpcId, npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), true, 0, true).asAttackable().addDamageHate(attacker, 0, 500);
	}
	
	public static void main(String[] args)
	{
		new PhantomMaskEffect();
	}
}
