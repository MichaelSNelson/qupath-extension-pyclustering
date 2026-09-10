package qupath.ext.qpcat.ui;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import qupath.ext.qpcat.model.ClusteringConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The "analyze current classifications" run mode.
 *
 * <p>A sub-cluster result contains only the sub-clustered population, so it can
 * never show Cluster 0 and 2 alongside Cluster 1.0..N -- those cells were not in
 * the extraction. The combined labelling lives on the objects, and this mode is
 * what turns it into a results window.
 */
class AnalyzeExistingModeTest {

    @Test
    void theModesAreDistinctAndNamed() {
        // A second boolean beside subclusterParentClass would give four states,
        // one of them meaningless. The enum is what keeps that from happening.
        assertThat(ClusteringDialog.RunMode.values()).containsExactlyInAnyOrder(
                ClusteringDialog.RunMode.CLUSTER,
                ClusteringDialog.RunMode.SUBCLUSTER,
                ClusteringDialog.RunMode.ANALYZE_EXISTING);
    }

    @Test
    void existingIsAnAlgorithmIdButNotNone() {
        // Deliberately not reusing "none": that id sets embedding_only in the
        // Python, which switches off marker ranking, PAGA and every plot --
        // exactly what this mode exists to produce.
        assertThat(ClusteringConfig.Algorithm.EXISTING.getId()).isEqualTo("existing");
        assertThat(ClusteringConfig.Algorithm.EXISTING.getId())
                .isNotEqualTo(ClusteringConfig.Algorithm.NONE.getId());
    }

    @Test
    void theMenuEntryExists() {
        // The engine shipped without a menu entry for several releases; the string
        // being present is what makes the feature reachable at all.
        ResourceBundle res = ResourceBundle.getBundle("qupath.ext.qpcat.ui.strings");
        assertThat(res.getString("menu.analyzeExisting")).contains("classification");
    }
}
