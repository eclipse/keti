/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.GraphResourceRepository;
import com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository;

@Profile("titan")
@Component
public class GraphBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
        // Do nothing.
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) {
        BeanDefinition resourceRepositoryBeanDefinition = new RootBeanDefinition(GraphResourceRepository.class);
        registry.registerBeanDefinition("resourceHierarchicalRepository", resourceRepositoryBeanDefinition);

        BeanDefinition subjectRepositoryBeanDefinition = new RootBeanDefinition(GraphSubjectRepository.class);
        registry.registerBeanDefinition("subjectHierarchicalRepository", subjectRepositoryBeanDefinition);
    }
}
