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

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.release.io.CommandResult;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.jira.IssueTracker;
import org.springframework.data.release.jira.Ticket;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.CommandUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Component to execut Git related operations.
 * 
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class GitOperations {

	private final GitServer server = new GitServer();
	private final Workspace workspace;
	private final Logger logger;
	private final PluginRegistry<IssueTracker, Project> issueTracker;
	private final Environment environment;

	private CredentialsProvider credentials;

	@PostConstruct
	public void init() {
		this.credentials = new UsernamePasswordCredentialsProvider(environment.getProperty("git.username"),
				environment.getProperty("git.password"));
	}

	public GitProject getGitProject(Project project) {
		return new GitProject(project, server);
	}

	/**
	 * Resets the repositories for all modules of the given {@link Train}.
	 * 
	 * @param train must not be {@literal null}.
	 * @throws Exception
	 */
	public void reset(TrainIteration train) throws Exception {

		Assert.notNull(train, "Train must not be null!");

		for (ModuleIteration module : train) {

			Branch branch = Branch.from(module);

			try (Git git = new Git(getRepository(module.getProject()))) {

				logger.log(module, "git reset --hard origin/%s", branch);

				git.reset().//
						setMode(ResetType.HARD).//
						setRef("origin/".concat(branch.toString())).//
						call();
			}
		}
	}

	/**
	 * Checks out all projects of the given {@link Train} at the tags for the given {@link Iteration}.
	 * 
	 * @param train
	 * @param iteration
	 * @throws Exception
	 */
	public void checkout(TrainIteration iteration) throws Exception {

		update(iteration.getTrain());

		for (ModuleIteration module : iteration) {

			Project project = module.getProject();
			ArtifactVersion artifactVersion = ArtifactVersion.from(module);

			Tag tag = findTagFor(project, artifactVersion);

			if (tag == null) {
				throw new IllegalStateException(
						String.format("No tag found for version %s of project %s, aborting.", artifactVersion, project));
			}

			try (Git git = new Git(getRepository(module.getProject()))) {

				logger.log(module, "git checkout %s", tag);
				git.checkout().setStartPoint(tag.toString());
			}
		}

		logger.log(iteration, "Successfully checked out projects.");
	}

	public void prepare(TrainIteration iteration) throws Exception {

		for (ModuleIteration module : iteration) {

			Branch branch = Branch.from(module);

			update(module.getProject());

			logger.log(module.getProject(), "git checkout %s && git pull origin %s", branch, branch);
			checkout(module.getProject(), branch);

			try (Git git = new Git(getRepository(module.getProject()))) {
				git.pull().//
						setRebase(true).//
						call();
			}
		}
	}

	public void update(Train train) throws Exception {

		List<Future<CommandResult>> executions = new ArrayList<>();

		for (Module module : train) {
			update(module.getProject());
		}

		for (Future<CommandResult> execution : executions) {
			CommandUtils.getCommandResult(execution);
		}
	}

	public void push(TrainIteration iteration) throws Exception {

		for (ModuleIteration module : iteration) {

			Branch branch = Branch.from(module);

			logger.log(module, "git push origin %s", branch);

			try (Git git = new Git(getRepository(module.getProject()))) {

				Ref ref = git.getRepository().getRef(branch.toString());

				git.push().//
						setRemote("origin").//
						setRefSpecs(new RefSpec(ref.getName())).//
						setCredentialsProvider(credentials).//
						call();
			}
		}
	}

	public void pushTags(Train train) throws Exception {

		for (Module module : train) {

			logger.log(module.getProject(), "git push --tags");

			try (Git git = new Git(getRepository(module.getProject()))) {

				git.push().//
						setRemote("origin").//
						setPushTags().//
						setCredentialsProvider(this.credentials).//
						call();
			}
		}
	}

	public void update(Project project) throws Exception {

		GitProject gitProject = new GitProject(project, server);
		String repositoryName = gitProject.getRepositoryName();

		Repository repository = getRepository(project);

		try (Git git = new Git(repository)) {

			if (workspace.hasProjectDirectory(project)) {

				logger.log(project, "Found existing repository %s. Obtaining latest changes…", repositoryName);
				logger.log(project, "git checkout master && git reset --hard && git fetch --tags && git pull origin master");

				checkout(project, Branch.MASTER);

				git.reset().setMode(ResetType.HARD).call();

				git.fetch().setTagOpt(TagOpt.FETCH_TAGS);

				git.pull().call();

				// return os.executeCommand("git checkout master && git reset --hard && git fetch --tags && git pull origin
				// master",
				// project);

			} else {

				logger.log(project, "No repository found! Cloning from %s…", gitProject.getProjectUri());
				clone(project);

				// return os.executeCommand(command);
			}
		}
	}

	public Tags getTags(Project project) throws Exception {

		try (Git git = new Git(getRepository(project))) {
			return new Tags(git.tagList().call().stream().map(ref -> new Tag(ref.getName())).collect(Collectors.toList()));
		}
	}

	public void tagRelease(TrainIteration iteration) throws Exception {

		for (ModuleIteration module : iteration) {

			Branch branch = Branch.from(module);
			Project project = module.getProject();

			try (Git git = new Git(getRepository(module.getProject()))) {

				logger.log(module, "git checkout %s", branch);
				checkout(project, branch);

				logger.log(module, "git pull origin %s", branch);
				git.pull().call();

				ObjectId hash = getReleaseHash(module);
				Tag tag = getTags(project).createTag(module);

				try (RevWalk walk = new RevWalk(git.getRepository())) {

					RevCommit commit = walk.parseCommit(hash);

					logger.log(module, "git tag %s %s", tag, hash.getName());
					git.tag().setName(tag.toString()).setObjectId(commit).call();
				}
			}
		}
	}

	/**
	 * Commits all changes currently made to all modules of the given {@link TrainIteration}. The summary can contain a
	 * single {@code %s} placeholder which the version of the current module will get replace into.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 * @throws Exception
	 */
	public void commit(TrainIteration iteration, String summary, String details) throws Exception {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		for (ModuleIteration module : iteration) {
			commit(module, expandSummary(summary, module, iteration), details);
		}
	}

	private String expandSummary(String summary, ModuleIteration module, TrainIteration iteration) {

		if (!summary.contains("%s")) {
			return summary;
		}

		return String.format(summary,
				ArtifactVersion.from(module).toString().concat(String.format(" (%s)", iteration.toString())));
	}

	/**
	 * Commits the given files for the given {@link ModuleIteration} using the given summary and details for the commit
	 * message. If no files are given, all pending changes are commited.
	 * 
	 * @param module must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 * @param files can be empty.
	 * @throws Exception
	 */
	public void commit(ModuleIteration module, String summary, String details, File... files) throws Exception {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		Project project = module.getProject();
		IssueTracker tracker = issueTracker.getPluginFor(project);
		Ticket ticket = tracker.getReleaseTicketFor(module);

		Commit commit = new Commit(ticket, summary, details);
		String author = environment.getProperty("git.author");
		String email = environment.getProperty("git.email");

		logger.log(module, "git commit -m \"%s\" --author=\"%s <%s>\"", commit, author, email);

		try (Git git = new Git(getRepository(module.getProject()))) {

			git.commit().//
					setMessage(commit.toString()).//
					setAuthor(author, email).//
					setAll(true).//
					call();
		}
	}

	private ObjectId getReleaseHash(ModuleIteration module) throws Exception {

		Project project = module.getProject();

		Ticket releaseTicket = issueTracker.getPluginFor(project).getReleaseTicketFor(module);
		String trigger = String.format("%s - Release", releaseTicket.getId());

		try (Git git = new Git(getRepository(module.getProject()))) {

			for (RevCommit commit : git.log().setMaxCount(50).call()) {

				String summary = commit.getShortMessage();

				if (summary.startsWith(trigger)) {
					return commit.getId();
				}
			}
		}

		throw new IllegalStateException(
				String.format("Did not find a release commit for project %s (ticket id %s)", project, releaseTicket.getId()));
	}

	/**
	 * Returns the {@link Tag} that represents the {@link ArtifactVersion} of the given {@link Project}.
	 * 
	 * @param project
	 * @param version
	 * @return
	 * @throws IOException
	 */
	private Tag findTagFor(Project project, ArtifactVersion version) throws Exception {

		for (Tag tag : getTags(project)) {

			if (tag.toArtifactVersion().equals(version)) {
				return tag;
			}
		}

		return null;
	}

	public void checkout(Project project, Branch branch) throws Exception {

		try (Git git = new Git(getRepository(project))) {

			Ref ref = git.getRepository().getRef(branch.toString());
			CheckoutCommand checkout = git.checkout().setName(branch.toString());

			if (ref == null) {

				checkout.setCreateBranch(true).//
						setUpstreamMode(SetupUpstreamMode.TRACK).//
						setStartPoint("origin/".concat(branch.toString()));
			}

			checkout.call();
		}
	}

	private Repository getRepository(Project project) throws Exception {
		return FileRepositoryBuilder.create(workspace.getFile(".git", project));
	}

	public void clone(Project project) throws Exception {

		Git git = Git.cloneRepository().//
				setURI(getGitProject(project).getProjectUri()).//
				setDirectory(workspace.getProjectDirectory(project)).//
				call();

		git.checkout().setName(Branch.MASTER.toString()).//
				call();
	}
}
