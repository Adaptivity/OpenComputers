package li.cil.oc.common.tileentity.traits

import li.cil.oc.common.inventory
import li.cil.oc.api.network.Node
import net.minecraft.nbt.NBTTagCompound
import cpw.mods.fml.relauncher.{SideOnly, Side}

trait ComponentInventory extends Environment with Inventory with inventory.ComponentInventory {
  override def componentContainer = this

  override def isComponentSlot(slot: Int) = isServer

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      connectComponents()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      disconnectComponents()
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    connectComponents()
    super.writeToNBTForClient(nbt)
    save(nbt)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    load(nbt)
  }
}
