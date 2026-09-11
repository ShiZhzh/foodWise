# 🥗 FoodWise

> **AI-Powered Smart Dietary Management App**
> **AI 驱动的智能饮食管理 Android 应用**

FoodWise 是一款面向日常饮食与健康管理的 Android 应用，融合 **食物图像识别、菜单识别、饮食记录、健康数据管理、个性化 AI 推荐与智能饮食 Agent**，帮助用户更方便地了解“吃了什么、摄入多少、应该怎么吃”。

FoodWise is an Android application for intelligent dietary and health management. It integrates **food image recognition, menu recognition, dietary tracking, health management, personalized AI recommendations, and a context-aware dietary AI Agent**.

> 🏆 软件创新大赛项目（华东赛区省三）
> Competition project — award information follows the original repository record.

---

## 📖 项目简介 / Overview

传统饮食管理应用通常依赖用户手动搜索食物并记录热量，操作成本较高，同时缺少针对个人饮食状况的动态分析。

FoodWise 尝试将计算机视觉、数据分析和 AI 交互结合起来，形成从：

```text
食物识别
   ↓
营养 / 饮食分析
   ↓
每日摄入记录
   ↓
健康数据管理
   ↓
个性化推荐
   ↓
AI 饮食助手
```

的一体化饮食管理流程。

Traditional dietary-management applications often require users to manually search for foods and record nutritional information.

FoodWise combines computer vision, dietary data analysis, and AI-assisted interaction to provide an integrated workflow:

```text
Food Recognition
      ↓
Diet Analysis
      ↓
Daily Intake Tracking
      ↓
Health Management
      ↓
Personalized Recommendation
      ↓
AI Dietary Assistant
```

---

## ✨ 核心功能 / Key Features

### 📷 1. 菜品识别 / Dish Recognition

用户可以选择食物图片，由系统发送至后端识别服务进行菜品分析。

Users can select a food image and submit it to the backend recognition service for dish identification and analysis.

主要流程：

```text
Food Image / 食物图片
        ↓
Image Encoding
        ↓
Recognition API
        ↓
Dish Information
        ↓
Dietary Analysis
```

相关实现：

```text
DishRecognitionActivity.kt
DishDetailActivity.kt
```

---

### 📋 2. 菜单识别 / Menu Recognition

FoodWise 支持针对菜单内容进行识别和解析，并将识别得到的菜品信息进一步用于饮食建议。

FoodWise supports menu recognition and parsing, allowing detected dishes and OCR information to be used as contextual evidence for dietary recommendations.

相关实现：

```text
MenuRecognitionActivity.kt
MenuDishAdapter.kt
```

---

### 🔍 3. 饮食分析 / Food Analysis

系统提供独立的饮食分析页面，用于组织和展示与食物及饮食相关的信息。

A dedicated food-analysis module is provided to organize and present food-related information.

```text
FoodAnalysisActivity.kt
```

---

### 🤖 4. AI 个性化菜品推荐 / AI Food Recommendation

FoodWise 提供基于 AI 的个性化菜品推荐功能。

用户可以根据：

* 菜品类型
* 指定食材
* 个人备注
* 用户信息

获得相应推荐。

FoodWise provides AI-assisted food recommendations based on information such as:

* Dish category
* Ingredients
* User preferences or notes
* User context

推荐模块支持不同推荐入口，并与后端 AI 服务交互。

```text
FoodRecommendActivity.kt
DietRecommendCache.kt
```

---

### 🍽️ 5. 每日饮食记录 / Daily Intake Tracking

用户可以记录每日摄入的食物及对应热量。

Users can record foods consumed during the day together with their calorie values.

支持：

* 添加饮食记录
* 查看今日摄入
* 统计今日总热量
* 删除单条记录
* 清空当日记录
* 与服务器同步数据

Features include:

* Add intake records
* View today's intake
* Calculate total daily calories
* Delete individual records
* Clear today's records
* Synchronize records with the backend

```text
TodayIntakeActivity.kt
```

---

### 📈 6. 卡路里趋势可视化 / Calorie Trend Visualization

应用主页使用折线图展示近期卡路里摄入趋势。

The home page visualizes recent calorie-intake trends with an interactive line chart.

```text
Recent Calorie Intake
         │
         ▼
┌───────────────────────┐
│   Calorie History     │
│   近 7 天摄入趋势      │
│                       │
│       ╭──╮            │
│   ╭───╯  ╰──╮         │
│ ──╯          ╰──      │
└───────────────────────┘
```

支持拖动、缩放以及趋势查看。

The chart supports interaction such as dragging, scaling, and trend inspection.

