/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.release.jira;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Train;

/**
 * @author Oliver Gierke
 */
public interface JiraConnector {

	void reset();

	/**
	 * Returns all {@link Tickets} for the given {@link Train} and {@link Iteration}.
	 * 
	 * @param train must not be {@literal null}.
	 * @param iteration must not be {@literal null}.
	 * @return
	 */
	Tickets getTicketsFor(Train train, Iteration iteration, Credentials credentials);

	void verifyBeforeRelease(Train train, Iteration iteration);

	void closeIteration(Train train, Iteration iteration, Credentials credentials);
}