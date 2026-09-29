package com.example.customtamingframework.menu;

import com.example.customtamingframework.pet.ModuleInventoryState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ModuleInventoryMenu extends AbstractContainerMenu {
    private final int entityId;
    private final String moduleId;
    private final CompoundTag moduleConfig;
    private final LivingEntity entity;
    private final SimpleContainer moduleContainer;
    private final int rows;
    private final int cols;
    private final int moduleSlotCount;
    private final boolean editable;

    public ModuleInventoryMenu(int windowId, Inventory playerInv, FriendlyByteBuf buffer) {
        this(windowId, playerInv, null,
                buffer.readVarInt(),
                safeTag(buffer.readNbt()),
                readStacks(buffer),
                buffer.readBoolean());
    }

    public ModuleInventoryMenu(int windowId, Inventory playerInv, LivingEntity entity, CompoundTag moduleConfig,
                               List<ItemStack> initialStacks, boolean editable) {
        this(windowId, playerInv, entity, entity != null ? entity.getId() : -1, safeTag(moduleConfig), initialStacks, editable);
    }

    private ModuleInventoryMenu(int windowId, Inventory playerInv, LivingEntity entity, int entityId,
                                CompoundTag moduleConfig, List<ItemStack> initialStacks, boolean editable) {
        super(com.example.customtamingframework.registry.CtfMenus.MODULE_INVENTORY.get(), windowId);
        this.entity = entity;
        this.entityId = entityId;
        this.moduleConfig = moduleConfig.copy();
        this.moduleId = ModuleInventoryState.moduleId(this.moduleConfig);
        this.rows = ModuleInventoryState.rows(this.moduleConfig);
        this.cols = ModuleInventoryState.cols(this.moduleConfig);
        this.moduleSlotCount = rows * cols;
        this.editable = editable;
        this.moduleContainer = new SimpleContainer(moduleSlotCount);
        loadInitialStacks(initialStacks);
        addModuleSlots();
        addPlayerInventory(playerInv);
    }

    private void loadInitialStacks(List<ItemStack> initialStacks) {
        for (int i = 0; i < moduleSlotCount; i++) {
            ItemStack stack = (initialStacks != null && i < initialStacks.size() && initialStacks.get(i) != null)
                    ? initialStacks.get(i).copy() : ItemStack.EMPTY;
            moduleContainer.setItem(i, stack);
        }
    }

    private void addModuleSlots() {
        int gridX = 8;
        int gridY = 18;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                final int slotIndex = row * cols + col;
                final int slotX = gridX + col * 18;
                final int slotY = gridY + row * 18;
                addSlot(new ModuleSlot(moduleContainer, slotIndex, slotX, slotY));
            }
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        int inventoryY = 18 + rows * 18 + 14;
        int left = 8;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, left + col * 18, inventoryY + row * 18));
            }
        }
        int hotbarY = inventoryY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, left + col * 18, hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return entity == null || entity.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!editable) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < moduleSlotCount) {
                if (!moveItemStackTo(current, moduleSlotCount, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(current, 0, moduleSlotCount, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (entity != null && !entity.level().isClientSide) {
            ModuleInventoryState.save(entity, moduleId, getModuleStacks());
        }
    }

    public String moduleId() {
        return moduleId;
    }

    public CompoundTag moduleConfig() {
        return moduleConfig.copy();
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public int moduleSlotCount() {
        return moduleSlotCount;
    }

    public boolean isEditable() {
        return editable;
    }

    public List<ItemStack> getModuleStacks() {
        List<ItemStack> stacks = new ArrayList<>(moduleSlotCount);
        for (int i = 0; i < moduleSlotCount; i++) {
            ItemStack stack = moduleContainer.getItem(i);
            stacks.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return stacks;
    }

    private final class ModuleSlot extends Slot {
        private ModuleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return editable && ModuleInventoryState.allowsItem(moduleConfig, stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return editable;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return ModuleInventoryState.maxStackSize(moduleConfig, stack);
        }
    }

    private static CompoundTag safeTag(CompoundTag tag) {
        return tag != null ? tag : new CompoundTag();
    }

    private static List<ItemStack> readStacks(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boolean present = buffer.readBoolean();
            if (present) {
                stacks.add(ItemStack.of(buffer.readNbt()));
            } else {
                stacks.add(ItemStack.EMPTY);
            }
        }
        return stacks;
    }
}
