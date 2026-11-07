package net.minecraftforge.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.logging.CrashReportAnalyser;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

import com.google.common.base.Strings;
import com.google.common.io.Files;

import cpw.mods.fml.client.FMLScreenText;
import cpw.mods.fml.client.SplashProgress;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * A window that shows a crash before the game exits: what happened, the suspected mods and the crash report, with
 * buttons to open or copy the report.
 *
 * Without it, a crashed game closes its window without a word. The window uses Swing rather than a game screen, so it
 * still opens when the crash broke the game's rendering, or came before the game window opened.
 */
public final class CrashWindow
{
    private static final int MAX_LINE_LENGTH = 160;
    private static final int COPIED_MILLIS = 2000;
    private static final Color LINK_COLOR = new Color(0x0066CC);
    // A char code rather than a unicode escape, which the build garbles
    private static final char BULLET = (char)0x2022;

    private CrashWindow() {}

    /**
     * Called by Minecraft.displayCrashReport once the crash report is saved. Returns when the window is closed, and the
     * game exits then.
     */
    public static void show(CrashReport report)
    {
        if (!ForgeModContainer.crashWindow || GraphicsEnvironment.isHeadless())
        {
            return;
        }
        try
        {
            Details details = gather(report);
            boolean gameWindowClosed = closeGameWindow();
            FMLLog.info("Spoon crash window: shown, the game exits when it is closed");
            showAndWait(details, !gameWindowClosed);
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.ERROR, t, "Spoon crash window: couldn't show it");
        }
    }

    private static Details gather(CrashReport report)
    {
        Details details = new Details();
        details.description = report.getDescription();
        Throwable cause = report.getCrashCause();
        details.error = cause != null ? cause.toString() : null;
        // The stacktrace of the report's head, so the window finds the same suspected mods as the report
        StackTraceElement[] head = null;
        try
        {
            head = ReflectionHelper.getPrivateValue(CrashReport.class, report, "stacktrace", "field_85060_g");
        }
        catch (Throwable ignored) {}
        List<ModContainer> mods = CrashReportAnalyser.findSuspectedMods(cause, head);
        if (mods != null)
        {
            details.suspects = new ArrayList<Suspect>();
            for (ModContainer mod : mods)
            {
                String url = mod.getMetadata() != null ? mod.getMetadata().url : null;
                details.suspects.add(new Suspect(String.format("%s (%s) %s", mod.getName(), mod.getModId(), Strings.nullToEmpty(mod.getDisplayVersion())).trim(), url));
            }
        }
        details.reportFile = report.getFile() != null ? report.getFile().getAbsoluteFile() : null;
        details.reportText = readReport(report);
        details.icons = loadIcons();
        return details;
    }

    private static String readReport(CrashReport report)
    {
        if (report.getFile() != null)
        {
            try
            {
                // CrashReport.saveToFile writes the report in the default charset
                return Files.toString(report.getFile(), Charset.defaultCharset());
            }
            catch (Throwable ignored) {}
        }
        return report.getCompleteReport();
    }

    private static List<Image> loadIcons()
    {
        List<Image> icons = new ArrayList<Image>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDefaultResourcePack == null)
        {
            return icons;
        }
        for (String name : new String[] {"icons/icon_16x16.png", "icons/icon_32x32.png"})
        {
            InputStream in = null;
            try
            {
                in = mc.mcDefaultResourcePack.func_152780_c(new ResourceLocation(name));
                Image icon = in != null ? ImageIO.read(in) : null;
                if (icon != null)
                {
                    icons.add(icon);
                }
            }
            catch (Throwable ignored) {}
            finally
            {
                IOUtils.closeQuietly(in);
            }
        }
        return icons;
    }

    /**
     * Closes the game window, so the crash window is not left behind it or behind a full screen game. Only the game's
     * own thread can, as it holds the GL context of the window, unless the splash screen still has it.
     *
     * @return whether the game window is gone
     */
    @SuppressWarnings("deprecation")
    private static boolean closeGameWindow()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !mc.func_152345_ab())
        {
            return !Display.isCreated();
        }
        try
        {
            if (mc.getSoundHandler() != null)
            {
                mc.getSoundHandler().stopSounds();
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon crash window: couldn't stop the sounds");
        }
        try
        {
            if (SplashProgress.isRunning())
            {
                SplashProgress.finish();
            }
            if (Display.isCreated())
            {
                Display.destroy();
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon crash window: couldn't close the game window");
        }
        return !Display.isCreated();
    }

    private static void showAndWait(final Details details, final boolean onTop) throws Exception
    {
        final CountDownLatch closed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(new Runnable()
        {
            @Override
            public void run()
            {
                JFrame frame = createFrame(details);
                frame.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        closed.countDown();
                    }
                });
                // A game window that could not be closed may be full screen
                frame.setAlwaysOnTop(onTop);
                frame.setVisible(true);
                frame.toFront();
            }
        });
        closed.await();
    }

    /**
     * Builds the window. It uses no Minecraft classes, apart from the translations, which fall back to English.
     */
    static JFrame createFrame(final Details details)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Throwable ignored) {}

        String title = text("spoon.crash.title", "Minecraft has crashed");
        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        if (!details.icons.isEmpty())
        {
            frame.setIconImages(details.icons);
        }

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        frame.setContentPane(content);

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title, UIManager.getIcon("OptionPane.errorIcon"), SwingConstants.LEADING);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() + 4));
        addLine(summary, heading, 0);
        addLine(summary, new JLabel(text("spoon.crash.description", "Description: %s", shorten(details.description))), 10);
        if (details.error != null)
        {
            addLine(summary, new JLabel(text("spoon.crash.error", "Error: %s", shorten(details.error))), 2);
        }

        if (details.suspects != null)
        {
            JLabel suspectsLabel = new JLabel(text("spoon.crash.suspects", "Suspected mods:"));
            suspectsLabel.setFont(suspectsLabel.getFont().deriveFont(Font.BOLD));
            addLine(summary, suspectsLabel, 10);
            if (details.suspects.isEmpty())
            {
                addLine(summary, indent(new JLabel(text("spoon.crash.suspects.none", "None found in the stacktrace"))), 2);
            }
            for (Suspect suspect : details.suspects)
            {
                addLine(summary, indent(suspectRow(suspect)), 2);
            }
        }

        String reportLine = details.reportFile != null
                ? text("spoon.crash.report", "Crash report: %s", details.reportFile.getPath())
                : text("spoon.crash.report.unsaved", "The crash report could not be saved. Copy it from below.");
        addLine(summary, new JLabel(reportLine), 10);
        addLine(summary, new JLabel(text("spoon.crash.hint", "When you report the crash, attach the crash report file rather than a screenshot.")), 2);
        content.add(summary, BorderLayout.NORTH);

        JTextArea reportArea = new JTextArea(details.reportText, 16, 100);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        reportArea.setCaretPosition(0);
        content.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        content.add(buttonRow(frame, details), BorderLayout.SOUTH);

        frame.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(frame.getWidth(), screen.width * 9 / 10), Math.min(frame.getHeight(), screen.height * 9 / 10));
        frame.setMinimumSize(new Dimension(480, 360));
        frame.setLocationRelativeTo(null);
        return frame;
    }

    private static JComponent buttonRow(final JFrame frame, final Details details)
    {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

        JButton openReport = new JButton(text("spoon.crash.openReport", "Open Crash Report"));
        JButton openFolder = new JButton(text("spoon.crash.openFolder", "Open Folder"));
        final File report = details.reportFile;
        openReport.setEnabled(report != null && report.isFile());
        openFolder.setEnabled(report != null && report.getParentFile() != null && report.getParentFile().isDirectory());
        openReport.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                open(report);
            }
        });
        openFolder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                open(report.getParentFile());
            }
        });

        final String copyText = text("spoon.crash.copy", "Copy Crash Report");
        final String copiedText = text("spoon.crash.copied", "Copied");
        final JButton copy = new JButton(copiedText);
        Dimension copiedSize = copy.getPreferredSize();
        copy.setText(copyText);
        Dimension copySize = copy.getPreferredSize();
        // Keeps the button from changing its width when its text changes
        copy.setPreferredSize(new Dimension(Math.max(copySize.width, copiedSize.width), Math.max(copySize.height, copiedSize.height)));
        final Timer restore = new Timer(COPIED_MILLIS, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                copy.setText(copyText);
            }
        });
        restore.setRepeats(false);
        copy.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(details.reportText), null);
                    copy.setText(copiedText);
                    restore.restart();
                }
                catch (Throwable t)
                {
                    FMLLog.log(Level.WARN, t, "Spoon crash window: couldn't copy the crash report");
                }
            }
        });

        JButton close = new JButton(text("spoon.crash.close", "Close"));
        close.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                frame.dispose();
            }
        });
        frame.getRootPane().setDefaultButton(close);

        row.add(openReport);
        row.add(Box.createHorizontalStrut(6));
        row.add(openFolder);
        row.add(Box.createHorizontalStrut(6));
        row.add(copy);
        row.add(Box.createHorizontalGlue());
        row.add(close);
        return row;
    }

    private static JComponent suspectRow(final Suspect suspect)
    {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(new JLabel(BULLET + " " + shorten(suspect.name)));
        if (suspect.url != null && !suspect.url.trim().isEmpty())
        {
            final String link = suspect.url.contains("://") ? suspect.url.trim() : "http://" + suspect.url.trim();
            JLabel website = new JLabel(text("spoon.crash.website", "Website"));
            website.setForeground(LINK_COLOR);
            website.setFont(website.getFont().deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)));
            website.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            website.setToolTipText(link);
            website.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    browse(link);
                }
            });
            row.add(Box.createHorizontalStrut(10));
            row.add(website);
        }
        return row;
    }

    private static void addLine(JPanel panel, JComponent line, int gapAbove)
    {
        if (gapAbove > 0)
        {
            panel.add(Box.createVerticalStrut(gapAbove));
        }
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(line);
    }

    private static JComponent indent(JComponent component)
    {
        component.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        return component;
    }

    /**
     * Opens a file in the program the system opens it with, or a folder in the file manager.
     */
    private static void open(File file)
    {
        try
        {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
            {
                Desktop.getDesktop().open(file);
                return;
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon crash window: couldn't open %s, trying the system browser", file);
        }
        Sys.openURL(file.toURI().toString());
    }

    private static void browse(String link)
    {
        try
        {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(new URI(link));
                return;
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon crash window: couldn't open %s, trying the system browser", link);
        }
        Sys.openURL(link);
    }

    /**
     * Returns the first line of a text, cut to a length that fits the window.
     */
    private static String shorten(String text)
    {
        if (text == null)
        {
            return "";
        }
        int newline = text.indexOf('\n');
        String line = (newline < 0 ? text : text.substring(0, newline)).trim();
        return line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH - 3) + "..." : line;
    }

    private static String text(String key, String english, Object... args)
    {
        try
        {
            return FMLScreenText.format(key, english, args);
        }
        catch (Throwable t)
        {
            // Before the game window opens, I18n has no language loaded at all
            return String.format(english, args);
        }
    }

    static final class Details
    {
        String description;
        String error;
        /** Null while the mods are not known yet, such as when the game crashed before loading them */
        List<Suspect> suspects;
        File reportFile;
        String reportText = "";
        List<Image> icons = new ArrayList<Image>();
    }

    static final class Suspect
    {
        final String name;
        final String url;

        Suspect(String name, String url)
        {
            this.name = name;
            this.url = url;
        }
    }
}
