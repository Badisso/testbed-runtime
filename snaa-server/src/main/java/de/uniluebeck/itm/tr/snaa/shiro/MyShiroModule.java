package de.uniluebeck.itm.tr.snaa.shiro;

import org.apache.shiro.guice.ShiroModule;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Extension of the abstract {@link ShiroModule} to configure Apache Shiro and to bind dependencies
 * for {@link ShiroSNAA}
 */
public class MyShiroModule extends ShiroModule {

	@Override
	protected void configureShiro() {

		try {
			bindRealm().to(TRJPARealm.class).in(Singleton.class);
			install(new FactoryModuleBuilder().build(ShiroSNAAFactory.class));
			expose(ShiroSNAAFactory.class);
		} catch (Exception e) {
			addError(e);
		}
	}

}