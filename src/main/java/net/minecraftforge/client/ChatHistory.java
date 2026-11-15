package net.minecraftforge.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraftforge.common.ForgeModContainer;

import org.apache.logging.log4j.Level;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Keeps the messages and commands sent in the chat, which the up arrow brings back, from one start of the game to the
 * next. Minecraft 1.7.10 forgets them when it closes.
 *
 * The file is chat_history.txt in the game folder. Commands of login plugins such as AuthMe carry a password, so they
 * are never written to it.
 */
public final class ChatHistory
{
    static final int MAX_SAVED = 100;
    static final List<String> SECRET_COMMANDS = Arrays.asList("/login", "/l", "/log", "/register", "/reg", "/changepassword",
            "/changepass", "/cp", "/unregister", "/unreg", "/email", "/2fa", "/totp", "/premium");

    private static File file;
    private static List<String> lastSaved = Collections.emptyList();
    // What the chat held when last looked at, to skip the work on the many ticks with nothing new
    private static int seenSize;
    private static Object seenLast;

    private ChatHistory() {}

    /**
     * Called by FMLClientHandler when loading is done, once the chat exists.
     */
    public static void loadingDone()
    {
        Minecraft mc = Minecraft.getMinecraft();
        file = new File(mc.mcDataDir, "chat_history.txt");
        if (ForgeModContainer.saveChatHistory && file.isFile())
        {
            try
            {
                List<String> loaded = Files.readLines(file, Charsets.UTF_8);
                lastSaved = toSave(loaded);
                addAll(mc.ingameGUI.getChatGUI(), lastSaved);
                FMLLog.info("Spoon chat history: loaded %d sent messages", lastSaved.size());
            }
            catch (Throwable t)
            {
                FMLLog.log(Level.WARN, t, "Spoon chat history: couldn't read %s", file);
            }
        }
        FMLCommonHandler.instance().bus().register(new ChatHistory.TickHandler());
    }

    @SuppressWarnings("unchecked")
    private static void addAll(GuiNewChat chat, List<String> messages)
    {
        chat.getSentMessages().addAll(messages);
    }

    public static class TickHandler
    {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END || !ForgeModContainer.saveChatHistory)
            {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI == null)
            {
                return;
            }
            List<?> sent = mc.ingameGUI.getChatGUI().getSentMessages();
            Object last = sent.isEmpty() ? null : sent.get(sent.size() - 1);
            if (sent.size() == seenSize && last == seenLast)
            {
                return;
            }
            seenSize = sent.size();
            seenLast = last;
            List<String> messages = new ArrayList<String>();
            for (Object message : sent)
            {
                messages.add(String.valueOf(message));
            }
            save(toSave(messages));
        }
    }

    private static void save(List<String> messages)
    {
        if (messages.equals(lastSaved))
        {
            return;
        }
        try
        {
            Files.write(messages.isEmpty() ? "" : Joiner.on('\n').join(messages) + "\n", file, Charsets.UTF_8);
            lastSaved = messages;
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon chat history: couldn't write %s", file);
        }
    }

    /**
     * Returns the last {@link #MAX_SAVED} messages, without the empty ones and the ones that carry a password.
     */
    static List<String> toSave(List<String> messages)
    {
        List<String> kept = new ArrayList<String>();
        for (String message : messages)
        {
            if (!message.trim().isEmpty() && !isSecret(message))
            {
                kept.add(message);
            }
        }
        return new ArrayList<String>(kept.subList(Math.max(0, kept.size() - MAX_SAVED), kept.size()));
    }

    static boolean isSecret(String message)
    {
        String command = message.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return SECRET_COMMANDS.contains(command);
    }
}
