/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ClipboardManagerBridge
import top.xjunz.tasker.engine.applet.action.Processor.Companion.unaryArgProcessor
import top.xjunz.tasker.engine.applet.action.unaryArgValueAction
import top.xjunz.tasker.ktx.firstGroupValue
import top.xjunz.tasker.task.applet.anno.AppletOrdinal

/**
 * @author xjunz 2023/01/06
 */
class TextActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0001)
    val extractText = appletOption(R.string.format_extract_text) {
        unaryArgProcessor<String, String> { arg, v ->
            if (v == null) null else arg?.firstGroupValue(v)
        }
    }.withRefArgument<String>(R.string.text)
        .withValueArgument<String>(R.string.regex)
        .withResult<String>(R.string.extracted_text)
        .withHelperText(R.string.help_extract_text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0002)
    val copyText = appletOption(R.string.format_copy_text) {
        unaryArgValueAction<String> { text ->
            ClipboardManagerBridge.copyToClipboard(text)
            true
        }
    }.withUnaryArgument<String>(R.string.text)
        .hasCompositeTitle()
}