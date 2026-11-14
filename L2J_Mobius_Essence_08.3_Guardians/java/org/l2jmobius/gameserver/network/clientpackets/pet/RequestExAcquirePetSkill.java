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
package org.l2jmobius.gameserver.network.clientpackets.pet;

import java.util.List;
import java.util.Optional;

import org.l2jmobius.gameserver.data.xml.PetAcquireList;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.holders.PetSkillAcquireHolder;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExAcquirePetSkillResult;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExPetSkillList;

/**
 * @author Berezkin Nikolay, Liamxroy
 */
public class RequestExAcquirePetSkill extends ClientPacket
{
	private int skillId;
	private int skillLevel;
	
	@Override
	protected void readImpl()
	{
		skillId = readInt();
		skillLevel = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Pet pet = player.getPet();
		if (pet == null)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
		if (skill == null)
		{
			return;
		}
		
		final Optional<PetSkillAcquireHolder> reqSkill = PetAcquireList.getInstance().getSkills(pet.getPetData().getType()).stream().filter(it -> (it.getSkillId() == skillId) && (it.getSkillLevel() == skillLevel)).findFirst();
		// Pet level, evolution and the previous skill level were checked by the client only.
		final Skill knownSkill = pet.getKnownSkill(skillId);
		final int knownLevel = knownSkill == null ? 0 : knownSkill.getLevel();
		if (!reqSkill.isPresent() || (pet.getLevel() < reqSkill.get().getReqLvl()) || (pet.getEvolveLevel() < reqSkill.get().getEvolve()) || (knownLevel >= skillLevel) || (knownLevel < (skillLevel - 1)))
		{
			player.sendPacket(new ExAcquirePetSkillResult(skillId, skillLevel, false));
			return;
		}
		
		// Every required item is needed: the old loop learned the skill after destroying the first one it could.
		final List<ItemHolder> requiredItems = reqSkill.get().getItems();
		for (ItemHolder item : requiredItems)
		{
			if (player.getInventory().getInventoryItemCount(item.getId(), -1) < item.getCount())
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS_TO_LEARN_THE_SKILL);
				player.sendPacket(new ExAcquirePetSkillResult(skillId, skillLevel, false));
				return;
			}
		}
		
		for (ItemHolder item : requiredItems)
		{
			if (!player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), item.getCount(), null, true))
			{
				player.sendPacket(new ExAcquirePetSkillResult(skillId, skillLevel, false));
				return;
			}
		}
		
		pet.addSkill(skill);
		pet.storePetSkills(skillId, skillLevel);
		player.sendPacket(new ExPetSkillList(false, pet));
		player.sendPacket(new ExAcquirePetSkillResult(skillId, skillLevel, true));
	}
}