---

### ❤️ 7. 健康管理 / Health Management

FoodWise 将饮食信息进一步与健康管理结合。

FoodWise extends dietary tracking into personal health management.

目前包含：

* 每日体重记录
* 历史体重查询
* 近 30 天体重趋势
* 体重统计
* AI 饮食推荐

Current functionality includes:

* Daily weight recording
* Weight history
* 30-day weight trends
* Weight statistics
* AI dietary recommendations

```text
HealthManagementActivity.kt
```

---

## 🧠 AI Dietary Agent / 智能饮食 Agent

FoodWise 进一步实现了一个具有**页面上下文感知能力**的饮食 Agent。

Instead of providing only a standalone chatbot, FoodWise implements a dietary Agent that can incorporate the context of the page from which the user starts the conversation.

整体结构：

```text
                User / 用户
                     │
                     ▼
              Current Page
                当前页面
                     │
                     ▼
          ┌────────────────────┐
          │ Context Resolver   │
          │ 页面上下文解析      │
          └─────────┬──────────┘
                    │
                    ▼
          ┌────────────────────┐
          │ Route Selector     │
          │ Agent 路由选择      │
          └─────────┬──────────┘
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
       Dish       Menu      General
      Advice     Advice       Chat
       菜品        菜单       通用问答
          │         │         │
          └─────────┼─────────┘
                    ▼
              AI Backend
                    │
                    ▼
          Personalized Advice
             个性化饮食建议
```

Agent 可以根据当前页面环境选择不同的处理路径，例如：

* 基于当前菜品进行建议
* 基于菜单识别结果进行建议
* 普通饮食健康对话

The Agent can select different routes according to the current application context:

* Advice based on recognized dishes
* Advice based on menu/OCR results
* General dietary conversation

主要相关模块：

```text
AgentApiClient.kt
AgentChatActivity.kt
AgentChatAdapter.kt
AgentChatMessage.kt

AgentContextProvider.kt
AgentContextStore.kt
AgentPageContext.kt
PageContextResolver.kt

AgentRouteSelector.kt
AgentEntryBinder.kt
```

---

## 🏗️ 系统架构 / Architecture

```text
┌─────────────────────────────────────┐
│         FoodWise Android App        │
│                                     │
│  Login / Register                   │
│  Dish & Menu Recognition            │
│  Food Recommendation                │
│  Intake Tracking                    │
│  Health Management                  │
│  Dietary Agent                      │
└─────────────────┬───────────────────┘
                  │
                  │ HTTP / JSON
                  ▼
┌─────────────────────────────────────┐
│             Backend API             │
│                                     │
│  Authentication                     │
│  User Data                          │
│  Food Recognition                   │
│  Dietary Records                    │
│  AI Recommendation                  │
│  Agent Services                     │
└─────────────────┬───────────────────┘
                  │
                  ▼
        AI / Recognition Services
```

---

## 🛠️ 技术栈 / Tech Stack

| Category / 类别  | Technology / 技术                      |
| -------------- | ------------------------------------ |
| Platform       | Android                              |
| Language       | Kotlin                               |
| Build System   | Gradle Kotlin DSL                    |
| UI             | Android XML / Material Components    |
| Networking     | OkHttp                               |
| JSON           | Gson / `org.json`                    |
| Visualization  | MPAndroidChart                       |
| Local State    | SharedPreferences                    |
| Architecture   | Activity-based modular design        |
| AI Integration | Backend AI APIs                      |
| Agent          | Context-aware routing & conversation |

---

## 📱 Android 配置 / Android Configuration

当前项目配置：

```text
minSdk      24
targetSdk   36
compileSdk  36
version     1.0
```

项目采用 Java 11 兼容配置。

The project uses Java 11 compatibility settings.

---

## 📁 项目结构 / Project Structure

