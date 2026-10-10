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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.RebirthManager;

public final class Rebirth implements IBypassHandler
{
	// 修正重點：精準補上 2 個修仙稱號的新 Bypass 指令
	private static final String[] COMMANDS =
	{
		"rebirth_openmenu",
		"rebirth_confirmrequest",
		"rebirth_selectskills",
		"rebirth_previewSkill",
		"rebirth_confirmSkill",
		"rebirth_resetSkill",
		"rebirth_showTitles",
		"rebirth_selectTitle"
	};
	
	@Override
	public boolean onCommand(String command, Player player, Creature bypassOrigin)
	{
		if ((command == null) || (player == null))
		{
			return false;
		}
		
		final int objectId = (bypassOrigin != null) ? bypassOrigin.getObjectId() : 0;
		
		if (command.equals("rebirth_openmenu"))
		{
			RebirthManager.getInstance().displayMainWindow(player, objectId);
			return true;
		}
		else if (command.equals("rebirth_confirmrequest"))
		{
			RebirthManager.getInstance().requestRebirth(player);
			return true;
		}
		else if (command.equals("rebirth_selectskills"))
		{
			RebirthManager.getInstance().displaySkillSelectionWindow(player, objectId);
			return true;
		}
		else if (command.startsWith("rebirth_previewSkill"))
		{
			final Integer skillId = parseSkillId(command);
			if (skillId == null) { player.sendMessage("Invalid skill."); return true; }
			RebirthManager.getInstance().displaySkillPreviewWindow(player, objectId, skillId.intValue());
			return true;
		}
		else if (command.startsWith("rebirth_confirmSkill"))
		{
			final Integer skillId = parseSkillId(command);
			if (skillId == null) { player.sendMessage("Invalid skill."); return true; }
			RebirthManager.getInstance().acquireRebirthSkill(player, objectId, skillId.intValue());
			return true;
		}
		else if (command.startsWith("rebirth_resetSkill"))
		{
			final Integer skillId = parseSkillId(command);
			if (skillId == null) { player.sendMessage("Invalid skill."); return true; }
			RebirthManager.getInstance().resetRebirthSkill(player, objectId, skillId.intValue());
			return true;
		}
		else if (command.equals("rebirth_showTitles"))
		{
			// 修正重點：成功串接並打開你的自訂修仙稱號選單
			RebirthManager.getInstance().displayTitleSelectionWindow(player, objectId);
			return true;
		}
		else if (command.startsWith("rebirth_selectTitle"))
		{
			// 修正重點：成功串接點擊配戴稱號的邏輯
			final Integer titleIndex = parseSkillId(command);
			if (titleIndex == null) { player.sendMessage("無效的稱號索引。"); return true; }
			RebirthManager.getInstance().selectRebirthTitle(player, objectId, titleIndex.intValue());
			return true;
		}
		
		return false;
	}
	
	private Integer parseSkillId(String command)
	{
		try
		{
			final int spaceIndex = command.indexOf(' ');
			if (spaceIndex > 0)
			{
				return Integer.valueOf(Integer.parseInt(command.substring(spaceIndex + 1).trim()));
			}
		}
		catch (Exception e) {}
		return null;
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
