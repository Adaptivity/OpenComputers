package li.cil.oc.client.gui

import li.cil.oc.client.{Textures, KeyBindings}
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.util.RenderState
import li.cil.oc.util.mods.NEI
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import scala.collection.mutable
import li.cil.oc.api
import net.minecraft.client.renderer.Tessellator

trait TextBuffer extends GuiScreen {
  protected def buffer: api.component.TextBuffer

  protected def hasKeyboard: Boolean

  private val pressedKeys = mutable.Map.empty[Int, Char]

  protected def bufferX: Int

  protected def bufferY: Int

  protected var currentWidth, currentHeight = -1

  private var shouldRecompileDisplayLists = true

  private var showKeyboardMissing = 0L

  protected var scale = 0.0

  def adjustToBufferChange() {
    shouldRecompileDisplayLists = true
  }

  override def doesGuiPauseGame = false

  override def initGui() = {
    super.initGui()
    MonospaceFontRenderer.init(Minecraft.getMinecraft.renderEngine)
    BufferRenderer.init(Minecraft.getMinecraft.renderEngine)
    Keyboard.enableRepeatEvents(true)
    adjustToBufferChange()
  }

  override def onGuiClosed() = {
    super.onGuiClosed()
    if (buffer != null) for ((code, char) <- pressedKeys) {
      buffer.keyUp(char, code, null)
    }
    Keyboard.enableRepeatEvents(false)
  }

  protected def drawBufferLayer() {
    if (shouldRecompileDisplayLists) {
      shouldRecompileDisplayLists = false
      if (buffer != null) {
        currentWidth = buffer.getWidth
        currentHeight = buffer.getHeight
      }
      else {
        currentWidth = 0
        currentHeight = 0
      }
      scale = changeSize(currentWidth, currentHeight)
    }
    GL11.glPushMatrix()
    RenderState.disableLighting()
    drawBuffer()
    GL11.glPopMatrix()

    if (System.currentTimeMillis() - showKeyboardMissing < 1000) {
      Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.guiKeyboardMissing)
      GL11.glDisable(GL11.GL_DEPTH_TEST)
      val t = Tessellator.instance
      t.startDrawingQuads()
      val x = bufferX + buffer.renderWidth - 16
      val y = bufferY + buffer.renderHeight - 16
      t.addVertexWithUV(x, y + 16, 0, 0, 1)
      t.addVertexWithUV(x + 16, y + 16, 0, 1, 1)
      t.addVertexWithUV(x + 16, y, 0, 1, 0)
      t.addVertexWithUV(x, y, 0, 0, 0)
      t.draw()
      GL11.glEnable(GL11.GL_DEPTH_TEST)
    }
  }

  protected def drawBuffer()

  override def handleKeyboardInput() {
    super.handleKeyboardInput()

    if (NEI.isInputFocused) return

    val code = Keyboard.getEventKey
    if (buffer != null && code != Keyboard.KEY_ESCAPE && code != Keyboard.KEY_F11) {
      if (hasKeyboard) {
        if (Keyboard.getEventKeyState) {
          val char = Keyboard.getEventCharacter
          if (!pressedKeys.contains(code) || !ignoreRepeat(char, code)) {
            buffer.keyDown(char, code, null)
            pressedKeys += code -> char
          }
        }
        else pressedKeys.remove(code) match {
          case Some(char) => buffer.keyUp(char, code, null)
          case _ => // Wasn't pressed while viewing the screen.
        }

        if (Keyboard.isKeyDown(KeyBindings.clipboardPaste.keyCode) && Keyboard.getEventKeyState) {
          buffer.clipboard(GuiScreen.getClipboardString, null)
        }
      }
      else {
        showKeyboardMissing = System.currentTimeMillis()
      }
    }
  }

  override protected def mouseClicked(x: Int, y: Int, button: Int) {
    super.mouseClicked(x, y, button)
    if (buffer != null && button == 2) {
      if (hasKeyboard) {
        buffer.clipboard(GuiScreen.getClipboardString, null)
      }
      else {
        showKeyboardMissing = System.currentTimeMillis()
      }
    }
  }

  protected def changeSize(w: Double, h: Double): Double

  private def ignoreRepeat(char: Char, code: Int) = {
    code == Keyboard.KEY_LCONTROL ||
      code == Keyboard.KEY_RCONTROL ||
      code == Keyboard.KEY_LMENU ||
      code == Keyboard.KEY_RMENU ||
      code == Keyboard.KEY_LSHIFT ||
      code == Keyboard.KEY_RSHIFT ||
      code == Keyboard.KEY_LMETA ||
      code == Keyboard.KEY_RMETA
  }
}
