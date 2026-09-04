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
package ai.others.Subjugation.Yand;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * @author Serenitty, Mobius
 */
public class Yand extends Script
{
	// NPC
	private static final int YAND = 34327;
	
	// Item
	private static final int MORGOS_MILITARY_SCROLL_MS = 90318605;
	
	// Location
	private static final Location TELEPORT_LOC = new Location(146915, -82589, -5128);
	
	// Misc
	private static final String MORGOS_MILITARY_FREE_VAR = "MORGOS_MILITARY_FREE";
	
	private Yand()
	{
		addFirstTalkId(YAND);
		addTalkId(YAND);
		addSpawnId(YAND);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "GoToInsideMorgos":
			{
				final int military = player.getVariables().getInt(MORGOS_MILITARY_FREE_VAR, 1);
				if (military == 0)
				{
					return "34327-01.html";
				}
				
				player.teleToLocation(TELEPORT_LOC);
				player.getVariables().set(MORGOS_MILITARY_FREE_VAR, 0);
				break;
			}
			case "BuyScrollMorgos":
			{
				MultisellData.getInstance().separateAndSend(MORGOS_MILITARY_SCROLL_MS, player, null, false);
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34327.html";
	}
	
	@RegisterEvent(EventType.ON_DAILY_RESET)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDailyReset(OnDailyReset event)
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
			{
				ps.setString(1, MORGOS_MILITARY_FREE_VAR);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not reset variables: " + e.getMessage());
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.getAccountVariables().remove(MORGOS_MILITARY_FREE_VAR);
		}
		
		LOGGER.info("MorgosMilitaryBase has been reset.");
	}
	
	public static void main(String[] args)
	{
		new Yand();
	}
}
