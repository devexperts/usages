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

import com.devexperts.usages.api.Member
import com.devexperts.usages.api.MemberType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

data class FindUsagesRequestConfiguration(
        // Package members
        val findClassUsages: Boolean = true,
        // Class members
        val findMethodUsages: Boolean = true,
        val findFieldUsages: Boolean = true,
        // Class hierarchy
        val findDerivedClassesUsages: Boolean = false,
//        val findBaseClassesUsages: Boolean = false,
        // Method hierarchy
        val findMethodOverridesUsages: Boolean = false,
//        val findBaseMethodsUsages: Boolean = false,
        // Search area restrictions
        val artifactMask: String = "*:*:*",
        val numberOfLastVersions: Int = 3,
        // Interface
        val openInNewTab: Boolean = false
)

@State(name = "findUsagesRequestConfiguration")
@Storage(StoragePathMacros.ROOT_CONFIG)
class FindUsagesRequestConfigurationComponent : PersistentStateComponent<FindUsagesRequestConfiguration> {
    private var settings: FindUsagesRequestConfiguration = FindUsagesRequestConfiguration()

    override fun getState(): FindUsagesRequestConfiguration = settings

    override fun loadState(settings: FindUsagesRequestConfiguration) {
        this.settings = settings
    }
}

class FindUsagesRequestConfigurationDialog(private val project: Project, private val member: Member) : DialogWrapper(project) {
    private val configComponent: FindUsagesRequestConfigurationComponent
    private lateinit var dialogPanel: FindUsagesRequestConfigurationPanelHolder

    init {
        configComponent = requireNotNull(
                project.getComponent(FindUsagesRequestConfigurationComponent::class.java))
        title = "Find Maven usages"
        init()
        setOKButtonText("Find")
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = FindUsagesRequestConfigurationPanelHolder()
        dialogPanel.jMemberDesc.text = member.toString()
        disableUselessCheckBoxes()
        loadConfig()
        return dialogPanel.contentPanel
    }

    private fun loadConfig() {
        val cfg = configComponent.state
        dialogPanel.jFindClasses.isSelected = cfg.findClassUsages
//        dialogPanel.jFindBaseClassesUsages.isSelected = cfg.findBaseClassesUsages
        dialogPanel.jFindDerivedClassesUsages.isSelected = cfg.findDerivedClassesUsages
        dialogPanel.jFindFields.isSelected = cfg.findFieldUsages
        dialogPanel.jFindMethods.isSelected = cfg.findMethodUsages
//        dialogPanel.jFindBaseMethodsUsages.isSelected = cfg.findBaseMethodsUsages
        dialogPanel.jFindMethodOverridesUsages.isSelected = cfg.findMethodOverridesUsages
        dialogPanel.jArtifactMask.text = cfg.artifactMask
        dialogPanel.jNumberOfLastVersions.text = cfg.numberOfLastVersions.toString()
        dialogPanel.jNewTab.isSelected = cfg.openInNewTab
    }

    private fun storeConfig() {
        // TODO check artifactMask and numberOfLastVersions
        configComponent.loadState(FindUsagesRequestConfiguration(
                findClassUsages = dialogPanel.jFindClasses.isSelected,
//                findBaseClassesUsages = dialogPanel.jFindBaseClassesUsages.isSelected,
                findDerivedClassesUsages = dialogPanel.jFindDerivedClassesUsages.isSelected,
                findFieldUsages = dialogPanel.jFindFields.isSelected,
                findMethodUsages = dialogPanel.jFindMethods.isSelected,
//                findBaseMethodsUsages = dialogPanel.jFindBaseMethodsUsages.isSelected,
                findMethodOverridesUsages = dialogPanel.jFindMethodOverridesUsages.isSelected,
                artifactMask = dialogPanel.jArtifactMask.text,
                numberOfLastVersions = dialogPanel.jNumberOfLastVersions.text.toInt(),
                openInNewTab = dialogPanel.jNewTab.isSelected
        ))
    }

    private fun disableUselessCheckBoxes() {
        if (member.type == MemberType.PACKAGE)
            return
        dialogPanel.jFindClasses.isEnabled = false
        if (member.type == MemberType.CLASS)
            return
        dialogPanel.jFindMethods.isEnabled = false
        dialogPanel.jFindFields.isEnabled = false
//        dialogPanel.jFindBaseClassesUsages.isEnabled = false
        dialogPanel.jFindDerivedClassesUsages.isEnabled = false
        if (member.type == MemberType.FIELD) {
//            dialogPanel.jFindBaseMethodsUsages.isEnabled = false
            dialogPanel.jFindMethodOverridesUsages.isEnabled = false
        }
    }

    override fun doOKAction() {
        // todo ok action
        storeConfig()
        findUsagesAndShow(project, member)
        super.doOKAction()
    }
}