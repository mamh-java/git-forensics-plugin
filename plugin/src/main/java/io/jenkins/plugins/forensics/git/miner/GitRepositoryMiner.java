package io.jenkins.plugins.forensics.git.miner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.plugins.gitclient.GitClient;

import io.jenkins.plugins.forensics.git.util.RemoteResultWrapper;
import io.jenkins.plugins.forensics.miner.CommitDiffItem;
import io.jenkins.plugins.forensics.miner.CommitStatistics;
import io.jenkins.plugins.forensics.miner.RepositoryMiner;
import io.jenkins.plugins.forensics.miner.RepositoryStatistics;

/**
 * Mines a Git repository and creates statistics for all available files.
 *
 * @author Giulia Del Bravo
 * @author Ullrich Hafner
 * @see io.jenkins.plugins.forensics.miner.RepositoryStatistics
 * @see io.jenkins.plugins.forensics.miner.FileStatistics
 * @see CommitDiffItem
 */
@SuppressFBWarnings(value = "SE", justification = "GitClient implementation is Serializable")
public class GitRepositoryMiner extends RepositoryMiner {
    private static final long serialVersionUID = 1157958118716013983L;

    private final GitClient gitClient;

    GitRepositoryMiner(final GitClient gitClient) {
        super();

        this.gitClient = gitClient;
    }

    @Override
    public RepositoryStatistics mine(final RepositoryStatistics previous, final FilteredLog logger)
            throws InterruptedException {
        try {
            long nano = System.nanoTime();
            logger.logInfo("Analyzing the commit log of the Git repository '%s'",
                    gitClient.getWorkTree());
            RemoteResultWrapper<ArrayList<CommitDiffItem>> wrapped = gitClient.withRepository(
                    new RepositoryStatisticsCallback(previous.getLatestCommitId()));
            logger.merge(wrapped);

            List<CommitDiffItem> commits = wrapped.getResult();
            logger.logInfo("-> Created report in %d seconds", 1 + (System.nanoTime() - nano) / 1_000_000_000L);
            CommitStatistics.logCommits(commits, logger);

            String latestCommitId;
            if (commits.isEmpty()) {
                latestCommitId = previous.getLatestCommitId();
            }
            else {
                latestCommitId = commits.get(0).getId();
            }
            RepositoryStatistics current = new RepositoryStatistics(latestCommitId);
            current.addAll(previous);
            Collections.reverse(commits); // make sure that we start with old commits to preserve the history
            current.addAll(commits);
            return current;
        }
        catch (IOException exception) {
            logger.logException(exception,
                    "Exception occurred while mining the Git repository using GitClient");
            return new RepositoryStatistics();
        }
    }
}
