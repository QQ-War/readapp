# ReadApp Android

Android 客户端基于 Jetpack Compose，实现了轻阅读后端（`/api/5`）的主要能力，聚焦段落级听书与阅读体验。

## 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34

## 目录结构
- `src/main/java/com/readapp/data/model`：数据模型
- `src/main/java/com/readapp/data`：API 与本地持久化（DataStore）
- `src/main/java/com/readapp/media`：播放服务、播放器管理、TTS 预加载与缓存
- `src/main/java/com/readapp/viewmodel`：阅读/书架/设置等状态管理
- `src/main/java/com/readapp/ui`：界面与导航
- `src/main/java/com/readapp/ui/screens`：页面实现
- `src/main/java/com/readapp/ui/theme`：主题与样式

## 运行
1. 使用 Android Studio 打开 `android/`。
2. 同步 Gradle 依赖后直接运行 Debug。
3. 首次启动在登录页配置服务端地址与 accessToken。

## 构建
- Debug APK：`./gradlew assembleDebug`
- Unsigned APK：`./gradlew assembleUnsigned`
- 输出目录：`android/build/outputs/apk/`

## 功能说明
- 登录与服务端配置（支持内外网回退）
- 书架、章节与阅读页
- HTTP TTS 段落级播放
- 播放进度与段落高亮
- 预加载与缓存策略，减少段落切换卡顿
- 后台播放与系统媒体控制