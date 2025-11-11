# IDEA 项目配置指南

如果IDEA无法识别源代码目录，请按照以下步骤操作：

## 方法一：Maven 重新导入（推荐）

1. 在IDEA中打开 `Maven` 工具窗口（View -> Tool Windows -> Maven）
2. 点击刷新按钮（Reload All Maven Projects）
3. 等待Maven同步完成

## 方法二：手动标记源代码目录

如果Maven刷新后仍有问题，手动标记：

1. 右键点击项目根目录 `pipeline-framework`
2. 选择 `Open Module Settings` (或按 F4)
3. 在左侧选择 `Modules`
4. 对每个模块（如 pipeline-api, pipeline-core 等）：
   - 展开模块
   - 右键 `src/main/java` -> Mark Directory as -> Sources Root (蓝色)
   - 右键 `src/main/resources` -> Mark Directory as -> Resources Root (紫色)
   - 右键 `src/test/java` -> Mark Directory as -> Test Sources Root (绿色)
   - 右键 `src/test/resources` -> Mark Directory as -> Test Resources Root (紫色)

## 方法三：清理并重新导入

1. 关闭IDEA
2. 删除以下文件/目录：
   ```bash
   rm -rf .idea
   rm -rf */*.iml
   rm -rf *.iml
   ```
3. 重新打开IDEA
4. 选择 `File -> Open` -> 选择 `pom.xml`
5. 选择 `Open as Project`
6. 在弹出的对话框中选择 `Import Maven project automatically`

## 方法四：使用Maven命令生成IDEA配置

在项目根目录执行：

```bash
mvn idea:idea
```

然后在IDEA中重新打开项目。

## 验证

成功配置后，你应该看到：
- `src/main/java` 目录图标为蓝色（Sources）
- `src/main/resources` 目录图标为紫色（Resources）
- `src/test/java` 目录图标为绿色（Tests）
- 类文件可以正常导航和自动补全

## 常见问题

### Q: 仍然提示 "move to source root"
A: 检查 pom.xml 中的 `<packaging>` 是否为 `jar`，确保不是 `pom`

### Q: 模块间依赖无法识别
A: 确保父pom.xml和各模块pom.xml中的版本号一致，都是 `1.0.0-SNAPSHOT`

### Q: Maven依赖下载失败
A: 检查Maven settings.xml配置，确保有正确的仓库配置

---

如果以上方法都不行，请提供：
1. IDEA版本
2. Maven版本（mvn -v）
3. 具体的错误截图
