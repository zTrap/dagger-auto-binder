package ru.ztrap.sample

import ru.ztrap.tools.autobinder.core.AutoBindTo

/**
 * @author pa.gulko zTrap (06.11.2019)
 */
interface IPresenter {
}

@AutoBindTo(IPresenter::class)
class Presenter : IPresenter