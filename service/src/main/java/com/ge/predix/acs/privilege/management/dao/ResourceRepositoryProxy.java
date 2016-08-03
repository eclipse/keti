package com.ge.predix.acs.privilege.management.dao;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

@Component
public class ResourceRepositoryProxy implements ResourceRepository, ResourceHierarchicalRepository, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRepositoryProxy.class);

    @Autowired(required = false)
    private GraphResourceRepository graphRepository;

    @Autowired
    @Qualifier("resourceRepository") // This is the bean id registered by Spring data JPA.
    private ResourceRepository nonGraphRepository;

    @Autowired
    private Environment environment;

    // This is set to the active repository being proxied to, based on the active profile. See afterPropertiesSet()
    private ResourceRepository activeRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Arrays.asList(this.environment.getActiveProfiles()).contains("titan")) {
            this.activeRepository = this.graphRepository;
            LOGGER.info("Resource hierarchical repository enabled.");
        } else {
            this.activeRepository = this.nonGraphRepository;
            LOGGER.info("Resource non-hierarchical repository enabled.");
        }
    }

    @Override
    public List<ResourceEntity> findAll() {
        throw new RuntimeException("method not supported");
    }

    @Override
    public List<ResourceEntity> findAll(final Sort arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public List<ResourceEntity> findAll(final Iterable<Long> arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public <S extends ResourceEntity> List<S> save(final Iterable<S> arg0) {
        return this.activeRepository.save(arg0);
    }

    @Override
    public void flush() {
        throw new RuntimeException("method not supported");
    }

    @Override
    public <S extends ResourceEntity> S saveAndFlush(final S arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public void deleteInBatch(final Iterable<ResourceEntity> arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public void deleteAllInBatch() {
        throw new RuntimeException("method not supported");
    }

    @Override
    public ResourceEntity getOne(final Long arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public Page<ResourceEntity> findAll(final Pageable arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public <S extends ResourceEntity> S save(final S arg0) {
        return this.activeRepository.save(arg0);
    }

    @Override
    public ResourceEntity findOne(final Long arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public boolean exists(final Long arg0) {
        throw new RuntimeException("method not supported");
    }

    @Override
    public long count() {
        throw new RuntimeException("method not supported");
    }

    @Override
    public void delete(final Long arg0) {
        this.activeRepository.delete(arg0);
    }

    @Override
    public void delete(final ResourceEntity arg0) {
        this.activeRepository.delete(arg0);

    }

    @Override
    public void delete(final Iterable<? extends ResourceEntity> arg0) {
        this.activeRepository.delete(arg0);
    }

    @Override
    public void deleteAll() {
        throw new RuntimeException("method not supported");
    }

    @Override
    public ResourceEntity getResourceWithInheritedAttributes(final ZoneEntity zone, final String resourceIdentifier) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getResourceWithInheritedAttributes(zone, resourceIdentifier);
        } else {
            return this.nonGraphRepository.getByZoneAndResourceIdentifier(zone, resourceIdentifier);
        }
    }

    @Override
    public List<ResourceEntity> findByZone(final ZoneEntity zone) {
        return this.activeRepository.findByZone(zone);
    }

    @Override
    public ResourceEntity getByZoneAndResourceIdentifier(final ZoneEntity zone, final String resourceIdentifier) {
        return this.activeRepository.getByZoneAndResourceIdentifier(zone, resourceIdentifier);
    }

    @Override
    public Set<String> getResourceEntityAndDescendantsIds(final ResourceEntity entity) {
        if (this.activeRepository == this.graphRepository) { // i.e. titan is enabled
            return this.graphRepository.getResourceEntityAndDescendantsIds(entity);
        } else {
            return new HashSet<String>(Arrays.asList(entity.getResourceIdentifier()));
        }
    }
}
