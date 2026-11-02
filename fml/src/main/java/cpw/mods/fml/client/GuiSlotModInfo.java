/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package cpw.mods.fml.client;

import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import com.google.common.collect.Lists;

/**
 * The scrolling details of the selected mod in the mod list, one wrapped line per entry. A line can carry a link
 * that opens when it is clicked.
 */
public class GuiSlotModInfo extends GuiScrollingList
{
    private GuiSpoonModList parent;
    private List<Line> lines = Lists.newArrayList();

    public static class Line
    {
        final String text;
        final int indent;
        final int color;
        final String link;

        public Line(String text, int indent, int color, String link)
        {
            this.text = text;
            this.indent = indent;
            this.color = color;
            this.link = link;
        }
    }

    public GuiSlotModInfo(GuiSpoonModList parent, int listWidth, int top, int bottom, int left)
    {
        super(parent.getMinecraftInstance(), listWidth, parent.height, top, bottom, left, GuiSpoonModList.LINE_HEIGHT);
        this.parent = parent;
        this.func_27258_a(false);
    }

    void setLines(List<Line> lines)
    {
        this.lines = lines;
    }

    boolean contains(int x, int y)
    {
        return x >= this.left && x < this.left + this.listWidth && y >= this.top && y < this.bottom;
    }

    @Override
    protected int getSize()
    {
        return this.lines.size();
    }

    /**
     * At least the visible height, so short details start at the top instead of being centred.
     */
    @Override
    protected int getContentHeight()
    {
        return Math.max(this.getSize() * GuiSpoonModList.LINE_HEIGHT, this.bottom - this.top - 4);
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick)
    {
        Line line = this.lines.get(index);
        if (line.link != null)
        {
            this.parent.openLink(line.link);
        }
    }

    @Override
    protected boolean isSelected(int index)
    {
        return false;
    }

    @Override
    protected void drawBackground()
    {
        // GuiSpoonModList draws the screen background before its lists
    }

    @Override
    protected void drawSlot(int index, int right, int top, int height, Tessellator tess)
    {
        Line line = this.lines.get(index);
        this.parent.getFontRenderer().drawString(line.text, this.left + GuiSpoonModList.TEXT_INSET + line.indent, top, line.color);
    }
}
