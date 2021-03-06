/***************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **************************************************************************/
package org.jenkinsci.plugins.jiraext.view;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.StreamBuildListener;
import hudson.scm.ChangeLogSet;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.jiraext.Config;
import org.jenkinsci.plugins.jiraext.MockChangeLogUtil;
import org.jenkinsci.plugins.jiraext.domain.JiraCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dalvizu
 */
public class FirstWordOfCommitStrategyTest
{
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp()
    {
        Config.getGlobalConfig().setPattern("FOO-,BAR-");
    }

    @Test
    public void testSaveConfig()
            throws Exception
    {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        JiraExtBuildStep builder = new JiraExtBuildStep(new FirstWordOfCommitStrategy(),
                Arrays.asList((JiraOperationExtension) new AddComment(true, "Hello World")));
        project.getBuildersList().add(builder);

        jenkinsRule.submit(jenkinsRule.createWebClient().getPage(project, "configure").getFormByName("config"));

        JiraExtBuildStep after = project.getBuildersList().get(JiraExtBuildStep.class);
        jenkinsRule.assertEqualBeans(builder, after, "issueStrategy");
    }

    @Test
    public void testFirstWordOfCommit()
            throws Exception
    {
        FirstWordOfCommitStrategy strategy = new FirstWordOfCommitStrategy();

        ChangeLogSet mockChangeSet = MockChangeLogUtil.mockChangeLogSet(
                new MockChangeLogUtil.MockChangeLog("FOO-101 first", "dalvizu"),
                new MockChangeLogUtil.MockChangeLog("BAR-102 again", "jsmith"),
                new MockChangeLogUtil.MockChangeLog("No Valid Ticket", "build robot"));
        AbstractBuild mockBuild = mock(AbstractBuild.class);
        when(mockBuild.getChangeSet()).thenReturn(mockChangeSet);
        List<JiraCommit> commits = strategy.getJiraCommits(mockBuild,
                new StreamBuildListener(System.out, Charset.defaultCharset()));
        assertEquals(commits.size(), 2);

        assertThat(commits, hasItem(Matchers.<JiraCommit>hasProperty("jiraTicket", equalTo("FOO-101"))));
        assertThat(commits, hasItem(Matchers.<JiraCommit>hasProperty("jiraTicket", equalTo("BAR-102"))));
    }
}
