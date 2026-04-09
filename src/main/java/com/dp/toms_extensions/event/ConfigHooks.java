package com.dp.toms_extensions.event;

import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.util.FacadeCompatOverrides;
import com.dp.toms_extensions.util.FacadeRegistryMapper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigHooks {
    private ConfigHooks() {
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModConfigData.SPEC) {
            FacadeCompatOverrides.reload();
            FacadeRegistryMapper.invalidate();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModConfigData.SPEC) {
            FacadeCompatOverrides.reload();
            FacadeRegistryMapper.invalidate();
        }
    }
}
