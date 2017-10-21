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
import com.devexperts.usages.api.MemberUsage
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

val USAGES_TOOL_WINDOW_TITLE = "Maven Usages"
private val FIND_USAGES_ICON = AllIcons.Actions.Find // todo use custom icon: IconLoader.getIcon("/find_usages.png")

/**
 * Shows usages in the project toolbar like standard "Find Usages" action
 */
class UsagesViewer(val project: Project, val member: Member, val newTab: Boolean) {
    private val usagesModel: UsagesTreeModel
    private val usagesTree: UsagesTree
    private val toolWindow: ToolWindow
    private val content: Content

    init {
        toolWindow = getOrInitToolWindow()
        val toolWindowPanel = SimpleToolWindowPanel(false)
        // Add usages tree
        val groupingStrategy = GroupingStrategy(arrayListOf(NodeType.ROOT,
                NodeType.SEARCHED_PACKAGE, NodeType.SEARCHED_CLASS, NodeType.SEARCHED_CLASS_MEMBER,
                NodeType.ARTIFACT, NodeType.USAGE_KIND,
                NodeType.TARGET_PACKAGE, NodeType.TARGET_CLASS, NodeType.TARGET_CLASS_MEMBER,
                NodeType.TARGET_LINE))
        usagesModel = UsagesTreeModel(groupingStrategy)
        usagesTree = UsagesTree(usagesModel)
        toolWindowPanel.setContent(JBScrollPane(usagesTree))
        // Add toolbar
        toolWindowPanel.setToolbar(createToolbar(toolWindowPanel))
        // Create content and add it to the window
        val contentTitle = "of ${member.simpleName()}" // "Maven Usages " is presented as toolWindow title
        content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, contentTitle, true)
        toolWindow.contentManager.addContent(content)
        // Show the content
        // todo process newTab parameter
        toolWindow.contentManager.setSelectedContent(content)
        if (!toolWindow.isActive) toolWindow.activate {}
        toolWindow.show {}
    }

    fun addUsages(newUsages: List<MemberUsage>) {
        val firstAdd = usagesModel.rootNode.usageCount == 0
        usagesModel.addUsages(newUsages)
        if (firstAdd) {
            println("FIRST!!!!")
            val firstUsagePath = usagesTree.expandFirstUsage()
            usagesTree.fireTreeWillExpand(firstUsagePath)
        }
    }

    private fun getOrInitToolWindow(): ToolWindow {
        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(USAGES_TOOL_WINDOW_TITLE)
        if (toolWindow == null) {
            toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(
                    USAGES_TOOL_WINDOW_TITLE, true, ToolWindowAnchor.BOTTOM)
            toolWindow.icon = FIND_USAGES_ICON
        }
        return toolWindow
    }

    private fun createToolbar(toolWindowPanel: JPanel): JComponent {
        val group = DefaultActionGroup()
        group.add(usagesToolbarAction(icon = AllIcons.General.Settings, title = "Settings",
                toolWindowPanel = toolWindowPanel, shortcut = "ctrl alt shift F9") {
            FindUsagesRequestConfigurationDialog(project, member).show()
        })
        group.add(usagesToolbarAction(icon = AllIcons.Actions.Rerun, title = "Rerun",
                toolWindowPanel = toolWindowPanel, shortcut = null) {
            // todo
        })
        group.add(usagesToolbarAction(icon = AllIcons.Actions.Cancel, title = "Close",
                toolWindowPanel = toolWindowPanel, shortcut = "ctrl shift F4") {
            toolWindow.contentManager.removeContent(content, false)
        })
        group.add(usagesToolbarAction(icon = AllIcons.Actions.Suspend, title = "Stop",
                toolWindowPanel = toolWindowPanel, shortcut = null) {
        })
        group.add(usagesToolbarAction(icon = AllIcons.Actions.Expandall, title = "Expand All",
                toolWindowPanel = toolWindowPanel, shortcut = "ctrl UP") {
            TreeUtil.expandAll(usagesTree)
        })
        group.add(usagesToolbarAction(icon = AllIcons.Actions.Collapseall, title = "Collapse All",
                toolWindowPanel = toolWindowPanel, shortcut = "ctrl DOWN") {
            TreeUtil.collapseAll(usagesTree, 3)
        })
        return ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false).component
    }

    private inline fun usagesToolbarAction(icon: Icon, title: String, toolWindowPanel: JPanel, shortcut: String?,
                                           crossinline action: (AnActionEvent) -> Unit): AnAction {
        return object : AnAction(title, null, icon) {
            init {
                if (shortcut != null)
                    registerCustomShortcutSet(CustomShortcutSet.fromString(shortcut), toolWindowPanel)
            }

            override fun actionPerformed(e: AnActionEvent) = action(e)
        }
    }
}


class GroupingStrategy(val groupingOrder: List<NodeType>) {
    fun getRank(nodeType: NodeType): Int = groupingOrder.indexOf(nodeType)
}