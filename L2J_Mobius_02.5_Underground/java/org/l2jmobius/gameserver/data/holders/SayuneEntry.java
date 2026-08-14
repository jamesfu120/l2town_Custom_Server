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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * @author UnAfraid, Mobius
 */
public class SayuneEntry implements ILocational
{
	private boolean _isSelector = false;
	private final int _id;
	private final Location _location;
	private final List<SayuneEntry> _innerEntries = new ArrayList<>();
	
	public SayuneEntry(int id)
	{
		_id = id;
		_location = new Location(0, 0, -10000);
	}
	
	public SayuneEntry(boolean isSelector, int id, int x, int y, int z)
	{
		_isSelector = isSelector;
		_id = id;
		_location = new Location(x, y, z);
	}
	
	public int getId()
	{
		return _id;
	}
	
	@Override
	public int getX()
	{
		return _location.getX();
	}
	
	@Override
	public int getY()
	{
		return _location.getY();
	}
	
	@Override
	public int getZ()
	{
		return _location.getZ();
	}
	
	@Override
	public int getHeading()
	{
		return 0;
	}
	
	@Override
	public ILocational getLocation()
	{
		return _location;
	}
	
	public boolean isSelector()
	{
		return _isSelector;
	}
	
	public List<SayuneEntry> getInnerEntries()
	{
		return _innerEntries;
	}
	
	public SayuneEntry addInnerEntry(SayuneEntry innerEntry)
	{
		_innerEntries.add(innerEntry);
		return innerEntry;
	}
}
