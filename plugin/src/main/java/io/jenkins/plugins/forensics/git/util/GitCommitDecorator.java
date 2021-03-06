package io.jenkins.plugins.forensics.git.util;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.Generated;

import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.scm.RepositoryBrowser;

import static j2html.TagCreator.*;

/**
 * A {@link RepositoryBrowser} for Git commits. Since a {@link RepositoryBrowser} has no API to generate links to simple
 * commits, this decorator adds such a functionality for Git. Basically, this implementation delegates to the {@link
 * GitRepositoryBrowser} implementation, if available. Otherwise a plain link will be rendered using a short
 * representation, see {@link #asText(String)}.
 *
 * @author Ullrich Hafner
 */
public class GitCommitDecorator extends GitCommitTextDecorator {
    private final GitRepositoryBrowser browser;

    GitCommitDecorator(final GitRepositoryBrowser browser) {
        super();

        this.browser = browser;
    }

    @Override
    public String asLink(final String id) {
        if (StringUtils.isNotBlank(id)) {
            return createLink(id).orElse(asText(id));
        }
        return id;
    }

    private Optional<String> createLink(final String id) {
        try {
            URL link = browser.getChangeSetLink(new DummyChangeSet(id));
            if (link != null) {
                return Optional.of(a().withText(asText(id)).withHref(link.toString()).render());
            }
        }
        catch (IOException exception) {
            // ignore and return nothing
        }
        return Optional.empty();
    }

    // TODO: move this class and the implementation to the Git plugin
    private static class DummyChangeSet extends GitChangeSet {
        private final String id;

        DummyChangeSet(final String id) {
            super(Collections.emptyList(), false);
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            DummyChangeSet that = (DummyChangeSet) o;
            return Objects.equals(id, that.id);
        }

        @Override @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), id);
        }
    }
}
