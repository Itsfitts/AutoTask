/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.action.Break
import top.xjunz.tasker.engine.applet.action.Repeat
import top.xjunz.tasker.engine.applet.action.Suspension
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.ktx.clickable
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.util.Router.launchAction
import top.xjunz.tasker.util.formatMinSecMills

/**
 * @author xjunz 2022/12/04
 */
class ControlActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0000)
    val ifAction = appletOption(R.string._if) { If() }

    @AppletOrdinal(0x0001)
    val suspension = appletOption(R.string.delay) {
        Suspension()
    }.withDescriber<Int> { applet, t ->
        R.string.format_delay.formatSpans(formatMinSecMills(t!!).foreColored().clickable {
            app.launchAction(AppletOption.ACTION_EDIT_VALUE, applet.hashCode())
        })
    }.descAsTitle()

    @AppletOrdinal(0x0002)
    val repeatFlow = appletOption(R.string.loop) {
        Repeat()
    }.withDescriber<Int> { applet, t ->
        R.string.format_repeat.formatSpans(t.toString().foreColored().clickable {
            app.launchAction(AppletOption.ACTION_EDIT_VALUE, applet.hashCode())
        })
    }.descAsTitle().withHelperText(R.string.input_repeat_count)
        .withResult<Repeat>(R.string.loop)

    @AppletOrdinal(0x00_03)
    val breakAction = appletOption(R.string.break_loop) {
        Break()
    }.withRefArgument<Repeat>(R.string.loop)
        .hasCompositeTitle()
}