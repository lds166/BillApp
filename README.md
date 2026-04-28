# 随手记 - Android 记账应用

一款基于 Jetpack Compose 的个人记账应用，支持账单记录、消费统计、日历视图、预算管理等功能。

## 功能特性

### 记账
- 首页快捷入口，一键选择常用分类快速记账
- 底部弹窗式记账界面，支持金额输入、分类选择、备注添加
- 账单编辑功能，长按账单可进入批量删除模式
- 账单列表展示分类图标、备注、时间和金额

### 统计
- 支持按周/月切换统计周期
- 折线图展示每日消费趋势
- 饼图展示分类消费占比
- 点击图例可下钻查看某分类的具体账单明细

### 日历
- 月度日历视图，消费热力图展示每日消费高低
- 点击日期可查看当日账单详情
- 支持前后月份切换浏览

### 预算管理
- 支持设置每日预算
- 首页卡片展示预算进度条（绿色→橙色→红色）
- 超支时顶部显示红色横幅提醒
- 系统通知提醒：消费达 80% 预警，超 100% 超支通知

### 其他
- 深色/浅色主题自动适配
- 数据清空功能（带确认弹窗）

## 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | 声明式 UI 框架 |
| Material 3 | UI 组件库 |
| Room | 本地数据库 |
| DataStore Preferences | 轻量配置存储 |
| Flow + StateFlow | 响应式数据流 |
| ViewModel | 状态管理 |
| KSP | 注解处理 |

## 项目结构

```
app/src/main/java/com/xuri/billapp/
├── MainActivity.kt                    # 应用入口 Activity
├── data/                              # 数据层
│   ├── Bill.kt                        # 账单实体类
│   ├── BillDao.kt                     # 账单数据库访问接口
│   ├── BillDatabase.kt                # Room 数据库单例
│   ├── BillRepository.kt              # 数据仓库，协调各 DAO
│   ├── BudgetManager.kt               # 预算管理（DataStore 持久化）
│   ├── Category.kt                    # 分类实体类
│   ├── CategoryDao.kt                 # 分类数据库访问接口
│   └── CategoryPreset.kt              # 8 个预置支出分类
├── ui/                                # UI 层
│   ├── AddBillDialog.kt               # 新增账单底部弹窗
│   ├── BillItem.kt                    # 账单列表项组件
│   ├── BillPage.kt                    # 记账页（三段式布局）
│   ├── CalendarPage.kt                # 日历视图页
│   ├── MainScreen.kt                  # 主屏幕 + 底部导航
│   ├── QuickEntryBar.kt               # 快捷记账入口栏
│   ├── StatsCard.kt                   # 今日消费卡片
│   ├── StatsPage.kt                   # 统计页
│   ├── components/                    # 自定义图表组件
│   │   ├── CalendarView.kt            # 日历组件（热力图）
│   │   ├── LineChart.kt               # 折线图组件
│   │   └── PieChart.kt                # 饼图组件
│   └── theme/                         # 主题相关
│       ├── Color.kt                   # 颜色定义
│       ├── Theme.kt                   # 主题配置
│       └── Type.kt                    # 字体排版
├── util/                              # 工具类
│   └── NotificationHelper.kt          # 预算预警通知
└── viewmodel/                         # ViewModel 层
    └── BillViewModel.kt               # 全局 ViewModel，状态管理核心
```

## 架构

采用 **MVVM + Repository** 架构：

```
UI (Compose) → ViewModel (StateFlow) → Repository → DAO → Room Database
                                         → BudgetManager (DataStore)
                                         → NotificationHelper
```

- **UI 层**：Jetpack Compose 声明式组件
- **ViewModel**：持有 StateFlow 数据流，处理业务逻辑，协调数据层
- **Repository**：抽象数据访问，委托给对应 DAO
- **数据层**：Room 数据库 + DataStore 配置存储

## 数据库设计

### bills 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (主键) | 自增 ID |
| amount | Double | 金额 |
| category_id | Long (外键) | 关联分类 |
| note | String | 备注 |
| date | Long | 时间戳 |
| created_at | Long | 创建时间 |

### categories 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (主键) | 自增 ID |
| name | String | 分类名称 |
| icon | String | 图标名称 |
| type | Enum | EXPENSE(支出) / INCOME(收入) |

### 预置分类

餐饮、购物、交通、娱乐、居住、医疗、教育、其他

## 构建与运行

### 环境要求

- Android Studio（推荐最新稳定版）
- JDK 17
- 最低运行版本：Android 8.0 (API 26)
- 目标 SDK：API 36

### 构建步骤

1. 克隆项目
2. 使用 Android Studio 打开项目根目录
3. 等待 Gradle 同步完成
4. 连接设备或启动模拟器
5. 点击运行按钮即可

## 版本

当前版本：v1.0
