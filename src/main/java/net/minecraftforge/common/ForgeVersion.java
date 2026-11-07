/**
 * This software is provided under the terms of the Minecraft Forge Public
 * License v1.0.
 */

package net.minecraftforge.common;
import static net.minecraftforge.common.ForgeVersion.Status.*;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

public class ForgeVersion
{
    //This number is incremented every time we remove deprecated code/major API changes, never reset
    public static final int majorVersion    = 10;
    //This number is incremented every minecraft release, never reset
    public static final int minorVersion    = 13;
    //This number is incremented every time a interface changes or new major feature is added, and reset every Minecraft version
    public static final int revisionVersion = 4;
    //Spoon pins this to the last Forge build for 1.7.10 so mods requiring Forge@[10.13.4.1614,) still load.
    public static final int buildVersion    = 1614;
    //Version of Spoon itself, shown in the branding next to the Forge version it is compatible with.
    public static final String spoonVersion = "1.0.0-beta.3";
    //Lists the latest and recommended Spoon versions in the format of Forge's promotions_slim.json. It is a static file
    //on GitHub Pages rather than the GitHub API, which allows only 60 requests an hour per IP address.
    private static final String updateJsonURL = "https://ukagaka-irono.github.io/minecraft-spoon/promotions_slim.json";
    private static final int timeoutMillis = 5000;

    // Written by the version check thread and read by the title screen
    private static volatile Status status = PENDING;
    private static volatile String target = null;

    public static int getMajorVersion()
    {
        return majorVersion;
    }

    public static int getMinorVersion()
    {
        return minorVersion;
    }

    public static int getRevisionVersion()
    {
        return revisionVersion;
    }

    public static int getBuildVersion()
    {
        return buildVersion;
    }

    public static Status getStatus()
    {
        return status;
    }

    public static String getTarget()
    {
        return target;
    }

    public static String getVersion()
    {
        return String.format("%d.%d.%d.%d", majorVersion, minorVersion, revisionVersion, buildVersion);
    }

    public static enum Status
    {
        PENDING,
        FAILED,
        UP_TO_DATE,
        OUTDATED,
        AHEAD,
        BETA,
        BETA_OUTDATED
    }

    public static void startVersionCheck()
    {
        Thread thread = new Thread("Spoon Version Check")
        {
            @SuppressWarnings("unchecked")
            @Override
            public void run()
            {
                try
                {
                    URLConnection con = new URL(updateJsonURL).openConnection();
                    con.setConnectTimeout(timeoutMillis);
                    con.setReadTimeout(timeoutMillis);
                    InputStream in = con.getInputStream();
                    String data = new String(ByteStreams.toByteArray(in), "UTF-8");
                    in.close();

                    Map<String, Object> json = new Gson().fromJson(data, Map.class);
                    //String homepage = (String)json.get("homepage");
                    checkPromos((Map<String, String>)json.get("promos"));
                    FMLLog.info("Spoon version check: %s%s", status, target != null ? ", new version " + target : "");
                }
                catch (Exception e)
                {
                    FMLLog.warning("Spoon could not check for a new version: %s", e);
                    status = FAILED;
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Compares Spoon's own version, not the pinned Forge version, with the promos of the update JSON.
     */
    private static void checkPromos(Map<String, String> promos)
    {
        String rec = promos.get(MinecraftForge.MC_VERSION + "-recommended");
        String lat = promos.get(MinecraftForge.MC_VERSION + "-latest");
        ArtifactVersion current = new DefaultArtifactVersion(spoonVersion);

        if (rec != null)
        {
            ArtifactVersion recommended = new DefaultArtifactVersion(rec);
            int diff = recommended.compareTo(current);

            if (diff == 0)
                status = UP_TO_DATE;
            else if (diff < 0)
            {
                status = AHEAD;
                if (lat != null)
                {
                    if (current.compareTo(new DefaultArtifactVersion(lat)) < 0)
                    {
                        status = OUTDATED;
                        target = lat;
                    }
                }
            }
            else
            {
                status = OUTDATED;
                target = rec;
            }
        }
        else if (lat != null)
        {
            if (current.compareTo(new DefaultArtifactVersion(lat)) < 0)
            {
                status = BETA_OUTDATED;
                target = lat;
            }
            else
                status = BETA;
        }
        else
            status = BETA;
    }
}

