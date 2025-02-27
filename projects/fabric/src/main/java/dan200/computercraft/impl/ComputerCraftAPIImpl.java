// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.shared.details.FluidDetails;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.loader.api.FabricLoader;

import javax.annotation.Nullable;

@AutoService(ComputerCraftAPIService.class)
public final class ComputerCraftAPIImpl extends AbstractComputerCraftAPI implements ComputerCraftAPIFabricService {
    private final DetailRegistry<StorageView<FluidVariant>> fluidDetails = new DetailRegistryImpl<>(FluidDetails::fillBasic);

    private @Nullable String version;

    @Override
    public String getInstalledVersion() {
        if (version != null) return version;
        return version = FabricLoader.getInstance().getModContainer(ComputerCraftAPI.MOD_ID)
            .map(x -> x.getMetadata().getVersion().toString())
            .orElse("unknown");
    }

    @Override
    public DetailRegistry<StorageView<FluidVariant>> getFluidDetailRegistry() {
        return fluidDetails;
    }
}
