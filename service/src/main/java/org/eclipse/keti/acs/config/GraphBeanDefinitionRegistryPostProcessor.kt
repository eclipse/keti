/*******************************************************************************
 * Copyright 2018 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.config

import org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository
import org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepository
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("graph")
@Component
class GraphBeanDefinitionRegistryPostProcessor : BeanDefinitionRegistryPostProcessor {

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        // Do nothing.
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val resourceRepositoryBeanDefinition = RootBeanDefinition(GraphResourceRepository::class.java)
        registry.registerBeanDefinition("resourceHierarchicalRepository", resourceRepositoryBeanDefinition)

        val subjectRepositoryBeanDefinition = RootBeanDefinition(GraphSubjectRepository::class.java)
        registry.registerBeanDefinition("subjectHierarchicalRepository", subjectRepositoryBeanDefinition)
    }
}
