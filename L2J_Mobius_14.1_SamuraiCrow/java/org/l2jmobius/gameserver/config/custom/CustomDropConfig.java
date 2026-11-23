package org.l2jmobius.gameserver.config.custom;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

public final class CustomDropConfig {
   private static final String FILE = "./config/Custom/DropItems.ini";
   private static final Map<Integer, List<DropEntry>> DROPS = new HashMap();
   private static final List<DropEntry> GROUP_DROPS = new ArrayList();
   private static final List<DropEntry> SPECIFIC_DATE_DROPS = new ArrayList();
   private static final Set<Integer> SPECIFIC_DATES = new HashSet();
   private static volatile boolean GROUP_DROP;
   private static volatile boolean SPECIFIC_DROP;
   private static volatile double GROUP_DROP_PROBABILITY;
   private static volatile double SPECIFIC_PROBABILITY;
   private static volatile int DROP_START_HOUR;
   private static volatile int DROP_END_HOUR;
   private static volatile boolean LOADED;

   private CustomDropConfig() {
   }

   public static synchronized void load() {
      DROPS.clear();
      GROUP_DROPS.clear();
      SPECIFIC_DATE_DROPS.clear();
      SPECIFIC_DATES.clear();
      GROUP_DROP = false;
      SPECIFIC_DROP = false;
      GROUP_DROP_PROBABILITY = (double)0.0F;
      SPECIFIC_PROBABILITY = (double)0.0F;
      DROP_START_HOUR = 0;
      DROP_END_HOUR = 0;

      try {
         Path var0 = Path.of("./config/Custom/DropItems.ini");
         if (!Files.exists(var0, new LinkOption[0])) {
            Path[] var1 = new Path[]{Path.of("./game/config/Custom/DropItems.ini"), Path.of("./dist/game/config/Custom/DropItems.ini")};
            var0 = (Path)Arrays.stream(var1).filter((var0x) -> Files.exists(var0x, new LinkOption[0])).findFirst().orElse(var0);
         }

         for(String var2 : Files.readAllLines(var0, StandardCharsets.UTF_8)) {
            String var3 = var2.replace("\ufeff", "").trim();
            if (!var3.isEmpty() && !var3.startsWith("#") && var3.contains("=")) {
               int var4 = var3.indexOf(61);
               String var5 = var3.substring(0, var4).trim();
               String var6 = var3.substring(var4 + 1).trim();

               try {
                  switch (var5) {
                     case "DropItems":
                        a(var6);
                        break;
                     case "GroupDrop":
                        GROUP_DROP = Boolean.parseBoolean(var6);
                        break;
                     case "GroupDropItems":
                        a(var6, GROUP_DROPS);
                        break;
                     case "GroupDropProbability":
                        GROUP_DROP_PROBABILITY = b(var6);
                        break;
                     case "DropTimeLimit":
                        d(var6);
                        break;
                     case "SpecificDrop":
                        SPECIFIC_DROP = Boolean.parseBoolean(var6);
                        break;
                     case "SpecificDate":
                        c(var6);
                        break;
                     case "SpecificDropID":
                        a(var6, SPECIFIC_DATE_DROPS);
                        break;
                     case "SpecificProbability":
                        SPECIFIC_PROBABILITY = b(var6);
                  }
               } catch (RuntimeException var9) {
               }
            }
         }
      } catch (Exception var10) {
      }

      LOADED = true;
   }

   private static void a(String var0) {
      if (!var0.isEmpty()) {
         for(String var4 : var0.split(";")) {
            String[] var5 = var4.trim().split(",");
            if (var5.length == 4) {
               try {
                  int var6 = Integer.parseInt(var5[0].trim());
                  int var7 = Integer.parseInt(var5[1].trim());
                  long var8 = Long.parseLong(var5[2].trim());
                  double var10 = b(var5[3]);
                  if (var6 > 0 && var7 > 0 && var8 > 0L && var10 > (double)0.0F) {
                     ((List)DROPS.computeIfAbsent(var6, (var0x) -> new ArrayList())).add(new DropEntry(var7, var8, var10));
                  }
               } catch (NumberFormatException var12) {
               }
            }
         }

      }
   }

