/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.model.dsl.internal.transform

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.EnableModelDsl
import org.gradle.model.internal.report.unbound.UnboundRule
import org.gradle.model.internal.report.unbound.UnboundRuleInput
import spock.lang.Unroll

import static org.gradle.model.report.unbound.UnboundRulesReportMatchers.unbound
import static org.hamcrest.Matchers.containsString

class ModelDslRuleInputDetectionIntegrationSpec extends AbstractIntegrationSpec {

    def setup() {
        executer.requireOwnGradleUserHomeDir()
        EnableModelDsl.enable(executer)
    }

    @Unroll
    def "only literal strings can be given to dollar - #code"() {
        when:
        buildScript """
        model {
          foo {
            $code
          }
        }
        """

        then:
        fails "tasks"
        failure.assertHasLineNumber 4
        failure.assertHasFileName("Build file '${buildFile}'")
        failure.assertThatCause(containsString(RuleVisitor.INVALID_ARGUMENT_LIST))

        where:
        code << [
                '$(1)',
                '$("$name")',
                '$("a" + "b")',
                'def a = "foo"; $(a)',
                '$("foo", "bar")',
                '$()',
                '$(null)',
                '$("")'
        ]
    }

    @Unroll
    def "dollar method is only detected with no explicit receiver - #code"() {
        when:
        buildScript """
            import org.gradle.model.*

            class MyPlugin {
              static class Rules extends RuleSource {
                @Model
                String foo() {
                  "foo"
                }
              }
            }

            apply type: MyPlugin

            model {
              foo {
                $code
              }
            }
        """

        then:
        succeeds "tasks" // succeeds because we don't fail on invalid usage, and don't fail due to unbound inputs

        where:
        code << [
                'something.$(1)',
//                'this.$("$name")',
//                'foo.bar().$("a" + "b")',
        ]
    }

    def "input references are found in nested code - #code"() {
        when:
        buildScript """
            import org.gradle.model.*
            import org.gradle.model.collection.*

            class MyPlugin {
                static class Rules extends RuleSource {
                    @Mutate void addPrintTask(CollectionBuilder<Task> tasks, List<String> strings) {
                        tasks.create("printMessage", PrintTask) {
                            it.message = strings
                        }
                    }

                    @Model String foo() {
                        "foo"
                    }

                    @Model List<String> strings() {
                        []
                    }
                }
            }

            class PrintTask extends DefaultTask {
                String message

                @TaskAction
                void doPrint() {
                    println "message: " + message
                }
            }

            apply type: MyPlugin

            model {
                strings {
                    $code
                }
            }
        """

        then:
        succeeds "printMessage"
        output.contains("message: [foo]")

        where:
        code << [
                'if (true) { add $("foo") }',
                'if (false) {} else if (true) { add $("foo") }',
                'if (false) {} else { add $("foo") }',
                'def i = true; while(i) { add $("foo"); i = false }',
                '[1].each { add $("foo") }',
                'add "${$("foo")}"',
                'def v = $("foo"); add(v)',
                'add($("foo"))',
                'add($("foo").toString())',
        ]
    }

    def "input model path must be valid"() {
        when:
        buildScript """
            import org.gradle.model.*

            class MyPlugin {
              static class Rules extends RuleSource {
                @Model
                List<String> strings() {
                  []
                }
              }
            }

            apply type: MyPlugin

            model {
              tasks {
                \$("foo. bar") // line 21
              }
            }
        """

        then:
        fails "tasks"
        failure.assertHasLineNumber(17)
        failure.assertThatCause(containsString("Invalid model path given as rule input."))
        failure.assertThatCause(containsString("Model path 'foo. bar' is invalid due to invalid name component."))
        failure.assertThatCause(containsString("Model element name ' bar' has illegal first character ' ' (names must start with an ASCII letter or underscore)."))
    }

    def "location and suggestions are provided for unbound rule inputs specified using a name"() {
        given:
        buildScript '''
            import org.gradle.model.*
            import org.gradle.model.collection.*

            class MyPlugin {
                static class Rules extends RuleSource {
                    @Mutate
                    void addTasks(CollectionBuilder<Task> tasks) {
                        tasks.create("foobar")
                        tasks.create("raboof")
                    }
                }
            }

            apply type: MyPlugin

            model {
                foo {
                    $('tasks.foonar')
                    $('tasks.fooar')
                    $('tasks.foonar')
                }
            }
        '''

        when:
        fails "tasks"

        then:
        failure.assertThatCause(unbound(
                UnboundRule.descriptor("model.foo", buildFile, 18, 17)
                        .mutableInput(UnboundRuleInput.type(Object).path("foo"))
                        .immutableInput(UnboundRuleInput.type(Object).path("tasks.foonar").suggestions("tasks.foobar").description("@ line 19"))
                        .immutableInput(UnboundRuleInput.type(Object).path("tasks.fooar").suggestions("tasks.foobar").description("@ line 20"))
        ))
    }

    def "can not access project or script from rule"() {
        when:
        buildScript """
            model {
                tasks {
                    assert owner == null
                    assert this == null

                    try {
                        project.tasks
                        assert false : "should not reach here"
                    } catch (MissingPropertyException ignore) {

                    }
                }
            }
        """

        then:
        succeeds "tasks"
    }
}
