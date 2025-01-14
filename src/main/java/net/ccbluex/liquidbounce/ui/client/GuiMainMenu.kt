/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.api.messageOfTheDay
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.resources.I18n

class GuiMainMenu : AbstractScreen() {

    override fun initGui() {
        val defaultHeight = height / 4 + 48

        +GuiButton(100, width / 2 - 100, defaultHeight + 24, 98, 20, translationMenu("altManager"))
        +GuiButton(103, width / 2 + 2, defaultHeight + 24, 98, 20, translationMenu("mods"))
        +GuiButton(101, width / 2 - 100, defaultHeight + 24 * 2, 98, 20, translationMenu("serverStatus"))
        +GuiButton(102, width / 2 + 2, defaultHeight + 24 * 2, 98, 20, translationMenu("configuration"))

        +GuiButton(1, width / 2 - 100, defaultHeight, 98, 20, I18n.format("menu.singleplayer"))
        +GuiButton(2, width / 2 + 2, defaultHeight, 98, 20, I18n.format("menu.multiplayer"))

        // Minecraft Realms
        //		this.buttonList.add(new GuiButton(14, this.width / 2 - 100, j + 24 * 2, I18n.format("menu.online", new Object[0])));

        +GuiButton(108, width / 2 - 100, defaultHeight + 24 * 3, translationMenu("contributors"))
        +GuiButton(0, width / 2 - 100, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.options"))
        +GuiButton(4, width / 2 + 2, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.quit"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        drawRoundedBorderRect(
            width / 2f - 115, height / 4f + 35, width / 2f + 115, height / 4f + 175,
            2f,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            3F
        )

        Fonts.fontBold180.drawCenteredString(CLIENT_NAME, width / 2F, height / 8F, 4673984, true)
        Fonts.font35.drawCenteredString(
            clientVersionText,
            width / 2F + 148,
            height / 8F + Fonts.font35.fontHeight,
            0xffffff,
            true
        )

        val messageOfTheDay = messageOfTheDay?.message
        if (messageOfTheDay?.isNotBlank() == true) {
            val lines = messageOfTheDay.lines()

            drawRoundedBorderRect(
                width / 2f - 115,
                height / 4f + 190,
                width / 2f + 115,
                height / 4f + 200 + (Fonts.font35.fontHeight * lines.size),
                2f,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                3F
            )

            // Draw rect below main rect and within draw MOTD text
            for ((index, line) in lines.withIndex()) {
                Fonts.font35.drawCenteredString(
                    line, width / 2F, height / 4f + 197.5f
                            + (Fonts.font35.fontHeight * index), 0xffffff, true
                )
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // When clicking the message of the day text
        val messageOfTheDay = messageOfTheDay?.message
        if (messageOfTheDay?.isNotBlank() == true) {
            val lines = messageOfTheDay.lines()
            val motdHeight = height / 4f + 198
            val motdWidth = width / 2f - 115
            val motdHeightEnd = motdHeight + 2 + (Fonts.font35.fontHeight * lines.size)

            if (mouseX >= motdWidth && mouseX <= width / 2f + 115 && mouseY >= motdHeight && mouseY <= motdHeightEnd) {
                // Open liquidbounce website
                MiscUtils.showURL("https://$CLIENT_WEBSITE")
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            4 -> mc.shutdown()
            100 -> mc.displayGuiScreen(GuiAltManager(this))
            101 -> mc.displayGuiScreen(GuiServerStatus(this))
            102 -> mc.displayGuiScreen(GuiClientConfiguration(this))
            103 -> mc.displayGuiScreen(GuiModsMenu(this))
            108 -> mc.displayGuiScreen(GuiContributors(this))
        }
    }
}