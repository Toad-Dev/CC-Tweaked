// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ComputerContainerData implements ContainerData {
    private final ComputerFamily family;
    private final TerminalState terminal;
    private final ItemStack displayStack;

    public ComputerContainerData(ServerComputer computer, ItemStack displayStack) {
        family = computer.getFamily();
        terminal = computer.getTerminalState();
        this.displayStack = displayStack;
    }

    public ComputerContainerData(FriendlyByteBuf buf) {
        family = buf.readEnum(ComputerFamily.class);
        terminal = new TerminalState(buf);
        displayStack = buf.readItem();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(family);
        terminal.write(buf);
        buf.writeItem(displayStack);
    }

    public ComputerFamily family() {
        return family;
    }

    public TerminalState terminal() {
        return terminal;
    }

    /**
     * Get a stack associated with this menu. This may be displayed on the client.
     *
     * @return The stack associated with this menu.
     */
    public ItemStack displayStack() {
        return displayStack;
    }
}
