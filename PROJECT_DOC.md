# 路亚日历 (LureCalendar) 技术文档

## 1. 项目概述
**路亚日历**是一款专为路亚（Lure Fishing）爱好者设计的移动应用。它通过整合气象大数据、地理位置信息与科学的钓鱼指数算法，解决钓鱼人“何时去、去哪钓、用啥饵”的核心痛点。
- **目标用户**：路亚钓鱼爱好者、职业钓手。
- **核心价值**：通过多维气象因子分析，量化出钓成功率；通过社区分享与装备分析，提升钓技。

## 2. 功能说明
- **钓鱼指数计算**：基于气压、气温、水温、风速、降水等多维气象数据计算实时钓鱼指数。
- **逐小时/逐日预报**：展示未来数日及 24 小时内的钓鱼指数趋势及气象详情。
- **钓点地图**：集成高德地图，支持长按标记钓点、查看周边钓点及活跃钓友。
- **鱼获记录**：支持上传照片、记录鱼种、尺寸、重量、使用装备及环境数据。
- **路亚饵建议**：根据目标鱼种和当前气象条件，智能推荐匹配的假饵（米诺、亮片、软虫等）。
- **装备分析**：通过历史记录分析特定竿型的实战效率，展示重量趋势及鱼种分布。
- **钓友圈 (Moments)**：支持发布动态、点赞、评论，形成垂直社交圈。
- **排行榜/成就系统**：展示单点及全局巨物榜，通过记录解锁成就勋章。
- **智能提醒**：支持设置气象阈值（如温度、风速），符合条件时推送出钓提醒。

## 3. 系统架构
```mermaid
graph TD
    subgraph Client [前端 (Android App)]
        UI[Jetpack Compose UI]
        VM[ViewModel / Hilt]
        Room[Local SQLite / Room]
        DataStore[Preferences DataStore]
    end

    subgraph Server [后端 (FastAPI)]
        API[FastAPI Routes]
        Logic[Business Logic / Services]
        IndexCalc[Fishing Index Calculator]
    end

    subgraph Data [数据持久化]
        MySQL[SQLite / MySQL]
        Uploads[File System / Uploads]
    end

    subgraph External [外部 API]
        QWeather[和风天气 API]
        Juhe[聚合天气 API]
        AMap[高德地图 SDK]
    end

    Client -- HTTP/REST --> API
    API -- ORM --> MySQL
    API -- Save --> Uploads
    Client -- SDK --> AMap
    Client -- REST --> QWeather
    Client -- REST --> Juhe
```

## 4. 前后端技术栈
### 前端 (移动端)
- **开发语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构模式**：MVVM + Clean Architecture
- **依赖注入**：Hilt / Dagger
- **本地数据库**：Room (SQLite)
- **配置存储**：Preferences DataStore
- **网络请求**：Retrofit + OkHttp
- **第三方 SDK**：高德地图 (AMap) Android SDK
- **图表组件**：Vico
- **图片加载**：Coil

### 后端
- **开发框架**：FastAPI (Python)
- **ORM 框架**：SQLAlchemy
- **数据库**：SQLite (默认，支持迁移至 MySQL)
- **PDF 导出**：ReportLab
- **部署工具**：Uvicorn

## 5. 算法说明
### 5.1 钓鱼指数算法
系统采用多因子加权评分算法，总分 0-100。
- **主要因子与权重**：
  - 温度 (25%)：18-25℃ 为最佳区间。
  - 气压变化 (15%)：±1 hPa/h 为稳定状态，剧升或剧降会降低得分。
  - 风速 (10%)：2-4 m/s (微风) 最利于增加水中溶氧，提升活性。
  - 水温 (20%)：15-25℃ 为大多数掠食性鱼类的活跃水温。
  - 鱼种系数 (15%)：不同鱼种的摄食积极性修正。
  - 时段因子 (15%)：清晨与傍晚判定为“黄金窗口期”。

### 5.2 鱼种活跃度系数
代码实现位于 `LureCalendar-Backend/services/fishing_index.py`：
- 鲈鱼 (Bass): 0.9
- 鳜鱼 (Mandarin Fish): 0.8
- 翘嘴 (Topmouth Culter): 0.85
- 默认系数: 0.7

## 6. 数据库设计
### 核心表结构
- **`users`**：存储用户凭据、昵称、头像、勋章进度及实时共享的地理位置坐标。
- **`fishing_spots`**：存储钓点物理信息（坐标、水深）及环境特征（底质、水域类型）。
- **`catch_records`**：核心流水表，记录鱼种、尺寸、环境快照（捕获时的气温气压）及关联装备。
- **`moments`**：社交动态内容，支持多图及地理位置关联。

## 7. API 接口设计
- `POST /api/auth/login`：用户登录验证。
- `POST /api/weather/index`：综合环境因子，返回钓鱼指数及拟饵建议。
- `GET /api/spots/leaderboard`：获取指定钓点捕获重量排名 Top 10。
- `GET /api/users/locations`：获取全图在线钓友分布（根据 `share_location` 开关过滤）。
- `GET /api/gear/stats`：根据装备名称统计该装备的历史鱼获分布及重量趋势。

## 8. 前端页面与交互
- **引导页**：介绍路亚日历、地图及装备分析三大核心板块。
- **首页**：实时指数展示，集成“快速新增鱼获”与“钓点地图”入口。
- **地图页**：支持层级切换（卫星/普通）、长按打点、定位及钓友 Marker 交互。
- **成就墙**：展示用户解锁的勋章，通过实战记录自动达成。

## 9. 扩展性说明
- **新增气象因子**：可在后端 `FishingIndexCalculator` 类中注入新的计算函数并调整权重配置。
- **新增鱼种**：前端 `FishSpecies` 类增加枚举，后端 `species_activity_map` 增加活跃系数。
- **后端迁移**：通过 `migrate.py` 脚本，可以快速在 SQLite 与远程 MySQL 之间同步表结构。

## 10. 环境配置与启动
### 后端
1. 安装 Python 3.9+。
2. `pip install -r requirements.txt`。
3. `python main.py` 启动服务。

### 前端
1. 在 `local.properties` 配置 `AMAP_API_KEY`。
2. 使用 Android Studio Flamingo 以上版本编译运行。

## 11. 安全说明
- **Key 隔离**：第三方 API Key 通过 Gradle 属性或 DataStore 加密存储，不进入源码库。
- **权限控制**：地理位置共享需用户在“个人中心”主动开启，后台通过 `is_admin` 严格控制全局位置视图权限。
