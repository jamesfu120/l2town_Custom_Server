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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCompoundChanceHolder;
import org.l2jmobius.gameserver.data.holders.RelicCompoundFeeHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicEnchantHolder;
import org.l2jmobius.gameserver.data.holders.RelicReplaceCategoryHolder;
import org.l2jmobius.gameserver.data.holders.RelicReplaceChanceHolder;
import org.l2jmobius.gameserver.data.holders.RelicSummonCategoryHolder;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author CostyKiller, Brado
 */
public class RelicData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicData.class.getName());
	
	private static final int MAX_ACTIVE_CATEGORIES = 20;
	private static final List<Integer> ENCHANT_CHANCES = new ArrayList<>();
	private static final Map<RelicGrade, ItemHolder> ENCHANT_FEE_HOLDERS = new HashMap<>();
	private static final Map<Integer, RelicSummonCategoryHolder> SUMMON_CATEGORIES = new HashMap<>(40); // Max Size (Client Restriction)
	private static final Set<RelicSummonCategoryHolder> ACTIVE_SUMMON_CATEGORIES = new HashSet<>(MAX_ACTIVE_CATEGORIES); // Max Size (Client Restriction)
	private static final Map<Integer, RelicDataHolder> RELICS = new HashMap<>();
	private static final Map<RelicGrade, Set<RelicDataHolder>> GRADE_RELICS = new HashMap<>();
	private static final Map<RelicGrade, Set<RelicDataHolder>> GRADE_COMMON_RELICS = new HashMap<>();
	private static final Map<RelicGrade, Set<RelicDataHolder>> GRADE_SHINING_RELICS = new HashMap<>();
	private static final Map<RelicGrade, RelicCompoundFeeHolder> GRADE_COMPOUND_FEES = new HashMap<>();
	private static final Map<RelicGrade, RelicCompoundChanceHolder> GRADE_COMPOUND_CHANCES = new HashMap<>();
	private static final Map<RelicGrade, RelicReplaceChanceHolder> GRADE_REPLACE_CHANCES = new HashMap<>();
	private static final Map<RelicGrade, RelicReplaceCategoryHolder> GRADE_REPLACE_CATEGORIES = new HashMap<>();
	private static int GUARANTEED_COMPOUND_MAX_POINTS = 220;
	private static int GUARANTEED_COMPOUND_POINTS_PER_ATTEMPT = 20;
	
	protected RelicData()
	{
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			load();
		}
	}
	
	@Override
	public void load()
	{
		RELICS.clear();
		GRADE_RELICS.clear();
		GRADE_COMMON_RELICS.clear();
		GRADE_SHINING_RELICS.clear();
		GRADE_COMPOUND_FEES.clear();
		GRADE_COMPOUND_CHANCES.clear();
		GRADE_REPLACE_CHANCES.clear();
		GRADE_REPLACE_CATEGORIES.clear();
		ENCHANT_FEE_HOLDERS.clear();
		ENCHANT_CHANCES.clear();
		SUMMON_CATEGORIES.clear();
		ACTIVE_SUMMON_CATEGORIES.clear();
		GUARANTEED_COMPOUND_MAX_POINTS = 220;
		GUARANTEED_COMPOUND_POINTS_PER_ATTEMPT = 20;
		
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			parseDatapackFile("data/RelicData.xml");
			generateGradeRelics();
		}
		
		if (!RELICS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELICS.size() + " relics.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("relicSummonCategoryData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("summonCategory".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final int categoryId = parseInteger(attrs, "id");
								final int priceId = parseInteger(attrs, "priceId");
								final long amount = parseLong(attrs, "amount");
								final boolean active = parseBoolean(attrs, "active");
								final int summonCount = parseInteger(attrs, "summonCount");
								final RelicSummonCategoryHolder holder = new RelicSummonCategoryHolder(categoryId, priceId, amount, summonCount);
								SUMMON_CATEGORIES.put(categoryId, holder);
								if (active && (ACTIVE_SUMMON_CATEGORIES.size() < MAX_ACTIVE_CATEGORIES))
								{
									ACTIVE_SUMMON_CATEGORIES.add(holder);
								}
							}
						}
					}
					else if ("relicEnchantData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("enchantFee".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final RelicGrade feeGrade = RelicGrade.valueOf(parseString(attrs, "grade"));
								final int feeId = parseInteger(attrs, "feeItemId", 57);
								final long feeCount = parseLong(attrs, "feeCount", 100L);
								ENCHANT_FEE_HOLDERS.put(feeGrade, new ItemHolder(feeId, feeCount));
							}
							
							if ("enchantData".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final String[] chances = parseString(attrs, "chancePerIngredients").split(",");
								for (String chance : chances)
								{
									ENCHANT_CHANCES.add(Integer.parseInt(chance.trim()));
								}
							}
						}
					}
					else if ("relicCombineData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("combineCategory".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "grade"));
								final int feeId = parseInteger(attrs, "feeItemId", 57);
								final long feeCount = parseLong(attrs, "feeCount", 100L);
								final RelicCompoundFeeHolder holder = new RelicCompoundFeeHolder(grade, feeId, feeCount);
								GRADE_COMPOUND_FEES.put(grade, holder);
							}
							else if ("combineData".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "chancePerGrade"));
								final int failCommon = parseInteger(attrs, "failCommonChance", 0);
								final int failShining = parseInteger(attrs, "failShiningChance", 0);
								final int successCommon = parseInteger(attrs, "successCommonChance", 0);
								final int successShining = parseInteger(attrs, "successShiningChance", 0);
								GRADE_COMPOUND_CHANCES.put(grade, new RelicCompoundChanceHolder(grade, failCommon, failShining, successCommon, successShining));
							}
							else if ("guaranteedCompound".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								GUARANTEED_COMPOUND_MAX_POINTS = parseInteger(attrs, "maxPoints", 220);
								GUARANTEED_COMPOUND_POINTS_PER_ATTEMPT = parseInteger(attrs, "pointsPerAttempt", 20);
							}
						}
					}
					else if ("relicReplaceData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("replaceCategory".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "grade"));
								final int replaceAttempts = parseInteger(attrs, "replaceAttempts", 5);
								final int feeItemId = parseInteger(attrs, "feeItemId", 57);
								final String[] feeCounts = parseString(attrs, "feeCountList").split(",");
								final List<ItemHolder> fees = new ArrayList<>();
								for (String feeCount : feeCounts)
								{
									fees.add(new ItemHolder(feeItemId, Long.parseLong(feeCount.trim())));
								}
								GRADE_REPLACE_CATEGORIES.put(grade, new RelicReplaceCategoryHolder(grade, replaceAttempts, fees));
							}
							else if ("replaceData".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "chancePerGrade"));
								final int failChance = parseInteger(attrs, "failChance", 0);
								final int successCommon = parseInteger(attrs, "successCommonChance", 0);
								final int successShining = parseInteger(attrs, "successShiningChance", 0);
								GRADE_REPLACE_CHANCES.put(grade, new RelicReplaceChanceHolder(grade, failChance, successCommon, successShining));
							}
						}
					}
					else if ("relic".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int relicId = parseInteger(attrs, "id");
						final String name = parseString(attrs, "name", "");
						final int parentRelicId = parseInteger(attrs, "baseRelicId");
						final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "grade"));
						final RelicType type = RelicType.valueOf(parseString(attrs, "type", "COMMON"));
						final long summonChance = parseLong(attrs, "summonChance");
						final float compoundChanceModifier = parseFloat(attrs, "compoundChanceModifier", 3.3f);
						final float compoundUpGradeChanceModifier = parseFloat(attrs, "compoundUpGradeChanceModifier", 3.3f);
						int enchantLevel = 0;
						int skillId = 0;
						int skillLevel = 0;
						List<RelicEnchantHolder> enchantHolder = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("relicStat".equalsIgnoreCase(b.getNodeName()))
							{
								enchantLevel = parseInteger(attrs, "enchantLevel");
								skillId = parseInteger(attrs, "skillId");
								skillLevel = parseInteger(attrs, "skillLevel");
								enchantHolder.add(new RelicEnchantHolder(enchantLevel, skillId, skillLevel));
							}
						}
						
						final RelicDataHolder template = new RelicDataHolder(relicId, name, parentRelicId, grade, type, summonChance, enchantHolder, compoundChanceModifier, compoundUpGradeChanceModifier);
						RELICS.put(relicId, template);
					}
				}
			}
		}
	}
	
	/**
	 * Populates the {@code GRADE_RELICS}, {@code GRADE_COMMON_RELICS} and {@code GRADE_SHINING_RELICS} maps with relic data grouped by grade and type.
	 */
	private void generateGradeRelics()
	{
		for (RelicDataHolder holder : RELICS.values())
		{
			// All relics by grade
			Set<RelicDataHolder> existingSet = GRADE_RELICS.get(holder.getGrade());
			if (existingSet == null)
			{
				existingSet = new HashSet<>();
				GRADE_RELICS.put(holder.getGrade(), existingSet);
			}
			existingSet.add(holder);
			
			// Split by type
			if (holder.isShining())
			{
				GRADE_SHINING_RELICS.computeIfAbsent(holder.getGrade(), _ -> new HashSet<>()).add(holder);
			}
			else
			{
				GRADE_COMMON_RELICS.computeIfAbsent(holder.getGrade(), _ -> new HashSet<>()).add(holder);
			}
		}
	}
	
	/**
	 * Retrieves all relics of the specified grade.
	 * @param grade the grade of the relics to retrieve
	 * @return a collection of {@code RelicDataHolder} objects of the specified grade, or {@code null} if no relics exist for that grade
	 */
	public Collection<RelicDataHolder> getRelicsByGrade(RelicGrade grade)
	{
		return GRADE_RELICS.get(grade);
	}
	
	/**
	 * Retrieves only COMMON-type relics of the specified grade.
	 * @param grade the grade of the relics to retrieve
	 * @return a collection of common {@code RelicDataHolder} objects, or {@code null} if none exist
	 */
	public Collection<RelicDataHolder> getCommonRelicsByGrade(RelicGrade grade)
	{
		return GRADE_COMMON_RELICS.get(grade);
	}
	
	/**
	 * Retrieves only SHINING-type relics of the specified grade.
	 * @param grade the grade of the relics to retrieve
	 * @return a collection of shining {@code RelicDataHolder} objects, or {@code null} if none exist
	 */
	public Collection<RelicDataHolder> getShiningRelicsByGrade(RelicGrade grade)
	{
		return GRADE_SHINING_RELICS.get(grade);
	}
	
	/**
	 * Retrieves the compound chance configuration for the specified grade.
	 * @param grade the relic grade
	 * @return the {@code RelicCompoundChanceHolder}, or {@code null} if not configured
	 */
	public RelicCompoundChanceHolder getCompoundChances(RelicGrade grade)
	{
		return GRADE_COMPOUND_CHANCES.get(grade);
	}
	
	/**
	 * Retrieves the replace chance configuration for the specified grade.
	 * @param grade the relic grade
	 * @return the {@code RelicReplaceChanceHolder}, or {@code null} if not configured
	 */
	public RelicReplaceChanceHolder getReplaceChances(RelicGrade grade)
	{
		return GRADE_REPLACE_CHANCES.get(grade);
	}
	
	/**
	 * Retrieves the replace category configuration (attempts + fees) for the specified grade.
	 * @param grade the relic grade
	 * @return the {@code RelicReplaceCategoryHolder}, or {@code null} if not configured
	 */
	public RelicReplaceCategoryHolder getReplaceCategory(RelicGrade grade)
	{
		return GRADE_REPLACE_CATEGORIES.get(grade);
	}
	
	/**
	 * Retrieves the maximum replacement attempts for the specified grade.
	 * @param grade the relic grade
	 * @return the number of allowed replace attempts, or 0 if not configured
	 */
	public int getReplaceAttempts(RelicGrade grade)
	{
		final RelicReplaceCategoryHolder cat = GRADE_REPLACE_CATEGORIES.get(grade);
		return cat != null ? cat.getReplaceAttempts() : 0;
	}
	
	/**
	 * Retrieves the ordered replacement fee list for the specified grade.
	 * @param grade the relic grade
	 * @return list of {@code ItemHolder} fees, or an empty list if not configured
	 */
	public List<ItemHolder> getReplaceFees(RelicGrade grade)
	{
		final RelicReplaceCategoryHolder cat = GRADE_REPLACE_CATEGORIES.get(grade);
		return cat != null ? cat.getFees() : new ArrayList<>();
	}
	
	/**
	 * Retrieves the maximum pity points before a guaranteed compound is available.
	 * @return the max points value from {@code <guaranteedCompound maxPoints="...">}
	 */
	public int getGuaranteedCompoundMaxPoints()
	{
		return GUARANTEED_COMPOUND_MAX_POINTS;
	}
	
	/**
	 * Retrieves the points awarded per compound attempt toward the guaranteed compound.
	 * @return the points-per-attempt value from {@code <guaranteedCompound pointsPerAttempt="...">}
	 */
	public int getGuaranteedCompoundPointsPerAttempt()
	{
		return GUARANTEED_COMPOUND_POINTS_PER_ATTEMPT;
	}
	
	/**
	 * Retrieves the number of guaranteed compound attempts available at max points. Derived as maxPoints / pointsPerAttempt.
	 * @return the guaranteed compound limit
	 */
	public int getGuaranteedCompoundLimit()
	{
		return GUARANTEED_COMPOUND_POINTS_PER_ATTEMPT > 0 ? GUARANTEED_COMPOUND_MAX_POINTS / GUARANTEED_COMPOUND_POINTS_PER_ATTEMPT : 0;
	}
	
	/**
	 * Retrieves the relic data associated with a specified relic ID.
	 * @param id the unique ID of the relic to retrieve
	 * @return the {@code RelicDataHolder} containing the details of the relic, or {@code null} if no relic is associated with the specified ID
	 */
	public RelicDataHolder getRelic(int id)
	{
		return RELICS.get(id);
	}
	
	/**
	 * Retrieves the skill ID associated with a specified relic ID.
	 * @param id the unique ID of the relic to retrieve the skill ID for
	 * @param enchant
	 * @return the skill ID of the relic, or {@code 0} if no relic is associated with the specified ID
	 */
	public int getRelicSkillId(int id, int enchant)
	{
		return RELICS.get(id).getEnchantHolderByEnchant(enchant).getSkillId();
	}
	
	/**
	 * Retrieves the skill level associated with a specified relic ID.
	 * @param id the unique ID of the relic to retrieve the skill level for
	 * @param enchant
	 * @return the skill level of the relic, or {@code 0} if no relic is associated with the specified ID
	 */
	public int getRelicSkillLevel(int id, int enchant)
	{
		return RELICS.get(id).getEnchantHolderByEnchant(enchant).getSkillLevel();
	}
	
	/**
	 * Retrieves the compound fee holder for the specified relic grade.
	 * @param grade the grade of the relic
	 * @return the {@code RelicCompoundFeeHolder} associated with the grade, or {@code null} if no fee holder is defined for that grade
	 */
	public RelicCompoundFeeHolder getCompoundFeeHolderByGrade(RelicGrade grade)
	{
		return GRADE_COMPOUND_FEES.get(grade);
	}
	
	/**
	 * Retrieves a collection of all available relic data.
	 * @return a collection of {@code RelicDataHolder} objects representing all relics
	 */
	public Collection<RelicDataHolder> getRelics()
	{
		return RELICS.values();
	}
	
	/**
	 * Retrieves the relic summon categories.
	 * @return a map of category IDs to {@code RelicSummonCategoryHolder} objects
	 */
	public Map<Integer, RelicSummonCategoryHolder> getRelicSummonCategories()
	{
		return SUMMON_CATEGORIES;
	}
	
	/**
	 * Retrieves the active relic summon categories.
	 * @return a collection of {@code RelicSummonCategoryHolder} objects representing active summon categories
	 */
	public Collection<RelicSummonCategoryHolder> getRelicActiveCategories()
	{
		return ACTIVE_SUMMON_CATEGORIES;
	}
	
	/**
	 * Retrieves a specific relic summon category by its ID.
	 * @param categoryId the ID of the summon category
	 * @return the {@code RelicSummonCategoryHolder} object, or {@code null} if no category exists for the specified ID
	 */
	public RelicSummonCategoryHolder getRelicSummonCategory(int categoryId)
	{
		return SUMMON_CATEGORIES.get(categoryId);
	}
	
	/**
	 * Retrieves the enchant fee details for a specific relic grade.
	 * @param grade the grade of the relic being enchanted
	 * @return the {@code ItemHolder} containing enchant fee information for that grade
	 */
	public ItemHolder getEnchantFee(RelicGrade grade)
	{
		return ENCHANT_FEE_HOLDERS.get(grade);
	}
	
	/**
	 * Retrieves the enchant rate based on the count of ingredients.
	 * @param count the number of ingredients
	 * @return the enchant rate for the given ingredient count
	 */
	public int getEnchantRateByIngredientCount(int count)
	{
		return ENCHANT_CHANCES.get(count - 1);
	}
	
	/**
	 * Retrieves a relic ID for summoning based on weighted random selection.
	 * @return the relic ID selected for summoning, or {@code 0} if no valid relics are available
	 */
	public int getRelicBySummon()
	{
		final List<RelicDataHolder> relics = new ArrayList<>(RelicData.getInstance().getRelics().stream().filter(relic -> relic.getSummonChance() > 0).toList());
		final long totalWeight = relics.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
		if (totalWeight <= 0)
		{
			LOGGER.warning("No valid relics available for summoning.");
			return 0;
		}
		
		final long rng = Rnd.get(totalWeight);
		long cumulativeWeight = 0;
		for (RelicDataHolder relic : relics)
		{
			cumulativeWeight += relic.getSummonChance();
			if (rng < cumulativeWeight)
			{
				return relic.getRelicId();
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves a relic for compounding based on its grade and the configured combineData chances. Uses the COMMON/SHINING type split defined in the XML to pick the result pool.
	 * @param grade the ingredient grade being compounded
	 * @return an {@code Entry} containing a success flag and the ID of the resulting relic
	 */
	public Entry<Boolean, Integer> getRelicByCompound(RelicGrade grade)
	{
		final RelicCompoundChanceHolder chances = GRADE_COMPOUND_CHANCES.get(grade);
		if (chances == null)
		{
			LOGGER.warning("No combineData configured for grade: " + grade);
			return new SimpleEntry<>(false, 0);
		}
		
		// Roll outcome bucket: failCommon / failShining / successCommon / successShining
		final int roll = Rnd.get(100);
		final boolean success;
		final boolean shining;
		
		final int failCommonTop = chances.getFailCommonChance();
		final int failShiningTop = failCommonTop + chances.getFailShiningChance();
		final int successCommonTop = failShiningTop + chances.getSuccessCommonChance();
		// remainder goes to successShining
		
		if (roll < failCommonTop)
		{
			success = false;
			shining = false;
		}
		else if (roll < failShiningTop)
		{
			success = false;
			shining = true;
		}
		else if (roll < successCommonTop)
		{
			success = true;
			shining = false;
		}
		else
		{
			success = true;
			shining = true;
		}
		
		// Select the result grade pool
		final RelicGrade resultGrade = success ? RelicGrade.values()[grade.ordinal() + 1] : grade;
		final Collection<RelicDataHolder> pool = shining ? getShiningRelicsByGrade(resultGrade) : getCommonRelicsByGrade(resultGrade);
		
		// Fall back to full grade pool if type pool is empty
		final Collection<RelicDataHolder> relics = ((pool != null) && !pool.isEmpty()) ? pool : getRelicsByGrade(resultGrade);
		if ((relics == null) || relics.isEmpty())
		{
			return new SimpleEntry<>(success, 0);
		}
		
		final List<RelicDataHolder> relicList = relics.stream().filter(r -> calculateCompoundChance(r.getRelicId(), grade) > 0).toList();
		if (relicList.isEmpty())
		{
			return new SimpleEntry<>(success, 0);
		}
		final long totalWeight = Math.max(1, relicList.stream().mapToLong(relic -> calculateCompoundChance(relic.getRelicId(), grade)).sum());
		final long relicRng = Rnd.get(totalWeight);
		long cumulativeWeight = 0;
		for (RelicDataHolder relic : relicList)
		{
			cumulativeWeight += calculateCompoundChance(relic.getRelicId(), grade);
			if (relicRng < cumulativeWeight)
			{
				return new SimpleEntry<>(success, relic.getRelicId());
			}
		}
		
		return new SimpleEntry<>(success, relicList.get(relicList.size() - 1).getRelicId());
	}
	
	/**
	 * Calculates the compound chance for a relic based on its ID and grade. Returns 0 for relics that cannot be compounded (modifier = 0).
	 * @param relicId the ID of the relic
	 * @param grade the ingredient grade being compounded
	 * @return the calculated compound chance as a long value, or 0 if not compoundable
	 */
	public long calculateCompoundChance(int relicId, RelicGrade grade)
	{
		final RelicDataHolder relic = RelicData.getInstance().getRelic(relicId);
		final boolean sameGrade = relic.getGrade() == grade;
		final float div = sameGrade ? relic.getCompoundChanceModifier() : relic.getCompoundUpGradeChanceModifier();
		
		// Relics with modifier 0 cannot be compounded (anniversary, Freya, shadow special relics).
		if (div <= 0)
		{
			return 0;
		}
		
		final long combineChance = relic.getSummonChance() == 0 ? (sameGrade ? 1000000000L : 100000000L) : relic.getSummonChance();
		final double raw = (combineChance / div) / 100000000.0;
		
		if (Double.isNaN(raw) || Double.isInfinite(raw) || (raw <= 0))
		{
			return 0;
		}
		
		return new BigDecimal(raw).setScale(4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100000000)).longValue();
	}
	
	/**
	 * Retrieves a guaranteed relic for compounding based on its grade (always returns a higher grade relic).
	 * @param grade the grade of the relic
	 * @return the ID of the resulting higher grade relic
	 */
	public int getGuaranteedRelicByCompound(RelicGrade grade)
	{
		final RelicGrade successGrade = RelicGrade.values()[grade.ordinal() + 1];
		final Collection<RelicDataHolder> relics = getRelicsByGrade(successGrade);
		if ((relics == null) || relics.isEmpty())
		{
			return 0;
		}
		
		final List<RelicDataHolder> relicList = relics.stream().filter(r -> calculateCompoundChance(r.getRelicId(), grade) > 0).toList();
		if (relicList.isEmpty())
		{
			return 0;
		}
		
		final long totalWeight = Math.max(1, relics.stream().mapToLong(relic -> calculateCompoundChance(relic.getRelicId(), grade)).sum());
		
		final long relicRng = Rnd.get(totalWeight);
		long cumulativeWeight = 0;
		for (RelicDataHolder relic : relicList)
		{
			cumulativeWeight += calculateCompoundChance(relic.getRelicId(), grade);
			if (relicRng < cumulativeWeight)
			{
				return relic.getRelicId();
			}
		}
		
		return relicList.get(Rnd.get(relicList.size())).getRelicId();
	}
	
	/**
	 * Generates a list of relics obtained through summoning.
	 * @param summonCount the number of relics to summon
	 * @return a list of relic IDs obtained through summoning
	 */
	public List<Integer> generateSummonRelics(int summonCount)
	{
		final List<Integer> relics = new ArrayList<>();
		for (int i = 1; i <= summonCount; i++)
		{
			final int obtainedRelicId = getRelicBySummon();
			relics.add(obtainedRelicId);
		}
		
		return relics;
	}
	
	public List<RelicDataHolder> getRelicsByParentId(int parentRelicId)
	{
		final List<RelicDataHolder> relics = new ArrayList<>();
		for (RelicDataHolder relic : RELICS.values())
		{
			if (relic.getParentRelicId() == parentRelicId)
			{
				relics.add(relic);
			}
		}
		
		return relics;
	}
	
	public static RelicData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicData INSTANCE = new RelicData();
	}
}