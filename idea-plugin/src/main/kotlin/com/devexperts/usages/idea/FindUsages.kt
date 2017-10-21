/**
 * Copyright (C) 2017 Devexperts LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package com.devexperts.usages.idea

import com.devexperts.usages.api.*
import com.intellij.notification.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppUIUtil


private val LOG = Logger.getInstance("com.devexperts.usages.idea.FindUsages")

fun findUsagesAndShow(project: Project, member: Member) {
    // Check that there is at least one Usages server in the plugin configuration
    val pluginConfiguration = checkNotNull(project.getComponent(PluginConfigurationComponent::class.java).state)
    val servers = pluginConfiguration.servers
    if (servers.isEmpty()) {
        val message = "Specify Maven Usages servers before using the plugin"
        showBalloonNotification(project, message, MessageType.ERROR)
        return
    }
    // Create a request processor
    val requestConfiguration = requireNotNull(
            project.getComponent(FindUsagesRequestConfigurationComponent::class.java)).state
    var usagesViewer: UsagesViewer? = null
    val requestProcessor = object : MemberUsageRequestProcessor(
            serverUrls = pluginConfiguration.servers,
            memberUsagesRequest = MemberUsageRequest(
                    member = member,
                    findClasses = requestConfiguration.findClassUsages,
                    findDerivedClassesUsages = requestConfiguration.findDerivedClassesUsages,
                    findFields = requestConfiguration.findFieldUsages,
                    findMethods = requestConfiguration.findMethodUsages,
                    findDerivedMethodsUsages = requestConfiguration.findMethodOverridesUsages,
                    searchScope = createArtifactMaskFromString(requestConfiguration.artifactMask + ":" + requestConfiguration.numberOfLastVersions)
            )
    ) {
        override fun onNewUsages(serverUrl: String, usages: List<MemberUsage>) {
            AppUIUtil.invokeOnEdt {
                if (usagesViewer == null)
                    usagesViewer = UsagesViewer(project, member, requestConfiguration.openInNewTab)
                usagesViewer!!.addUsages(usages)
            }
        }

        override fun onError(serverUrl: String, message: String, throwable: Throwable?) {
            val msg = "Error during find maven usages request from server $serverUrl: $message"
            showBalloonNotification(project, message, MessageType.ERROR)
            LOG.warn(msg, throwable)
        }

        override fun onComplete() {}
    }
    // Do request as background task, could be cancelled by user
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Find Maven usages...", true) {
        override fun run(progressIndicator: ProgressIndicator) {
            try {
                requestProcessor.doRequest()
            } catch (e: ProcessCanceledException) {
                // Cancelled by user
            }
        }

        override fun onSuccess() {
            if (usagesViewer == null) {
                val message = "No Maven usages found for ${member.simpleName()}"
                showBalloonNotification(project, message, MessageType.INFO)
            }
        }

        override fun onCancel() {
            requestProcessor.cancel()
        }
    })
}

fun showBalloonNotification(project: Project, message: String, messageType: MessageType) {
    val statusBar = WindowManager.getInstance().getStatusBar(project)
    JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, messageType, null)
            .createBalloon().showInCenterOf(statusBar.component)
}