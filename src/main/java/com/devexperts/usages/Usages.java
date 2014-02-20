/*
 * usages - Usages Analysis Tool
 * Copyright (C) 2002-2014  Devexperts LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.devexperts.usages;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.objectweb.asm.*;

class Usages {
	final Cache cache;
	private final Map<String, ClassUsages> usages = new TreeMap<String, ClassUsages>();
	private final Map<String, Set<String>> descendants = new HashMap<String, Set<String>>();

	Usages(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}

	private Set<String> getDescendantsRec(String className) {
		Set<String> ds = descendants.get(className);
		if (ds != null)
			return ds;
		descendants.put(className, Collections.<String>emptySet()); // avoid infinite recursion
		ClassUsages classUsage = usages.get(className);
		Set<String> result = new HashSet<String>();
		if (classUsage != null) {
			Set<String> immediateDescendants = classUsage.getDescendantClasses();
			result.addAll(immediateDescendants);
			for (String descendant : immediateDescendants)
				result.addAll(getDescendantsRec(descendant));
		}
		if (!result.isEmpty())
			descendants.put(className, result);
		return result;
	}

	public void analyze() {
		System.out.println("Analyzing overrides");
		// build descendants map recursively with memoization
		for (Map.Entry<String, ClassUsages> entry : usages.entrySet()) {
			String className = entry.getKey();
			ClassUsages cu = entry.getValue();
			Set<String> classMembers = cu.getInheritableMembers();
			Set<String> classDescendants = getDescendantsRec(className);
			for (String descendantClassName : classDescendants) {
				ClassUsages descendantCU = usages.get(descendantClassName);
				if (descendantCU != null)
					for (String descendantMember : descendantCU.getInheritableMembers())
					    if (classMembers.contains(descendantMember) && descendantMember.endsWith(")"))
							cu.addMemberUsage(descendantMember, cache.resolveMember(descendantClassName, descendantMember), UseKind.OVERRIDE);
			}
		}
		System.out.println("Analyzing inheritance");
		// build ancestors map by reversing descendants map
		Map<String, Set<ClassUsages>> ancestors = new HashMap<String, Set<ClassUsages>>();
		for (Map.Entry<String, Set<String>> entry : descendants.entrySet()) {
			ClassUsages ancestor = usages.get(entry.getKey());
			for (String descendant : entry.getValue()) {
				Set<ClassUsages> classAncestors = ancestors.get(descendant);
				if (classAncestors == null)
					ancestors.put(descendant, classAncestors = new HashSet<ClassUsages>());
				classAncestors.add(ancestor);
			}
		}
		for (Map.Entry<String, ClassUsages> entry : usages.entrySet()) {
			String className = entry.getKey();
			Set<ClassUsages> classAncestors = ancestors.get(className);
			if (classAncestors == null)
				continue;
			ClassUsages cu = entry.getValue();
			cu.inheritUseTo(classAncestors);
		}
		System.out.println("Cleaning up inner class usages");
		for (ClassUsages cu : usages.values()) {
			cu.cleanupInnerUsages();
		}
	}

	public void removeUsesFromClasses(Set<String> implClasses) {
		for (ClassUsages classUsages : usages.values())
			classUsages.removeUsesFromClasses(implClasses);
	}

	public boolean isClassUsed(String className) {
		ClassUsages cu = usages.get(className);
		return cu != null && !cu.isEmpty();
	}

	public boolean isMemberUsed(Member member) {
		ClassUsages cu = usages.get(member.getClassName());
		return cu != null && cu.isMemberUsed(member.getMemberName());
	}

	public void writeToZipFile(File zipFile) throws IOException {
		if (usages.isEmpty()) {
			System.out.println("Nothing found");
			return;
		}
		System.out.println("Writing " + zipFile);
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
		try {
			int size = usages.size();
			int done = 0;
			int lastPercent = 0;
			long lastTime = System.currentTimeMillis();

			for (Map.Entry<String, ClassUsages> entry : usages.entrySet()) {
				String name = entry.getKey();
				ClassUsages usages = entry.getValue();
				if (usages.isEmpty())
					continue;
				zos.putNextEntry(new ZipEntry(name.replace('.', '/') + Main.USAGES_SUFFIX));
				usages.writeToStream(zos);
				zos.closeEntry();
				done++;
				int percent = 100 * done / size;
				long time = System.currentTimeMillis();
				if (done == size || percent > lastPercent && time > lastTime + 1000) {
					lastPercent = percent;
					lastTime = time;
					System.out.println(percent + "% done");
				}
			}
			System.out.println("Writing zip file directory");
			zos.finish();
		} finally {
			zos.close();
		}
		System.out.println("Completed");
	}

	public ClassUsages getUsagesForClass(String className) {
		ClassUsages result = usages.get(className);
		if (result == null) {
			if (className.endsWith("]") || className.endsWith(";") || className.contains("/"))
				throw new AssertionError("Not a class name: " + className);
			usages.put(cache.resolveString(className), result = new ClassUsages(cache, className));
		}
		return result;
	}

	public void parseClass(String className, InputStream inStream) throws IOException {
		ClassReader cr = new ClassReader(inStream);
		if (!className.equals(cr.getClassName().replace('/', '.')))
			System.out.println("Unexpected class name: " + cr.getClassName());
		else
			cr.accept(
				new ClassUsagesAnalyzer(this, className),
				ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

}
