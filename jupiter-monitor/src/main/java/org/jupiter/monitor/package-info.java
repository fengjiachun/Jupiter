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
 * 这是个监控模块, 它还是个孩子, 还不够成熟, 只是来实验一种思路, 我期望它本身对其他模块是不侵入的,
 * 你只要在同一个java进程内启动它, 它就可以自动监控jupiter其他模块.
 */
package org.jupiter.monitor;
