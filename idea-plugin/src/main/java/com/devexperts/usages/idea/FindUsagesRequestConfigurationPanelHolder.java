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
package com.devexperts.usages.idea;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;

public class FindUsagesRequestConfigurationPanelHolder {
    JLabel jMemberDesc;
    JCheckBox jFindClasses;
    JCheckBox jFindMethodOverridesUsages;
    JCheckBox jFindMethods;
    JCheckBox jFindFields;
    JCheckBox jFindDerivedClassesUsages;
    JTextField jArtifactMask;
    JTextField jNumberOfLastVersions;
    JCheckBox jNewTab;
    JPanel contentPanel;

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) {
            return null;
        }
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(),
            size >= 0 ? size : currentFont.getSize());
    }

    {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(8, 5, new Insets(5, 5, 5, 5), -1, -1));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, -1, label1.getFont());
        if (label1Font != null) {
            label1.setFont(label1Font);
        }
        label1.setText("Find Maven usages of");
        contentPanel.add(label1,
            new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, Font.BOLD, -1, label2.getFont());
        if (label2Font != null) {
            label2.setFont(label2Font);
        }
        label2.setText("Search scope");
        contentPanel.add(label2, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Artifact mask");
        contentPanel.add(label3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(86, 16), null, 0,
            false));
        final JSeparator separator1 = new JSeparator();
        contentPanel.add(separator1,
            new GridConstraints(4, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
                false));
        final JSeparator separator2 = new JSeparator();
        contentPanel.add(separator2,
            new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
                false));
        jArtifactMask = new JTextField();
        jArtifactMask.setText("");
        jArtifactMask.setToolTipText(
            "Artifact mask in the following format:\n<groupId>:<artifactId>:<packaging>:<classifier>:<version>\nUse \"*\" for any value");
        contentPanel.add(jArtifactMask,
            new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1),
                null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Last artifact versions to analyze");
        contentPanel.add(label4, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(86, 16), null, 0,
            false));
        jNumberOfLastVersions = new JTextField();
        jNumberOfLastVersions.setText("");
        jNumberOfLastVersions
            .setToolTipText("Number of last artifact versions to be analyzed.\nPass \"0\" to analyze all versions");
        contentPanel.add(jNumberOfLastVersions,
            new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1),
                null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(panel1,
            new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        jFindClasses = new JCheckBox();
        jFindClasses.setText("Find class usages");
        panel1.add(jFindClasses, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jFindMethods = new JCheckBox();
        jFindMethods.setText("Find method usages");
        panel1.add(jFindMethods, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jFindFields = new JCheckBox();
        jFindFields.setText("Find field usages");
        panel1.add(jFindFields, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(panel2,
            new GridConstraints(2, 3, 1, 2, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        jFindDerivedClassesUsages = new JCheckBox();
        jFindDerivedClassesUsages.setText("Find derived classes usages");
        panel2.add(jFindDerivedClassesUsages,
            new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jFindMethodOverridesUsages = new JCheckBox();
        jFindMethodOverridesUsages.setText("Find derived methods usages");
        panel2.add(jFindMethodOverridesUsages,
            new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jNewTab = new JCheckBox();
        jNewTab.setText("Open in new tab");
        contentPanel.add(jNewTab,
            new GridConstraints(7, 3, 1, 2, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        contentPanel.add(spacer1,
            new GridConstraints(6, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jMemberDesc = new JLabel();
        Font jMemberDescFont = this.$$$getFont$$$("Menlo", -1, -1, jMemberDesc.getFont());
        if (jMemberDescFont != null) {
            jMemberDesc.setFont(jMemberDescFont);
        }
        jMemberDesc.setText("com.devexperts.util.IndexedSet#<init>");
        contentPanel.add(jMemberDesc,
            new GridConstraints(0, 2, 1, 3, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() { return contentPanel; }
}