/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package instances.EvasHiddenSpace;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;

/**
 * Eva's Hidden Space instance zone.
 * @author CostyKiller
 */
public class EvasHiddenSpace extends InstanceScript
{
	// NPCs
	private static final int EVAS_AVATAR = 34515;
	
	// Misc
	private static final int TEMPLATE_ID = 217;
	
	public EvasHiddenSpace()
	{
		super(TEMPLATE_ID);
		addStartNpc(EVAS_AVATAR);
		addFirstTalkId(EVAS_AVATAR);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		finishInstance(player, 1);
		return null;
	}
	
	public static void main(String[] args)
	{
		new EvasHiddenSpace();
	}
}
