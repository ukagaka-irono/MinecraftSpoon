/*
 * The FML Forge Mod Loader suite.
 * Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package cpw.mods.fml.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.common.ForgeVersion;

import org.apache.logging.log4j.Level;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.client.config.HoverChecker;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.InjectedModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState.ModState;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModConfigFiles;
import cpw.mods.fml.common.ModMetadata;

/**
 * The mod list: a searchable list of mods on the left and the selected mod's details on the right.
 * It opens wherever a GuiModList would. GuiModList itself stays as Forge wrote it, because mods such as
 * EnderCore extend it and read its private fields.
 *
 * @author cpw
 *
 */
public class GuiSpoonModList extends GuiScreen implements GuiYesNoCallback
{
    static final int COLOR_TEXT = 0xFFFFFF;
    static final int COLOR_SECONDARY = 0xCCCCCC;
    static final int COLOR_ERROR = 0xFF2222;
    static final int COLOR_LINK = 0x5599FF;
    private static final int COLOR_PLACEHOLDER = 0x808080;
    private static final int COLOR_BADGE_CORE = 0x2266CC;
    private static final int COLOR_UPDATE = 0x22AA44;
    static final int ICON_SIZE = 32;
    static final int ENTRY_HEIGHT = ICON_SIZE + 4;
    static final int LINE_HEIGHT = 10;
    static final int GAP = 4;
    static final int TEXT_INSET = 4;

    private static final int MARGIN = 10;
    private static final int TITLE_Y = 8;
    private static final int CONTROL_HEIGHT = 20;
    private static final int SORT_BUTTON_WIDTH = 24;
    private static final int SEARCH_Y = 22;
    private static final int SEARCH_MAX_LENGTH = 64;
    private static final int COUNT_Y = SEARCH_Y + CONTROL_HEIGHT + GAP;
    private static final int LIST_TOP = COUNT_Y + LINE_HEIGHT + 2;
    private static final int LIST_BOTTOM_OFFSET = 32;
    private static final int BUTTON_BOTTOM_OFFSET = 29;
    private static final int BUTTON_ROW_GAP = 5;
    private static final int MIN_BUTTON_WIDTH = 100;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int INDENT = 8;
    private static final int BADGE_HEIGHT = 11;
    private static final int BADGE_PADDING = 3;
    private static final int BADGE_SPACING = 2;
    private static final int TOOLTIP_DELAY = 800;
    private static final int INFO_WHEEL_STEPS = 3;

    private static final int BUTTON_DONE = 6;
    private static final int BUTTON_MODS_LIST_UP = 7;
    private static final int BUTTON_MODS_LIST_DOWN = 8;
    private static final int BUTTON_INFO_UP = 9;
    private static final int BUTTON_INFO_DOWN = 10;
    private static final int BUTTON_SORT = 11;
    private static final int BUTTON_WEBSITE = 12;
    private static final int BUTTON_CONFIG = 20;
    private static final int BUTTON_OPEN_FOLDER = 22;
    private static final int CONFIRM_LINK = 0;

    private static final int DRAG_NONE = 0;
    private static final int DRAG_MODS = 1;
    private static final int DRAG_INFO = 2;
    private static final int DRAG_OTHER = 3;

    private static final ResourceLocation UNKNOWN_ICON = new ResourceLocation("textures/misc/unknown_pack.png");

    private GuiScreen mainMenu;
    private GuiSlotSpoonModList modList;
    private GuiSlotModInfo modInfo;
    private GuiTextField searchField;
    private GuiButton sortButton;
    private HoverChecker sortHover;
    private GuiButton websiteButton;
    private GuiButton configModButton;
    // The config files the Config button opens when the selected mod has no config screen
    private List<File> configFiles = Collections.emptyList();
    private final ArrayList<ModContainer> mods;
    private final ArrayList<ModContainer> visibleMods = new ArrayList<ModContainer>();
    private ModContainer selectedMod;
    private String pendingLink;
    private boolean sortDescending;
    private int dragTarget = DRAG_NONE;
    private int paneWidth;
    private int detailsLeft;
    private int infoTop;
    private int listBottom;
    private final Map<ModContainer, ModIcon> icons = Maps.newHashMap();

