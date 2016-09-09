package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

public class GraphResourceConsistencyTest {
    private static final JsonUtils JSON_UTILS = new JsonUtils();

    private GraphResourceRepository resourceRepository;
    private Graph graph;

    public boolean useLocalCassandra = true;

    @BeforeClass
    public void setup() throws Exception {
        this.resourceRepository = new GraphResourceRepository();
        setupTitanGraph();
        this.resourceRepository.setGraph(this.graph);
    }

    private void setupTitanGraph() throws InterruptedException, ExecutionException {
        this.graph = TitanFactory.build().set("storage.backend", "inmemory").open();
        GraphConfig.createVertexLabel(this.graph, GraphResourceRepository.RESOURCE_LABEL);
        GraphConfig.createVertexLabel(this.graph, GraphSubjectRepository.SUBJECT_LABEL);
        GraphConfig.createIndex(this.graph, GraphConfig.BY_ZONE_INDEX_NAME, ZONE_ID_KEY);
        GraphConfig.createTwoKeyUniqueCompositeIndex(this.graph, GraphConfig.BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME,
                ZONE_ID_KEY, RESOURCE_ID_KEY);
        GraphConfig.createTwoKeyUniqueCompositeIndex(this.graph, GraphConfig.BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME,
                ZONE_ID_KEY, SUBJECT_ID_KEY);
        GraphConfig.createEdgeIndex(this.graph, GraphConfig.BY_SCOPE_INDEX_NAME, PARENT_EDGE_LABEL, SCOPE_PROPERTY_KEY);
    }

    @Test(/* threadPoolSize = 2, invocationCount = 2, */ dataProvider = "resourcesForTestConcurrent",
            successPercentage = 97)
    public void testConcurrentWriteByZoneAndResourceIdentifier(final String resourceId) {
        System.out.println("thread_id: " + Long.toString(Thread.currentThread().getId()) + ", resource_id:" + resourceId);
        ZoneEntity z = new ZoneEntity();
        z.setName("z1");
        persistResourceToZoneAndAssert(z, resourceId, Collections.emptySet());
    }

    @DataProvider(parallel = true)
    public Object[][] resourcesForTestConcurrent() {
        return IntStream.range(0, 100).mapToObj(i -> new Object[] { "r" + Integer.toString(i) })
                .toArray(Object[][]::new);
    }

    private ResourceEntity persistResourceToZoneAndAssert(final ZoneEntity zoneEntity, final String resourceIdentifier,
            final Set<Attribute> attributes) {
        ResourceEntity resource = new ResourceEntity(zoneEntity, resourceIdentifier);
        resource.setAttributes(attributes);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        ResourceEntity resourceEntity = this.resourceRepository.save(resource);
        assertThat(this.resourceRepository.findOne(resourceEntity.getId()), equalTo(resource));
        return resourceEntity;
    }
}
