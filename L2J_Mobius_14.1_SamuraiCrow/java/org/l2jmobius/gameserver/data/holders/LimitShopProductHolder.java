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

/**
 * @author Mobius
 */
public class LimitShopProductHolder
{
	private final int _id;
	private final int _category;
	private final int _minLevel;
	private final int _maxLevel;
	private final int[] _ingredientIds;
	private final long[] _ingredientQuantities;
	private final int[] _ingredientEnchants;
	private final int _productionId;
	private final long _count;
	private final float _chance;
	private final boolean _announce;
	private final int _enchant;
	private final int _productionId2;
	private final int _productionEnchant2;
	private final long _count2;
	private final float _chance2;
	private final boolean _announce2;
	private final int _productionId3;
	private final int _productionEnchant3;
	private final long _count3;
	private final float _chance3;
	private final boolean _announce3;
	private final int _productionId4;
	private final int _productionEnchant4;
	private final long _count4;
	private final float _chance4;
	private final boolean _announce4;
	private final int _productionId5;
	private final int _productionEnchant5;
	private final long _count5;
	private final float _chance5;
	private final boolean _announce5;
	private final int _accountDailyLimit;
	private final int _accountWeeklyLimit;
	private final int _accountMonthlyLimit;
	private final int _accountBuyLimit;
	private final boolean _keepOption;
	private final long _keepOptionFeeAmount;
	private final boolean[] _isMultiChoice; // Flag: Which slots are multi-choice (cost1-5).
	private final int[] _multiChoiceSlotMapping; // Maps each ingredient to its slot (0-4 or -1 if normal).
	
	public LimitShopProductHolder(int id, int category, int minLevel, int maxLevel, int[] ingredientIds, long[] ingredientQuantities, int[] ingredientEnchants, int productionId, long count, float chance, boolean announce, int enchant, int productionId2, int productionEnchant2, long count2, float chance2, boolean announce2, int productionId3, int productionEnchant3, long count3, float chance3, boolean announce3, int productionId4, int productionEnchant4, long count4, float chance4, boolean announce4, int productionId5, int productionEnchant5, long count5, float chance5, boolean announce5, int accountDailyLimit, int accountWeeklyLimit, int accountMonthlyLimit, int accountBuyLimit, boolean keepOption, long keepOptionFeeAmount, boolean[] isMultiChoice, int[] multiChoiceSlotMapping)
	{
		_id = id;
		_category = category;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_ingredientIds = ingredientIds;
		_ingredientQuantities = ingredientQuantities;
		_ingredientEnchants = ingredientEnchants;
		_productionId = productionId;
		_count = count;
		_chance = chance;
		_announce = announce;
		_enchant = enchant;
		_productionId2 = productionId2;
		_productionEnchant2 = productionEnchant2;
		_count2 = count2;
		_chance2 = chance2;
		_announce2 = announce2;
		_productionId3 = productionId3;
		_productionEnchant3 = productionEnchant3;
		_count3 = count3;
		_chance3 = chance3;
		_announce3 = announce3;
		_productionId4 = productionId4;
		_productionEnchant4 = productionEnchant4;
		_count4 = count4;
		_chance4 = chance4;
		_announce4 = announce4;
		_productionId5 = productionId5;
		_productionEnchant5 = productionEnchant5;
		_count5 = count5;
		_chance5 = chance5;
		_announce5 = announce5;
		_accountDailyLimit = accountDailyLimit;
		_accountWeeklyLimit = accountWeeklyLimit;
		_accountMonthlyLimit = accountMonthlyLimit;
		_accountBuyLimit = accountBuyLimit;
		_keepOption = keepOption;
		_keepOptionFeeAmount = keepOptionFeeAmount;
		_isMultiChoice = isMultiChoice;
		_multiChoiceSlotMapping = multiChoiceSlotMapping;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getCategory()
	{
		return _category;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public int[] getIngredientIds()
	{
		return _ingredientIds;
	}
	
	public long[] getIngredientQuantities()
	{
		return _ingredientQuantities;
	}
	
	public int[] getIngredientEnchants()
	{
		return _ingredientEnchants;
	}
	
	public int getProductionId()
	{
		return _productionId;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	public float getChance()
	{
		return _chance;
	}
	
	public boolean isAnnounce()
	{
		return _announce;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public int getProductionId2()
	{
		return _productionId2;
	}
	
	public int getProductionEnchant2()
	{
		return _productionEnchant2;
	}
	
	public long getCount2()
	{
		return _count2;
	}
	
	public float getChance2()
	{
		return _chance2;
	}
	
	public boolean isAnnounce2()
	{
		return _announce2;
	}
	
	public int getProductionId3()
	{
		return _productionId3;
	}
	
	public int getProductionEnchant3()
	{
		return _productionEnchant3;
	}
	
	public long getCount3()
	{
		return _count3;
	}
	
	public float getChance3()
	{
		return _chance3;
	}
	
	public boolean isAnnounce3()
	{
		return _announce3;
	}
	
	public int getProductionId4()
	{
		return _productionId4;
	}
	
	public int getProductionEnchant4()
	{
		return _productionEnchant4;
	}
	
	public long getCount4()
	{
		return _count4;
	}
	
	public float getChance4()
	{
		return _chance4;
	}
	
	public float getChance5()
	{
		return _chance5;
	}
	
	public boolean isAnnounce4()
	{
		return _announce4;
	}
	
	public int getProductionId5()
	{
		return _productionId5;
	}
	
	public int getProductionEnchant5()
	{
		return _productionEnchant5;
	}
	
	public long getCount5()
	{
		return _count5;
	}
	
	public boolean isAnnounce5()
	{
		return _announce5;
	}
	
	public int getAccountDailyLimit()
	{
		return _accountDailyLimit;
	}
	
	public int getAccountWeeklyLimit()
	{
		return _accountWeeklyLimit;
	}
	
	public int getAccountMonthlyLimit()
	{
		return _accountMonthlyLimit;
	}
	
	public int getAccountBuyLimit()
	{
		return _accountBuyLimit;
	}
	
	public boolean hasKeepOption()
	{
		return _keepOption;
	}
	
	public long getKeepOptionFeeAmount()
	{
		return _keepOptionFeeAmount;
	}
	
	public boolean[] getIsMultiChoice()
	{
		return _isMultiChoice;
	}
	
	public boolean isMultiChoice(int slot)
	{
		return (_isMultiChoice != null) && (slot >= 0) && (slot < _isMultiChoice.length) && _isMultiChoice[slot];
	}
	
	public int[] getMultiChoiceSlotMapping()
	{
		return _multiChoiceSlotMapping;
	}
	
	public int getIngredientSlot(int ingredientIndex)
	{
		if ((_multiChoiceSlotMapping == null) || (ingredientIndex < 0) || (ingredientIndex >= _multiChoiceSlotMapping.length))
		{
			return -1;
		}
		return _multiChoiceSlotMapping[ingredientIndex];
	}
}