    private static class ModIcon
    {
        final ResourceLocation texture;
        final int width;
        final int height;

        ModIcon(ResourceLocation texture, int width, int height)
        {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * @param mainMenu
     */
    public GuiSpoonModList(GuiScreen mainMenu)
    {
        this.mainMenu=mainMenu;
        this.mods=new ArrayList<ModContainer>();
        FMLClientHandler.instance().addSpecialModEntries(mods);
        for (ModContainer mod : Loader.instance().getModList()) {
            if (mod.getMetadata()!=null && mod.getMetadata().parentMod==null && !Strings.isNullOrEmpty(mod.getMetadata().parent)) {
                String parentMod = mod.getMetadata().parent;
                ModContainer parentContainer = Loader.instance().getIndexedModList().get(parentMod);
                if (parentContainer != null)
                {
                    mod.getMetadata().parentMod = parentContainer;
                    parentContainer.getMetadata().childMods.add(mod);
                    continue;
                }
            }
            else if (mod.getMetadata()!=null && mod.getMetadata().parentMod!=null)
            {
                 continue;
            }
            mods.add(mod);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.paneWidth = (this.width - MARGIN * 3) / 2;
        this.detailsLeft = MARGIN * 2 + this.paneWidth;
        this.infoTop = LIST_TOP + CONTROL_HEIGHT + GAP;
        this.listBottom = this.height - LIST_BOTTOM_OFFSET;
        int buttonY = this.height - BUTTON_BOTTOM_OFFSET;

        String previousSearch = this.searchField != null ? this.searchField.getText() : "";
        this.searchField = new GuiTextField(this.fontRendererObj, MARGIN, SEARCH_Y, this.paneWidth - SORT_BUTTON_WIDTH - GAP, CONTROL_HEIGHT);
        this.searchField.setMaxStringLength(SEARCH_MAX_LENGTH);
        this.searchField.setText(previousSearch);
        this.searchField.setFocused(true);

        this.sortButton = new GuiButtonExt(BUTTON_SORT, MARGIN + this.paneWidth - SORT_BUTTON_WIDTH, SEARCH_Y, SORT_BUTTON_WIDTH, CONTROL_HEIGHT, "");
        this.buttonList.add(this.sortButton);
        this.sortHover = new HoverChecker(this.sortButton, TOOLTIP_DELAY);

        int linkWidth = (this.paneWidth - GAP) / 2;
        this.websiteButton = new GuiButtonExt(BUTTON_WEBSITE, this.detailsLeft, LIST_TOP, linkWidth, CONTROL_HEIGHT, I18n.format("fml.menu.mods.website"));
        this.configModButton = new GuiButtonExt(BUTTON_CONFIG, this.detailsLeft + linkWidth + GAP, LIST_TOP, this.paneWidth - linkWidth - GAP, CONTROL_HEIGHT, I18n.format("fml.menu.mods.config"));
        this.buttonList.add(this.websiteButton);
        this.buttonList.add(this.configModButton);

        String doneText = I18n.format("gui.done");
        String folderText = I18n.format("fml.menu.mods.openFolder");
        int doneWidth = Math.max(this.fontRendererObj.getStringWidth(doneText) + 20, MIN_BUTTON_WIDTH);
        int folderWidth = Math.max(this.fontRendererObj.getStringWidth(folderText) + 20, MIN_BUTTON_WIDTH);
        int rowLeft = this.width / 2 - (folderWidth + BUTTON_ROW_GAP + doneWidth) / 2;
        this.buttonList.add(new GuiButtonExt(BUTTON_OPEN_FOLDER, rowLeft, buttonY, folderWidth, CONTROL_HEIGHT, folderText));
        this.buttonList.add(new GuiButtonExt(BUTTON_DONE, rowLeft + folderWidth + BUTTON_ROW_GAP, buttonY, doneWidth, CONTROL_HEIGHT, doneText));

        refreshMods();
        if (this.selectedMod == null && !this.visibleMods.isEmpty())
        {
            this.selectedMod = this.visibleMods.get(0);
        }
        updateDetails();
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
        for (ModIcon icon : this.icons.values())
        {
            if (icon.texture != UNKNOWN_ICON)
            {
                this.mc.getTextureManager().deleteTexture(icon.texture);
            }
        }
        this.icons.clear();
    }

    @Override
    public void updateScreen()
    {
        this.searchField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.mc.displayGuiScreen(this.mainMenu);
        }
        else if (this.searchField.textboxKeyTyped(typedChar, keyCode))
        {
            refreshMods();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput()
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0)
        {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            scrollListAt(mouseX, mouseY, wheel);
        }
    }

    /**
     * Both lists read the mouse wheel from the event queue while they draw, so whichever draws first would
     * take every scroll. drawScreen drains the queue first, and the wheel scrolls only the list under the cursor.
     */
    private void scrollListAt(int mouseX, int mouseY, int wheel)
    {
        if (this.modList.contains(mouseX, mouseY))
        {
            this.modList.actionPerformed(new GuiButton(wheel > 0 ? BUTTON_MODS_LIST_UP : BUTTON_MODS_LIST_DOWN, 0, 0, ""));
        }
        else if (this.modInfo.contains(mouseX, mouseY))
        {
            // A scroll button moves two thirds of a line, so a wheel notch takes a few of them
            GuiButton step = new GuiButton(wheel > 0 ? BUTTON_INFO_UP : BUTTON_INFO_DOWN, 0, 0, "");
            for (int i = 0; i < INFO_WHEEL_STEPS; i++)
            {
                this.modInfo.actionPerformed(step);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled)
        {
            switch (button.id)
            {
                case BUTTON_DONE:
                    this.mc.displayGuiScreen(this.mainMenu);
                    return;
                case BUTTON_SORT:
                    this.sortDescending = !this.sortDescending;
                    refreshMods();
                    return;
                case BUTTON_WEBSITE:
                    openLink(this.selectedMod.getMetadata().url);
                    return;
                case BUTTON_OPEN_FOLDER:
                    openModsFolder();
                    return;
                case BUTTON_CONFIG:
                    if (!this.configFiles.isEmpty())
                    {
                        openInSystem(configTarget(this.configFiles));
                        return;
                    }
                    try
                    {
                        IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(selectedMod);
                        GuiScreen newScreen = guiFactory.mainConfigGuiClass().getConstructor(GuiScreen.class).newInstance(this);
                        this.mc.displayGuiScreen(newScreen);
                    }
                    catch (Exception e)
                    {
                        FMLLog.log(Level.ERROR, e, "There was a critical issue trying to build the config GUI for %s", selectedMod.getModId());
                    }
                    return;
            }
        }
        super.actionPerformed(button);
    }

    /**
     * Asks before opening a link in the browser, the way chat links do.
     */
    void openLink(String url)
    {
        url = url.trim();
        this.pendingLink = url.contains("://") ? url : "http://" + url;
        this.mc.displayGuiScreen(new GuiConfirmOpenLink(this, this.pendingLink, CONFIRM_LINK, false));
    }

    @Override
    public void confirmClicked(boolean result, int id)
    {
        if (id == CONFIRM_LINK)
        {
            if (result)
            {
                try
                {
                    Class<?> desktop = Class.forName("java.awt.Desktop");
                    Object instance = desktop.getMethod("getDesktop").invoke(null);
                    desktop.getMethod("browse", URI.class).invoke(instance, new URI(this.pendingLink));
                }
                catch (Throwable t)
                {
                    FMLLog.log(Level.ERROR, t, "Couldn't open the link %s", this.pendingLink);
                }
            }
            this.pendingLink = null;
            this.mc.displayGuiScreen(this);
        }
    }

    private void openModsFolder()
    {
        File modsDir = new File(this.mc.mcDataDir, "mods");
        modsDir.mkdirs();
        openInSystem(modsDir);
    }

    /**
     * Returns the file to open for a mod's config files: the file itself, or the folder all of them are in.
     */
    static File configTarget(List<File> files)
    {
        if (files.size() == 1)
        {
            return files.get(0);
        }
        File folder = files.get(0).getParentFile();
        for (File file : files)
        {
            while (folder != null && !file.getPath().startsWith(folder.getPath() + File.separator))
            {
                folder = folder.getParentFile();
            }
        }
        return folder != null ? folder : files.get(0).getParentFile();
    }

    /**
     * Opens a folder in the file manager, or a file in the program the system opens it with.
     */
    private static void openInSystem(File file)
    {
        String path = file.getAbsolutePath();
        try
        {
            if (Util.getOSType() == Util.EnumOS.WINDOWS)
            {
                Runtime.getRuntime().exec(String.format("cmd.exe /C start \"Open file\" \"%s\"", path));
                return;
            }
            if (Util.getOSType() == Util.EnumOS.OSX)
            {
                // -t opens a file in the text editor, because config files usually have no program of their own
                Runtime.getRuntime().exec(file.isDirectory() ? new String[] {"/usr/bin/open", path} : new String[] {"/usr/bin/open", "-t", path});
                return;
            }
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            if (file.isDirectory())
            {
                desktop.getMethod("browse", URI.class).invoke(instance, file.toURI());
            }
            else
            {
                desktop.getMethod("open", File.class).invoke(instance, file);
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Couldn't open %s, trying the system browser", path);
            Sys.openURL("file://" + path);
        }
    }

    /**
     * Filters the mods by the search text and sorts them by name. Child mods are listed on their own only when they match a search.
     */
    private void refreshMods()
    {
        String query = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        this.visibleMods.clear();
        for (ModContainer mod : this.mods)
        {
            if (query.isEmpty() || matches(mod, query))
            {
                this.visibleMods.add(mod);
            }
            if (!query.isEmpty() && mod.getMetadata() != null)
            {
                for (ModContainer child : mod.getMetadata().childMods)
                {
                    if (matches(child, query))
                    {
                        this.visibleMods.add(child);
                    }
                }
            }
        }
        Collections.sort(this.visibleMods, new Comparator<ModContainer>()
        {
            @Override
            public int compare(ModContainer a, ModContainer b)
            {
                int order = a.getName().compareToIgnoreCase(b.getName());
                return sortDescending ? -order : order;
            }
        });
        this.sortButton.displayString = I18n.format(this.sortDescending ? "fml.menu.mods.sort.za" : "fml.menu.mods.sort.az");
        // A new list starts scrolled to the top
        this.modList = new GuiSlotSpoonModList(this, this.visibleMods, this.paneWidth, LIST_TOP, this.listBottom, MARGIN);
        this.modList.registerScrollButtons(this.buttonList, BUTTON_MODS_LIST_UP, BUTTON_MODS_LIST_DOWN);
    }

    private static boolean matches(ModContainer mod, String query)
    {
        ModMetadata meta = mod.getMetadata();
        return mod.getName().toLowerCase(Locale.ROOT).contains(query)
                || mod.getModId().toLowerCase(Locale.ROOT).contains(query)
                || (meta != null && meta.description != null && meta.description.toLowerCase(Locale.ROOT).contains(query));
    }

    private void updateDetails()
    {
        // A new details list starts at the top of the newly selected mod
        this.modInfo = new GuiSlotModInfo(this, this.paneWidth, this.infoTop, this.listBottom, this.detailsLeft);
        this.modInfo.registerScrollButtons(this.buttonList, BUTTON_INFO_UP, BUTTON_INFO_DOWN);
        this.modInfo.setLines(this.selectedMod == null ? new ArrayList<GuiSlotModInfo.Line>() : detailLines(this.selectedMod));
        boolean hasMetadata = this.selectedMod != null && !this.selectedMod.getMetadata().autogenerated;
        this.websiteButton.enabled = hasMetadata && !Strings.isNullOrEmpty(this.selectedMod.getMetadata().url);
        IModGuiFactory guiFactory = this.selectedMod == null ? null : FMLClientHandler.instance().getGuiFactoryFor(this.selectedMod);
        boolean hasConfigScreen = guiFactory != null && guiFactory.mainConfigGuiClass() != null;
        // Most 1.7.10 mods have no config screen, so the button opens their config file instead
        this.configFiles = this.selectedMod == null || hasConfigScreen ? Collections.<File>emptyList() : ModConfigFiles.get(this.selectedMod);
        this.configModButton.enabled = hasConfigScreen || !this.configFiles.isEmpty();
        this.configModButton.displayString = I18n.format(this.configFiles.isEmpty() ? "fml.menu.mods.config"
                : this.configFiles.size() == 1 ? "fml.menu.mods.configFile" : "fml.menu.mods.configFolder");
        this.websiteButton.visible = this.selectedMod != null;
        this.configModButton.visible = this.selectedMod != null;
    }

    private List<GuiSlotModInfo.Line> detailLines(ModContainer mod)
    {
        List<GuiSlotModInfo.Line> lines = Lists.newArrayList();
        int wrap = this.paneWidth - TEXT_INSET * 2 - SCROLLBAR_WIDTH;
        ModMetadata meta = mod.getMetadata();
        String newVersion = newVersionOf(mod);
        if (newVersion != null)
        {
            // Other mods can draw over the notice on the title screen, so the mod list shows it too
            addWrapped(lines, I18n.format("spoon.update.newversion", newVersion), 0, wrap, COLOR_UPDATE, Strings.emptyToNull(meta.updateUrl));
            lines.add(new GuiSlotModInfo.Line("", 0, COLOR_TEXT, null));
        }
        if (meta.autogenerated)
        {
            addWrapped(lines, I18n.format("fml.menu.mods.noMetadata"), 0, wrap, COLOR_SECONDARY, null);
        }
        else
        {
            addWrapped(lines, Strings.isNullOrEmpty(meta.description) ? I18n.format("fml.menu.mods.noDescription") : meta.description, 0, wrap, COLOR_TEXT, null);
            if (!Strings.isNullOrEmpty(meta.url) || !Strings.isNullOrEmpty(meta.updateUrl))
            {
                addHeading(lines, "fml.menu.mods.links");
                if (!Strings.isNullOrEmpty(meta.url))
                {
                    addWrapped(lines, meta.url, INDENT, wrap - INDENT, COLOR_LINK, meta.url);
                }
                if (!Strings.isNullOrEmpty(meta.updateUrl) && !meta.updateUrl.equals(meta.url))
                {
                    addWrapped(lines, meta.updateUrl, INDENT, wrap - INDENT, COLOR_LINK, meta.updateUrl);
                }
            }
            if (!meta.authorList.isEmpty() || !Strings.isNullOrEmpty(meta.credits))
            {
                addHeading(lines, "fml.menu.mods.credits");
                for (String author : meta.authorList)
                {
                    addWrapped(lines, author, INDENT, wrap - INDENT, COLOR_SECONDARY, null);
                }
                if (!Strings.isNullOrEmpty(meta.credits))
                {
                    addWrapped(lines, meta.credits, INDENT, wrap - INDENT, COLOR_SECONDARY, null);
                }
            }
            if (!meta.childMods.isEmpty())
            {
                addHeading(lines, "fml.menu.mods.children");
                for (ModContainer child : meta.childMods)
                {
                    addWrapped(lines, child.getName(), INDENT, wrap - INDENT, COLOR_SECONDARY, null);
                }
            }
        }
        lines.add(new GuiSlotModInfo.Line("", 0, COLOR_TEXT, null));
        addWrapped(lines, I18n.format("fml.menu.mods.modId", mod.getModId()), 0, wrap, COLOR_SECONDARY, null);
        String state = Loader.instance().getModState(mod).name().toLowerCase(Locale.ROOT);
        addWrapped(lines, I18n.format("fml.menu.mods.state", I18n.format("fml.menu.mods.state." + state)), 0, wrap, COLOR_SECONDARY, null);
        return lines;
    }

    /**
     * Returns the newer Spoon version the version check found, for Spoon's own entry, or null. Like the mod list of
     * newer Forge versions, this reads ForgeVersion directly.
     */
    private static String newVersionOf(ModContainer mod)
    {
        if (!"Forge".equals(mod.getModId()))
        {
            return null;
        }
        ForgeVersion.Status status = ForgeVersion.getStatus();
        return status == ForgeVersion.Status.OUTDATED || status == ForgeVersion.Status.BETA_OUTDATED ? ForgeVersion.getTarget() : null;
    }

    private void addHeading(List<GuiSlotModInfo.Line> lines, String key)
    {
        lines.add(new GuiSlotModInfo.Line("", 0, COLOR_TEXT, null));
        lines.add(new GuiSlotModInfo.Line(I18n.format(key), 0, COLOR_TEXT, null));
    }

    @SuppressWarnings("unchecked")
    private void addWrapped(List<GuiSlotModInfo.Line> lines, String text, int indent, int width, int color, String link)
    {
        for (String line : (List<String>) this.fontRendererObj.listFormattedStringToWidth(text, width))
        {
            lines.add(new GuiSlotModInfo.Line(line, indent, color, link));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        // Take the wheel and click events before the lists do; see scrollListAt
        while (Mouse.next())
        {
            this.handleMouseInput();
        }
        if (this.mc.currentScreen != this)
        {
            // A click above closed this screen, and onGuiClosed already freed the logos
            return;
        }

        // A press belongs to the list it started on, so dragging one list never scrolls the other
        boolean pressed = Mouse.isButtonDown(0);
        if (!pressed)
        {
            this.dragTarget = DRAG_NONE;
        }
        else if (this.dragTarget == DRAG_NONE)
        {
            this.dragTarget = this.modList.contains(mouseX, mouseY) ? DRAG_MODS : this.modInfo.contains(mouseX, mouseY) ? DRAG_INFO : DRAG_OTHER;
        }
        boolean modsGetMouse = !pressed || this.dragTarget == DRAG_MODS;
        boolean infoGetsMouse = !pressed || this.dragTarget == DRAG_INFO;
        drawClipped(this.modInfo, this.detailsLeft, this.infoTop, infoGetsMouse ? mouseX : -1, infoGetsMouse ? mouseY : -1, partialTicks);
        if (this.mc.currentScreen != this)
        {
            // A click on a link line opened the confirmation screen; drawing the mod list now would reload the logos
            return;
        }
        drawClipped(this.modList, MARGIN, LIST_TOP, modsGetMouse ? mouseX : -1, modsGetMouse ? mouseY : -1, partialTicks);

        this.drawCenteredString(this.fontRendererObj, I18n.format("fml.menu.mods.title"), this.width / 2, TITLE_Y, COLOR_TEXT);
        this.searchField.drawTextBox();
        if (this.searchField.getText().isEmpty())
        {
            int placeholderX = MARGIN + TEXT_INSET + (this.searchField.isFocused() ? this.fontRendererObj.getStringWidth("_") + 1 : 0);
            this.fontRendererObj.drawString(I18n.format("fml.menu.mods.search"), placeholderX, SEARCH_Y + (CONTROL_HEIGHT - 8) / 2, COLOR_PLACEHOLDER);
        }
        String count;
        if (this.visibleMods.isEmpty())
        {
            count = I18n.format("fml.menu.mods.noResults");
        }
        else if (this.visibleMods.size() == 1)
        {
            count = I18n.format("fml.menu.mods.showing.one");
        }
        else
        {
            count = I18n.format("fml.menu.mods.showing", this.visibleMods.size());
        }
        this.fontRendererObj.drawString(count, MARGIN, COUNT_Y, COLOR_SECONDARY);

        if (this.selectedMod != null)
        {
            drawDetailsHeader(this.selectedMod);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.sortHover.checkHover(mouseX, mouseY))
        {
            this.func_146283_a(Arrays.asList(I18n.format(this.sortDescending ? "fml.menu.mods.sort.descending" : "fml.menu.mods.sort.ascending")), mouseX, mouseY);
        }
    }

    /**
     * Draws a list cut to its own area. GuiScrollingList draws rows that are partly scrolled out in full and covers
     * the overflow with a strip that assumes the list starts at the left edge, which the details list does not.
     */
    private void drawClipped(GuiScrollingList list, int left, int top, int mouseX, int mouseY, float partialTicks)
    {
        int scale = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left * scale, this.mc.displayHeight - this.listBottom * scale, this.paneWidth * scale, (this.listBottom - top) * scale);
        list.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawDetailsHeader(ModContainer mod)
    {
        drawModIcon(mod, this.detailsLeft, SEARCH_Y);
        int textLeft = this.detailsLeft + ICON_SIZE + GAP + 2;
        int textWidth = this.detailsLeft + this.paneWidth - textLeft;
        drawNameAndBadges(mod, textLeft, SEARCH_Y, textWidth, true);
        drawTrimmed(mod.getDisplayVersion(), textLeft, SEARCH_Y + 1 + LINE_HEIGHT, textWidth, COLOR_SECONDARY, false);
        if (!mod.getMetadata().autogenerated && !mod.getMetadata().authorList.isEmpty())
        {
            drawTrimmed(I18n.format("fml.menu.mods.authors", mod.getMetadata().getAuthorList()), textLeft, SEARCH_Y + 1 + LINE_HEIGHT * 2, textWidth, COLOR_SECONDARY, false);
        }
    }

    /**
     * Draws a mod's name followed by its badges, keeping room for the badges so a long name cannot push them out.
     */
    void drawNameAndBadges(ModContainer mod, int x, int y, int width, boolean shadow)
    {
        List<String> badges = Lists.newArrayList();
        List<Integer> colors = Lists.newArrayList();
        ModState state = Loader.instance().getModState(mod);
        if (state == ModState.DISABLED)
        {
            badges.add(I18n.format("fml.menu.mods.badge.disabled"));
            colors.add(COLOR_ERROR);
        }
        else if (state == ModState.ERRORED)
        {
            badges.add(I18n.format("fml.menu.mods.badge.error"));
            colors.add(COLOR_ERROR);
        }
        if (newVersionOf(mod) != null)
        {
            badges.add(I18n.format("fml.menu.mods.badge.update"));
            colors.add(COLOR_UPDATE);
        }
        if (mod instanceof InjectedModContainer)
        {
            badges.add(I18n.format("fml.menu.mods.badge.core"));
            colors.add(COLOR_BADGE_CORE);
        }
        int badgesWidth = 0;
        for (String badge : badges)
        {
            badgesWidth += badgeWidth(badge) + BADGE_SPACING;
        }
        int nameWidth = Math.max(0, width - (badgesWidth > 0 ? badgesWidth + GAP : 0));
        int badgeX = x + drawTrimmed(mod.getName(), x, y + 1, nameWidth, COLOR_TEXT, shadow) + GAP;
        for (int i = 0; i < badges.size(); i++)
        {
            badgeX = drawBadge(badges.get(i), badgeX, y, colors.get(i));
        }
    }

    /**
     * Draws text cut to the width with an ellipsis, and returns the width drawn.
     */
    int drawTrimmed(String text, int x, int y, int width, int color, boolean shadow)
    {
        if (this.fontRendererObj.getStringWidth(text) > width)
        {
            text = this.fontRendererObj.trimStringToWidth(text, Math.max(0, width - this.fontRendererObj.getStringWidth("..."))) + "...";
        }
        if (shadow)
        {
            this.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
        else
        {
            this.fontRendererObj.drawString(text, x, y, color);
        }
        return this.fontRendererObj.getStringWidth(text);
    }

    private int badgeWidth(String text)
    {
        return this.fontRendererObj.getStringWidth(text) + BADGE_PADDING * 2;
    }

    private int drawBadge(String text, int x, int y, int color)
    {
        int width = badgeWidth(text);
        Gui.drawRect(x, y, x + width, y + BADGE_HEIGHT, 0xFF000000 | color);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + BADGE_HEIGHT - 1, 0xFF000000 | darker(color));
        this.fontRendererObj.drawString(text, x + BADGE_PADDING, y + 2, COLOR_TEXT);
        return x + width + BADGE_SPACING;
    }

    /** A quarter of the colour's brightness, for a badge's fill inside its outline. */
    private static int darker(int color)
    {
        return (color >> 2) & 0x3F3F3F;
    }

    /**
     * Draws a mod's logo fitted into an icon square, or the unknown pack icon when it has none.
     */
    void drawModIcon(ModContainer mod, int x, int y)
    {
        ModIcon icon = this.icons.get(mod);
        if (icon == null)
        {
            icon = loadIcon(mod);
            this.icons.put(mod, icon);
        }
        float scale = Math.min((float) ICON_SIZE / icon.width, (float) ICON_SIZE / icon.height);
        int w = Math.round(icon.width * scale);
        int h = Math.round(icon.height * scale);
        int left = x + (ICON_SIZE - w) / 2;
        int top = y + (ICON_SIZE - h) / 2;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        this.mc.getTextureManager().bindTexture(icon.texture);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(left,     top + h, this.zLevel, 0, 1);
        tess.addVertexWithUV(left + w, top + h, this.zLevel, 1, 1);
        tess.addVertexWithUV(left + w, top,     this.zLevel, 1, 0);
        tess.addVertexWithUV(left,     top,     this.zLevel, 0, 0);
        tess.draw();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private ModIcon loadIcon(ModContainer mod)
    {
        ModMetadata meta = mod.getMetadata();
        if (meta != null && !Strings.isNullOrEmpty(meta.logoFile))
        {
            try
            {
                BufferedImage logo = null;
                IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(mod.getModId());
                if (pack != null)
                {
                    logo = pack.getPackImage();
                }
                else
                {
                    InputStream logoResource = getClass().getResourceAsStream(meta.logoFile);
                    if (logoResource != null)
                    {
                        logo = ImageIO.read(logoResource);
                    }
                }
                if (logo != null)
                {
                    ResourceLocation texture = this.mc.getTextureManager().getDynamicTextureLocation("modlogo", new DynamicTexture(logo));
                    return new ModIcon(texture, logo.getWidth(), logo.getHeight());
                }
            }
            catch (Exception e)
            {
                FMLLog.log(Level.DEBUG, e, "Couldn't load the logo of %s", mod.getModId());
            }
        }
        return new ModIcon(UNKNOWN_ICON, ICON_SIZE, ICON_SIZE);
    }

    Minecraft getMinecraftInstance() {
        return mc;
    }

    FontRenderer getFontRenderer() {
        return fontRendererObj;
    }

    /**
     * @param index
     */
    public void selectModIndex(int index)
    {
        ModContainer mod = index >= 0 && index < this.visibleMods.size() ? this.visibleMods.get(index) : null;
        if (mod != this.selectedMod)
        {
            this.selectedMod = mod;
            updateDetails();
        }
    }

    public boolean modIndexSelected(int index)
    {
        return index >= 0 && index < this.visibleMods.size() && this.visibleMods.get(index) == this.selectedMod;
    }
}
