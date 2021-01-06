package io.jenkins.plugins.forensics;

import java.time.Duration;
import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.git.GitScm;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;

import static io.jenkins.plugins.forensics.DetailsTable.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Acceptance tests for the Git Forensics Plugin.
 *
 * @author Ullrich Hafner
 */
@WithPlugins({"forensics-api", "git-forensics", "git"})
public class ForensicsPluginUiTest extends AbstractJUnitTest {
    private static final String REPOSITORY_URL = "https://github.com/jenkinsci/git-forensics-plugin.git";

    /**
     * Tests the build overview page by running a Build with the forensics plugin analyzing a commit hash of the
     * git-forensics-plugin repository. Checks the contents of the result summary.
     */
    @Test
    public void shouldAggregateToolsIntoSingleResult() {
        FreeStyleJob job = createFreeStyleJob();
        job.addPublisher(ForensicsPublisher.class);

        GitScm gitScm = createGitScm(job);
        gitScm.url(REPOSITORY_URL).branch("28af63def44286729e3b19b03464d100fd1d0587");
        job.save();
        Build build = buildSuccessfully(job);

        assertThat(build.getConsole()).contains(
                "Found 428 commits",
                "-> 16510 lines added",
                "-> 10444 lines deleted");

        build.open();
        assertThat(getSummaryText(build, 3)).contains(
                "Revision: 28af63def44286729e3b19b03464d100fd1d0587", "detached");

        // TODO: create page objects
        assertThat(getSummaryText(build, 4)).contains(
                "SCM: git https://github.com/jenkinsci/git-forensics-plugin.git",
                "Initial recording of 200 commits",
                "Latest commit: 28af63d");

        assertThat(getSummaryText(build, 5)).contains(
                "SCM Statistics",
                "51 repository files",
                "total lines of code: 6066",
                "total churn: 16966",
                "New added lines: 16510",
                "New deleted lines: 10444",
                "New commits: 402",
                "from 4 authors",
                "in 131 files");
    }

    private GitScm createGitScm(final FreeStyleJob job) {
        GitScm gitScm = job.useScm(GitScm.class);

        WebDriverWait wait = new WebDriverWait(driver, 2, 100);
        WebElement url = gitScm.find(By.xpath(".//input[contains(@name, '_.url')]"));
        wait.withTimeout(Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(url));

        return gitScm;
    }

    private String getSummaryText(final Build referenceBuild, final int row) {
        return referenceBuild.getElement(
                By.xpath("/html/body/div[4]/div[2]/table/tbody/tr[" + row + "]/td[2]")).getText();
    }

    /**
     * Tests the Details table on ScmForensics page.
     */
    @Test
    public void shouldShowTableWithCompleteFunctionality() {
        FreeStyleJob job = createFreeStyleJob();
        job.addPublisher(ForensicsPublisher.class);

        GitScm gitScm = createGitScm(job);
        gitScm.url(REPOSITORY_URL).branch("28af63def44286729e3b19b03464d100fd1d0587");
        job.save();
        Build build = buildSuccessfully(job);

        ScmForensics scmForensics = new ScmForensics(build, "forensics");
        scmForensics.open();
        DetailsTable detailsTable = new DetailsTable(scmForensics);

        assertThat(scmForensics.getTotal()).isEqualTo(51);
        assertTableHeaders(detailsTable);
        assertTableEntriesAndSorting(detailsTable);
        assertSearch(detailsTable);
        assertPagination(detailsTable);
    }

    /**
     * asserts the headers of the table by their size and entries.
     *
     * @param detailsTable
     *         detailsTable object we want to assert the headers for.
     */
    private void assertTableHeaders(final DetailsTable detailsTable) {
        assertThat(detailsTable.getHeaderSize()).isEqualTo(7);

        List<String> tableHeaders = detailsTable.getHeaders();
        assertThat(tableHeaders.get(0)).isEqualTo(FILE_NAME);
        assertThat(tableHeaders.get(1)).isEqualTo(AUTHORS);
        assertThat(tableHeaders.get(2)).isEqualTo(COMMITS);
        assertThat(tableHeaders.get(3)).isEqualTo(LAST_COMMIT);
        assertThat(tableHeaders.get(4)).isEqualTo(ADDED);
        assertThat(tableHeaders.get(5)).isEqualTo(LOC);
        assertThat(tableHeaders.get(6)).isEqualTo(CHURN);
    }

    /**
     * asserts the certain table entries and then assert them again after sorting.
     *
     * @param detailsTable
     *         detailsTable object we want to assert the entries for.
     */
    private void assertTableEntriesAndSorting(final DetailsTable detailsTable) {
        assertThat(detailsTable.getNumberOfTableEntries()).isEqualTo(10);
        assertThat(detailsTable.getForensicsInfo()).isEqualTo("Showing 1 to 10 of 51 entries");

        detailsTable.showFiftyEntries();
        assertThat(detailsTable.getNumberOfTableEntries()).isEqualTo(50);

        detailsTable.sortColumn(FILE_NAME);
        assertRow(detailsTable,
                0,
                "config.yml",
                1,
                1
        );

        detailsTable.sortColumn(AUTHORS);
        detailsTable.sortColumn(AUTHORS);
        assertRow(detailsTable,
                0,
                "pom.xml",
                4,
                298
        );

        detailsTable.sortColumn(COMMITS);
        detailsTable.sortColumn(COMMITS);
        assertRow(detailsTable,
                1,
                "GitBlamer.java",
                3,
                46
        );

        detailsTable.showTenEntries();
    }

    /**
     * asserts the search of the Table by searching for a filename and then clearing the search afterwards.
     *
     * @param detailsTable
     *         detailsTable object we want to assert the search for.
     */
    private void assertSearch(final DetailsTable detailsTable) {
        detailsTable.searchTable("ui-tests/pom.xml");
        assertThat(detailsTable.getNumberOfTableEntries()).isEqualTo(1);

        detailsTable.sortColumn(AUTHORS);
        detailsTable.sortColumn(AUTHORS);

        assertRow(detailsTable,
                0,
                "pom.xml",
                2,
                2
        );
        detailsTable.clearSearch();
        assertThat(detailsTable.getTableRows().size()).isEqualTo(10);
    }

    private void assertPagination(final DetailsTable detailsTable) {
        assertThat(detailsTable.getForensicsInfo()).isEqualTo("Showing 1 to 10 of 51 entries");

        detailsTable.clickOnPagination(2);
        assertThat(detailsTable.getForensicsInfo()).isEqualTo("Showing 11 to 20 of 51 entries");

        detailsTable.showFiftyEntries();
        assertThat(detailsTable.getForensicsInfo()).isEqualTo("Showing 1 to 50 of 51 entries");

        detailsTable.clickOnPagination(2);
        assertThat(detailsTable.getForensicsInfo()).isEqualTo("Showing 51 to 51 of 51 entries");
    }

    private void assertRow(final DetailsTable detailsTable,
            final int rowNum, final String fileName, final int numAuthors, final int numCommits) {
        DetailsTableRow secondRow = detailsTable.getTableRows().get(rowNum);

        assertThat(secondRow.getFileName()).isEqualTo(fileName);
        assertThat(secondRow.getNumberOfAuthors()).isEqualTo(numAuthors);
        assertThat(secondRow.getNumberOfCommits()).isEqualTo(numCommits);
    }

    private FreeStyleJob createFreeStyleJob(final String... resourcesToCopy) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        ScrollerUtil.hideScrollerTabBar(driver);
        for (String resource : resourcesToCopy) {
            job.copyResource("/" + resource);
        }
        return job;
    }

    protected Build buildSuccessfully(final Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }
}

