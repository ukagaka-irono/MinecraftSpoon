package net.minecraftforge.client;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Copies a chat message to the clipboard when its line is clicked with Ctrl held in the open chat. Minecraft 1.7.10
 * has no other way to copy what the chat shows, such as coordinates or an error.
 */
public final class ChatCopy
{
    // The id of the notice, so a new notice replaces the last one instead of filling the chat
    private static final int NOTICE_ID = 0x53706F6F;

    private static Field messagesField;
    private static Field drawnLinesField;
    private static Field scrollField;

    private ChatCopy() {}

    /**
     * Called by GuiChat for a left click with Ctrl held, with the mouse position in window pixels from the bottom left.
     *
     * @return whether a message was copied
     */
    public static boolean copyAt(int mouseX, int mouseY)
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            GuiNewChat chat = mc.ingameGUI.getChatGUI();
            ChatLine line = lineAt(mc, chat, mouseX, mouseY);
            if (line == null)
            {
                return false;
            }
            GuiScreen.setClipboardString(messageOf(chat, line));
            ChatComponentTranslation notice = new ChatComponentTranslation("spoon.chat.copied");
            notice.getChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(Boolean.TRUE);
            chat.printChatMessageWithOptionalDeletion(notice, NOTICE_ID);
            return true;
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon chat copy: couldn't copy the chat line");
            return false;
        }
    }

    /**
     * Finds the drawn line under the mouse, as GuiNewChat.func_146236_a does for the links in the chat.
     */
    private static ChatLine lineAt(Minecraft mc, GuiNewChat chat, int mouseX, int mouseY) throws Exception
    {
        if (!chat.getChatOpen())
        {
            return null;
        }
        drawnLinesField = field(drawnLinesField, "field_146253_i", "drawnChatLines");
        scrollField = field(scrollField, "field_146250_j", "scrollPos");
        List<?> drawnLines = (List<?>)drawnLinesField.get(chat);
        int scroll = scrollField.getInt(chat);
        int scaleFactor = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        float chatScale = chat.func_146244_h();
        int x = MathHelper.floor_float((mouseX / scaleFactor - 3) / chatScale);
        int y = MathHelper.floor_float((mouseY / scaleFactor - 27) / chatScale);
        if (x < 0 || y < 0)
        {
            return null;
        }
        int shown = Math.min(chat.func_146232_i(), drawnLines.size());
        if (x > MathHelper.floor_float(chat.func_146228_f() / chatScale) || y >= mc.fontRenderer.FONT_HEIGHT * shown + shown)
        {
            return null;
        }
        int index = y / mc.fontRenderer.FONT_HEIGHT + scroll;
        return index >= 0 && index < drawnLines.size() ? (ChatLine)drawnLines.get(index) : null;
    }

    /**
     * Returns the whole message a drawn line belongs to, as a long message is drawn on several lines, or else the line.
     */
    private static String messageOf(GuiNewChat chat, ChatLine line) throws Exception
    {
        String lineText = plain(line.func_151461_a().getUnformattedText()).trim();
        messagesField = field(messagesField, "field_146252_h", "chatLines");
        List<?> messages = (List<?>)messagesField.get(chat);
        for (Object object : messages)
        {
            ChatLine message = (ChatLine)object;
            if (message.getUpdatedCounter() == line.getUpdatedCounter() && message.getChatLineID() == line.getChatLineID())
            {
                String messageText = plain(message.func_151461_a().getUnformattedText());
                if (messageText.contains(lineText))
                {
                    return messageText;
                }
            }
        }
        return lineText;
    }

    private static String plain(String text)
    {
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        return plain == null ? "" : plain;
    }

    private static Field field(Field cached, String srgName, String mcpName)
    {
        return cached != null ? cached : ReflectionHelper.findField(GuiNewChat.class, srgName, mcpName);
    }
}
