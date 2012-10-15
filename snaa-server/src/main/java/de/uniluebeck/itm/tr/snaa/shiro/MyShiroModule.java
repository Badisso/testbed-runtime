package de.uniluebeck.itm.tr.snaa.shiro;

import org.apache.shiro.config.Ini;
import org.apache.shiro.guice.ShiroModule;

import com.google.inject.Provides;

public class MyShiroModule extends ShiroModule {
    protected void configureShiro() {
        try {
//            bindRealm().toConstructor(IniRealm.class.getConstructor(Ini.class));
            bindRealm().toInstance(new TRJPARealm());
        } catch (Exception e) {
            addError(e);
        }
    }

    @Provides
    Ini loadShiroIni() {
        return Ini.fromResourcePath("../localConfigs/shiro.ini");
    }
}