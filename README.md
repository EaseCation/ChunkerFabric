# ChunkerFabric

基于 Chunker 的 Fabric 服务端地图转换 Mod。

## 功能

在 Fabric 服务端中提供地图转换命令：

- **导入** - 将外部世界（Java/Bedrock）导入到服务器
- **导出** - 将当前世界导出为 Bedrock 格式
- **复制** - 复制当前世界到新维度

## 版本要求

| 组件 | 版本 |
|------|------|
| Minecraft | 1.20.4 |
| Fabric Loader | 0.16.10+ |
| Fabric API | 0.97.2+ |
| Java | 17+ |

## 命令

### 导入世界

```
/chunker import <namespace:path> <源路径>
```

- `namespace:path` - 新世界的标识符（如 `minecraft:my_world`）
- `源路径` - 外部世界的文件路径

### 导出世界

```
/chunker export
```

将当前所在世界导出到 `chunker_export/<时间戳>/` 目录。

### 复制世界

```
/chunker copy <namespace:path>
```

复制当前世界到新的维度。

## 特性

- **网易版支持** - 默认启用 `preserveUnknownEntities` 和 `preserveUnknownBlockEntities`
- **Multiworld 集成** - 自动创建 multiworld 配置文件
- **异步转换** - 不阻塞服务器主线程
- **实时进度** - 每 500ms 更新转换进度

## 构建

### 前置条件

先构建 ChunkerPatch：

```bash
cd ../ChunkerPatch/Chunker
git apply ../0001-Implement-netease-support.patch
./gradlew :cli:publishToMavenLocal
```

### 构建 Mod

```bash
cd ChunkerFabric
./gradlew build
```

构建产物在 `build/libs/chunker-fabric-1.0.0.jar`。

## 配置

Mod 会自动检测 multiworld 插件并创建相应配置。配置文件位于：

```
config/multiworld/worlds/<namespace>/<path>.yml
```

## 依赖

- [Chunker](https://github.com/HiveGamesOSS/Chunker) - 核心转换库
- [ChunkerPatch](../ChunkerPatch) - 网易版支持补丁
- [Fabric Language Kotlin](https://github.com/FabricMC/fabric-language-kotlin) - Kotlin 支持

## 相关项目

- [ChunkerPatch](../ChunkerPatch) - Chunker 补丁项目
- [ChunkerSpigot](../ChunkerSpigot) - Spigot 版本
