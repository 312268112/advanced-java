# IDEA 刷新指南

如果遇到 "Java file is located outside of the module source root" 错误，请按以下步骤操作：

## 方法1：Maven 重新导入（推荐）⭐

1. 在IDEA中打开 **Maven** 工具窗口
   - 快捷键：`Ctrl+Shift+A`（Mac: `Cmd+Shift+A`），输入 "Maven"
   - 或者：`View` → `Tool Windows` → `Maven`

2. 点击 **🔄 刷新按钮**（Reload All Maven Projects）
   - 位于Maven工具窗口顶部
   - 等待同步完成（可能需要几分钟）

3. 如果仍有问题，执行 **Clean**
   - 右键点击项目根目录 `pipeline-framework`
   - 选择 `Maven` → `Clean`
   - 然后再次点击刷新

## 方法2：清理IDEA缓存

1. 关闭IDEA

2. 删除缓存目录
   ```bash
   cd /workspace/pipeline-framework
   rm -rf .idea/
   find . -name "*.iml" -delete
   ```

3. 重新打开IDEA
   - `File` → `Open`
   - 选择 `/workspace/pipeline-framework/pom.xml`
   - 选择 **"Open as Project"**

4. 等待IDEA索引完成

## 方法3：手动标记源代码目录

1. 右键点击 `pipeline-api` 模块

2. 选择 `Open Module Settings`（或按 `F4`）

3. 在左侧选择 `Modules`

4. 展开 `pipeline-api` 模块

5. 标记目录：
   - 右键 `src/main/java` → `Mark Directory as` → **`Sources Root`** (蓝色图标)
   - 右键 `src/main/resources` → `Mark Directory as` → **`Resources Root`** (紫色图标)
   - 右键 `src/test/java` → `Mark Directory as` → **`Test Sources Root`** (绿色图标)
   - 右键 `src/test/resources` → `Mark Directory as` → **`Test Resources Root`** (紫色图标)

6. 对所有模块重复上述步骤

## 方法4：使用Maven命令生成配置

在项目根目录执行：

```bash
cd /workspace/pipeline-framework
mvn idea:idea
```

然后在IDEA中重新打开项目。

## 方法5：强制刷新

1. 在IDEA中按 `Ctrl+Alt+Shift+/`（Mac: `Cmd+Alt+Shift+/`）

2. 选择 **"Invalidate Caches"**

3. 在弹出的对话框中选择：
   - ☑️ Invalidate and Restart
   - ☑️ Clear file system cache and Local History
   - ☑️ Clear VCS Log caches and indexes

4. 点击 **"Invalidate and Restart"**

5. 等待IDEA重启并重新索引

## 验证成功

成功配置后，你应该看到：

✅ `src/main/java` 目录显示为 **蓝色** 图标（Sources Root）
✅ `src/main/resources` 目录显示为 **紫色** 图标（Resources Root）
✅ `src/test/java` 目录显示为 **绿色** 图标（Test Sources Root）
✅ Java文件可以正常跳转和自动补全
✅ 不再出现 "outside of the module source root" 警告

## 常见问题

### Q: 刷新后还是报错？
A: 
1. 确认JDK版本是17或更高
2. 检查 `File` → `Project Structure` → `Project` → `SDK` 是否正确
3. 确认Maven配置正确：`File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

### Q: 某些子包显示错误？
A: 
1. 检查包名是否正确（不能有空格或特殊字符）
2. 确认目录下有 `.java` 文件
3. 重新标记 `src/main/java` 为 Sources Root

### Q: Maven依赖下载失败？
A: 
1. 检查网络连接
2. 配置Maven镜像（如阿里云镜像）
3. 清理本地仓库：`rm -rf ~/.m2/repository/com/pipeline`

### Q: 模块之间的依赖无法识别？
A: 
1. 确保父 `pom.xml` 中的 `<modules>` 列表正确
2. 确保各模块的 `pom.xml` 中的依赖版本一致
3. 执行 `mvn clean install` 重新构建

## 快速命令

```bash
# 一键修复（推荐）
cd /workspace/pipeline-framework
rm -rf .idea/
find . -name "*.iml" -delete
# 然后在IDEA中重新打开项目

# 清理并重新构建
mvn clean install -DskipTests

# 生成IDEA配置
mvn idea:idea

# 查看模块结构
ls -d */src/main/java
```

## 截图示例

正确配置后的目录结构应该是：

```
pipeline-api/
  src/main/java/           [蓝色图标]
    com/pipeline/framework/api/
      connector/
        ✅ Connector.java
        ✅ ConnectorReader.java
        ✅ ConnectorWriter.java
        adapter/
          ✅ ConnectorAdapter.java
        factory/
          ✅ ConnectorFactory.java
```

---

如果以上方法都不行，请提供：
1. IDEA版本
2. JDK版本
3. Maven版本
4. 具体的错误截图

---

**最后更新**：2025-11-10
