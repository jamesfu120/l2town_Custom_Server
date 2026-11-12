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

/**
 * Centralizes the L2J Mobius product identity used by the Dashboard and its managed services. The Dashboard branding remains distribution-neutral so the same interface can be integrated across supported sources.
 * @author BazookaRpm
 */
public final class DashboardProfile
{
	public static final String PRODUCT_NAME = "L2J Mobius Control Dashboard";
	public static final String DISTRIBUTION_NAME = "Part of L2J Mobius";
	public static final String CHRONICLE_NAME = "L2J Mobius";
	public static final String PROFILE_LABEL = "L2J MOBIUS";
	public static final String WINDOW_TITLE = PRODUCT_NAME;
	public static final String EDITION_LABEL = "Control Dashboard";
	public static final String MANAGEMENT_LABEL = "L2J Mobius Dashboard";
	public static final String ADMINISTRATION_DESCRIPTION = "Server administration dashboard";
	
	private DashboardProfile()
	{
	}
}
