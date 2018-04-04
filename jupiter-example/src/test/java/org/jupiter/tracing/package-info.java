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
 * OpenTracing example:
 *
 * Client1 --> Server1AndClient2 --> Server2
 *
 * 1. 先启动 JupiterRegistryServer
 * 2. 再启动 Server2
 * 3. 接着启动 Server1AndClient2
 * 4. 最后启动 Client1
 */
package org.jupiter.tracing;
