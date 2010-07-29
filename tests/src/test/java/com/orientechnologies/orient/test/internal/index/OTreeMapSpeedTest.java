/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.internal.index;

import java.util.NavigableMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.orientechnologies.common.collection.OTreeMapMemory;
import com.orientechnologies.common.test.SpeedTestMonoThread;

public class OTreeMapSpeedTest extends SpeedTestMonoThread {

	private NavigableMap<Integer, Integer>	tree	= new OTreeMapMemory<Integer, Integer>();

	@Override
	@Test(enabled = false)
	public void cycle() {
		final int NUMS = 1000000;

		tree.put(1, 1);
		tree.put(55, 1);

		System.out.println("Inserting " + NUMS + " values in OBTreeMap...");
		for (int i = 0; i < NUMS; ++i) {
			tree.put(getKey(i), i);
			// printTree();
		}
		data.printSnapshot();

		Assert.assertTrue(tree.size() == NUMS);

		System.out.println("Navigate the tree in ascending order...");
		int counter = 0;
		for (Comparable<Integer> k : tree.navigableKeySet()) {
			++counter;
		}
		data.printSnapshot();

		Assert.assertTrue(counter == NUMS);

		System.out.println("Check each value in sequence...");
		for (int i = 0; i < NUMS; i++) {
			// System.out.println("Checking " + i + "...");
			if (tree.get(getKey(i)) != i)
				System.out.println("Find error at " + i + "!!!");
		}
		data.printSnapshot();

		// if (tree instanceof OTreeMap<?, ?>) {
		// System.out.println("Total nodes: " + ((OTreeMap<?, ?>) tree).getNodes());
		// }

		System.out.println("Delete all the elements one by one...");
		for (int i = 0; i < NUMS; i++)
			tree.remove(getKey(i));
		data.printSnapshot();

		Assert.assertTrue(tree.size() == 0);
	}

	private Integer getKey(int i) {
		return i;
		// return String.valueOf(i);
	}

	private void printTree() {
		System.out.printf("%3d: ", tree.size());

		Comparable prevK = "";
		for (Comparable k : tree.navigableKeySet()) {
			Assert.assertTrue(k.compareTo(prevK) > 0, k + " is not minor or equals than " + prevK);
			prevK = k;
			System.out.print(k + ",");
		}
		System.out.println();
	}
}
