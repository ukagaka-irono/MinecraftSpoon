package net.minecraftforge.client.ime;

/**
 * Implement this on a GuiScreen that takes text input without using a GuiTextField,
 * so the IME is attached to the game window while it is open.
 */
public interface IIMETextInput
{
    /**
     * @return true while the screen is accepting text and needs the IME
     */
    boolean wantsIME();
}
