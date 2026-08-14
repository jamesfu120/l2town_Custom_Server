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
package ai.others.ClassChange;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EnchantItemExpHolder;
import org.l2jmobius.gameserver.data.xml.SkillEnchantData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Galagard
 */
public class ClassChangeManager implements IXmlReader
{
	private final StatSet _settings = new StatSet();
	private final StatSet _skills = new StatSet();
	
	protected ClassChangeManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/scripts/ai/others/ClassChange/config.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "configuration", configurationNode ->
			{
				forEach(configurationNode, "param", paramNode ->
				{
					final StatSet set = new StatSet(parseAttributes(paramNode));
					_settings.set(set.getString("name"), set.getString("value"));
				});
			});
			
			forEach(listNode, "skills", skillsNode ->
			{
				forEach(skillsNode, "param", paramNode ->
				{
					final StatSet set = new StatSet(parseAttributes(paramNode));
					_skills.set(set.getString("name"), set.getInt("value"));
				});
			});
		});
	}
	
	public int getSkillId(String name)
	{
		return _skills.getInt(name, 0);
	}
	
	public int getCouponId(String name)
	{
		return _settings.getInt(name, 0);
	}
	
	public boolean isCustomMode()
	{
		return _settings.getBoolean("customMode", false);
	}
	
	public int getExtractionRewardId(int classId, Skill skill)
	{
		if (PlayerClass.getPlayerClass(classId) == PlayerClass.CARDINAL)
		{
			return (skill.getId() == getSkillId("eventideMasterSkill")) ? getCouponId("legendaryCoupon") : getCouponId("heroicCoupon");
		}
		
		return (skill.getLevel() >= 2) ? getCouponId("packCoupon") : getCouponId("heroicCoupon");
	}
	
	public List<Skill> getCurrentHighGradeSkills(Player player)
	{
		final List<Skill> result = new ArrayList<>();
		final PlayerClass currentClass = player.getPlayerClass();
		if (currentClass == PlayerClass.CARDINAL)
		{
			final Skill eventide = player.getKnownSkill(getSkillId("eventideMasterSkill"));
			if (eventide != null)
			{
				final Skill tranquilityMaster = player.getKnownSkill(getSkillId("tranquilityMasterSkill"));
				if (tranquilityMaster != null)
				{
					result.add(tranquilityMaster);
				}
				result.add(eventide);
			}
			else
			{
				final Skill tranquility = player.getKnownSkill(getSkillId("tranquilityBaseSkill"));
				if (tranquility != null)
				{
					result.add(tranquility);
				}
			}
			return result;
		}
		
		final Map<Long, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(currentClass);
		Skill selectedSkill = null;
		
		for (Skill skill : player.getAllSkills())
		{
			final SkillLearn matchedLearn = getBestMatchingLearn(skillTree, skill);
			if ((matchedLearn != null) && hasRequiredBookForStar(matchedLearn, 4))
			{
				if ((selectedSkill == null) || (skill.getLevel() > selectedSkill.getLevel()))
				{
					selectedSkill = skill;
				}
			}
		}
		
		if (selectedSkill != null)
		{
			result.add(selectedSkill);
		}
		
		return result;
	}
	
	public boolean hasRequiredBookForStar(SkillLearn learn, int expectedStarLevel)
	{
		if (learn == null)
		{
			return false;
		}
		
		if ((learn.getRequiredItems() == null) || learn.getRequiredItems().isEmpty())
		{
			return false;
		}
		
		final Map<Integer, EnchantItemExpHolder> enchantItems = SkillEnchantData.getInstance().getEnchantItem(expectedStarLevel);
		if ((enchantItems == null) || enchantItems.isEmpty())
		{
			return false;
		}
		
		for (List<ItemHolder> group : learn.getRequiredItems())
		{
			for (ItemHolder holder : group)
			{
				final EnchantItemExpHolder enchantItem = enchantItems.get(holder.getId());
				if ((enchantItem != null) && (enchantItem.getStarLevel() == expectedStarLevel) && (enchantItem.getExp() >= 300000))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public SkillLearn getBestMatchingLearn(Map<Long, SkillLearn> skillTree, Skill skill)
	{
		SkillLearn best = null;
		
		for (SkillLearn learn : skillTree.values())
		{
			if (learn.getSkillId() != skill.getId())
			{
				continue;
			}
			
			if (learn.getSkillLevel() > skill.getLevel())
			{
				continue;
			}
			
			if ((best == null) || (learn.getSkillLevel() > best.getSkillLevel()))
			{
				best = learn;
			}
		}
		
		return best;
	}
	
	public boolean canCheckAction(Player player)
	{
		if (!player.isInsideZone(ZoneId.PEACE))
		{
			return fail(player, SystemMessageId.YOU_CAN_CHANGE_THE_CLASS_ONLY_IN_A_PEACEFUL_ZONE);
		}
		if (player.isInCombat())
		{
			return fail(player, SystemMessageId.NOT_AVAILABLE_IN_COMBAT);
		}
		if (player.isTransformed())
		{
			return fail(player, SystemMessageId.YOU_CANNOT_CHANGE_CLASSES_WHILE_YOU_ARE_TRANSFORMED);
		}
		if (player.isDead())
		{
			return fail(player, SystemMessageId.YOU_CANNOT_PERFORM_THIS_ACTION_WHILE_DEAD);
		}
		if (player.isImmobilized())
		{
			return fail(player, SystemMessageId.YOU_CANNOT_PERFORM_THIS_ACTION_WHILE_IMMOBILIZED_PETRIFIED_PARALIZED_ETC);
		}
		if (player.isInStoreMode() || player.isInStoreSellOrBuyMode())
		{
			return fail(player, SystemMessageId.YOU_CANNOT_PERFORM_THIS_ACTION_WHILE_USING_PRIVATE_WORKSHOP_OR_PRIVATE_STORE);
		}
		
		return true;
	}
	
	private boolean fail(Player player, SystemMessageId messageId)
	{
		player.sendPacket(messageId);
		return false;
	}
	
	/**
	 * Gets the single instance of {@code ClassChangeManager}.
	 * @return single instance of {@code ClassChangeManager}
	 */
	public static ClassChangeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassChangeManager INSTANCE = new ClassChangeManager();
	}
}