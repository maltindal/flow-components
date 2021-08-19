/*
 * Copyright 2000-2021 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.data.renderer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.provider.AbstractComponentDataGenerator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;

import elemental.json.JsonObject;

/**
 * A {@link DataGenerator} that manages the creation and passivation of
 * components generated by {@link ComponentRenderer}s. It also manages the
 * generation of the {@code nodeId} property needed by the
 * {@code flow-component-renderer} webcomponent.
 * <p>
 * This class is used internally by listing components that support
 * ComponentRenderers.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the date type
 */
public class ComponentDataGenerator<T>
        extends AbstractComponentDataGenerator<T> {

    private final ComponentRenderer<? extends Component, T> componentRenderer;
    private final ValueProvider<T, String> keyMapper;
    private String nodeIdPropertyName;
    private Element container;

    /**
     * Creates a new generator.
     *
     * @param componentRenderer
     *            the renderer used to produce components based on data items
     * @param keyMapper
     *            the DataKeyMapper used to fetch keys for items
     */
    public ComponentDataGenerator(
            ComponentRenderer<? extends Component, T> componentRenderer,
            ValueProvider<T, String> keyMapper) {
        this.componentRenderer = componentRenderer;
        this.keyMapper = keyMapper;
    }

    @Override
    public void generateData(T item, JsonObject jsonObject) {
        /*
         * If no nodeIdPropertyName set do nothing. It is supposed to be set up
         * by setupTemplateWhenAttached which is triggered through
         * setupTemplate.
         */
        if (nodeIdPropertyName == null) {
            return;
        }

        String itemKey = getItemKey(item);
        Component oldRenderedComponent = getRenderedComponent(itemKey);

        int nodeId;
        // If we have a component for the given item use that, else create new
        // component and register it.
        if (oldRenderedComponent != null) {
            nodeId = oldRenderedComponent.getElement().getNode().getId();
        } else {
            Component renderedComponent = createComponent(item);
            if (renderedComponent.getParent().isPresent()) {
                LoggerFactory.getLogger(ComponentDataGenerator.class).warn(
                        "The 'createComponent' method returned a component '{}' which already has a parent."
                                + " It means that most likely your component renderer '{}' class is implemented incorrectly",
                        renderedComponent.getClass().getName(),
                        componentRenderer.getClass().getName());
            }
            registerRenderedComponent(itemKey, renderedComponent);

            nodeId = renderedComponent.getElement().getNode().getId();
        }

        jsonObject.put(nodeIdPropertyName, nodeId);
    }

    @Override
    protected Component createComponent(T item) {
        return componentRenderer.createComponent(item);
    }

    @Override
    protected Component updateComponent(Component currentComponent, T item) {
        return componentRenderer.updateComponent(currentComponent, item);
    }

    @Override
    protected String getItemKey(T item) {
        if (keyMapper == null) {
            return null;
        }
        return keyMapper.apply(item);
    }

    @Override
    protected Element getContainer() {
        return container;
    }

    public void setContainer(Element container) {
        this.container = container;
    }

    public ComponentRenderer<? extends Component, T> getComponentRenderer() {
        return componentRenderer;
    }

    public String getNodeIdPropertyName() {
        return nodeIdPropertyName;
    }

    public void setNodeIdPropertyName(String nodeIdPropertyName) {
        this.nodeIdPropertyName = nodeIdPropertyName;
    }

}
