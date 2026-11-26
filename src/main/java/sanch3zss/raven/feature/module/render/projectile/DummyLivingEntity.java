package sanch3zss.raven.feature.module.render.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import sanch3zss.raven.feature.module.render.projectile.PlayerSnapshotManager;

public class DummyLivingEntity extends LivingEntity {
    public DummyLivingEntity(PlayerSnapshotManager playerSnapshotManager, EntityType entityType, Level world) {
        super(entityType, world);
    }

    public Iterable<ItemStack> getArmorItems() {
        return null;
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return null;
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    public HumanoidArm getMainArm() {
        return null;
    }
}
