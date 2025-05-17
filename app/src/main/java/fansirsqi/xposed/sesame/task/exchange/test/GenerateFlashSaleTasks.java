package fansirsqi.xposed.sesame.task.exchange.test;

import fansirsqi.xposed.sesame.task.exchange.BaseFlashSaleTask.ExchangeItem;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ 三、如何使用这个工具类？
 * 步骤如下：
 * 在你的模块中添加依赖：
 * 确保 BaseFlashSaleTask.java 已存在
 * 添加 ModelGroup, CompletedKeyEnum, BooleanModelField 等基础类
 * 编写测试类或脚本，调用 generate(...) 方法
 * 将生成的字符串保存为 .java 文件即可
 */
public class GenerateFlashSaleTasks {

    public static void main(String[] args) {
        // 示例：青春特权大额兑换
        List<ExchangeItem> privilegeLargeItems = new ArrayList<>();
        privilegeLargeItems.add(new ExchangeItem("large1", 20, 20));
        privilegeLargeItems.add(new ExchangeItem("large2", 50, 50));
        privilegeLargeItems.add(new ExchangeItem("large3", 100, 100));

        String privilegeLargeCode = FlashSaleTemplateGenerator.generate(
                "PrivilegeLargeEX",
                ModelGroup.PrivilegeEX,
                privilegeLargeItems,
                true,
                10
        );

        writeToFile("PrivilegeLargeEX.java", privilegeLargeCode);

        // 示例：健康岛兑换
        List<ExchangeItem> neverLandItems = new ArrayList<>();
        neverLandItems.add(new ExchangeItem("neverland1", 0.5, 150));
        neverLandItems.add(new ExchangeItem("neverland2", 1.0, 300));
        neverLandItems.add(new ExchangeItem("neverland3", 2.0, 600));

        String neverLandCode = FlashSaleTemplateGenerator.generate(
                "NeverLandEX",
                ModelGroup.NeverLandEX,
                neverLandItems,
                true,
                10
        );

        writeToFile("NeverLandEX.java", neverLandCode);
    }

    private static void writeToFile(String filename, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(content);
            System.out.println("✅ 已生成文件：" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
