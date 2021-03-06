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
package org.springframework.data.release.git;

import static org.springframework.data.release.model.Projects.*;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class GitOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired GitOperations gitOperations;

	@Test
	@Ignore
	public void testname() throws Exception {
		gitOperations.update(ReleaseTrains.CODD);
	}

	@Test
	@Ignore
	public void showTags() throws Exception {

		Tags tags = gitOperations.getTags(COMMONS);
		System.out.println(StringUtils.collectionToDelimitedString(tags.asList(), "\n"));
	}

	@Test
	public void foo() throws Exception {

		gitOperations.update(ReleaseTrains.EVANS);
	}
}
