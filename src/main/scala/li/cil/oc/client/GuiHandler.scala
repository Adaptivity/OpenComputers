package li.cil.oc.client

import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.{GuiHandler => CommonGuiHandler, item, tileentity, GuiType}
import li.cil.oc.{Settings, Items}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraft.util.StatCollector
import net.minecraft.client.Minecraft

object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef =
    world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case if id == GuiType.Case.id =>
        new gui.Case(player.inventory, computer)
      case disassembler: tileentity.Disassembler if id == GuiType.Disassembler.id =>
        new gui.Disassembler(player.inventory, disassembler)
      case drive: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
        new gui.DiskDrive(player.inventory, drive)
      case proxy: tileentity.RobotProxy if id == GuiType.Robot.id =>
        new gui.Robot(player.inventory, proxy.robot)
      case rack: tileentity.Rack if id == GuiType.Rack.id =>
        new gui.Rack(player.inventory, rack)
      case assembler: tileentity.RobotAssembler if id == GuiType.RobotAssembler.id =>
        new gui.RobotAssembler(player.inventory, assembler)
      case screen: tileentity.Screen if id == GuiType.Screen.id =>
        new gui.Screen(screen.origin.buffer, screen.tier > 0, () => screen.origin.hasKeyboard, () => screen.origin.buffer.isRenderingEnabled)
      case _ => Items.multi.subItem(player.getCurrentEquippedItem) match {
        case Some(server: item.Server) if id == GuiType.Server.id =>
          new gui.Server(player.inventory, new ServerInventory {
            override def tier = server.tier

            override def container = player.getCurrentEquippedItem

            override def isUseableByPlayer(player: EntityPlayer) = player == player
          })
        case Some(terminal: item.Terminal) if id == GuiType.Terminal.id =>
          val stack = player.getCurrentEquippedItem
          if (stack.hasTagCompound) {
            val address = stack.getTagCompound.getString(Settings.namespace + "server")
            val key = stack.getTagCompound.getString(Settings.namespace + "key")
            if (key != null && !key.isEmpty && address != null && !address.isEmpty) {
              tileentity.Rack.list.
                flatMap(_.terminals).
                find(term => term.rack.isPresent(term.number) match {
                case Some(value) => value == address
                case _ => false
              }) match {
                case Some(term) =>
                  def inRange = player.isEntityAlive && !term.rack.isInvalid && term.rack.getDistanceFrom(player.posX, player.posY, player.posZ) < term.rack.range * term.rack.range
                  if (inRange) {
                    if (term.keys.contains(key)) return new gui.Screen(term.buffer, true, () => true, () => {
                      // Check if someone else bound a term to our server.
                      if (stack.getTagCompound.getString(Settings.namespace + "key") != key) {
                        Minecraft.getMinecraft.displayGuiScreen(null)
                      }
                      // Check whether we're still in range.
                      if (!inRange) {
                        Minecraft.getMinecraft.displayGuiScreen(null)
                      }
                      true
                    })
                    else player.addChatMessage(StatCollector.translateToLocal(Settings.namespace + "gui.Terminal.InvalidKey"))
                  }
                  else player.addChatMessage(StatCollector.translateToLocal(Settings.namespace + "gui.Terminal.OutOfRange"))
                case _ => player.addChatMessage(StatCollector.translateToLocal(Settings.namespace + "gui.Terminal.OutOfRange"))
              }
            }
          }
          null
        case _ => null
      }
    }
}
