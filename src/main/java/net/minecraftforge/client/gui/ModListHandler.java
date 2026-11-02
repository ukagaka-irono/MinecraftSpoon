package net.minecraftforge.client.gui;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.google.common.base.Throwables;

import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.client.GuiSpoonModList;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Opens Spoon's mod list wherever Forge's would open.
 *
 * GuiModList is kept as Forge wrote it, because mods extend it and read its private fields: EnderCore swaps
 * every GuiModList for its own subclass, which crashes if the fields are gone. This handler runs first and
 * swaps only a plain GuiModList, so the title screen and mods that open GuiModList show Spoon's list, and a
 * mod's own subclass of GuiModList still opens as it is.
 */
public class ModListHandler
{
    private static final Field mainMenu = ReflectionHelper.findField(GuiModList.class, "mainMenu");

    public static void register()
    {
        MinecraftForge.EVENT_BUS.register(new ModListHandler());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event)
    {
        if (event.gui != null && event.gui.getClass() == GuiModList.class)
        {
            try
            {
                event.gui = new GuiSpoonModList((GuiScreen) mainMenu.get(event.gui));
            }
            catch (IllegalAccessException e)
            {
                throw Throwables.propagate(e);
            }
        }
    }
}
