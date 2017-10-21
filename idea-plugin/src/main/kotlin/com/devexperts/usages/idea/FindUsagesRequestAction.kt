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
import com.intellij.CommonBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class FindUsagesRequestAction : FindUsagesRequestAbstractAction() {
    override fun find(project: Project, member: Member) {
        findUsagesAndShow(project, member)
    }
}

class FindUsagesRequestWithConfigurationAction : FindUsagesRequestAbstractAction() {
    override fun find(project: Project, member: Member) {
        FindUsagesRequestConfigurationDialog(project, member).show()
    }
}

private const val FIND_NO_USAGES_AT_CURSOR = "Cannot search for Maven usages.\n" +
        "Position to an element to find usages for, and try again."

abstract class FindUsagesRequestAbstractAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = checkNotNull(event.getData(PlatformDataKeys.PROJECT))
        val editor = event.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            Messages.showMessageDialog(project, FIND_NO_USAGES_AT_CURSOR, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
            return
        }
        val member = createMember(event)
        if (member == null) {
            HintManager.getInstance().showErrorHint(editor, FIND_NO_USAGES_AT_CURSOR)
            return
        }
        find(project, member)
    }

    abstract fun find(project: Project, member: Member)

    private fun createMember(event: AnActionEvent): Member? {
        val psiElement = event.getData(LangDataKeys.PSI_ELEMENT)
        return when (psiElement) {
            is PsiPackage -> createPackageMember(psiElement)
            is PsiClass -> createClassMember(psiElement)
            is PsiField -> createFieldMember(psiElement)
            is PsiMethod -> createMethodMember(psiElement)
            else -> null
        }
    }

    private fun createClassMember(psiClass: PsiClass): Member? {
        val qualifiedName = psiClass.qualifiedName ?: return null
        return Member(qualifiedName, emptyList(), MemberType.CLASS)
    }

    private fun createPackageMember(psiPackage: PsiPackage)
            = Member(psiPackage.qualifiedName, emptyList(), MemberType.PACKAGE)

    private fun createFieldMember(psiField: PsiField): Member? {
        val className = getClassName(psiField) ?: return null
        return Member(className + "#" + psiField.name, emptyList(), MemberType.FIELD)
    }

    private fun createMethodMember(psiMethod: PsiMethod): Member? {
        val className = getClassName(psiMethod) ?: return null
        val qualifiedMethodName = className + "#" + psiMethod.name
        val parameterTypes = psiMethod.parameterList.parameters
                .map { it.type.canonicalText }
        return Member(qualifiedMethodName, parameterTypes, MemberType.METHOD)
    }

    private fun getClassName(element: PsiElement)
            = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)?.qualifiedName
}