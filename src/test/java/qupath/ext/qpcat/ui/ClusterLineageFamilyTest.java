package qupath.ext.qpcat.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import qupath.ext.qpcat.service.ClusteringResultManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which saved results the Modify Clusters dialog may target.
 *
 * <p>Opened from a Results window the chooser previously listed every result in
 * the project and merely pre-selected the right one, so an unrelated run was one
 * click away. Applying an edit writes a copy of whatever is selected AND relabels
 * detections across the images that result covers -- a different image set -- so
 * the wrong selection is a data change, not just a wrong view.
 */
class ClusterLineageFamilyTest {

    private static ClusteringResultManager.ResultEntry entry(String name, String derivedFrom) {
        ClusteringResultManager.ResultEntry e = new ClusteringResultManager.ResultEntry();
        e.name = name;
        e.derivedFrom = derivedFrom;
        return e;
    }

    /** run -> run_renamed -> run_merged, plus an unrelated second run. */
    private static List<ClusteringResultManager.ResultEntry> project() {
        List<ClusteringResultManager.ResultEntry> all = new ArrayList<>();
        all.add(entry("run", null));
        all.add(entry("run_renamed", "run"));
        all.add(entry("run_merged", "run_renamed"));
        all.add(entry("other_run", null));
        all.add(entry("other_renamed", "other_run"));
        return all;
    }

    @Test
    void anUnrelatedRunIsNeverInTheFamily() {
        // The property the whole change exists for.
        Set<String> family = ClusterManagementDialog.lineageFamily(project(), "run_renamed");
        assertThat(family).doesNotContain("other_run", "other_renamed");
    }

    @Test
    void theFamilyReachesBothWaysFromTheViewedResult() {
        Set<String> family = ClusterManagementDialog.lineageFamily(project(), "run_renamed");
        assertThat(family).containsExactlyInAnyOrder("run", "run_renamed", "run_merged");
    }

    @Test
    void descendantsSeveralEditsDeepAreReached() {
        // A single pass over the list would find run_renamed but miss run_merged,
        // which is the version an edit chain actually leaves you on.
        Set<String> family = ClusterManagementDialog.lineageFamily(project(), "run");
        assertThat(family).contains("run_merged");
    }

    @Test
    void anOriginalRunWithNoEditsIsJustItself() {
        Set<String> family = ClusterManagementDialog.lineageFamily(project(), "other_run");
        assertThat(family).containsExactlyInAnyOrder("other_run", "other_renamed");

        List<ClusteringResultManager.ResultEntry> lone = List.of(entry("solo", null));
        assertThat(ClusterManagementDialog.lineageFamily(lone, "solo")).containsExactly("solo");
    }

    @Test
    void aSubclusterIsPartOfItsParentFamily() {
        // Sub-clusters record derivedFrom too, so they join the family. That is
        // wanted: they are versions of this analysis, not an unrelated run.
        List<ClusteringResultManager.ResultEntry> all = new ArrayList<>(project());
        all.add(entry("sub_of_cluster1", "run"));
        assertThat(ClusterManagementDialog.lineageFamily(all, "run_merged"))
                .contains("sub_of_cluster1");
    }

    @Test
    void aCycleDoesNotHangTheDialog() {
        // Nothing we write produces one, but a hand-edited JSON could, and an
        // unguarded ancestor walk would spin forever behind a modal dialog.
        List<ClusteringResultManager.ResultEntry> all = new ArrayList<>();
        all.add(entry("a", "b"));
        all.add(entry("b", "a"));
        assertThat(ClusterManagementDialog.lineageFamily(all, "a"))
                .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void aMissingParentStopsTheWalkRatherThanFailing() {
        // The parent was deleted in Manage Saved Results; the child still opens.
        List<ClusteringResultManager.ResultEntry> all = List.of(entry("orphan", "deleted_run"));
        assertThat(ClusterManagementDialog.lineageFamily(all, "orphan"))
                .containsExactlyInAnyOrder("orphan", "deleted_run");
    }

    @Test
    void anUnknownRootYieldsOnlyItself() {
        assertThat(ClusterManagementDialog.lineageFamily(project(), "not_a_result"))
                .containsExactly("not_a_result");
    }
}
