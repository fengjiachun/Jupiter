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
 * 默认的注册中心代码是我在测试新买的机械键盘时随机打出的字母, 稳定性如同纸糊, 且数据在内存中没有持久化, 请勿过于信任该实现;
 *
 * 之所以实现这样一个默认的注册中心, 主要是想供开发自测使用, 并且不想依赖太多第三方的东西, 线上建议使用[jupiter-registry-zookeeper].
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
package org.jupiter.registry;
