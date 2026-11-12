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
package org.l2jmobius.tools.dashboard.bridge;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Provides a small immutable bridge between the server lifecycle and JMX.
 * @author BazookaRpm
 */
public final class RuntimeManagementBean implements RuntimeManagementMXBean
{
	private final String _role;
	private final Supplier<String> _displayNameSupplier;
	private final IntSupplier _onlinePlayersSupplier;
	private final IntSupplier _maximumPlayersSupplier;
	private final IntSupplier _connectedGameServersSupplier;
	private final IntSupplier _authenticatedClientsSupplier;
	private final IntConsumer _shutdownAction;
	private final IntConsumer _restartAction;
	private final BooleanSupplier _cancelAction;
	private final String _controlToken;
	
	public RuntimeManagementBean(String role, Supplier<String> displayNameSupplier, IntSupplier onlinePlayersSupplier, IntSupplier maximumPlayersSupplier, IntSupplier connectedGameServersSupplier, IntSupplier authenticatedClientsSupplier, IntConsumer shutdownAction, IntConsumer restartAction, BooleanSupplier cancelAction)
	{
		_role = Objects.requireNonNull(role);
		_displayNameSupplier = Objects.requireNonNull(displayNameSupplier);
		_onlinePlayersSupplier = Objects.requireNonNull(onlinePlayersSupplier);
		_maximumPlayersSupplier = Objects.requireNonNull(maximumPlayersSupplier);
		_connectedGameServersSupplier = Objects.requireNonNull(connectedGameServersSupplier);
		_authenticatedClientsSupplier = Objects.requireNonNull(authenticatedClientsSupplier);
		_shutdownAction = Objects.requireNonNull(shutdownAction);
		_restartAction = Objects.requireNonNull(restartAction);
		_cancelAction = Objects.requireNonNull(cancelAction);
		final String environmentToken = System.getenv("L2J_DASHBOARD_TOKEN");
		_controlToken = ((environmentToken != null) && !environmentToken.isEmpty()) ? environmentToken : System.getProperty("l2j.dashboard.token", "");
	}
	
	@Override
	public String runtimeRole()
	{
		return _role;
	}
	
	@Override
	public String runtimeName()
	{
		return safeString(_displayNameSupplier);
	}
	
	@Override
	public int[] populationSnapshot()
	{
		return new int[]
		{
			safeInt(_onlinePlayersSupplier),
			safeInt(_maximumPlayersSupplier),
			safeInt(_connectedGameServersSupplier),
			safeInt(_authenticatedClientsSupplier)
		};
	}
	
	@Override
	public boolean lifecycleRequest(String token, String action, int seconds)
	{
		if ((seconds < 0) || !isAuthorized(token))
		{
			return false;
		}
		
		if ("shutdown".equalsIgnoreCase(action))
		{
			_shutdownAction.accept(seconds);
			return true;
		}
		
		if ("restart".equalsIgnoreCase(action))
		{
			_restartAction.accept(seconds);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean cancelLifecycleRequest(String token)
	{
		// The action reports false when no countdown is pending or it already finished.
		return isAuthorized(token) && _cancelAction.getAsBoolean();
	}
	
	void parentProcessLost()
	{
		_shutdownAction.accept(0);
	}
	
	private boolean isAuthorized(String token)
	{
		return !_controlToken.isEmpty() && constantTimeEquals(_controlToken, token);
	}
	
	private static int safeInt(IntSupplier supplier)
	{
		try
		{
			return supplier.getAsInt();
		}
		catch (RuntimeException e)
		{
			return -1;
		}
	}
	
	private static String safeString(Supplier<String> supplier)
	{
		try
		{
			final String value = supplier.get();
			return (value == null) ? "Unknown" : value;
		}
		catch (RuntimeException e)
		{
			return "Unknown";
		}
	}
	
	private static boolean constantTimeEquals(String expected, String provided)
	{
		if (provided == null)
		{
			return false;
		}
		
		final int expectedLength = expected.length();
		final int providedLength = provided.length();
		int difference = expectedLength ^ providedLength;
		final int length = Math.max(expectedLength, providedLength);
		for (int index = 0; index < length; index++)
		{
			final char left = (index < expectedLength) ? expected.charAt(index) : 0;
			final char right = (index < providedLength) ? provided.charAt(index) : 0;
			difference |= left ^ right;
		}
		
		return difference == 0;
	}
}
