/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modified by LexManos 10/23/2013 to remove FileSystemMirroringFileTree
// Modified for Spoon: copied from the GTNH ForgeGradle 1.2.13 fork so that buildSrc shadows the plugin's copy.
// getFile() now extracts the entry, because Gradle 4.10 hashes the file of every input entry.
package net.minecraftforge.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.FileSystemSubset.Builder;
import org.gradle.api.internal.file.collections.MinimalFileTree;
import org.gradle.util.DeprecationLogger;
import org.gradle.util.GFileUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileTree implements MinimalFileTree {
    private final File zipFile;

    public ZipFileTree(File zipFile) {
        this.zipFile = zipFile;
    }

    public String getDisplayName() {
        return String.format("ZIP '%s'", zipFile);
    }

    public void visit(FileVisitor visitor) {
        if (!zipFile.exists()) {
            DeprecationLogger.nagUserOfDeprecatedBehaviour(
                    String.format("The specified zip file %s does not exist and will be silently ignored", getDisplayName())
            );
            return;
        }
        if (!zipFile.isFile()) {
            throw new InvalidUserDataException(String.format("Cannot expand %s as it is not a file.", getDisplayName()));
        }

        AtomicBoolean stopFlag = new AtomicBoolean();

        try {
            ZipFile zip = new ZipFile(zipFile);
            try {
                // The iteration order of zip.getEntries() is based on the hash of the zip entry. This isn't much use
                // to us. So, collect the entries in a map and iterate over them in alphabetical order.
                Map<String, ZipEntry> entriesByName = new TreeMap<String, ZipEntry>();
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    entriesByName.put(entry.getName(), entry);
                }
                Iterator<ZipEntry> sortedEntries = entriesByName.values().iterator();
                while (!stopFlag.get() && sortedEntries.hasNext()) {
                    ZipEntry entry = sortedEntries.next();
                    if (entry.isDirectory()) {
                        visitor.visitDir(new DetailsImpl(entry, zip, stopFlag));
                    } else {
                        visitor.visitFile(new DetailsImpl(entry, zip, stopFlag));
                    }
                }
            } finally {
                zip.close();
            }
        } catch (Exception e) {
            throw new GradleException(String.format("Could not expand %s.", getDisplayName()), e);
        }
    }

    @Override
    public void visitTreeOrBackingFile(FileVisitor fileVisitor) {
        visit(fileVisitor);
    }

    private class DetailsImpl implements FileVisitDetails {
        private final ZipEntry entry;
        private final ZipFile zip;
        private final AtomicBoolean stopFlag;
        private File file;

        public DetailsImpl(ZipEntry entry, ZipFile zip, AtomicBoolean stopFlag) {
            this.entry = entry;
            this.zip = zip;
            this.stopFlag = stopFlag;
        }

        public String getDisplayName() {
            return String.format("zip entry %s!%s", zipFile, entry.getName());
        }

        public void stopVisiting() {
            stopFlag.set(true);
        }

        /**
         * Extracts the entry next to the zip on first use, so Gradle can hash it for up-to-date checks.
         */
        public File getFile() {
            if (file == null) {
                file = new File(new File(zipFile.getParentFile(), zipFile.getName() + "-extracted"), entry.getName());
                copyTo(file);
            }
            return file;
        }

        public long getLastModified() {
            return entry.getTime();
        }

        public boolean isDirectory() {
            return entry.isDirectory();
        }

        public long getSize() {
            return entry.getSize();
        }

        public InputStream open() {
            try {
                return zip.getInputStream(entry);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public RelativePath getRelativePath() {
            return new RelativePath(!entry.isDirectory(), entry.getName().split("/"));
        }

        // Stuff below this line was    --------------------------------------------------
        // Stolen from Gradle's  org.gradle.api.internal.file.AbstractFileTreeElement 

        public String toString() {
            return getDisplayName();
        }

        public String getName() {
            return getRelativePath().getLastName();
        }

        public String getPath() {
            return getRelativePath().getPathString();
        }

        public void copyTo(OutputStream outstr) {
            try {
                InputStream inputStream = open();
                try {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outstr.write(buffer, 0, read);
                    }
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public boolean copyTo(File target) {
            validateTimeStamps();
            try {
                if (isDirectory()) {
                    GFileUtils.mkdirs(target);
                } else {
                    GFileUtils.mkdirs(target.getParentFile());
                    copyFile(target);
                }
                return true;
            } catch (Exception e) {
                throw new GradleException(String.format("Could not copy %s to '%s'.", new Object[]{getDisplayName(), target}), e);
            }
        }

        private void validateTimeStamps() {
            long lastModified = getLastModified();
            if (lastModified < 0L)
                throw new GradleException(String.format("Invalid Timestamp %s for '%s'.", new Object[]{Long.valueOf(lastModified), getDisplayName()}));
        }

        private void copyFile(File target) throws IOException {
            FileOutputStream outputStream = new FileOutputStream(target);
            try {
                copyTo(outputStream);
            } finally {
                outputStream.close();
            }
        }

        public int getMode() {
            return ((isDirectory()) ? 493 : 420);
        }
    }

    @Override
    public void registerWatchPoints(Builder arg0) {
        // uh.. nothing..
    }
}
