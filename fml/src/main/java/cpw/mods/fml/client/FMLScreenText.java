package cpw.mods.fml.client;

import net.minecraft.client.resources.I18n;

/**
 * Translates the text of FML's own screens.
 *
 * Some of them, such as the duplicate mods screen, open before FML's language files are loaded, so a key that has no
 * translation yet falls back to the English text instead of showing the key.
 */
public class FMLScreenText
{
    public static String format(String key, String english, Object... args)
    {
        String text = I18n.format(key, args);
        return key.equals(text) ? String.format(english, args) : text;
    }
}
