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

import com.devexperts.usages.api.MemberType
import com.devexperts.usages.api.MemberType.FIELD
import com.devexperts.usages.api.MemberType.METHOD
import com.devexperts.usages.api.MemberUsage
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class UsagesTree(model: UsagesTreeModel) : SimpleTree(model) {
    init {
        // On expand action if node has one child, expand it too (recursively)
        addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                var node = (event.path.lastPathComponent ?: return) as DefaultMutableTreeNode
                while (node.childCount == 1)
                    node = node.getChildAt(0) as DefaultMutableTreeNode
                expandPath(TreePath(node))
            }

            override fun treeCollapsed(event: TreeExpansionEvent?) {}
        })
        // Set custom cell renderer
        setCellRenderer(UsagesTreeCellRenderer())
        // Install additional actions
        TreeUtil.installActions(this)
        TreeSpeedSearch(this) // todo does not highlight text, fix it
        // Do not show [SyntheticRootNode]
        isRootVisible = false
    }

    /**
     * Expands the beginning of the tree to show the first usage
     */
    fun expandFirstUsage(): TreePath {
        var node = model.root as DefaultMutableTreeNode
        while (!node.isLeaf) {
            node = node.firstChild as DefaultMutableTreeNode
            expandPath(TreePath(node.path))
        }
        return TreePath(node)
    }
}

/**
 * Model for work with [UsagesTree] with the specified rendering [strategy]
 */
class UsagesTreeModel(val strategy: GroupingStrategy) : DefaultTreeModel(SyntheticRootNode()) {
    val rootNode = getRoot() as Node

    fun addUsages(usages: List<MemberUsage>) {
        usages.forEach { usage ->
            var curNode = rootNode
            strategy.groupingOrder.forEach { nodeType ->
                val key = nodeType.key(usage)
                if (key != null) {
                    val rank = strategy.getRank(nodeType)
                    curNode = curNode.getOrCreate(rank, key, nodeType, usage, this)
                    curNode.usageCount++
                }
            }
        }
        reload()
    }

    /**
     * Notify that [child] node has been inserted into [parent] one by the specified [index]
     */
    fun fireNodeInserted(parent: Node, child: Node, index: Int) = fireTreeNodesInserted(
            this, parent.path, intArrayOf(index), arrayOf(child))
}

/**
 * Describes node types in [UsagesTree] and its rendering characteristics
 */
