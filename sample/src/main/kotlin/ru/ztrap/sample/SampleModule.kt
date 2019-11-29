package ru.ztrap.sample

import dagger.Module
import ru.ztrap.tools.autobinder.core.AutoBinderModule

/**
 * @author pa.gulko zTrap (29.11.2019)
 */
@Module(includes = [AutoBinder_SampleModule::class])
//@Module
@AutoBinderModule
object SampleModule {
}