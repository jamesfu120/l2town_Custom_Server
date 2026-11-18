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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * TODO: System messages:<br>
 * ADD: 3223: The previous name is being registered. Please try again later.<br>
 * DEL 3219: $s1 was successfully deleted from your Contact List.<br>
 * DEL 3217: The name is not currently registered.
 * @author UnAfraid, mrTJO, JankisLT
 */
public class ContactList
{
	private static final Logger LOGGER = Logger.getLogger(ContactList.class.getName());
	
	private final Player _player;
	private final Set<String> _contacts = ConcurrentHashMap.newKeySet();
	
	private static final String QUERY_ADD = "REPLACE INTO character_contacts (charId, contactId) VALUES (?, ?)";
	private static final String QUERY_REMOVE = "DELETE FROM character_contacts WHERE charId = ? and contactId = ?";
	private static final String QUERY_LOAD = "SELECT contactId FROM character_contacts WHERE charId = ?";
	
	public ContactList(Player player)
	{
		_player = player;
		restore();
	}
	
	public void restore()
	{
		_contacts.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_LOAD))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				int contactId;
				String contactName;
				while (rset.next())
				{
					contactId = rset.getInt(1);
					contactName = CharInfoTable.getInstance().getNameById(contactId);
					if ((contactName == null) || contactName.equals(_player.getName()) || (contactId == _player.getObjectId()))
					{
						continue;
					}
					
					_contacts.add(contactName);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
	}
	
	public boolean add(String name)
	{
		if (_contacts.contains(name))
		{
			_player.sendMessage("The name already exists on the added list.");
			return false;
		}
		
		// Names are found case-insensitively, so a case-sensitive self check let a player add themselves.
		if (_player.getName().equalsIgnoreCase(name))
		{
			_player.sendMessage("You cannot add your own name.");
			return false;
		}
		
		if (_contacts.size() >= 100)
		{
			_player.sendMessage("The maximum number of names (100) has been reached. You cannot register any more.");
			return false;
		}
		
		final int contactId = CharInfoTable.getInstance().getIdByName(name);
		if (contactId < 1)
		{
			_player.sendMessage("The name " + name + " doesn't exist. Please try another name.");
			return false;
		}
		
		for (String contactName : _contacts)
		{
			if (contactName.equalsIgnoreCase(name))
			{
				_player.sendMessage("The name already exists on the added list.");
				return false;
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_ADD))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			// Keep the stored spelling; the typed one only matched until the list was reloaded on login.
			final String contactName = CharInfoTable.getInstance().getNameById(contactId);
			_contacts.add(contactName != null ? contactName : name);
			
			_player.sendMessage(name + " was successfully added to your Contact List.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	public void remove(String name)
	{
		// The client may still list the name as it was typed, while the stored entry has the real spelling.
		String stored = null;
		for (String contactName : _contacts)
		{
			if (contactName.equalsIgnoreCase(name))
			{
				stored = contactName;
				break;
			}
		}
		
		if (stored == null)
		{
			_player.sendMessage("The name is not currently registered.");
			return;
		}
		
		final int contactId = CharInfoTable.getInstance().getIdByName(name);
		if (contactId < 1)
		{
			// TODO: Message?
			return;
		}
		
		_contacts.remove(stored);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_REMOVE))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			_player.sendMessage(name + " was successfully deleted from your Contact List.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
	}
	
	public Set<String> getAllContacts()
	{
		return _contacts;
	}
}
