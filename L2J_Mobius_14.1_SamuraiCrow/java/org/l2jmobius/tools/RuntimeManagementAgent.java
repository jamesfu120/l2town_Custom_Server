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

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * Publishes the local management endpoint only for processes explicitly launched by the dashboard.
 * @author BazookaRpm
 */
public final class RuntimeManagementAgent
{
	public static final String MANAGED_PROPERTY = "l2j.dashboard.managed";
	public static final String PARENT_PID_PROPERTY = "l2j.dashboard.parentPid";
	public static final String PARENT_START_PROPERTY = "l2j.dashboard.parentStartEpochMillis";
	public static final String OBJECT_NAME = "org.l2jmobius.tools.dashboard:type=RuntimeManagement";
	private static final long PARENT_CHECK_INTERVAL_MILLIS = 2000;
	
	private RuntimeManagementAgent()
	{
	}
	
	public static boolean isManagedProcess()
	{
		final String requested = System.getProperty(MANAGED_PROPERTY, "false");
		return "true".equalsIgnoreCase(requested.trim());
	}
	
	public static void publish(RuntimeManagementBean managementBean, Logger logger)
	{
		if (!isManagedProcess())
		{
			return;
		}
		
		try
		{
			replaceRegistration(managementBean);
			startParentWatchdog(managementBean, logger);
			logger.info(DashboardProfile.MANAGEMENT_LABEL + " management endpoint is available.");
		}
		catch (Exception exception)
		{
			logger.log(Level.WARNING, DashboardProfile.MANAGEMENT_LABEL + " management endpoint registration failed.", exception);
		}
	}
	
	private static void startParentWatchdog(RuntimeManagementBean managementBean, Logger logger)
	{
		final long parentPid = readLongProperty(PARENT_PID_PROPERTY);
		if (parentPid <= 0)
		{
			return;
		}
		
		final long parentStart = readLongProperty(PARENT_START_PROPERTY);
		final Thread watchdog = new Thread(() ->
		{
			while (true)
			{
				try
				{
					Thread.sleep(PARENT_CHECK_INTERVAL_MILLIS);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					return;
				}
				
				if (matchesParent(parentPid, parentStart))
				{
					continue;
				}
				
				logger.warning(DashboardProfile.MANAGEMENT_LABEL + " parent process is no longer available; requesting managed shutdown.");
				try
				{
					managementBean.parentProcessLost();
				}
				catch (RuntimeException e)
				{
					logger.log(Level.WARNING, DashboardProfile.MANAGEMENT_LABEL + " parent-loss shutdown request failed.", e);
				}
				return;
			}
		}, "Dashboard-Parent-Watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}
	
	private static boolean matchesParent(long parentPid, long parentStart)
	{
		final ProcessHandle handle = ProcessHandle.of(parentPid).orElse(null);
		if ((handle == null) || !handle.isAlive())
		{
			return false;
		}
		
		if (parentStart <= 0)
		{
			return true;
		}
		
		return handle.info().startInstant().map(instant -> instant.toEpochMilli() == parentStart).orElse(false);
	}
	
	private static long readLongProperty(String name)
	{
		try
		{
			return Long.parseLong(System.getProperty(name, "0"));
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
	
	private static void replaceRegistration(RuntimeManagementBean managementBean) throws Exception
	{
		final MBeanServer platformServer = ManagementFactory.getPlatformMBeanServer();
		final ObjectName objectName = ObjectName.getInstance(OBJECT_NAME);
		if (platformServer.isRegistered(objectName))
		{
			platformServer.unregisterMBean(objectName);
		}
		
		platformServer.registerMBean(new StandardMBean(managementBean, RuntimeManagementMXBean.class, true), objectName);
	}
}
