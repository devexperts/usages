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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

data class PluginConfiguration(
        // List of server component addresses
        val servers: List<String>,
        // List of server component addresses
        val sourceRepos: List<SourceRepository>
)

data class SourceRepository(val id: String, val url: String)

private val URL_PREFIX = "http://"

@State(name = "findUsagesPluginConfiguration")
@Storage(StoragePathMacros.ROOT_CONFIG)
class PluginConfigurationComponent : PersistentStateComponent<PluginConfiguration> {
    private var settings: PluginConfiguration = PluginConfiguration(servers = emptyList(), sourceRepos = emptyList())

    override fun getState(): PluginConfiguration = settings

    override fun loadState(settings: PluginConfiguration) {
        this.settings = settings
    }
}

class OpenPluginConfigurationAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = checkNotNull(event.getData(PlatformDataKeys.PROJECT))
        PluginConfigurationDialog(project).show()
    }
}

private class PluginConfigurationDialog(project: Project) : DialogWrapper(project) {
    private val configComponent: PluginConfigurationComponent
    private lateinit var dialogPanel: PluginConfigurationDialogPanelHolder
    private lateinit var serverAddressPanels: MutableList<ServerAddressPanelHolder>
    private lateinit var sourceRepoPanels: MutableList<SourceRepositoryPanelHolder>

    init {
        configComponent = requireNotNull(
                project.getComponent(PluginConfigurationComponent::class.java))
        title = "Configure Usages plugin"
        init()
    }

    override fun doOKAction() {
        // Store plugin configuration
        // todo validate
        val urls = serverAddressPanels
                .map { it.urlField.text }
                .filter { !it.isEmpty() }
                .map { canonicalAddressView(it) }
        val sourceRepos = sourceRepoPanels
                .filter { !it.urlField.text.isEmpty() }
                .map { SourceRepository(id = it.idField.text, url = it.urlField.text) }
        configComponent.loadState(PluginConfiguration(servers = urls, sourceRepos = sourceRepos))
        super.doOKAction()
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = PluginConfigurationDialogPanelHolder()
        // Set server URLs from config
        val urls = configComponent.state.servers.toMutableList()
        if (urls.isEmpty()) // Should be at least one URL (may be empty)
            urls.add(URL_PREFIX)
        // Fill panel with server addresses
        serverAddressPanels = arrayListOf()
        urls.forEach { addUrlItem(it) }
        // Set add new server action
        dialogPanel.addServerButton.addActionListener { addUrlItem() }
        // Set source repositories from config
        val sourceRepos = configComponent.state.sourceRepos.toMutableList()
        // Fill panel with source repositories
        sourceRepoPanels = arrayListOf()
        sourceRepos.forEach { addSourceRepoItem(id = it.id, url = it.url) }
        // Set new source repository action
        dialogPanel.addSourceRepoButton.addActionListener { addSourceRepoItem() }
        // Return root panel
        return dialogPanel.contentPanel
    }

    private fun updateUI() {
        dialogPanel.contentPanel.revalidate()
        dialogPanel.contentPanel.repaint()
    }

    private fun addUrlItem(url: String = URL_PREFIX) {
        val serverAddressPanel = ServerAddressPanelHolder()
        serverAddressPanel.urlField.text = url
        serverAddressPanel.removeButton.addActionListener {
            serverAddressPanels.remove(serverAddressPanel)
            dialogPanel.urlList.remove(serverAddressPanel.contentPanel)
            updateUI()
        }
        serverAddressPanels.add(serverAddressPanel)
        dialogPanel.urlList.add(serverAddressPanel.contentPanel)
        updateUI()
    }

    private fun addSourceRepoItem(id: String = "", url: String = URL_PREFIX) {
        val sourceRepoPanel = SourceRepositoryPanelHolder()
        sourceRepoPanel.idField.text = id
        sourceRepoPanel.urlField.text = url
        sourceRepoPanel.removeButton.addActionListener {
            sourceRepoPanels.remove(sourceRepoPanel)
            dialogPanel.sourceRepos.remove(sourceRepoPanel.contentPanel)
            updateUI()
        }
        sourceRepoPanels.add(sourceRepoPanel)
        dialogPanel.sourceRepos.add(sourceRepoPanel.contentPanel)
        updateUI()
    }

    // Lead server address to canonical view
    private fun canonicalAddressView(address: String): String {
        var canonicalAddress = address
        if (!canonicalAddress.contains("://"))
            canonicalAddress = "http://" + address
        if (canonicalAddress.endsWith('/'))
            canonicalAddress = canonicalAddress.substring(0, canonicalAddress.length - 1)
        return canonicalAddress
    }
}