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

/**
 * 默认的注册中心稳定性方面基本上跟纸糊的一样, 且用且小心.
 * 之所以实现一个默认的注册中心, 主要是因为不想依赖太多第三方的东西, 最好还是用redis或者zookeeper实现一个.
 *
 * jupiter
 * org.jupiter.hot.exec
 *
 * @author jiachun.fjc
 */
package org.jupiter.registry;
