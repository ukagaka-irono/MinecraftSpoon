package net.minecraftforge.common.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.launchwrapper.Launch;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;

/**
 * Zips a world folder into the backups folder, as the Make Backup button of newer Minecraft versions does: the backup
 * is backups/&lt;date&gt;_&lt;world folder&gt;.zip, with the world folder inside. The Backup button of the world selection
 * screen runs it on a thread of its own, and FML's backup of a world with missing mods on the server's thread.
 */
public final class WorldBackup implements Runnable
{
    public static enum State
    {
        RUNNING, DONE, CANCELLED, FAILED
    }

    // Held by the game while the world is open, and of no use in a backup
    private static final String LOCK_FILE = "session.lock";
    private static final String PART_SUFFIX = ".part";
    private static final int BUFFER_SIZE = 64 * 1024;

    private final File worldDir;
    private final File backupsDir;
    private volatile State state = State.RUNNING;
    private volatile boolean cancelRequested;
    private volatile long totalBytes = -1;
    private volatile long doneBytes;
    private volatile File backupFile;
    private volatile Throwable error;

    private WorldBackup(File worldDir, File backupsDir)
    {
        this.worldDir = worldDir;
        this.backupsDir = backupsDir;
    }

    /**
     * Returns the backups folder of the game folder, where newer Minecraft versions keep their backups too.
     */
    public static File getBackupsDir()
    {
        return new File(Launch.minecraftHome, "backups");
    }

    /**
     * Backs up a world folder into the backups folder on this thread.
     *
     * @return the zip file
     */
    public static File backup(File worldDir) throws IOException
    {
        WorldBackup backup = new WorldBackup(worldDir, getBackupsDir());
        backup.run();
        if (backup.state != State.DONE)
        {
            throw backup.error instanceof IOException ? (IOException)backup.error : new IOException(backup.error);
        }
        return backup.backupFile;
    }

    /**
     * Starts backing up a world folder on a new thread.
     */
    public static WorldBackup start(File worldDir, File backupsDir)
    {
        WorldBackup backup = new WorldBackup(worldDir, backupsDir);
        Thread thread = new Thread(backup, "Spoon world backup");
        thread.setDaemon(true);
        thread.start();
        return backup;
    }

    public State getState()
    {
        return this.state;
    }

    /**
     * Returns how much of the world is written, from 0 to 1.
     */
    public float getProgress()
    {
        long total = this.totalBytes;
        if (total < 0)
        {
            return 0;
        }
        return total == 0 ? 1 : Math.min(1, (float)this.doneBytes / total);
    }

    /**
     * Returns the zip file once the backup is done.
     */
    public File getBackupFile()
    {
        return this.backupFile;
    }

    /**
     * Returns why the backup failed.
     */
    public Throwable getError()
    {
        return this.error;
    }

    /**
     * Stops the backup and deletes the unfinished zip file.
     */
    public void cancel()
    {
        this.cancelRequested = true;
    }

    public boolean isCancelRequested()
    {
        return this.cancelRequested;
    }

    @Override
    public void run()
    {
        long start = System.currentTimeMillis();
        File part = null;
        try
        {
            if (!this.worldDir.isDirectory())
            {
                throw new IOException("There is no world folder at " + this.worldDir);
            }
            List<File> files = new ArrayList<File>();
            List<String> names = new ArrayList<String>();
            listFiles(this.worldDir, this.worldDir.getName() + "/", files, names);
            long total = 0;
            for (File file : files)
            {
                total += file.length();
            }
            this.totalBytes = total;

            if (!this.backupsDir.isDirectory() && !this.backupsDir.mkdirs())
            {
                throw new IOException("Couldn't create the folder " + this.backupsDir);
            }
            File target = availableFile();
            // Written under another name first, so an unfinished backup never looks like a finished one
            part = new File(target.getPath() + PART_SUFFIX);
            write(files, names, part);
            if (this.cancelRequested)
            {
                part.delete();
                this.state = State.CANCELLED;
                FMLLog.info("Spoon world backup: cancelled the backup of %s", this.worldDir.getName());
                return;
            }
            if (!part.renameTo(target))
            {
                throw new IOException("Couldn't rename " + part + " to " + target.getName());
            }
            this.backupFile = target;
            this.state = State.DONE;
            FMLLog.info("Spoon world backup: backed up %s to %s (%d files, %d bytes) in %d ms", this.worldDir.getName(), target, files.size(), target.length(), System.currentTimeMillis() - start);
        }
        catch (Throwable t)
        {
            if (part != null)
            {
                part.delete();
            }
            this.error = t;
            this.state = State.FAILED;
            FMLLog.log(Level.ERROR, t, "Spoon world backup: couldn't back up %s", this.worldDir);
        }
    }

    private static void listFiles(File dir, String prefix, List<File> files, List<String> names) throws IOException
    {
        File[] children = dir.listFiles();
        if (children == null)
        {
            throw new IOException("Couldn't list the files in " + dir);
        }
        for (File child : children)
        {
            if (child.isDirectory())
            {
                listFiles(child, prefix + child.getName() + "/", files, names);
            }
            else if (!child.getName().equals(LOCK_FILE))
            {
                files.add(child);
                names.add(prefix + child.getName());
            }
        }
    }

    private File availableFile()
    {
        String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + "_" + this.worldDir.getName();
        File file = new File(this.backupsDir, name + ".zip");
        for (int i = 1; file.exists() || new File(file.getPath() + PART_SUFFIX).exists(); i++)
        {
            file = new File(this.backupsDir, name + " (" + i + ").zip");
        }
        return file;
    }

    private void write(List<File> files, List<String> names, File zipFile) throws IOException
    {
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFFER_SIZE));
        try
        {
            // Region files hold compressed chunks already, so harder compression costs time and saves little
            zip.setLevel(Deflater.BEST_SPEED);
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < files.size() && !this.cancelRequested; i++)
            {
                File file = files.get(i);
                ZipEntry entry = new ZipEntry(names.get(i));
                entry.setTime(file.lastModified());
                zip.putNextEntry(entry);
                InputStream in = new FileInputStream(file);
                try
                {
                    for (int read; !this.cancelRequested && (read = in.read(buffer)) > 0; )
                    {
                        zip.write(buffer, 0, read);
                        this.doneBytes += read;
                    }
                }
                finally
                {
                    in.close();
                }
                zip.closeEntry();
            }
        }
        finally
        {
            zip.close();
        }
    }
}