enum class NodeType(
        /**
         * Returns key for ordering nodes with the same rank.
         * Returns `false` if this [NodeType] does not support the specified usage.
         */
        val key: (MemberUsage) -> String?,
        /**
         * Renders the specified node
         */
        val renderFunc: (node: UsageGroupNode, renderer: UsagesTreeCellRenderer, usage: MemberUsage) -> Unit
) {
    ROOT(key = { "" }, renderFunc = { node, renderer, _ ->
        renderer.append("Found usages", com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        renderer.appendUsagesCount(node = node, bold = true)
    }),


    //    SEARCHED_FILE, todo add it
    SEARCHED_PACKAGE(key = { it.member.packageName() }, renderFunc = { node, renderer, usage ->
        renderer.icon = PlatformIcons.PACKAGE_ICON
        renderer.append(usage.member.packageName())
        renderer.appendUsagesCount(node = node, bold = false)
    }),
    SEARCHED_CLASS(key = { classKey(it) }, renderFunc = { node, renderer, usage ->
        renderer.icon = PlatformIcons.CLASS_ICON
        renderer.append(usage.member.simpleClassName())
        renderer.appendUsagesCount(node = node, bold = false)
    }),
    SEARCHED_CLASS_MEMBER(key = { classMemberKey(it) }, renderFunc = { node, renderer, usage ->
        renderer.icon = when (usage.member.type) {
            METHOD -> PlatformIcons.METHOD_ICON
            FIELD -> PlatformIcons.FIELD_ICON
            else -> throw IllegalStateException()
        }
        renderer.append(usage.member.simpleMemberName())
        renderer.appendUsagesCount(node = node, bold = false)
    }),


    ARTIFACT(key = { it.location.artifact.toString() }, renderFunc = { node, renderer, usage ->
        renderer.icon = PlatformIcons.LIBRARY_ICON
        renderer.append(usage.location.artifact.toString())
        renderer.appendUsagesCount(node = node, bold = false)
    }),
    USAGE_KIND(key = { it.usageKind.ordinal.toString() }, renderFunc = { node, renderer, usage ->
        renderer.append(usage.usageKind.description)
        renderer.appendUsagesCount(node = node, bold = false)
    }),


    TARGET_FILE(key = { it.location.file }, renderFunc = { node, renderer, usage ->
        renderer.icon = PlatformIcons.FILE_ICON
        renderer.append(usage.location.file.toString())
        renderer.appendUsagesCount(node = node, bold = false)
    }),
    TARGET_PACKAGE(key = { it.location.member.packageName() }, renderFunc = { node, renderer, usage ->
        renderer.icon = PlatformIcons.PACKAGE_ICON
        renderer.append(usage.location.member.packageName())
        renderer.appendUsagesCount(node = node, bold = false)
    }),
    TARGET_CLASS(key = { classKey(it) }, renderFunc = { node, renderer, usage ->
        renderer.icon = PlatformIcons.CLASS_ICON
        renderer.append(usage.member.simpleClassName())
        renderer.appendUsagesCount(node = node, bold = false)
    }),
    TARGET_CLASS_MEMBER(key = { classMemberKey(it) }, renderFunc = { node, renderer, usage ->
        renderer.icon = when (usage.member.type) {
            METHOD -> PlatformIcons.METHOD_ICON
            FIELD -> PlatformIcons.FIELD_ICON
            else -> throw IllegalStateException()
        }
        renderer.append(usage.member.simpleMemberName())
        renderer.appendUsagesCount(node = node, bold = false)
    }),


    TARGET_LINE(key = { "${it.location.file} ${it.location.lineNumber}" }, renderFunc = { node, renderer, usage ->
        renderer.append(usage.location.lineNumber.toString(), com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES)
        renderer.append(" ")
        renderer.append(usage.location.file.toString())
        renderer.appendUsagesCount(node = node, bold = false)
    }) // should be leaf always
}

/**
 * Creates key for classes.
 * Returns `null` if the searched element is package.
 */
private fun classKey(usage: MemberUsage): String? {
    return if (usage.member.type == MemberType.PACKAGE) null else usage.member.className()
}

/**
 * Creates key for class member (field or method).
 * Returns `null` if the searched element is not one of them.
 */
private fun classMemberKey(usage: MemberUsage) = when (usage.member.type) {
    FIELD, METHOD -> usage.member.simpleName()
    else -> null
}

abstract class Node : DefaultMutableTreeNode() {
    var usageCount: Int = 0
    private var textForSearch: String? = null

    fun getOrCreate(rank: Int, key: String, type: NodeType, usage: MemberUsage, treeModel: UsagesTreeModel): UsageGroupNode {
        // Find index with first child (child_rank, child_key) >= (rank, key)
        // ATTENTION! BINARY SEARCH HERE!
        var l = -1
        var r = childCount
        while (l + 1 < r) {
            val m = (l + r) / 2
            val mChild = getChildAt(m) as UsageGroupNode
            val less = mChild.rank < rank || (mChild.rank == rank && mChild.key < key)
            if (less) l = m else r = m
        }
        // children[r] >= searched
        // If found child has searched rank and key, return it
        print("${children().toList().map { it as UsageGroupNode }.map { "(${it.key} ${it.rank})" }}   $r   ")
        val insertIndex = r
        if (insertIndex < childCount) {
            val child = getChildAt(insertIndex) as UsageGroupNode
            if (child.rank == rank && child.key == key) {
                println("   FOUND")
                return child
            }
        }
        // Create new node and insert
        val newNode = UsageGroupNode(type, rank, key, usage)
        insert(newNode, insertIndex)
        treeModel.fireNodeInserted(this, newNode, insertIndex)
        println(children().toList().map { it as UsageGroupNode }.map { "(${it.key} ${it.rank})" })
        return newNode
    }

    fun render(renderer: UsagesTreeCellRenderer) {
        renderImpl(renderer)
        textForSearch = renderer.toString()
    }

    abstract fun renderImpl(renderer: UsagesTreeCellRenderer)

    override fun getUserObject() = this

    override fun toString(): String {
        return textForSearch ?: super.toString()
    }
}

class UsageGroupNode(val type: NodeType, val rank: Int, val key: String, val representativeUsage: MemberUsage) : Node() {
    override fun renderImpl(renderer: UsagesTreeCellRenderer) = type.renderFunc(this, renderer, representativeUsage)

    override fun toString(): String {
        return type.key(representativeUsage)!!
    }
}

class SyntheticRootNode : Node() {
    override fun renderImpl(renderer: UsagesTreeCellRenderer) {
        // This node is synthetic and should not be shown, do nothing
    }
}

/**
 * Renders [UsagesTree] nodes, delegates rendering to [UsageGroupNode]s
 */
class UsagesTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(tree: JTree, node: Any, selected: Boolean, expanded: Boolean,
                                       leaf: Boolean, row: Int, hasFocus: Boolean) {
        if (node is UsageGroupNode)
            node.render(this)
        else
            append(node.toString())
        SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, true, mySelected)
    }

    fun appendUsagesCount(node: UsageGroupNode, bold: Boolean) {
        val msg = " ${node.usageCount} ${(if (node.usageCount == 1) "usage" else "usages")}"
        val attributes = if (bold) SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
        else SimpleTextAttributes.GRAYED_ATTRIBUTES
        this.append(msg, attributes)
    }
}