package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.machine.Robot
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.api.driver.Container

class UpgradeInventoryController(val owner: Container with Robot) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("inventory_controller", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Get the number of slots in the inventory on the specified side of the robot.""")
  def getInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    InventoryUtils.inventoryAt(owner.world, owner.xPosition.toInt + facing.offsetX, owner.yPosition.toInt + facing.offsetY, owner.zPosition.toInt + facing.offsetZ) match {
      case Some(inventory) => result(inventory.getSizeInventory)
      case _ => result(Unit, "no inventory")
    }
  }

  @Callback(doc = """function(facing:number, slot:number[, count:number]):boolean -- Drops the selected item stack into the specified slot of an inventory.""")
  def dropIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(2)
    val selectedSlot = owner.selectedSlot
    val stack = owner.getStackInSlot(selectedSlot)
    if (stack != null && stack.stackSize > 0) {
      InventoryUtils.inventoryAt(owner.world, owner.xPosition.toInt + facing.offsetX, owner.yPosition.toInt + facing.offsetY, owner.zPosition.toInt + facing.offsetZ) match {
        case Some(inventory) =>
          val slot = args.checkSlot(inventory, 1)
          if (!InventoryUtils.insertIntoInventorySlot(stack, inventory, facing.getOpposite, slot, count)) {
            // Cannot drop into that inventory.
            return result(false, "inventory full/invalid slot")
          }
          else if (stack.stackSize == 0) {
            // Dropped whole stack.
            owner.setInventorySlotContents(selectedSlot, null)
          }
          else {
            // Dropped partial stack.
            owner.onInventoryChanged()
          }
        case _ => return result(false, "no inventory")
      }

      context.pause(Settings.get.dropDelay)

      result(true)
    }
    else result(false)
  }

  @Callback(doc = """function(facing:number, slot:number[, count:number]):boolean -- Sucks items from the specified slot of an inventory.""")
  def suckFromSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(2)

    InventoryUtils.inventoryAt(owner.world, owner.xPosition.toInt + facing.offsetX, owner.yPosition.toInt + facing.offsetY, owner.zPosition.toInt + facing.offsetZ) match {
      case Some(inventory) =>
        val slot = args.checkSlot(inventory, 1)
        if (InventoryUtils.extractFromInventorySlot(owner.player.inventory.addItemStackToInventory, inventory, facing.getOpposite, slot, count)) {
          context.pause(Settings.get.suckDelay)
          result(true)
        }
        else result(false)
      case _ => result(false, "no inventory")
    }
  }

  @Callback(doc = """function():boolean -- Swaps the equipped tool with the content of the currently selected inventory slot.""")
  def equip(context: Context, args: Arguments): Array[AnyRef] = {
    if (owner.inventorySize > 0) {
      val selectedSlot = owner.selectedSlot
      val equipped = owner.getStackInSlot(0)
      val selected = owner.getStackInSlot(selectedSlot)
      owner.setInventorySlotContents(0, selected)
      owner.setInventorySlotContents(selectedSlot, equipped)
      result(true)
    }
    else result(false)
  }

  private def checkSideForAction(args: Arguments, n: Int) = owner.toGlobal(args.checkSideForAction(n))
}
