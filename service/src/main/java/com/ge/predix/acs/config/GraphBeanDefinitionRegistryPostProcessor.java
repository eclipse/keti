package com.ge.predix.acs.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.GraphResourceRepository;
import com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository;

@Component
@Profile("titan")
public class GraphBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Value("${ENABLE_TITAN:false}")
    private boolean titanEnabled;

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Do nothing.
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        //if (this.titanEnabled) {
            BeanDefinition resourceRepositoryBeanDefinition = new RootBeanDefinition(GraphResourceRepository.class);
            registry.registerBeanDefinition("resourceRepository", resourceRepositoryBeanDefinition);

            BeanDefinition subjectRepositoryBeanDefinition = new RootBeanDefinition(GraphSubjectRepository.class);
            registry.registerBeanDefinition("subjectRepository", subjectRepositoryBeanDefinition);
        //}
    }
}
