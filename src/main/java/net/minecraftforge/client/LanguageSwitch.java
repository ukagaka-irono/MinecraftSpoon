package net.minecraftforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.LanguageManager;
import net.minecraftforge.common.ForgeModContainer;

import cpw.mods.fml.common.FMLLog;

/**
 * Switches the language without reloading every resource. Minecraft 1.7.10 reloads all textures, models and sounds
 * when the language changes, which takes a minute in a big modpack, though only the language files depend on it:
 * LanguageManager reads them, merges FML's table and updates StringTranslate.
 */
public final class LanguageSwitch
{
    private LanguageSwitch() {}

    /**
     * Called by GuiLanguage once the new language is set, in place of Minecraft.refreshResources.
     */
    public static void reload(Minecraft mc, LanguageManager languageManager)
    {
        long start = System.nanoTime();
        if (ForgeModContainer.fastLanguageSwitch)
        {
            languageManager.onResourceManagerReload(mc.getResourceManager());
        }
        else
        {
            mc.refreshResources();
        }
        FMLLog.info("Spoon language: switched to %s in %d ms%s", languageManager.getCurrentLanguage().getLanguageCode(),
                (System.nanoTime() - start) / 1000000, ForgeModContainer.fastLanguageSwitch ? ", reloading only the language files" : "");
    }
}