```text
foodWise/
│
├── app/
│   ├── src/main/
│   │   │
│   │   ├── java/com/example/food_project/
│   │   │   │
│   │   │   ├── MainActivity.kt
│   │   │   │
│   │   │   ├── LoginActivity.kt
│   │   │   ├── RegisterActivity.kt
│   │   │   ├── UserDetailActivity.kt
│   │   │   │
│   │   │   ├── DishRecognitionActivity.kt
│   │   │   ├── MenuRecognitionActivity.kt
│   │   │   ├── DishDetailActivity.kt
│   │   │   ├── FoodAnalysisActivity.kt
│   │   │   ├── FoodRecommendActivity.kt
│   │   │   │
│   │   │   ├── TodayIntakeActivity.kt
│   │   │   ├── HealthManagementActivity.kt
│   │   │   │
│   │   │   ├── AgentChatActivity.kt
│   │   │   ├── AgentApiClient.kt
│   │   │   ├── AgentRouteSelector.kt
│   │   │   ├── AgentContextProvider.kt
│   │   │   ├── AgentContextStore.kt
│   │   │   ├── AgentPageContext.kt
│   │   │   ├── PageContextResolver.kt
│   │   │   ├── AgentEntryBinder.kt
│   │   │   │
│   │   │   ├── ApiHelper.kt
│   │   │   └── DietRecommendCache.kt
│   │   │
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   ├── anim/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

---

## 🚀 快速开始 / Getting Started

### 1. 克隆项目 / Clone

```bash
git clone https://github.com/ShiZhzh/foodWise.git
cd foodWise
```

### 2. 使用 Android Studio 打开 / Open with Android Studio

推荐直接在 Android Studio 中打开项目根目录。

Open the repository root directory with Android Studio.

等待 Gradle 完成依赖同步：

Wait for Gradle synchronization to complete.

```text
Gradle Sync → Build → Run
```

---

### 3. 运行应用 / Run

选择 Android Emulator 或 Android 真机后运行：

Select an Android emulator or physical device and run:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
gradlew.bat assembleDebug
```

也可以直接使用 Android Studio 的 **Run ▶**。

Alternatively, use **Run ▶** directly in Android Studio.

---

## 🌐 后端配置 / Backend Configuration

> **Important / 重要**

当前仓库主要包含 **Android 客户端代码**。

This repository primarily contains the **Android client**.

部分功能依赖外部后端服务，包括：

Several features require the corresponding backend service:

* 用户登录与注册 / Authentication
* 菜品识别 / Dish recognition
* 菜单识别 / Menu recognition
* 摄入记录同步 / Intake synchronization
* 体重数据 / Weight data
* AI 菜品推荐 / AI recommendations
* AI Agent / Dietary Agent

后端地址统一在：

```text
app/src/main/java/com/example/food_project/ApiHelper.kt
```

中配置。

当前 Android Emulator 开发环境默认使用：

```kotlin
private const val BASE_URL = "http://10.0.2.2:5000"
```

其中：

```text
10.0.2.2
```

是 Android Emulator 访问宿主机 `localhost` 的特殊地址。

When using the Android Emulator, `10.0.2.2` refers to the host machine's localhost.

如果使用真机，请将其替换为运行后端服务的局域网 IP 或服务器地址。

For a physical Android device, replace it with the LAN IP or server address of the backend service.

---

## 🔐 权限 / Permissions

应用需要网络访问权限：

The application requires Internet access:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

为了实现图片选择与菜品识别，同时声明了对应的媒体 / 存储读取权限。

Media/storage permissions are also declared for image selection and food recognition.

---

## 🔄 典型使用流程 / Typical Workflow

```text
Login / 登录
    │
    ▼
Home / 首页
    │
    ├──► Food Recognition / 菜品识别
    │
    ├──► Menu Recognition / 菜单识别
    │
    ├──► Food Analysis / 饮食分析
    │
    ├──► AI Recommendation / AI推荐
    │
    ├──► Daily Intake / 今日摄入
    │
    └──► Health Management / 健康管理
                    │
                    ▼
             Dietary Agent
             智能饮食助手
```

---

## 🎯 项目目标 / Project Goals

FoodWise 的目标不是简单提供一个“食物识别工具”，而是探索如何将：

* 移动端应用
* 食物视觉识别
* 饮食数据管理
* 健康数据可视化
* AI 个性化推荐
* 上下文感知 Agent

整合到统一的智能饮食管理场景中。

FoodWise is more than a standalone food-recognition demo. It explores how to integrate:

* Mobile applications
* Visual food recognition
* Dietary data management
* Health-data visualization
* Personalized AI recommendations
* Context-aware AI Agents

into a unified intelligent dietary-management experience.

---

## ⚠️ 说明 / Disclaimer

本项目主要用于软件创新、学习与实验目的。

This project is intended primarily for software innovation, learning, and experimental purposes.

应用中的饮食、营养与健康建议不应被视为专业医疗诊断或治疗建议。

Dietary, nutritional, or health-related suggestions generated by the application should not be considered professional medical diagnosis or treatment advice.

---

## 📌 Repository

**ShiZhzh / foodWise**

```text
https://github.com/ShiZhzh/foodWise
```

---

<p align="center">
  <b>Eat Smarter. Live Better.</b>
  <br/>
  <b>让每一次饮食选择，都更加智慧。</b>
</p>
