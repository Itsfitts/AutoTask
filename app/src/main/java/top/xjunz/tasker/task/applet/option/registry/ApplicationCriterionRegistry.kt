/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import androidx.annotation.StringRes
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.criterion.*
import top.xjunz.tasker.engine.applet.criterion.CollectionCriterion.Companion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.LambdaCriterion.Companion.newCriterion
import top.xjunz.tasker.ktx.bold
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.ktx.isSystemApp
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.ui.model.PackageInfoWrapper.Companion.wrapped

/**
 * @author xjunz 2022/09/22
 */
class ApplicationCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun invertibleApplicationOption(
        @StringRes name: Int, creator: () -> Applet
    ): AppletOption {
        return invertibleAppletOption(name, creator).withRefArgument<ComponentInfoWrapper>(
            R.string.app, substitution = R.string.empty
        ).hasCompositeTitle()
    }

    private fun applicationOption(@StringRes name: Int, creator: () -> Applet): AppletOption {
        return appletOption(name, creator).withRefArgument<ComponentInfoWrapper>(
            R.string.app, substitution = R.string.empty
        ).hasCompositeTitle()
    }

    @AppletOrdinal(0x00_00)
    val isCertainApp = invertibleApplicationOption(R.string.is_certain_app) {
        ArgumentCriterion<ComponentInfoWrapper, String, ComponentInfoWrapper>(Applet.VAL_TYPE_TEXT,
            { it.packageName }) { info, pkgName ->
            info.packageName == pkgName
        }
    }.withBinaryArgument<String, ComponentInfoWrapper>(
        R.string.is_which_app,
        VariantType.TEXT_PACKAGE_NAME,
        R.string.a_certain_app
    ).withValueDescriber<String> {
        PackageManagerBridge.loadLabelOfPackage(it)
    }.hasCompositeTitle()

    @AppletOrdinal(0x00_01)
    val appCollection = invertibleApplicationOption(R.string.in_app_collection) {
        collectionCriterion<ComponentInfoWrapper, String> {
            it.packageName
        }
    }.withValueArgument<String>(R.string.app_collection, VariantType.TEXT_APP_LIST)
        .withValueDescriber<Collection<String>> { value ->
            if (value.size == 1) {
                PackageManagerBridge.loadLabelOfPackage(value.first())
            } else {
                R.string.format_pkg_collection_desc.formatSpans(
                    value.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                        PackageManagerBridge.loadLabelOfPackage(name)
                    }.joinToString("、"), value.size.toString().bold()
                )
            }
        }

    @AppletOrdinal(0x00_02)
    val activityCollection = invertibleApplicationOption(R.string.in_activity_collection) {
        collectionCriterion<ComponentInfoWrapper, String> {
            it.activityName?.run {
                ComponentName(it.packageName, this).flattenToShortString()
            }
        }
    }.withValueArgument<String>(R.string.activity_collection, VariantType.TEXT_ACTIVITY_LIST)
        .withValueDescriber<Collection<String>> {
            if (it.size == 1) {
                val comp = ComponentName.unflattenFromString(it.single())!!
                (PackageManagerBridge.loadPackageInfo(comp.packageName)
                    ?.wrapped()?.label?.toString()
                    ?: comp.packageName) + "/" + it.single().substringAfterLast('/')
            } else {
                R.string.format_act_collection_desc.formatSpans(it.size.toString().foreColored())
            }
        }.withTitleModifier("Activity")

    @AppletOrdinal(0x00_03)
    val paneTitle = applicationOption(R.string.with_pane_title) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.paneTitle == v
        }
    }.withValueArgument<String>(R.string.pane_title)

    @AppletOrdinal(0x01_00)
    private val isSystem = invertibleApplicationOption(R.string.is_system_app) {
        PropertyCriterion<ComponentInfoWrapper> {
            it.packageInfo.applicationInfo.isSystemApp
        }
    }

    @AppletOrdinal(0x01_01)
    private val isLauncher = invertibleApplicationOption(R.string.is_launcher) {
        PropertyCriterion<ComponentInfoWrapper> {
            it.packageName == uiAutomatorBridge.launcherPackageName
        }
    }

    @AppletOrdinal(0x02_00)
    private val startsWith = invertibleApplicationOption(R.string.pkg_name_starts_with) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.packageName.startsWith(v)
        }
    }.withValueArgument<String>(R.string.prefix)

    @AppletOrdinal(0x02_01)
    private val endsWith = invertibleApplicationOption(R.string.pkg_name_ends_with) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.packageName.endsWith(v)
        }
    }.withValueArgument<String>(R.string.suffix)

    @AppletOrdinal(0x02_02)
    private val containsText = invertibleApplicationOption(R.string.pkg_name_contains_text) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.packageName.contains(v)
        }
    }.withValueArgument<String>(R.string.containment)

    @AppletOrdinal(0x02_03)
    private val matchesPattern = invertibleApplicationOption(R.string.pkg_name_matches_pattern) {
        newCriterion<ComponentInfoWrapper, String> { t, v ->
            t.packageName.matches(Regex(v))
        }
    }.withValueArgument<String>(R.string.regex)

    override val categoryNames: IntArray =
        intArrayOf(R.string.component_info, R.string.property, R.string.package_name)

}