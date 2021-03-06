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
package org.springframework.data.release.maven;

import org.springframework.data.release.model.Iteration;

/**
 * @author Oliver Gierke
 */
public class Repository {

	private static final String BASE = "https://repo.spring.io/libs-";

	private final String id;
	private final String url;

	public Repository(Iteration iteration) {
		this.id = iteration.isPublicVersion() ? "spring-libs-release" : "spring-libs-milestone";
		this.url = iteration.isPublicVersion() ? BASE.concat("release") : BASE.concat("milestone");
	}

	public String getId() {
		return id;
	}

	public String getSnapshotId() {
		return "spring-libs-snapshot";
	}

	public String getUrl() {
		return url;
	}

	public String getSnapshotUrl() {
		return BASE.concat("snapshot");
	}
}
