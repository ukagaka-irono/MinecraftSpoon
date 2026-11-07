package net.minecraftforge.client.gui;

import java.io.File;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.util.WorldBackup;

import com.google.common.base.Strings;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.FileOpener;

/**
 * Backs up a world from the world selection screen into the backups folder, showing the progress and then where the
 * backup is.
 */
public class GuiBackupWorld extends GuiScreen
{
    /** The id of the Backup button on the world selection screen */
    public static final int BUTTON_ID = 1700;

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_OPEN_FOLDER = 1;
    private static final int ROW_BUTTON_WIDTH = 58;
    private static final int ROW_GAP = 4;
    // The second row of the world selection screen: Rename, Delete, Re-Create, Backup and Cancel
    private static final int[] ROW_ORDER = {6, 2, 7, BUTTON_ID, 0};
    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 6;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_SECONDARY = 0xA0A0A0;
    private static final int MAX_ERROR_LINES = 4;

    private final GuiScreen parent;
    private final String worldFolder;
    private final String worldName;
    private WorldBackup backup;
    private WorldBackup.State shownState;

    public GuiBackupWorld(GuiScreen parent, String worldFolder, String worldName)
    {
        this.parent = parent;
        this.worldFolder = worldFolder;
        this.worldName = Strings.isNullOrEmpty(worldName) ? worldFolder : worldName;
    }

    /**
     * Adds the Backup button to the second row of the world selection screen, which then holds five narrower buttons.
     * Like Rename, the button works on the selected world.
     */
    @SuppressWarnings("unchecked")
    public static void addButton(GuiScreen screen, @SuppressWarnings("rawtypes") List buttons, final GuiButton rename)
    {
        GuiButton backup = new GuiButton(BUTTON_ID, 0, screen.height - 28, ROW_BUTTON_WIDTH, 20, I18n.format("spoon.backup.button"))
        {
            @Override
            public void drawButton(Minecraft mc, int mouseX, int mouseY)
            {
                this.enabled = rename.enabled;
                super.drawButton(mc, mouseX, mouseY);
            }
        };
        backup.enabled = rename.enabled;
        buttons.add(backup);

        int x = screen.width / 2 - (ROW_ORDER.length * ROW_BUTTON_WIDTH + (ROW_ORDER.length - 1) * ROW_GAP) / 2;
        for (int id : ROW_ORDER)
        {
            for (Object object : buttons)
            {
                GuiButton button = (GuiButton)object;
                if (button.id == id && button.yPosition == screen.height - 28)
                {
                    button.xPosition = x;
                    button.width = ROW_BUTTON_WIDTH;
                }
            }
            x += ROW_BUTTON_WIDTH + ROW_GAP;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui()
    {
        if (this.backup == null)
        {
            // Closes the region files still open from the last world played, as deleting a world does
            this.mc.getSaveLoader().flushCache();
            this.backup = WorldBackup.start(new File(FMLClientHandler.instance().getSavesDirectory(), this.worldFolder), WorldBackup.getBackupsDir());
        }
        this.shownState = this.backup.getState();
        this.buttonList.clear();
        int y = this.height / 2 + 40;
        if (this.shownState == WorldBackup.State.RUNNING)
        {
            GuiButton cancel = new GuiButton(BUTTON_DONE, this.width / 2 - 100, y, I18n.format("gui.cancel"));
            cancel.enabled = !this.backup.isCancelRequested();
            this.buttonList.add(cancel);
        }
        else if (this.shownState == WorldBackup.State.DONE)
        {
            this.buttonList.add(new GuiButton(BUTTON_OPEN_FOLDER, this.width / 2 - 154, y, 150, 20, I18n.format("spoon.backup.openFolder")));
            this.buttonList.add(new GuiButton(BUTTON_DONE, this.width / 2 + 4, y, 150, 20, I18n.format("gui.done")));
        }
        else
        {
            this.buttonList.add(new GuiButton(BUTTON_DONE, this.width / 2 - 100, y, I18n.format("gui.done")));
        }
    }

    @Override
    public void updateScreen()
    {
        WorldBackup.State state = this.backup.getState();
        if (state == WorldBackup.State.CANCELLED)
        {
            this.mc.displayGuiScreen(this.parent);
        }
        else if (state != this.shownState)
        {
            initGui();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (!button.enabled)
        {
            return;
        }
        if (button.id == BUTTON_OPEN_FOLDER)
        {
            FileOpener.open(WorldBackup.getBackupsDir());
        }
        else if (button.id == BUTTON_DONE)
        {
            close();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
    {
        if (keyCode == 1)
        {
            close();
        }
    }

    /**
     * Cancels a running backup, which returns to the world list once it has stopped, or returns there at once.
     */
    private void close()
    {
        if (this.backup.getState() == WorldBackup.State.RUNNING)
        {
            this.backup.cancel();
            initGui();
        }
        else
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        int centerX = this.width / 2;
        int y = this.height / 2 - 40;
        if (this.shownState == WorldBackup.State.RUNNING)
        {
            drawCenteredString(this.fontRendererObj, I18n.format("spoon.backup.title", this.worldName), centerX, y, COLOR_TEXT);
            float progress = this.backup.getProgress();
            int left = centerX - BAR_WIDTH / 2;
            int top = y + 24;
            Gui.drawRect(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, 0xFFA0A0A0);
            Gui.drawRect(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, 0xFF000000);
            Gui.drawRect(left, top, left + Math.round(BAR_WIDTH * progress), top + BAR_HEIGHT, 0xFF80E080);
            drawCenteredString(this.fontRendererObj, Math.round(progress * 100) + "%", centerX, top + BAR_HEIGHT + 6, COLOR_SECONDARY);
        }
        else if (this.shownState == WorldBackup.State.DONE)
        {
            File file = this.backup.getBackupFile();
            drawCenteredString(this.fontRendererObj, I18n.format("spoon.backup.done", this.worldName), centerX, y, COLOR_TEXT);
            drawCenteredString(this.fontRendererObj, "backups/" + file.getName(), centerX, y + 24, COLOR_SECONDARY);
            drawCenteredString(this.fontRendererObj, formatSize(file.length()), centerX, y + 36, COLOR_SECONDARY);
        }
        else
        {
            drawCenteredString(this.fontRendererObj, I18n.format("spoon.backup.failed", this.worldName), centerX, y, COLOR_TEXT);
            Throwable error = this.backup.getError();
            String message = error == null ? "" : error.getMessage() != null ? error.getMessage() : error.toString();
            List<?> lines = this.fontRendererObj.listFormattedStringToWidth(message, this.width - 40);
            for (int i = 0; i < lines.size() && i < MAX_ERROR_LINES; i++)
            {
                drawCenteredString(this.fontRendererObj, (String)lines.get(i), centerX, y + 24 + i * 12, COLOR_SECONDARY);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    static String formatSize(long bytes)
    {
        if (bytes < 1024L * 1024L)
        {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L)
        {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
