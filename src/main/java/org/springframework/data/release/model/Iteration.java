/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.model;

import lombok.NonNull;
import lombok.Value;

import org.springframework.util.Assert;

/**
 * Value object to represent an individual release train iteration.
 * 
 * @author Oliver Gierke
 */
@Value
public class Iteration {

	public static final Iteration SR6 = new Iteration("SR6", null);
	public static final Iteration SR5 = new Iteration("SR6", SR6);
	public static final Iteration SR4 = new Iteration("SR4", SR5);
	public static final Iteration SR3 = new Iteration("SR3", SR4);
	public static final Iteration SR2 = new Iteration("SR2", SR3);
	public static final Iteration SR1 = new Iteration("SR1", SR2);
	public static final Iteration GA = new Iteration("GA", SR1);
	public static final Iteration RC1 = new Iteration("RC1", GA);
	public static final Iteration M1 = new Iteration("M1", RC1);

	/**
	 * The name of the iteration.
	 */
	private final @NonNull String name;
	private final Iteration next;

	Iteration(String name, Iteration next) {

		Assert.hasText(name, "Name must not be null or empty!");

		this.name = name;
		this.next = next;
	}

	public boolean isGAVersion() {
		return this.equals(GA);
	}

	public boolean isPublicVersion() {
		return isServiceIteration() || this.equals(GA);
	}

	public boolean isServiceIteration() {
		return name.startsWith("SR");
	}

	public boolean isNext(Iteration iteration) {
		return next.equals(iteration);
	}

	public boolean isInitialIteration() {
		return this.equals(M1);
	}

	public int getBugfixValue() {
		return name.startsWith("SR") ? Integer.parseInt(name.substring(2)) : 0;
	}
}
