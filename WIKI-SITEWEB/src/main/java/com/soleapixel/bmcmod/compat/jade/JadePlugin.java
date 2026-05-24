package com.soleapixel.bmcmod.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

@snownee.jade.api.WailaPlugin
public final class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        JadeProviders.registerClient(registration);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        JadeProviders.registerCommon(registration);
    }
}
