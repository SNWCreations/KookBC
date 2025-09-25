/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * KookBC 性能基准测试运行器
 *
 * 统一执行所有基准测试并生成性能报告
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        System.out.println("=".repeat(80));
        System.out.println("KookBC 高性能重构 - 性能基准测试");
        System.out.println("测试时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Java 版本: " + System.getProperty("java.version"));
        System.out.println("JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        System.out.println("操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("CPU 核心数: " + Runtime.getRuntime().availableProcessors());
        System.out.println("最大内存: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        System.out.println("=".repeat(80));
        System.out.println();

        // 检查是否在支持虚拟线程的 Java 版本上运行
        checkVirtualThreadSupport();

        // 根据命令行参数选择测试套件
        String testSuite = args.length > 0 ? args[0] : "all";

        switch (testSuite.toLowerCase()) {
            case "virtual":
                runVirtualThreadTests();
                break;
            case "json":
                runJsonProcessingTests();
                break;
            case "system":
                runSystemTests();
                break;
            case "quick":
                runQuickTests();
                break;
            case "all":
            default:
                runAllTests();
                break;
        }
    }

    private static void checkVirtualThreadSupport() {
        try {
            // 尝试创建虚拟线程
            Thread.ofVirtual().start(() -> {}).join();
            System.out.println("✅ 虚拟线程支持: 已启用");
        } catch (Exception e) {
            System.out.println("❌ 虚拟线程支持: 未启用 - " + e.getMessage());
            System.out.println("请使用 Java 21+ 版本运行基准测试");
            System.exit(1);
        }
        System.out.println();
    }

    /**
     * 运行虚拟线程性能测试
     */
    private static void runVirtualThreadTests() throws RunnerException {
        System.out.println("🚀 执行虚拟线程性能测试...");
        System.out.println();

        Options opt = new OptionsBuilder()
                .include(VirtualThreadBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx2g", "-Xms1g")
                .result("benchmark-results-virtual-threads.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }

    /**
     * 运行 JSON 处理性能测试
     */
    private static void runJsonProcessingTests() throws RunnerException {
        System.out.println("📊 执行 JSON 处理性能测试...");
        System.out.println();

        Options opt = new OptionsBuilder()
                .include(JsonProcessingBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx2g", "-Xms1g")
                .result("benchmark-results-json-processing.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }

    /**
     * 运行系统整体性能测试
     */
    private static void runSystemTests() throws RunnerException {
        System.out.println("🏗️ 执行系统整体性能测试...");
        System.out.println();

        Options opt = new OptionsBuilder()
                .include(SystemPerformanceBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx4g", "-Xms2g") // 系统测试需要更多内存
                .result("benchmark-results-system-performance.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }

    /**
     * 运行快速测试（较少的迭代次数）
     */
    private static void runQuickTests() throws RunnerException {
        System.out.println("⚡ 执行快速性能测试...");
        System.out.println();

        Options opt = new OptionsBuilder()
                .include(".*Benchmark")
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx2g", "-Xms1g")
                .result("benchmark-results-quick.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }

    /**
     * 运行所有性能测试
     */
    private static void runAllTests() throws RunnerException {
        System.out.println("🔥 执行完整性能基准测试套件...");
        System.out.println("这可能需要较长时间，请耐心等待...");
        System.out.println();

        Options opt = new OptionsBuilder()
                .include(".*Benchmark")
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx4g", "-Xms2g")
                .result("benchmark-results-complete.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        long startTime = System.currentTimeMillis();
        new Runner(opt).run();
        long endTime = System.currentTimeMillis();

        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("🎉 性能基准测试完成！");
        System.out.println("总耗时: " + (endTime - startTime) / 1000 + " 秒");
        System.out.println("结果已保存到: benchmark-results-complete.json");
        System.out.println("=".repeat(80));
    }
}