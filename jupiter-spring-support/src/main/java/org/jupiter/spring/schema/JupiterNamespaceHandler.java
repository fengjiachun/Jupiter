/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.spring.schema;

import org.jupiter.spring.support.JupiterSpringClient;
import org.jupiter.spring.support.JupiterSpringConsumerBean;
import org.jupiter.spring.support.JupiterSpringProviderBean;
import org.jupiter.spring.support.JupiterSpringServer;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Jupiter
 * org.jupiter.spring.schema
 *
 * @author jiachun.fjc
 */
public class JupiterNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("server", new JupiterBeanDefinitionParser(JupiterSpringServer.class));
        registerBeanDefinitionParser("client", new JupiterBeanDefinitionParser(JupiterSpringClient.class));
        registerBeanDefinitionParser("provider", new JupiterBeanDefinitionParser(JupiterSpringProviderBean.class));
        registerBeanDefinitionParser("consumer", new JupiterBeanDefinitionParser(JupiterSpringConsumerBean.class));
    }
}
