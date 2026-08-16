package com.scto.mobile.ide.plugin.typst.commands.cli

import android.content.res.Resources
import com.scto.mobile.ide.plugin.typst.R
import com.scto.mobile.ide.plugin.typst.utils.TypstInstallationAction
import com.scto.mobile.ide.plugin.typst.utils.TypstInstallationManager
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.GlobalCommand
import com.scto.mobile.ide.core.common.icons.Icon

class TypstUpdateCommand(
    private val icon: Icon,
    private val resources: Resources,
    private val typstInstallationManager: TypstInstallationManager,
) : GlobalCommand() {

    override val id = "typst.cli.update"

    override val prefix = "Typst"

    override fun getLabel() = resources.getString(R.string.update_cli)

    override fun getIcon() = icon

    override fun isEnabled(): Boolean {
        return typstInstallationManager.cachedPendingAction == TypstInstallationAction.UPDATE
    }

    override fun action(actionContext: ActionContext) {
        typstInstallationManager.performAction(TypstInstallationAction.UPDATE)
    }
}
