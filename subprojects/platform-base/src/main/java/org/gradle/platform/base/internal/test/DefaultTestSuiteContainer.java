/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal.test;

import org.gradle.internal.util.BiFunction;
import org.gradle.model.internal.core.DefaultCollectionBuilder;
import org.gradle.model.internal.core.ModelCreators;
import org.gradle.model.internal.core.ModelReference;
import org.gradle.model.internal.core.MutableModelNode;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.test.TestSuiteContainer;
import org.gradle.platform.base.test.TestSuiteSpec;

public class DefaultTestSuiteContainer extends DefaultCollectionBuilder<TestSuiteSpec> implements TestSuiteContainer {
    public DefaultTestSuiteContainer(ModelType<TestSuiteSpec> elementType, ModelRuleDescriptor sourceDescriptor, MutableModelNode modelNode,
                                     BiFunction<? extends ModelCreators.Builder, ? super MutableModelNode, ? super ModelReference<? extends TestSuiteSpec>> creatorFunction) {
        super(elementType, sourceDescriptor, modelNode, creatorFunction);
    }
}
