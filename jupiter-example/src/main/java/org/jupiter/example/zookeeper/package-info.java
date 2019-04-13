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
 * 使用zookeeper作为注册中心的example
 *
 * RegistryServer是基于SPI的, 使用zookeeper的话maven引入jupiter-registry-zookeeper即可
 *
 * 1. maven引入jupiter-registry-zookeeper
 * 2. 先启动外部的zookeeper
 * 3. 再启动JupiterServer
 * 4. 最后启动JupiterClient
 */
package org.jupiter.example.zookeeper;
