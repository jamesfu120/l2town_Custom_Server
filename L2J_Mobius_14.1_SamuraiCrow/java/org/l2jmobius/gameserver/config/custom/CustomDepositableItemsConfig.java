package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class CustomDepositableItemsConfig {
   private static final String CUSTOM_DEPOSITABLE_ITEMS_CONFIG_FILE = "./config/Custom/CustomDepositableItems.ini";
   public static boolean CUSTOM_DEPOSITABLE_ENABLED;
   public static boolean CUSTOM_DEPOSITABLE_QUEST_ITEMS;

   public static void load() {
      ConfigReader var0 = new ConfigReader("./config/Custom/CustomDepositableItems.ini");
      CUSTOM_DEPOSITABLE_ENABLED = var0.getBoolean("CustomDepositableEnabled", false);
      CUSTOM_DEPOSITABLE_QUEST_ITEMS = var0.getBoolean("DepositableQuestItems", false);
   }
}
