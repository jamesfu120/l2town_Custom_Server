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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClassChangeRequest;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquireSkillResult;
import org.l2jmobius.gameserver.network.serverpackets.RestartResponse;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeFail;

/**
 * @author Galagard
 */
public class PlayerClassChange extends Script
{
	@RegisterEvent(EventType.ON_PLAYER_CLASS_CHANGE_REQUEST)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onClassChangeRequest(OnPlayerClassChangeRequest event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!player.getVariables().getBoolean(PlayerVariables.CLASS_CHANGE_COUPON_ACTIVE, false) || !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendPacket(ExClassChangeFail.STATIC_PACKET);
			return;
		}
		
		final PlayerClass newClass = PlayerClass.getPlayerClass(event.getClassId());
		if (newClass == null)
		{
			player.sendPacket(ExClassChangeFail.STATIC_PACKET);
			return;
		}
		
		final int couponItemId = player.getVariables().getInt(PlayerVariables.CLASS_CHANGE_COUPON_ITEM_ID, -1);
		final Item coupon = couponItemId > 0 ? player.getInventory().getItemByItemId(couponItemId) : null;
		if (coupon == null)
		{
			player.sendPacket(ExClassChangeFail.STATIC_PACKET);
			return;
		}
		
		final boolean originHasSkills = !ClassChangeManager.getInstance().getCurrentHighGradeSkills(player).isEmpty();
		final boolean destHasHistory = hasHistoryForClass(player, event.getClassId());
		
