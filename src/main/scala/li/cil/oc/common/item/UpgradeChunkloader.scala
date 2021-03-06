package li.cil.oc.common.item

import li.cil.oc.{Settings, server}
import li.cil.oc.util.{Tooltip, Rarity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.client.renderer.texture.IconRegister

class UpgradeChunkloader(val parent: Delegator) extends Delegate {
  val unlocalizedName = "UpgradeChunkloader"

  override def rarity = Rarity.byTier(server.driver.item.UpgradeChunkloader.tier(createItemStack()))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":upgrade_chunkloader")
  }
}
