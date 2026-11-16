package io.stackgres.systemtest.catalog;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.Versions;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `stackgres cluster version list` — the user-facing version catalog. DOCIR advertises a {@code latest}
 * alias, per-major {@code <major>.latest} aliases, and concrete {@code <major>.<minor>} versions; {@code
 * latest} resolves to the highest concrete. The list command only <em>lists</em> (it doesn't resolve {@code
 * latest} — that's pinned in {@link ExtensionsTest}), so here we assert the shape, the current majors, and
 * that the highest concrete is present with its {@code .latest} alias (the list being consistent with the
 * resolution). Black-box (no gRPC): the command's own rendering is what's under test.
 */
class VersionsTest extends BaseSystemTest {

    /** Current majors we expect listed. Bump when the supported set shifts (a deliberate, visible edit). */
    private static final Set<String> EXPECTED_MAJORS = Set.of("16", "17");

    @Test
    void versionListShowsAliasesAndCurrentMajors() {
        String out = versionList();

        assertThat(out).as("the `latest` alias").contains("latest");
        for (String major : EXPECTED_MAJORS) {
            assertThat(out).as("the %s.latest alias", major).contains(major + ".latest");
            assertThat(out).as("a concrete %s.<minor> version", major).containsPattern(major + "\\.\\d+");
        }
    }

    @Test
    void highestConcreteVersionIsListedWithItsAlias() {
        String out = versionList();

        List<String> concrete = Versions.concrete(out);
        assertThat(concrete).as("the catalog lists concrete versions").isNotEmpty();

        String highest = Versions.highestConcrete(concrete);
        assertThat(highest).as("a highest concrete version").isNotNull();

        String highestMajor = highest.substring(0, highest.indexOf('.'));
        assertThat(out).as("the newest major %s (of highest concrete %s) carries its `.latest` alias", highestMajor, highest)
                .contains(highestMajor + ".latest");
    }

    private String versionList() {
        CommandResult r = cli.run("cluster", "version", "list", "-F", "postgres");
        assertThat(r.isSuccess()).as("version list failed: %s", r.output()).isTrue();
        return r.output();
    }
}