   private static void a(String var0, List<DropEntry> var1) {
      if (!var0.isEmpty()) {
         for(String var5 : var0.split(";")) {
            String[] var6 = var5.trim().split(",");
            if (var6.length == 2) {
               try {
                  int var7 = Integer.parseInt(var6[0].trim());
                  long var8 = Long.parseLong(var6[1].trim());
                  if (var7 > 0 && var8 > 0L) {
                     var1.add(new DropEntry(var7, var8, (double)100.0F));
                  }
               } catch (NumberFormatException var10) {
               }
            }
         }

      }
   }

   private static double b(String var0) {
      double var1 = Double.parseDouble(var0.trim().replace("%", ""));
      return var1 > (double)0.0F && var1 <= (double)100.0F ? var1 : (double)0.0F;
   }

   private static void c(String var0) {
      if (!var0.isEmpty()) {
         for(String var4 : var0.split(";")) {
            String var5 = var4.trim();
            if (var5.matches("\\d{4}")) {
               try {
                  int var6 = Integer.parseInt(var5.substring(0, 2));
                  int var7 = Integer.parseInt(var5.substring(2, 4));
                  MonthDay.of(var6, var7);
                  SPECIFIC_DATES.add(var6 * 100 + var7);
               } catch (RuntimeException var8) {
               }
            }
         }

      }
   }

   private static void d(String var0) {
      String[] var1 = var0.split(",");
      if (var1.length == 2) {
         int var2 = Integer.parseInt(var1[0].trim());
         int var3 = Integer.parseInt(var1[1].trim());
         if (var2 >= 0 && var2 <= 23 && var3 >= 0 && var3 <= 24) {
            DROP_START_HOUR = var2;
            DROP_END_HOUR = var3;
         }

      }
   }

   public static List<ItemHolder> roll(int var0) {
      if (!LOADED) {
         load();
      }

      ArrayList var1 = new ArrayList();
      List var2 = (List)DROPS.get(var0);
      if (var2 != null) {
         a((List)var2, var1);
      }

      LocalDateTime var3 = LocalDateTime.now();
      if (GROUP_DROP && a(var3)) {
         a(GROUP_DROPS, var1, GROUP_DROP_PROBABILITY);
      }

      int var4 = var3.getMonthValue() * 100 + var3.getDayOfMonth();
      if (SPECIFIC_DROP && SPECIFIC_DATES.contains(var4)) {
         a(SPECIFIC_DATE_DROPS, var1, SPECIFIC_PROBABILITY);
      }

            return var1;
   }

   private static boolean a(LocalDateTime var0) {
      if (DROP_START_HOUR == 0 && DROP_END_HOUR == 0) {
         return true;
      } else {
         int var1 = var0.getHour();
         if (DROP_END_HOUR == 24) {
            return var1 >= DROP_START_HOUR;
         } else if (DROP_START_HOUR < DROP_END_HOUR) {
            return var1 >= DROP_START_HOUR && var1 < DROP_END_HOUR;
         } else {
            return var1 >= DROP_START_HOUR || var1 < DROP_END_HOUR;
         }
      }
   }

   private static void a(List<DropEntry> var0, List<ItemHolder> var1) {
      for(DropEntry var3 : var0) {
         if (ThreadLocalRandom.current().nextDouble((double)100.0F) < var3.chance) {
            var1.add(new ItemHolder(var3.itemId, var3.count));
         }
      }

   }

   private static void a(List<DropEntry> var0, List<ItemHolder> var1, double var2) {
      if (!(var2 <= (double)0.0F) && !(var2 > (double)100.0F)) {
         for(DropEntry var5 : var0) {
            if (ThreadLocalRandom.current().nextDouble((double)100.0F) < var2) {
               var1.add(new ItemHolder(var5.itemId, var5.count));
            }
         }

      }
   }

   private static record DropEntry(int itemId, long count, double chance) {
   }
}