		// Case 1: origin has skills, destination has NO history.
		// extract = extracts from the source, endpoint
		// no extract = saves source in history
		if (originHasSkills && !destHasHistory)
		{
			if (event.isExtractSkill())
			{
				handleExtractionFromPlayer(player, event.getCommissionId(), coupon);
			}
			else
			{
				saveClassHistory(player);
			}
		}
		// Case 2: origin has NO skills, destination has history.
		// extract = extracts from the destination's history, clears the entry
		// no extract = keeps destination history intact
		else if (!originHasSkills && destHasHistory)
		{
			if (event.isExtractSkill())
			{
				handleExtractionFromHistory(player, event.getCommissionId(), event.getClassId(), coupon);
			}
		}
		// Case 3: Origin has skills AND destination has history.
		// extract = saves source + extracts from destination history (required by the changelog)
		// no extract = saves source in history
		else if (originHasSkills && destHasHistory)
		{
			saveClassHistory(player);
			if (event.isExtractSkill())
			{
				handleExtractionFromHistory(player, event.getCommissionId(), event.getClassId(), coupon);
			}
		}
		// Case 4: nobody has the skill - just a class change.
		wipePlayer(player);
		applyClass(player, event.getClassId(), event.getSex());
		learnSkills(player, newClass);
		finalizeChange(player, coupon);
		clientRestart(player);
	}
	
	private boolean hasHistoryForClass(Player player, int classId)
	{
		final String data = player.getVariables().getString(PlayerVariables.CLASS_CHANGE_HISTORY + classId, null);
		return (data != null) && !data.isEmpty();
	}
	
	private void handleExtractionFromPlayer(Player player, int commissionId, Item coupon)
	{
		if (commissionId <= 0)
		{
			return;
		}
		
		final List<Skill> toExtract = ClassChangeManager.getInstance().getCurrentHighGradeSkills(player);
		if (toExtract.isEmpty())
		{
			return;
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, commissionId, 1, player, true))
		{
			return;
		}
		
		final int currentClassId = player.getActiveClass();
		for (Skill skill : toExtract)
		{
			final int rewardId = ClassChangeManager.getInstance().getExtractionRewardId(currentClassId, skill);
			if (rewardId > 0)
			{
				player.addItem(ItemProcessType.REWARD, rewardId, 1, player, true);
				player.removeSkill(skill, true, true);
			}
		}
		
		player.getInventory().updateDatabase();
	}
	
	private void handleExtractionFromHistory(Player player, int commissionId, int destClassId, Item coupon)
	{
		if (commissionId <= 0)
		{
			return;
		}
		
		final String historyKey = PlayerVariables.CLASS_CHANGE_HISTORY + destClassId;
		final String data = player.getVariables().getString(historyKey, null);
		if ((data == null) || data.isEmpty())
		{
			return;
		}
		
		final List<Skill> toExtract = new ArrayList<>();
		for (String s : data.split("\\|"))
		{
			final String[] p = s.split(",");
			if (p.length >= 3)
			{
				final Skill sk = SkillData.getInstance().getSkill(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
				if (sk != null)
				{
					toExtract.add(sk);
				}
			}
		}
		
		if (toExtract.isEmpty())
		{
			return;
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, commissionId, 1, player, true))
		{
			return;
		}
		
		final boolean isCardinal = PlayerClass.getPlayerClass(destClassId) == PlayerClass.CARDINAL;
		if (isCardinal)
		{
			// Cardinal: each skill has its own reward.
			// Tranquility (base or Master) = heroic, Eventide = legendary
			for (Skill skill : toExtract)
			{
				final int rewardId = ClassChangeManager.getInstance().getExtractionRewardId(destClassId, skill);
				if (rewardId > 0)
				{
					player.addItem(ItemProcessType.REWARD, rewardId, 1, player, true);
				}
			}
		}
		else
		{
			// Normal classes: use the highest level skill to determine the reward.
			// lv1 = heroic, lv2 = pack (already contains heroic + legendary, not both separately)
			Skill best = toExtract.get(0);
			for (Skill s : toExtract)
			{
				if (s.getLevel() > best.getLevel())
				{
					best = s;
				}
			}
			
			final int rewardId = ClassChangeManager.getInstance().getExtractionRewardId(destClassId, best);
			if (rewardId > 0)
			{
				player.addItem(ItemProcessType.REWARD, rewardId, 1, player, true);
			}
		}
		
		// Delete the entry from the destination's history after extraction.
		player.getVariables().remove(historyKey);
		player.getVariables().storeMe();
		player.getInventory().updateDatabase();
	}
	
	private void saveClassHistory(Player player)
	{
		final int currentClassId = player.getActiveClass();
		final List<Skill> highGradeSkills = ClassChangeManager.getInstance().getCurrentHighGradeSkills(player);
		if (highGradeSkills.isEmpty())
		{
			player.getVariables().set(PlayerVariables.CLASS_CHANGE_HISTORY + currentClassId, "");
			player.getVariables().storeMe();
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		if (player.getPlayerClass() == PlayerClass.CARDINAL)
		{
			// Cardinal: saves exactly the skills you have.
			for (Skill s : highGradeSkills)
			{
				if (sb.length() > 0)
				{
					sb.append('|');
				}
				sb.append(s.getId()).append(',').append(s.getLevel()).append(',').append(s.getSubLevel());
			}
		}
		else
		{
			final Skill best = highGradeSkills.get(0);
			final Skill lv1 = SkillData.getInstance().getSkill(best.getId(), 1, 0);
			if (lv1 != null)
			{
				sb.append(lv1.getId()).append(',').append(lv1.getLevel()).append(',').append(lv1.getSubLevel());
			}
			
			if (best.getLevel() >= 2)
			{
				final Skill lv2 = SkillData.getInstance().getSkill(best.getId(), 2, 0);
				if (lv2 != null)
				{
					if (sb.length() > 0)
					{
						sb.append('|');
					}
					sb.append(lv2.getId()).append(',').append(lv2.getLevel()).append(',').append(lv2.getSubLevel());
				}
			}
		}
		
		player.getVariables().set(PlayerVariables.CLASS_CHANGE_HISTORY + currentClassId, sb.toString());
		player.getVariables().storeMe();
	}
	
	private void wipePlayer(Player player)
	{
		player.disarmWeapons();
		player.disarmShield();
		player.disarmUnusableEquipment();
		new ArrayList<>(player.getAllSkills()).forEach(s -> player.removeSkill(s, true, true));
		player.getAllShortcuts().stream().filter(sc -> sc.getType() == ShortcutType.SKILL).forEach(sc -> player.deleteShortcut(sc.getSlot(), sc.getPage()));
	}
	
	private void applyClass(Player player, int id, int sex)
	{
		player.setPlayerClass(id);
		player.setBaseClass(id);
		if (sex == 1)
		{
			player.getAppearance().setFemale();
		}
		else
		{
			player.getAppearance().setMale();
		}
		player.store(false);
	}
	
	private void learnSkills(Player player, PlayerClass newClass)
	{
		player.giveAvailableSkills(false, true, true);
		
		final ClassChangeManager manager = ClassChangeManager.getInstance();
		if (newClass == PlayerClass.CARDINAL)
		{
			// Cardinal: removes all variants of Tranquility and Eventide.
			final int[] cardinalSkills =
			{
				manager.getSkillId("tranquilityBaseSkill"),
				manager.getSkillId("tranquilityMasterSkill"),
				manager.getSkillId("eventideMasterSkill")
			};
			
			for (int skillId : cardinalSkills)
			{
				final Skill known = player.getKnownSkill(skillId);
				if (known != null)
				{
					player.removeSkill(known, true, true);
				}
			}
		}
		else
		{
			final Map<Long, SkillLearn> newClassTree = SkillTreeData.getInstance().getCompleteClassSkillTree(newClass);
			for (SkillLearn learn : newClassTree.values())
			{
				if (manager.hasRequiredBookForStar(learn, 4))
				{
					final Skill known = player.getKnownSkill(learn.getSkillId());
					if (known != null)
					{
						player.removeSkill(known, true, true);
					}
				}
			}
		}
	}
	
	private void finalizeChange(Player player, Item coupon)
	{
		player.destroyItem(ItemProcessType.FEE, coupon, 1, player, true);
		player.getVariables().remove(PlayerVariables.CLASS_CHANGE_COUPON_ACTIVE);
		player.getVariables().remove(PlayerVariables.CLASS_CHANGE_COUPON_ITEM_ID);
		player.getStat().recalculateStats(false);
		player.getVariables().storeMe();
		player.broadcastUserInfo();
		player.sendSkillList();
		player.sendPacket(new ExAcquireSkillResult(0, 0, true, SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_THE_CLASS_CHANGE));
	}
	
	private void clientRestart(Player player)
	{
		final GameClient client = player.getClient();
		if (client != null)
		{
			Disconnection.of(client, player).storeAndDelete();
			client.setConnectionState(ConnectionState.AUTHENTICATED);
			client.sendPacket(RestartResponse.TRUE);
			final CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
			client.sendPacket(cl);
			client.setCharSelection(cl.getCharInfo());
		}
	}
	
	public static void main(String[] args)
	{
		new PlayerClassChange();
	}
}