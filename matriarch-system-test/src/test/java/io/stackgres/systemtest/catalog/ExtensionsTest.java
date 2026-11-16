package io.stackgres.systemtest.catalog;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.Versions;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * `stackgres cluster extension list` — the user-facing extension catalog (DOCIR-backed). Asserts a stable
 * set of core contrib extensions, a sensible breadth, and the invariant that `-V` versions for a given
 * `-v <pg>` carry that Postgres version (contrib like {@code uuid-ossp} are versioned with Postgres).
 * Black-box, via the CLI.
 */
class ExtensionsTest extends BaseSystemTest {

    /** Core PG contrib — present for every supported version (avoid third-party like timescaledb/vchord here). */
    private static final Set<String> STABLE_CONTRIB = Set.of("amcheck", "auto_explain", "unaccent", "uuid-ossp", "xml2");

    /** A contrib extension whose version equals the Postgres version it was built for. */
    private static final String VERSION_TRACKING = "uuid-ossp";

    @Test
    void extensionListShowsStableContrib() {
        CommandResult r = cli.run("cluster", "extension", "list", "-F", "postgres");
        assertThat(r.isSuccess()).as("extension list failed: %s", r.output()).isTrue();

        assertThat(STABLE_CONTRIB).allSatisfy(name ->
                assertThat(r.output()).as("lists %s", name).contains(name));
        long listed = r.output().lines().filter(l -> !l.isBlank()).count();
        assertThat(listed).as("a sizable catalog, not a degenerate response").isGreaterThanOrEqualTo(20);
    }

    @Test
    void extensionVersionsTrackThePgVersion() {
        String pg = concreteVersion();
        assumeTrue(pg != null, "no concrete <major>.<minor> version from `cluster version list`");

        CommandResult r = cli.run("cluster", "extension", "list", "-F", "postgres", "-v", pg, "-V");
        assertThat(r.isSuccess()).as("extension list -V failed: %s", r.output()).isTrue();

        String row = r.output().lines().filter(l -> l.contains(VERSION_TRACKING)).findFirst().orElse(null);
        assertThat(row).as("%s should be listed for PG %s", VERSION_TRACKING, pg).isNotNull();
        assertThat(row).as("%s is versioned with PG %s", VERSION_TRACKING, pg).contains(pg);
    }

    @Test
    void latestResolvesToHighestConcreteVersion() {
        String highest = Versions.highestConcrete(catalogVersions());
        assumeTrue(highest != null, "no concrete <major>.<minor> version from `cluster version list`");

        CommandResult r = cli.run("cluster", "extension", "list", "-F", "postgres", "-v", "latest", "-V");
        assertThat(r.isSuccess()).as("extension list -v latest failed: %s", r.output()).isTrue();

        String row = r.output().lines().filter(l -> l.contains(VERSION_TRACKING)).findFirst().orElse(null);
        assertThat(row).as("%s should be listed for -v latest", VERSION_TRACKING).isNotNull();
        // `-v latest` resolves to the highest concrete version, so the contrib carries exactly that
        assertThat(row).as("`-v latest` resolves to the highest version %s (%s row: %s)", highest, VERSION_TRACKING, row)
                .contains(highest);
    }

    /** First concrete {@code <major>.<minor>} from `cluster version list` (never an alias). */
    private String concreteVersion() {
        return catalogVersions().stream().findFirst().orElse(null);
    }

    /** The concrete {@code <major>.<minor>} versions the catalog advertises, read from the CLI. */
    private List<String> catalogVersions() {
        CommandResult r = cli.run("cluster", "version", "list", "-F", "postgres");
        return r.isSuccess() ? Versions.concrete(r.output()) : List.of();
    }
}
