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

package org.jupiter.common.util;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Formatter;

import static org.jupiter.common.util.SystemPropertyUtil.*;

/**
 * Jupiter constants.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class JConstants {

    /** 换行符 */
    public static final String NEWLINE;
    /** 字符编码 */
    public static final String UTF8_CHARSET = "UTF-8";
    public static final Charset UTF8;
    static {
        String newLine;
        try {
            newLine = new Formatter().format("%n").toString();
        } catch (Exception e) {
            newLine = "\n";
        }
        NEWLINE = newLine;

        Charset charset = null;
        try {
            charset = Charset.forName(UTF8_CHARSET);
        } catch (UnsupportedCharsetException ignored) {}
        UTF8 = charset;
    }

    /** Cpu核心数 */
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    /** 未知应用名称 */
    public static final String UNKNOWN_APP_NAME = "UNKNOWN";
    /** 服务默认组别 */
    public static final String DEFAULT_GROUP = "Jupiter";
    /** 服务默认版本号 */
    public static final String DEFAULT_VERSION = "1.0.0";
    /** 默认的调用超时时间为3秒 **/
    public static final long DEFAULT_TIMEOUT = getInt("jupiter.rpc.invoke.timeout", 3 * 1000);
    /** Server链路read空闲检测, 默认60秒, 60秒没读到任何数据会强制关闭连接 */
    public static final int READER_IDLE_TIME_SECONDS = getInt("jupiter.io.reader.idle.time.seconds", 60);
    /** Client链路write空闲检测, 默认30秒, 30秒没有向链路中写入任何数据时Client会主动向Server发送心跳数据包 */
    public static final int WRITER_IDLE_TIME_SECONDS = getInt("jupiter.io.writer.idle.time.seconds", 30);

    /** Load balancer 默认预热时间 **/
    public static final int DEFAULT_WARM_UP = getInt("jupiter.rpc.load-balancer.warm-up", 10 * 60 * 1000);
    /** Load balancer 默认权重 **/
    public static final int DEFAULT_WEIGHT = getInt("jupiter.rpc.load-balancer.default.weight", 50);
    /** Load balancer 最大权重 **/
    public static final int MAX_WEIGHT = getInt("jupiter.rpc.load-balancer.max.weight", 100);

    /** Suggest that the count of connections **/
    public static final int DEFAULT_CONNECTION_COUNT = getInt("jupiter.rpc.suggest.connection.count", AVAILABLE_PROCESSORS);

    /** Processor.executor 核心线程数 **/
    public static final int PROCESSOR_CORE_WORKERS = getInt("jupiter.processor.executor.core.workers", AVAILABLE_PROCESSORS << 1);
    /** Processor.executor 最大线程数的限制 **/
    public static final int PROCESSOR_MAX_WORKS = getInt("jupiter.processor.executor.max.workers", 512);
    /** Processor.executor 队列的容量限制 **/
    public static final int PROCESSOR_WORKER_QUEUE_CAPACITY = getInt("jupiter.processor.executor.queue.capacity", 32768);

    /** Metrics csv reporter */
    public static final boolean METRIC_CSV_REPORTER = getBoolean("jupiter.metric.csv.reporter", false);
    /** Metrics csv reporter directory */
    public static final String METRIC_CSV_REPORTER_DIRECTORY = get("jupiter.metric.csv.reporter.directory", get("user.dir"));
    /** Metrics reporter period */
    public static final int METRIC_REPORT_PERIOD = getInt("jupiter.metric.report.period", 15);
}
