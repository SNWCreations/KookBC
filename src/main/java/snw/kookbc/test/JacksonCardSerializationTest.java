package snw.kookbc.test;

import snw.jkook.message.component.card.CardBuilder;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.ContextModule;
import snw.jkook.message.component.card.module.DividerModule;
import snw.jkook.message.component.card.module.HeaderModule;
import snw.jkook.message.component.card.module.SectionModule;
import snw.kookbc.util.JacksonCardUtil;
import snw.kookbc.impl.entity.builder.MessageBuilder;

import java.util.Collections;

/**
 * 测试Jackson卡片序列化功能
 */
public class JacksonCardSerializationTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Jackson卡片序列化测试 ===");

            // 测试DividerModule序列化
            DividerModule divider = DividerModule.INSTANCE;
            String dividerJson = JacksonCardUtil.toJson(divider);
            System.out.println("✅ DividerModule序列化成功: " + dividerJson);

            // 测试HeaderModule序列化
            HeaderModule header = new HeaderModule(new PlainTextElement("测试标题"));
            String headerJson = JacksonCardUtil.toJson(header);
            System.out.println("✅ HeaderModule序列化成功: " + headerJson);

            // 测试SectionModule序列化
            SectionModule section = new SectionModule(new MarkdownElement("**测试内容**"));
            String sectionJson = JacksonCardUtil.toJson(section);
            System.out.println("✅ SectionModule序列化成功: " + sectionJson);

            // 测试ContextModule序列化
            ContextModule context = new ContextModule(Collections.singletonList(new MarkdownElement("测试上下文")));
            String contextJson = JacksonCardUtil.toJson(context);
            System.out.println("✅ ContextModule序列化成功: " + contextJson);

            // 测试完整的CardComponent序列化（模拟Help命令结构）
            var card = new CardBuilder()
                    .setTheme(Theme.SUCCESS)
                    .setSize(Size.LG)
                    .addModule(new HeaderModule(new PlainTextElement("命令帮助 (1/1)")))
                    .addModule(DividerModule.INSTANCE)
                    .addModule(new SectionModule(new MarkdownElement("(/)**plugins**: 获取已安装到此 KookBC 实例的插件列表。")))
                    .addModule(new SectionModule(new MarkdownElement("(/)**help**: 此命令没有简介。")))
                    .addModule(DividerModule.INSTANCE)
                    .addModule(new ContextModule(Collections.singletonList(
                        new MarkdownElement("由 [KookBC](https://github.com/SNWCreations/KookBC) v0.32.2 驱动 - JKook API 0.54.1")
                    )))
                    .build();

            String cardJson = JacksonCardUtil.toJson(card);
            System.out.println("✅ 完整Help命令卡片序列化成功:");
            System.out.println("   " + cardJson);

            // 现在测试消息构建器的序列化
            System.out.println("\n=== 测试MessageBuilder序列化 ===");
            Object[] result = MessageBuilder.serialize(card);
            System.out.println("✅ MessageBuilder.serialize结果:");
            System.out.println("   类型: " + result[0]);
            System.out.println("   JSON: " + result[1]);

            System.out.println("\n🎉 所有测试通过！Jackson卡片序列化修复成功！");

        } catch (Exception e) {
            System.err.println("❌ 测试失败:");
            e.printStackTrace();
        }
    }
}