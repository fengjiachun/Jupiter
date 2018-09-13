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

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Pair;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.consumer.ConsumerInterceptor;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.spring.support.JupiterSpringClient;
import org.jupiter.spring.support.JupiterSpringConsumerBean;
import org.jupiter.spring.support.JupiterSpringProviderBean;
import org.jupiter.spring.support.JupiterSpringServer;
import org.jupiter.transport.JOption;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.ManagedArray;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;

/**
 * Jupiter
 * org.jupiter.spring.schema
 *
 * @author jiachun.fjc
 */
public class JupiterBeanDefinitionParser implements BeanDefinitionParser {

    private final Class<?> beanClass;

    public JupiterBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        if (beanClass == JupiterSpringServer.class) {
            return parseJupiterServer(element, parserContext);
        } else if (beanClass == JupiterSpringClient.class) {
            return parseJupiterClient(element, parserContext);
        } else if (beanClass == JupiterSpringProviderBean.class) {
            return parseJupiterProvider(element, parserContext);
        } else if (beanClass == JupiterSpringConsumerBean.class) {
            return parseJupiterConsumer(element, parserContext);
        } else {
            throw new BeanDefinitionValidationException("Unknown class to definition: " + beanClass.getName());
        }
    }

    private BeanDefinition parseJupiterServer(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addProperty(def, element, "registryType", false);
        addPropertyReference(def, element, "acceptor", false);

        List<Pair<JOption<Object>, String>> parentOptions = Lists.newArrayList();
        List<Pair<JOption<Object>, String>> childOptions = Lists.newArrayList();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "registryServerAddresses", false);
                    addPropertyReferenceArray(
                            def,
                            (Element) item,
                            ProviderInterceptor.class.getName(),
                            "globalProviderInterceptors",
                            false);
                    addPropertyReference(def, (Element) item, "globalFlowController", false);
                } else if ("netOptions".equals(localName)) {
                    NodeList configList = item.getChildNodes();
                    for (int j = 0; j < configList.getLength(); j++) {
                        Node configItem = configList.item(j);
                        if (configItem instanceof Element) {
                            parseNetOption(configItem, parentOptions, childOptions);
                        }
                    }
                }
            }
        }

        if (!parentOptions.isEmpty()) {
            def.getPropertyValues().addPropertyValue("parentNetOptions", parentOptions);
        }

        if (!childOptions.isEmpty()) {
            def.getPropertyValues().addPropertyValue("childNetOptions", childOptions);
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterClient(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addProperty(def, element, "appName", false);
        addProperty(def, element, "registryType", false);
        addPropertyReference(def, element, "connector", false);

        List<Pair<JOption<Object>, String>> childOptions = Lists.newArrayList();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "registryServerAddresses", false);
                    addProperty(def, (Element) item, "providerServerAddresses", false);
                    addPropertyReferenceArray(
                            def,
                            (Element) item,
                            ConsumerInterceptor.class.getName(),
                            "globalConsumerInterceptors",
                            false);
                } else if ("netOptions".equals(localName)) {
                    NodeList configList = item.getChildNodes();
                    for (int j = 0; j < configList.getLength(); j++) {
                        Node configItem = configList.item(j);
                        if (configItem instanceof Element) {
                            parseNetOption(configItem, null, childOptions);
                        }
                    }
                }
            }
        }

        if (!childOptions.isEmpty()) {
            def.getPropertyValues().addPropertyValue("childNetOptions", childOptions);
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterProvider(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addPropertyReference(def, element, "server", true);
        addPropertyReference(def, element, "providerImpl", true);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "weight", false);
                    addPropertyReferenceArray(
                            def,
                            (Element) item,
                            ProviderInterceptor.class.getName(),
                            "providerInterceptors",
                            false);
                    addPropertyReference(def, (Element) item, "executor", false);
                    addPropertyReference(def, (Element) item, "flowController", false);
                    addPropertyReference(def, (Element) item, "providerInitializer", false);
                    addPropertyReference(def, (Element) item, "providerInitializerExecutor", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterConsumer(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addPropertyReference(def, element, "client", true);
        addProperty(def, element, "interfaceClass", true);

        List<MethodSpecialConfig> methodSpecialConfigs = Lists.newArrayList();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "version", false);
                    addProperty(def, (Element) item, "serializerType", false);
                    addProperty(def, (Element) item, "loadBalancerType", false);
                    addProperty(def, (Element) item, "extLoadBalancerName", false);
                    addProperty(def, (Element) item, "waitForAvailableTimeoutMillis", false);
                    addProperty(def, (Element) item, "invokeType", false);
                    addProperty(def, (Element) item, "dispatchType", false);
                    addProperty(def, (Element) item, "timeoutMillis", false);
                    addProperty(def, (Element) item, "providerAddresses", false);
                    addProperty(def, (Element) item, "clusterStrategy", false);
                    addProperty(def, (Element) item, "failoverRetries", false);
                    addPropertyReferenceArray(
                            def,
                            (Element) item,
                            ConsumerInterceptor.class.getName(),
                            "consumerInterceptors",
                            false);
                } else if ("methodSpecials".equals(localName)) {
                    NodeList configList = item.getChildNodes();
                    for (int j = 0; j < configList.getLength(); j++) {
                        Node configItem = configList.item(j);
                        if (configItem instanceof Element) {
                            if ("methodSpecial".equals(configItem.getLocalName())) {
                                String methodName = ((Element) configItem).getAttribute("methodName");
                                String timeoutMillis = ((Element) configItem).getAttribute("timeoutMillis");
                                String clusterStrategy = ((Element) configItem).getAttribute("clusterStrategy");
                                String failoverRetries = ((Element) configItem).getAttribute("failoverRetries");

                                MethodSpecialConfig config = MethodSpecialConfig.of(methodName)
                                        .timeoutMillis(Long.parseLong(timeoutMillis))
                                        .strategy(ClusterStrategyConfig.of(clusterStrategy, failoverRetries));
                                methodSpecialConfigs.add(config);
                            }
                        }
                    }
                }
            }
        }

        if (!methodSpecialConfigs.isEmpty()) {
            def.getPropertyValues().addPropertyValue("methodSpecialConfigs", methodSpecialConfigs);
        }

        return registerBean(def, element, parserContext);
    }

    @SuppressWarnings("unchecked")
    private void parseNetOption(
            Node configItem, List<Pair<JOption<Object>, String>> parentOptions, List<Pair<JOption<Object>, String>> childOptions) {

        String localName = configItem.getLocalName();

        if ("parentOption".equals(localName) && parentOptions != null) {
            for (JOption<?> op : JOption.ALL_OPTIONS) {
                String value = ((Element) configItem).getAttribute(op.name());
                if (Strings.isNotBlank(value)) {
                    parentOptions.add(Pair.of((JOption<Object>) op, value));
                }
            }
        } else if ("childOption".equals(localName) && childOptions != null) {
            for (JOption<?> op : JOption.ALL_OPTIONS) {
                String value = ((Element) configItem).getAttribute(op.name());
                if (Strings.isNotBlank(value)) {
                    childOptions.add(Pair.of((JOption<Object>) op, value));
                }
            }
        }
    }

    private BeanDefinition registerBean(RootBeanDefinition definition, Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        if (Strings.isNullOrEmpty(id)) {
            id = beanClass.getSimpleName();
        }
        if (parserContext.getRegistry().containsBeanDefinition(id)) {
            throw new IllegalStateException("Duplicate jupiter bean id: " + id);
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());

        return definition;
    }

    private static void addProperty(
            RootBeanDefinition definition, Element element, String propertyName, boolean required) {
        String ref = element.getAttribute(propertyName);
        if (required) {
            checkAttribute(propertyName, ref);
        }
        if (!Strings.isNullOrEmpty(ref)) {
            definition.getPropertyValues().addPropertyValue(propertyName, ref);
        }
    }

    private static void addPropertyReference(
            RootBeanDefinition definition, Element element, String propertyName, boolean required) {
        String ref = element.getAttribute(propertyName);
        if (required) {
            checkAttribute(propertyName, ref);
        }
        if (!Strings.isNullOrEmpty(ref)) {
            definition.getPropertyValues().addPropertyValue(propertyName, new RuntimeBeanReference(ref));
        }
    }

    private static void addPropertyReferenceArray(
            RootBeanDefinition definition, Element element, String elementTypeName, String propertyName, boolean required) {
        String[] refArray = Strings.split(element.getAttribute(propertyName), ',');
        List<RuntimeBeanReference> refBeanList = Lists.newArrayListWithCapacity(refArray.length);
        for (String ref : refArray) {
            ref = ref.trim();
            if (required) {
                checkAttribute(propertyName, ref);
            }
            if (!Strings.isNullOrEmpty(ref)) {
                refBeanList.add(new RuntimeBeanReference(ref));
            }
        }

        if (!refBeanList.isEmpty()) {
            ManagedArray managedArray = new ManagedArray(elementTypeName, refBeanList.size());
            managedArray.addAll(refBeanList);
            definition.getPropertyValues().addPropertyValue(propertyName, managedArray);
        }
    }

    private static String checkAttribute(String attributeName, String attribute) {
        if (Strings.isNullOrEmpty(attribute)) {
            throw new BeanDefinitionValidationException("Attribute [" + attributeName + "] is required.");
        }
        return attribute;
    }
}
