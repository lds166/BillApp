# 随手记 (BillApp) Code Wiki

> 一款基于 Jetpack Compose 的 Android 个人记账应用。本文档面向开发者，全面说明项目的整体架构、模块职责、关键类与函数、依赖关系以及构建运行方式。

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [整体架构](#3-整体架构)
4. [目录结构](#4-目录结构)
5. [数据层 (data)](#5-数据层-data)
6. [ViewModel 层 (viewmodel)](#6-viewmodel-层-viewmodel)
7. [UI 层 (ui)](#7-ui-层-ui)
8. [自定义图表组件 (ui/components)](#8-自定义图表组件-uicomponents)
9. [主题系统 (ui/theme)](#9-主题系统-uitheme)
10. [工具层 (util)](#10-工具层-util)
11. [应用入口 (MainActivity)](#11-应用入口-mainactivity)
12. [数据库设计](#12-数据库设计)
13. [关键业务流程](#13-关键业务流程)
14. [模块间依赖关系](#14-模块间依赖关系)
15. [构建与运行](#15-构建与运行)
16. [配置文件说明](#16-配置文件说明)

---

## 1. 项目概述

| 项目 | 说明 |
|------|------|
| 应用名称 | 随手记 (BillApp) |
| 包名 / applicationId | `com.xuri.billapp` |
| 版本 | `versionCode = 1`，`versionName = "1.0"` |
| 开发语言 | Kotlin 2.0.0 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM + Repository |
| 最低支持 | Android 8.0 (API 26) |
| 目标 / 编译 SDK | API 36 |
| JDK 版本 | Java 17 |
| 构建工具 | Gradle 8.7 + AGP 8.5.2 |

### 核心功能

- **记账**：首页快捷入口、底部弹窗记账、账单编辑、长按批量删除
- **统计**：按周/月切换、消费趋势折线图、分类占比饼图、下钻分类明细
- **日历**：月度热力图日历、点击日期查看当日账单
- **预算管理**：每日预算设置、进度条（绿→橙→红）、80% 预警 / 100% 超支系统通知
- **其他**：深浅色主题自动适配、数据清空

---

## 2. 技术栈与依赖

所有依赖通过 Gradle Version Catalog（`gradle/libs.versions.toml`）统一管理。

### 核心依赖

| 类别 | 依赖 | 版本 | 用途 |
|------|------|------|------|
| UI 框架 | `androidx.compose:compose-bom` | 2024.09.00 | Compose BOM（统一管理 Compose 各库版本） |
| UI 组件 | `androidx.compose.ui` / `ui-graphics` / `material3` | 随 BOM | Compose UI 与 Material 3 组件 |
| 图标库 | `androidx.compose.material:material-icons-extended` | 随 BOM | 扩展 Material 图标（分类图标映射） |
| 预览工具 | `androidx.compose.ui:ui-tooling-preview` / `ui-tooling` | 随 BOM | Compose 预览 |
| Activity | `androidx.activity:activity-compose` | 1.9.1 | Compose Activity 入口 |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.4 | ViewModel + Compose 集成 |
| Lifecycle | `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.4 | 生命周期运行时 |
| Core | `androidx.core:core-ktx` | 1.13.1 | AndroidX Core KTX |
| 数据库 | `androidx.room:room-runtime` / `room-ktx` | 2.6.1 | Room 本地数据库 |
| 数据库编译器 | `androidx.room:room-compiler` (KSP) | 2.6.1 | Room 注解处理 |
| 配置存储 | `androidx.datastore:datastore-preferences` | 1.1.1 | DataStore Preferences（预算持久化） |
| 通知 | `androidx.core:core-ktx` (NotificationCompat) | 1.13.1 | 兼容通知 API |

### 构建插件

| 插件 | 版本 | 用途 |
|------|------|------|
| `com.android.application` (AGP) | 8.5.2 | Android 应用构建 |
| `org.jetbrains.kotlin.android` | 2.0.0 | Kotlin Android 支持 |
| `org.jetbrains.kotlin.plugin.compose` | 2.0.0 | Compose 编译器插件 |
| `com.google.devtools.ksp` | 2.0.0-1.0.24 | KSP 注解处理（Room） |

### 测试依赖（未实际使用）

`junit 4.13.2`、`androidx.test.ext:junit 1.2.1`、`espresso-core 3.6.1`、`androidx.ui:ui-test-junit4`。

### 仓库镜像

`settings.gradle.kts` 配置了阿里云 Maven 镜像（`maven.aliyun.com`）加速国内依赖下载，`gradle-wrapper.properties` 使用腾讯云 Gradle 镜像（`mirrors.cloud.tencent.com`）。

---

## 3. 整体架构

项目采用经典的 **MVVM + Repository** 分层架构，单向数据流：

```
┌──────────────────────────────────────────────────────────────┐
│                        UI 层 (Compose)                        │
│  MainScreen / BillPage / StatsPage / CalendarPage / MinePage  │
│  自定义组件: LineChart / PieChart / CalendarView              │
└───────────────┬──────────────────────────────────────────────┘
                │ collectAsState() 订阅 StateFlow
                │ 调用 viewModel 方法触发操作
                ▼
┌──────────────────────────────────────────────────────────────┐
│                  ViewModel 层 (StateFlow)                     │
│                    BillViewModel                               │
│   持有 UI 状态 (StateFlow) + 业务逻辑 + 统计计算               │
└───────┬──────────────────────────────┬───────────────────────┘
        │                              │
        ▼                              ▼
┌───────────────────┐      ┌────────────────────────┐
│   Repository 层    │      │   BudgetManager        │
│  BillRepository    │      │ (DataStore 配置存储)    │
└───┬───────────┬───┘      └────────────────────────┘
    │           │
    ▼           ▼
┌─────────┐ ┌──────────┐
│ BillDao │ │CategoryDao│   ← Room DAO 接口
└────┬────┘ └────┬─────┘
     │           │
     ▼           ▼
┌─────────────────────────┐
│   Room Database          │
│   (bill_database)        │
│  bills 表 / categories 表│
└─────────────────────────┘
```

**单向数据流原则**：
- 数据只能从底层向上流动（数据库 → DAO → Repository → ViewModel → UI）
- UI 事件只能向下传递（UI → ViewModel → Repository → DAO）
- ViewModel 持有 `StateFlow` 作为唯一数据源，UI 通过 `collectAsState()` 订阅

### 响应式数据流概览

```
Room Flow (数据库变化)
    → Repository Flow
    → ViewModel 内部 collect → 更新 MutableStateFlow
    → UI collectAsState() → 重组
```

预算预警通知是一个旁路：
```
todayTotal 变化 → checkBudgetNotification() → NotificationHelper 发送系统通知
```

---

## 4. 目录结构

```
/workspace
├── app/
│   └── src/main/
│       ├── java/com/xuri/billapp/
│       │   ├── MainActivity.kt              # 应用入口 Activity
│       │   ├── data/                        # 数据层
│       │   │   ├── Bill.kt                  # 账单实体
│       │   │   ├── BillDao.kt               # 账单 DAO
│       │   │   ├── BillDatabase.kt          # Room 数据库单例 + TypeConverter
│       │   │   ├── BillRepository.kt        # 数据仓库
│       │   │   ├── BudgetManager.kt         # 预算管理 (DataStore)
│       │   │   ├── Category.kt              # 分类实体 + CategoryType 枚举
│       │   │   ├── CategoryDao.kt           # 分类 DAO
│       │   │   └── CategoryPreset.kt        # 8 个预置支出分类
│       │   ├── ui/                          # UI 层
│       │   │   ├── MainScreen.kt            # 主屏幕 + 底部导航 + 编辑弹窗 + 我的页
│       │   │   ├── BillPage.kt              # 记账页（三段式 + 批量删除）
│       │   │   ├── AddBillDialog.kt         # 新增账单底部弹窗
│       │   │   ├── BillItem.kt              # 账单列表项 + 删除动画包装
│       │   │   ├── CalendarPage.kt          # 日历视图页
│       │   │   ├── QuickEntryBar.kt         # 快捷记账入口栏 + 图标/颜色映射
│       │   │   ├── StatsCard.kt             # 今日消费卡片
│       │   │   ├── StatsPage.kt             # 统计页 + StatsPeriod 枚举
│       │   │   ├── components/              # 自定义 Canvas 图表组件
│       │   │   │   ├── CalendarView.kt      # 月历热力图组件
│       │   │   │   ├── LineChart.kt         # 折线图组件
│       │   │   │   └── PieChart.kt          # 饼图组件
│       │   │   └── theme/                   # 主题
│       │   │       ├── Color.kt             # 颜色定义 + 分类颜色映射
│       │   │       ├── Theme.kt             # BillAppTheme
│       │   │       └── Type.kt              # 字体排版
│       │   ├── util/
│       │   │   └── NotificationHelper.kt    # 预算预警通知工具
│       │   └── viewmodel/
│       │       └── BillViewModel.kt         # 全局 ViewModel（状态管理核心）
│       └── res/
│           ├── values/strings.xml           # 字符串资源（部分 UI 文案）
│           ├── values/themes.xml            # XML 主题（启动前）
│           ├── xml/backup_rules.xml         # 备份规则
│           └── mipmap-*/ic_launcher*        # 应用图标
├── gradle/
│   ├── libs.versions.toml                   # 版本目录（依赖集中管理）
│   └── wrapper/gradle-wrapper.properties    # Gradle Wrapper 配置
├── build.gradle.kts                         # 根构建脚本
├── settings.gradle.kts                      # 项目设置（仓库镜像、模块包含）
├── gradle.properties                        # Gradle 全局参数
└── AndroidManifest.xml                      # 清单文件
```

> 注：`res/values/strings.xml` 中定义了部分字符串资源，但当前 UI 层大多直接使用硬编码中文字符串（仅 `app_name`、`Theme.BillApp` 等在 Manifest/XML 中引用）。

---

## 5. 数据层 (data)

数据层位于 `com.xuri.billapp.data` 包，负责数据的持久化与访问抽象。

### 5.1 `Bill.kt` — 账单实体

```kotlin
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,          // 外键关联 categories.id
    val note: String = "",
    val date: Long,                // 账单时间戳（用于按日查询）
    val createdAt: Long = System.currentTimeMillis()  // 创建时间
)
```

- 对应数据库表 `bills`
- `id` 自增主键；`date` 与 `createdAt` 分离，`date` 是记账时间（可被编辑场景覆盖），`createdAt` 是记录创建时间
- `categoryId` 逻辑上关联 `Category.id`（Room 未声明外键约束，由业务层维护）

### 5.2 `Category.kt` — 分类实体与枚举

```kotlin
enum class CategoryType { EXPENSE, INCOME }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,              // 图标名称（Material 图标 key）
    val type: CategoryType = CategoryType.EXPENSE
)
```

- 对应数据库表 `categories`
- `CategoryType` 枚举通过 `CategoryTypeConverter` 以字符串形式持久化
- `icon` 字段存储图标 key（如 `"restaurant"`），由 UI 层 `getCategoryIcon()` 映射到 Material 图标

### 5.3 `CategoryPreset.kt` — 预置分类

```kotlin
object CategoryPreset {
    val DEFAULT_EXPENSE_CATEGORIES = listOf(
        Category(name = "餐饮", icon = "restaurant", type = EXPENSE),
        Category(name = "购物", icon = "shopping_cart", ...),
        Category(name = "交通", icon = "directions_car", ...),
        Category(name = "娱乐", icon = "theater_comedy", ...),
        Category(name = "居住", icon = "home", ...),
        Category(name = "医疗", icon = "medical_services", ...),
        Category(name = "教育", icon = "school", ...),
        Category(name = "其他", icon = "more_horiz", ...),
    )
}
```

- 单例对象，提供 8 个默认支出分类
- 在 `BillViewModel.init` 中，若数据库无分类则插入此列表（`OnConflictStrategy.IGNORE`）

### 5.4 `BillDao.kt` — 账单数据访问接口

```kotlin
@Dao
interface BillDao {
    fun getAllBills(): Flow<List<Bill>>                              // 观察所有账单（响应式）
    suspend fun getAllBillsOnce(): List<Bill>                        // 一次性查询
    fun getBillsByDateRange(start, end): Flow<List<Bill>>            // 按时间范围观察
    fun getTotalByDateRange(start, end): Flow<Double?>               // 按范围求和（响应式）
    suspend fun getTotalByDateRangeOnce(start, end): Double?         // 按范围求和（一次性）
    suspend fun insert(bill: Bill): Long                             // 插入，返回新 id
    suspend fun delete(bill: Bill)
    suspend fun update(bill: Bill)
    suspend fun getBillById(id: Long): Bill?
    suspend fun deleteAllBills()
}
```

**设计要点**：
- 区分 `Flow`（持续观察，数据库变化自动推送）与 `suspend fun`（一次性查询）
- 统计类查询（统计页、折线图）使用一次性 `suspend` 版本，由 `statsRefreshTrigger` 触发刷新
- 首页今日数据使用 `Flow` 版本，实时响应数据库变化

### 5.5 `CategoryDao.kt` — 分类数据访问接口

```kotlin
@Dao
interface CategoryDao {
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)
    suspend fun update(category: Category)
}
```

- `insertAll` 使用 `IGNORE` 策略，避免预置分类重复插入
- 目前业务仅使用 `EXPENSE` 类型（`INCOME` 类型已预留但未在 UI 暴露）

### 5.6 `BillDatabase.kt` — Room 数据库单例

```kotlin
class CategoryTypeConverter {
    @TypeConverter fun fromCategoryType(type: CategoryType): String = type.name
    @TypeConverter fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
}

@Database(entities = [Bill::class, Category::class], version = 1, exportSchema = false)
@TypeConverters(CategoryTypeConverter::class)
abstract class BillDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: BillDatabase? = null
        fun getDatabase(context: Context): BillDatabase { ... }  // 双重检查锁单例
    }
}
```

- 数据库名：`bill_database`
- 版本：`1`（`exportSchema = false`，未导出 schema）
- 使用 `@Volatile` + `synchronized` 实现线程安全单例
- `applicationContext` 避免内存泄漏
- `CategoryTypeConverter` 将 `CategoryType` 枚举与 String 互转，供 Room 持久化

### 5.7 `BillRepository.kt` — 数据仓库

```kotlin
class BillRepository(
    private val billDao: BillDao,
    private val categoryDao: CategoryDao
) {
    // 账单操作（透传 DAO）
    fun getAllBills() / getBillsByDateRange() / getTotalByDateRange() ...
    suspend fun insertBill() / deleteBill() / updateBill() / getBillById() / deleteAllBills()
    // 分类操作
    fun getExpenseCategories(): Flow<List<Category>>   // 封装：只取 EXPENSE
    suspend fun getCategoryById(id) / insertCategories(list)
}
```

- 单一职责：协调 `BillDao` 与 `CategoryDao`，对上层提供统一数据访问入口
- 当前实现较薄（直接透传），但作为抽象层便于未来切换数据源或添加缓存

### 5.8 `BudgetManager.kt` — 预算管理（DataStore）

```kotlin
val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_settings")

class BudgetManager(private val context: Context) {
    private val budgetKey = doublePreferencesKey("daily_budget")
    val dailyBudget: Flow<Double>           // 观察每日预算（默认 0.0）
    suspend fun setBudget(amount: Double)   // 设置预算
    suspend fun clearBudget()               // 清空预算
}
```

- 使用 DataStore Preferences 持久化每日预算（替代旧 SharedPreferences）
- DataStore 实例通过 `Context` 扩展属性 `budgetDataStore` 创建（文件名为 `budget_settings`）
- 预算 key 为 `"daily_budget"`，类型 `Double`
- `dailyBudget` 为 `Flow<Double>`，ViewModel 持续观察并同步到 `StateFlow`

---

## 6. ViewModel 层 (viewmodel)

### 6.1 `BillViewModel.kt` — 全局状态管理核心

继承 `AndroidViewModel`（需要 `Application` Context 初始化数据库、DataStore、通知）。

#### 6.1.1 初始化

```kotlin
init {
    val db = BillDatabase.getDatabase(application)
    repository = BillRepository(db.billDao(), db.categoryDao())
    budgetManager = BudgetManager(application)
    // 若数据库无分类，插入预置分类
    viewModelScope.launch {
        if (repository.getExpenseCategories().first().isEmpty()) {
            repository.insertCategories(CategoryPreset.DEFAULT_EXPENSE_CATEGORIES)
        }
    }
}
```

#### 6.1.2 暴露的 UI 状态（StateFlow）

| StateFlow | 类型 | 说明 | 数据来源 |
|-----------|------|------|----------|
| `expenseCategories` | `List<Category>` | 支出分类列表 | `categoryDao.getCategoriesByType(EXPENSE)` Flow |
| `todayBills` | `List<Bill>` | 今日账单列表 | `getBillsByDateRange(今日范围)` Flow |
| `todayTotal` | `Double` | 今日消费总额 | `getTotalByDateRange(今日范围)` Flow |
| `yesterdayTotal` | `Double` | 昨日消费总额 | `getTotalByDateRange(昨日范围)` Flow |
| `dailyBudget` | `Double` | 每日预算 | `budgetManager.dailyBudget` Flow |
| `comparisonPercentage` | `Double?` | 今日较昨日变化百分比 | `combine(todayTotal, yesterdayTotal)` |
| `statsRefreshTrigger` | `Long` | 统计刷新触发器 | 账单增删改时 `value = System.currentTimeMillis()` |

**实现模式**：每个 `StateFlow` 通过 `MutableStateFlow(...).also { flow -> viewModelScope.launch { source.collect { flow.value = it } } }` 桥接底层 `Flow`。这是一种将冷流转换为热流的模式，使 UI 始终能获取最新值（`collectAsState` 初始值为 `emptyList()` / `0.0`，避免空指针）。

`comparisonPercentage` 是唯一一个使用 `combine` + `stateIn` 派生的 StateFlow：
```kotlin
val comparisonPercentage = combine(todayTotal, yesterdayTotal) { today, yesterday ->
    if (yesterday > 0) ((today - yesterday) / yesterday) * 100 else null
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
```

#### 6.1.3 预算预警通知逻辑

```kotlin
private var notified80Percent = false
private var notified100Percent = false

private val todayTotalForNotification: StateFlow<Double> = ...
    .collect { newValue ->
        flow.value = total
        checkBudgetNotification(total)   // 每次变化检查阈值
    }

private fun checkBudgetNotification(todayTotal: Double) {
    val ratio = todayTotal / budget
    // ratio >= 1.0 → 发超支通知（标记已发）
    // ratio >= 0.8 → 发 80% 预警通知（标记已发）
    // ratio 回落时重置标记，允许下次再触发
}
```

**关键特性**：
- `todayTotalForNotification` 是独立于 `todayTotal` 的第二个订阅，专门用于触发通知（避免与 UI 状态耦合）
- 通过 `notified80Percent` / `notified100Percent` 两个标记位防止重复发送
- 当消费回落到阈值以下时重置标记，支持一天内多次预警（如消费→退款→再消费）

#### 6.1.4 业务操作方法

| 方法 | 说明 |
|------|------|
| `addBill(amount, categoryId, note, date)` | 新增账单，同时更新 `statsRefreshTrigger` |
| `updateBill(oldBill, amount, categoryId, note)` | 更新账单（`oldBill.copy(...)`），刷新统计 |
| `deleteBill(bill)` | 删除账单，刷新统计 |
| `setBudget(amount)` | 设置每日预算 |
| `clearAllData()` | 清空所有账单 + 清空预算 |
| `formatDate(timestamp)` | 格式化为 `HH:mm` |
| `formatAmount(amount)` | 格式化为保留两位小数字符串 |
| `getBillsInRange(start, end)` | 一次性查询范围内账单（统计用） |
| `getCategoryTotalsInRange(start, end)` | 范围内各分类消费总额 `Map<categoryId, total>`（饼图用） |
| `getDailyTotals(days)` | 近 N 天每日消费 `List<Pair<timestamp, total>>`（折线图用，补齐无数据日为 0） |
| `getBillsByDate(dateTimestamp)` | 指定日期的账单列表（日历点击用） |
| `getMonthDailyTotals(year, month)` | 指定月份每日消费 `List<Pair<day, total>>`（日历热力图用） |

#### 6.1.5 时间范围工具（私有）

- `getTodayRange()` / `getYesterdayRange()`：返回当日/昨日 `00:00:00.000` ~ `23:59:59.999` 的毫秒时间戳 `Pair`
- 所有统计查询基于这些范围，确保按自然日聚合

---

## 7. UI 层 (ui)

UI 层全部使用 Jetpack Compose 声明式构建，位于 `com.xuri.billapp.ui` 包。

### 7.1 `MainScreen.kt` — 主屏幕与导航

#### `AppTab` 密封类
```kotlin
sealed class AppTab(val title: String, val icon: ImageVector) {
    object Home : AppTab("记账", Icons.Default.Home)
    object Stats : AppTab("统计", Icons.Default.Star)
    object Calendar : AppTab("日历", Icons.Default.DateRange)
    object Mine : AppTab("我的", Icons.Default.AccountCircle)
    companion object { fun allTabs(): List<AppTab> = ... }
}
```
定义 4 个底部导航 Tab，每个携带标题与图标。

#### `MainScreen` Composable
应用根组件，职责：
1. 创建 `BillViewModel`（`viewModel()` 默认工厂）
2. 订阅核心状态：`todayBills`、`todayTotal`、`comparisonPercentage`、`dailyBudget`、`categories`
3. 维护本地 UI 状态：`selectedTab`、`showAddSheet`、`addSheetCategoryId`、`showEditSheet`、`showClearConfirm`
4. 使用 `Scaffold` 搭建 `TopAppBar` + 内容区 + `NavigationBar` 底部导航
5. 根据 `selectedTab` 分发渲染 `BillPage` / `StatsPage` / `CalendarPage` / `MinePage`
6. 统一管理全局弹窗：新增账单 `AddBillBottomSheet`、编辑账单 `EditBillBottomSheet`、清空确认 `AlertDialog`

#### `EditBillBottomSheet` Composable
编辑账单底部弹窗，结构与新增弹窗类似（金额、分类、备注），初始值来自 `bill`，保存时调用 `viewModel.updateBill`。

#### `EditCategoryItem` 私有 Composable
编辑弹窗中的分类选择项（圆形图标 + 名称），选中态改变背景色与文字色。

#### `MinePage` Composable
"我的"页面，包含三张卡片：
- **每日预算**：点击弹出 `AlertDialog` 设置预算（`viewModel.setBudget`）
- **关于我们**：展示版本信息
- **清空所有数据**：点击触发 `onClearData` → 主屏幕显示确认弹窗 → `viewModel.clearAllData()`

### 7.2 `BillPage.kt` — 记账页

三段式布局（`LazyColumn` 滚动）：

1. **顶部**：`StatsCard` 今日消费卡片
2. **中部**：`QuickEntryBar` 快捷记账入口
3. **底部**：今日账单列表（`BillItem`），空状态显示引导提示

**批量删除模式**：
- 状态：`batchMode: Boolean`、`selectedBills: Set<Bill>`、`showDeleteConfirm: Boolean`
- 长按账单项进入批量模式（`onLongPress`），顶部滑入工具栏（`AnimatedVisibility` + `slideInVertically`），显示已选数量与删除按钮
- 列表项显示 `Checkbox`，点击切换选中
- 删除按钮弹出 `AlertDialog` 确认后批量调用 `viewModel.deleteBill`

### 7.3 `AddBillDialog.kt` — 新增账单弹窗

#### `AddBillBottomSheet` Composable
`ModalBottomSheet` 底部弹窗（`skipPartiallyExpanded = true`，圆角 16dp）：
- 金额输入：大号 `OutlinedTextField`，正则 `^\d*\.?\d{0,2}$` 限制最多两位小数，前缀 `¥`
- 分类选择：`LazyVerticalGrid` 4 列网格，渲染 `CategoryChip`
- 备注：单行 `OutlinedTextField`
- 保存按钮：金额 > 0 且分类已选时启用，保存后 `scope.launch { sheetState.hide() }` 关闭弹窗
- 支持 `initialCategoryId` 预填（快捷入口传入）

#### `CategoryChip` 私有 Composable
分类选择项：圆形背景（选中实色 / 未选中 10% 透明度）+ Material 图标 + 名称。

### 7.4 `BillItem.kt` — 账单列表项

#### `BillItem` Composable
单行布局：`[Checkbox?] [圆形分类首字图标] [分类名 + 备注 / 时间] [金额]`
- 通过 `presetNames.indexOf(category.name)` 查找分类索引，用 `getCategoryColorByIndex` 取色
- 未找到分类时回退到索引 7（灰色 "其他"）
- 金额显示为 `-$amountString`（当前应用只支持支出）

#### `BillItemWithDeleteAnimation` Composable
带删除动画的包装器：用 `AnimatedVisibility`（`fadeOut` + `shrinkVertically`，200ms）实现列表项滑出消失。**注：当前 `BillPage` 实际使用的是 `BillItem` 而非此动画版本，此组件为预留/未接入。**

### 7.5 `CalendarPage.kt` — 日历视图页

- 状态：`selectedYear` / `selectedMonth`（初始为当前年月）、`dailyTotals: Map<Int, Double>`、`selectedDay`、`selectedDayBills`
- `LaunchedEffect(selectedYear, selectedMonth)`：切换月份时调用 `viewModel.getMonthDailyTotals` 加载热力数据
- 点击日期（`onDateClick`）→ 设置 `selectedDay` → 内层 `LaunchedEffect(selectedDay)` 加载 `selectedDayBills`
- 布局：`CalendarView`（月历热力图）→ 选中日期合计卡片 → 当日账单列表

### 7.6 `QuickEntryBar.kt` — 快捷记账入口

#### `QuickEntryBar` Composable
水平滚动（`LazyRow`）展示前 6 个分类（`categories.take(6)`），点击触发 `onCategoryClick(categoryId)` → 主屏幕弹出新增弹窗并预填分类。

#### `getCategoryIcon(iconName: String): ImageVector`
将分类 `icon` 字段（数据库存储的字符串 key）映射到 Material Icons：
| icon key | Material 图标 |
|----------|--------------|
| `restaurant` | `Icons.Default.Restaurant` |
| `shopping_cart` | `Icons.Default.ShoppingCart` |
| `directions_car` | `Icons.Default.DirectionsCar` |
| `theater_comedy` | `Icons.Default.TheaterComedy` |
| `home` | `Icons.Default.Home` |
| `medical_services` | `Icons.Default.MedicalServices` |
| `school` | `Icons.Default.School` |
| `more_horiz` | `Icons.Default.MoreHoriz` |
| 其他 | `Icons.Default.Home`（兜底） |

#### `getCategoryColor(categoryName: String): Color`
按分类名称映射到 `getCategoryColorByIndex`（保留向后兼容的辅助函数，当前 `StatsPage` 饼图着色使用）。

### 7.7 `StatsCard.kt` — 今日消费卡片

展示内容：
- 超支横幅（`AnimatedVisibility`，`budgetRatio > 1.0` 时滑入，红色背景）
- "今日消费" 标签 + 大号金额（36sp）
- 昨日对比：`↑ +x.x%`（红）/ `↓ x.x%`（绿）/ "暂无昨日数据"
- 预算进度条（`dailyBudget > 0` 时显示）：
  - 进度 `(todayTotal / dailyBudget).coerceIn(0,1)`
  - 颜色：`>=100%` 红 / `>=80%` 橙 / 否则绿
  - 显示百分比文本

### 7.8 `StatsPage.kt` — 统计页

#### `StatsPeriod` 枚举
```kotlin
enum class StatsPeriod(val label: String) { WEEK("本周"), MONTH("本月") }
```
WEEK = 近 7 天，MONTH = 近 30 天。

#### `StatsPage` Composable
- 订阅 `viewModel.statsRefreshTrigger`，账单变化时自动刷新
- `LaunchedEffect(period, statsTrigger)`：
  - 调用 `viewModel.getDailyTotals(days)` → `toChartPoints()` 生成折线图数据
  - 计算近 N 天范围 → `viewModel.getCategoryTotalsInRange` → 转换为 `PieChartData` 列表（按金额降序）
- 布局（`LazyColumn`）：
  1. 周期切换 `FilterChip` 组
  2. 折线图卡片（`LineChart` + 合计金额）
  3. 饼图卡片（`PieChart` + `PieChartLegend`，点击图例下钻）
  4. 分类明细列表（点击图例后显示该分类的账单明细）

---

## 8. 自定义图表组件 (ui/components)

所有图表均使用 Compose `Canvas` 手绘，未引入第三方图表库。

### 8.1 `LineChart.kt` — 消费趋势折线图

#### `ChartPoint` 数据类
```kotlin
data class ChartPoint(val value: Double, val label: String)
```

#### `LineChart` Composable
手绘折线图，特性：
- 5 段水平虚线网格（`PathEffect.dashPathEffect`）
- 左侧 Y 轴实线 + 底部 X 轴实线
- 5 个 Y 轴刻度标签（右侧 `Column` 渲染）
- X 轴日期标签旋转 -30° 避免重叠（`graphicsLayer { rotationZ = -30f }`）
- 折线下方半透明填充区域（`fillPath`）
- 数据点：外圈彩色 + 内圈白色圆点
- 单数据点特殊处理（居中显示）
- 空数据显示 "暂无数据" 占位

#### `computeNiceMax(maxValue): Double`（私有）
计算整洁的 Y 轴最大值：
- `<=100` → 向上取整到 5 的倍数
- `<=1000` → 50 的倍数
- `<=10000` → 500 的倍数
- 更大 → 1000 的倍数

#### `List<Pair<Long, Double>>.toChartPoints(dateFormat = "MM/dd")` 扩展函数
将 ViewModel 返回的 `(时间戳, 金额)` 列表转换为 `ChartPoint` 列表，时间戳按 `MM/dd` 格式化。

### 8.2 `PieChart.kt` — 分类占比饼图

#### `PieChartData` 数据类
```kotlin
data class PieChartData(val label: String, val value: Double, val color: Color)
```

#### `PieChart` Composable
- 使用 `Canvas.drawArc` 绘制各扇区，`startAngle` 从 -90°（顶部）开始
- 每个扇区叠加白色描边（`Stroke width = 2f`）作为分隔线
- 空数据 / 总额 ≤ 0 时显示占位文本
- 预留 `onSliceClick` 回调（当前 UI 通过图例点击触发下钻，未直接用扇区点击）

#### `PieChartLegend` Composable
图例列表，每项：颜色圆点 + 分类名 + 金额与占比（`%.2f 元 (%.1f%%)`）。点击项触发 `onLegendClick` → `StatsPage` 加载该分类明细。

### 8.3 `CalendarView.kt` — 月历热力图

#### `CalendarView` Composable
参数：`year`、`month`（1-12）、`dailyTotals: Map<Int, Double>`、`onMonthChange`、`onDateClick`。

特性：
- 顶部月份导航（左右箭头切换，自动处理跨年）
- 星期标题行（周一 ~ 周日）
- 计算月首偏移量（`offset`，周一为第一天：周日 → 6，其他 → `firstDayOfWeek - 2`）
- 按行渲染日期格子，空白处留空 `Box`
- 高亮今日（粗体 + 主题色）
- 每格下方显示消费金额（取整 `%.0f`）

#### `CalendarCell` 私有 Composable
热力图着色：
```kotlin
val ratio = (amount / maxAmount).coerceIn(0.0, 1.0)
val bgColor = Color.Green.copy(alpha = 0.1f + ratio.toFloat() * 0.5f)
```
消费越高，绿色越深（alpha 0.1 ~ 0.6）。

---

## 9. 主题系统 (ui/theme)

### 9.1 `Color.kt` — 颜色定义

| 颜色 | 值 | 用途 |
|------|-----|------|
| `Green500` | `#10B981` | 主色（品牌绿） |
| `Green600` | `#059669` | 深色主色 |
| `Green50` | `#ECFDF5` | 浅绿背景 |
| `Orange500` | `#F59E0B` | 预算 80% 警告色 |
| `Orange600` | `#D97706` | 深橙 |
| `Red500` | `#EF4444` | 超支 / 删除危险色 |
| `Red600` / `Red50` | `#DC2626` / `#FEE2E2` | 深红 / 浅红背景 |
| `TextPrimary` | `#1E293B` | 主文字 |
| `TextSecondary` | `#64748B` | 次要文字 |
| `BackgroundLight` | `#F8FAFC` | 浅色背景 |
| `CardLight` | `#FFFFFF` | 卡片背景 |

#### `getCategoryColorByIndex(index): Color`
8 种分类颜色循环映射（`index % 8`）：

| 索引 | 颜色 | 对应分类 |
|------|------|----------|
| 0 | 绿 `#10B981` | 餐饮 |
| 1 | 橙 `#F59E0B` | 购物 |
| 2 | 蓝 `#3B82F6` | 交通 |
| 3 | 紫 `#8B5CF6` | 娱乐 |
| 4 | 粉 `#EC4899` | 居住 |
| 5 | 红 `#EF4444` | 医疗 |
| 6 | 青 `#06B6D4` | 教育 |
| 7 | 灰 `#6B7280` | 其他 |

### 9.2 `Theme.kt` — 主题配置

```kotlin
private val DarkColorScheme = darkColorScheme(primary = Green500, background = #0F172A, surface = #1E293B, ...)
private val LightColorScheme = lightColorScheme(primary = Green500, background = BackgroundLight, ...)

@Composable
fun BillAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // 默认关闭动态取色
    content: @Composable () -> Unit
)
```

- 主色统一为 `Green500`
- 深浅色方案各定义一套 `ColorScheme`
- `dynamicColor = false`，不使用 Android 12+ 动态取色（保持品牌色一致）
- `SideEffect` 中设置状态栏外观（`isAppearanceLightStatusBars = !darkTheme`）并启用 Edge-to-Edge（`setDecorFitsSystemWindows(window, false)`）

### 9.3 `Type.kt` — 字体排版

自定义 `Typography`，覆盖 `bodyLarge`、`titleLarge`、`labelSmall`、`headlineLarge` 四种样式（均使用系统默认字体 `FontFamily.Default`）。

---

## 10. 工具层 (util)

### `NotificationHelper.kt` — 预算预警通知

`object` 单例工具类，负责发送预算预警系统通知。

| 常量 / 方法 | 说明 |
|-------------|------|
| `CHANNEL_ID = "budget_alert_channel"` | 通知渠道 ID |
| `CHANNEL_NAME = "预算预警"` | 渠道名称 |
| `NOTIFICATION_80_PERCENT = 1` | 80% 预警通知 ID |
| `NOTIFICATION_100_PERCENT = 2` | 超支通知 ID |
| `createNotificationChannel(context)` | 创建通知渠道（Android 8.0+ 必需，`IMPORTANCE_HIGH`） |
| `sendBudget80PercentNotification(context, spent, budget)` | 发送 80% 预警通知 |
| `sendBudgetExceededNotification(context, spent, budget)` | 发送超支通知 |

**通知内容**：
- 80% 预警：标题 "预算预警"，内容 "今日已消费 ¥xx，已达预算 ¥yy 的 80%，请节约开支！"
- 超支：标题 "预算超支"，内容 "今日已消费 ¥xx，已超过预算 ¥yy，请注意控制开支！"
- 点击通知跳转 `MainActivity`（`FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`，`PendingIntent.FLAG_IMMUTABLE`）

> **注意**：`createNotificationChannel` 在每次发送通知时都会调用（内部有版本判断，重复创建无害），但更规范的做法是在 Application 启动时创建一次。当前实现未自定义 `Application` 类，因此在发送时懒创建。

---

## 11. 应用入口 (MainActivity)

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}
```

- 继承 `ComponentActivity`（Compose 推荐基类）
- `enableEdgeToEdge()` 启用边到边显示
- `setContent` 设置 Compose 内容，包裹 `BillAppTheme` 与 `Surface`
- `MainScreen` 作为根组件，内部通过 `viewModel()` 获取 `BillViewModel`

**Manifest 注册**（`AndroidManifest.xml`）：
- `MainActivity` 为 LAUNCHER Activity（`exported = true`）
- 应用主题 `@style/Theme.BillApp`（`Theme.Material.Light.NoActionBar`，启动前的 XML 主题，Compose 加载后由 `BillAppTheme` 接管）
- `allowBackup = true`，数据提取规则 `@xml/backup_rules`

---

## 12. 数据库设计

### 12.1 `bills` 表

| 字段 | 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|------|
| `id` | `id` | Long | PRIMARY KEY AUTOINCREMENT | 自增 ID |
| `amount` | `amount` | Double | NOT NULL | 金额 |
| `categoryId` | `category_id` | Long | NOT NULL | 关联分类 ID |
| `note` | `note` | String | 默认 `""` | 备注 |
| `date` | `date` | Long | NOT NULL | 账单时间戳（毫秒） |
| `createdAt` | `created_at` | Long | 默认当前时间 | 创建时间戳 |

### 12.2 `categories` 表

| 字段 | 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|------|
| `id` | `id` | Long | PRIMARY KEY AUTOINCREMENT | 自增 ID |
| `name` | `name` | String | NOT NULL | 分类名称 |
| `icon` | `icon` | String | NOT NULL | 图标 key |
| `type` | `type` | String | NOT NULL | `CategoryType`（经 TypeConverter 转为字符串存储） |

### 12.3 数据存储概览

| 数据 | 存储方式 | 文件 / 位置 |
|------|----------|-------------|
| 账单记录 | Room (SQLite) | `bill_database`（应用沙盒 databases 目录） |
| 分类数据 | Room (SQLite) | `bill_database` |
| 每日预算 | DataStore Preferences | `budget_settings.preferences_pb`（应用沙盒 files/datastore 目录） |
| UI 状态 | 内存（StateFlow） | 进程内，进程销毁丢失 |

---

## 13. 关键业务流程

### 13.1 新增账单流程

```
用户点击快捷分类 / 记一笔
  → MainScreen 设置 showAddSheet = true, addSheetCategoryId = categoryId
  → 渲染 AddBillBottomSheet（预填分类）
  → 用户输入金额、选分类、填备注，点击保存
  → onSave(amount, categoryId, note)
  → viewModel.addBill(amount, categoryId, note)
      → viewModelScope.launch {
            repository.insertBill(Bill(...))
            _statsRefreshTrigger.value = System.currentTimeMillis()
        }
  → Room 写入 bills 表
  → todayBills / todayTotal 的 Flow 自动发射新值 → StateFlow 更新 → UI 重组
  → 统计页（若打开）监听 statsRefreshTrigger → LaunchedEffect 重新查询刷新
  → todayTotalForNotification 变化 → checkBudgetNotification → 可能发送预算预警通知
  → sheetState.hide() 关闭弹窗
```

### 13.2 预算预警通知流程

```
任何导致今日总消费变化的操作（新增/删除/更新账单）
  → todayTotalForNotification 的 Flow 收集到新值
  → checkBudgetNotification(total)
  → ratio = total / dailyBudget（budget <= 0 直接 return）
  → ratio >= 1.0 && !notified100Percent
      → NotificationHelper.sendBudgetExceededNotification
      → notified100Percent = true; notified80Percent = true
  → else ratio >= 0.8 && !notified80Percent
      → NotificationHelper.sendBudget80PercentNotification
      → notified80Percent = true
  → ratio 回落 < 0.8 → 重置两个标记（允许再次触发）
  → ratio 回落 < 1.0 → 重置 notified100Percent
```

### 13.3 统计页数据刷新流程

```
StatsPage 订阅 viewModel.statsRefreshTrigger (StateFlow)
  → LaunchedEffect(period, statsTrigger) 依赖两者任一变化时执行
  → viewModel.getDailyTotals(days)  ← 近 N 天每日消费
  → dailyData.toChartPoints()       ← 转换为折线图数据
  → viewModel.getCategoryTotalsInRange(start, end)  ← 分类汇总
  → 转换为 PieChartData 列表（按金额降序）
  → 更新 lineChartData / pieChartData 本地状态 → UI 重组
```

### 13.4 日历热力图加载流程

```
CalendarPage 初始化 selectedYear/selectedMonth = 当前年月
  → LaunchedEffect(selectedYear, selectedMonth)
  → viewModel.getMonthDailyTotals(year, month)
      → 计算该月起止时间戳
      → getBillsInRange(start, end) 查询该月所有账单
      → 按 yyyy-MM-dd 分组求和
      → 遍历该月每一天，补齐无数据日为 0.0
      → 返回 List<Pair<day, total>>
  → dailyTotals = totals.associate { it.first to it.second }
  → CalendarView 渲染：每格根据 amount/maxAmount 计算绿色深度
  → 用户点击某日期 → selectedDay = day
  → 内层 LaunchedEffect(selectedDay) → viewModel.getBillsByDate → 加载当日账单列表
```

---

## 14. 模块间依赖关系

### 14.1 包依赖图

```
com.xuri.billapp
├── MainActivity ──→ ui.MainScreen
│
├── viewmodel.BillViewModel ──→ data.* (Bill, BillDatabase, BillRepository,
│                                    BudgetManager, Category*, CategoryType)
│                          └──→ util.NotificationHelper
│
├── ui.* ──→ viewmodel.BillViewModel
│        ├──→ data.Bill / data.Category
│        ├──→ ui.components.*
│        ├──→ ui.theme.*
│        └──→ (MainScreen.kt 还定义了 EditBillBottomSheet / MinePage)
│
├── ui.components.* ──→ (纯 Compose，无外部业务依赖)
│
├── ui.theme.* ──→ (纯 Compose 主题，无外部依赖)
│
├── util.NotificationHelper ──→ MainActivity (PendingIntent 跳转)
│
└── data.*
     ├── Bill / Category (实体，无依赖)
     ├── BillDao / CategoryDao (依赖实体 + Room)
     ├── BillDatabase (依赖 DAO + 实体)
     ├── BillRepository (依赖 DAO)
     ├── BudgetManager (依赖 AndroidX DataStore)
     └── CategoryPreset (依赖 Category + CategoryType)
```

### 14.2 依赖方向约束

- **UI 层不直接访问数据层**：所有数据通过 `BillViewModel` 获取（`MainScreen` 的子页面均接收 `viewModel` 参数或状态参数）
- **ViewModel 依赖 Repository 与 BudgetManager**，不直接操作 DAO / DataStore
- **Repository 依赖 DAO**，不感知 UI
- **图表组件 (components) 完全无状态**：纯展示，通过参数接收数据与回调，可独立复用
- **`getCategoryIcon` / `getCategoryColor` 定义在 `QuickEntryBar.kt` 中**，但被 `AddBillDialog`、`MainScreen`（EditCategoryItem）、`BillItem`、`StatsPage` 跨文件引用——这些是顶层 `fun`，位于 `com.xuri.billapp.ui` 包内，同包可直接访问。若未来需模块化，应提取到独立的工具文件。

### 14.3 潜在耦合点 / 改进建议

1. `BillItem.kt` 中硬编码了 `presetNames = listOf("餐饮", "购物", ...)` 用于颜色映射，与 `CategoryPreset` 的数据重复，建议改为从分类列表的索引派生。
2. `BillViewModel` 中 `todayTotal` 与 `todayTotalForNotification` 重复订阅了同一数据源（`getTotalByDateRange(今日范围)`），可合并为单个 StateFlow 共享，减少数据库查询。
3. `CategoryType.INCOME` 已定义但 UI 未暴露收入记账入口，当前所有账单显示为 `-$amount`。
4. 未自定义 `Application` 类，通知渠道在发送时懒创建；建议在 Application `onCreate` 中预创建。
5. `exportSchema = false`，未导出 Room schema，不利于数据库迁移追踪与 CI 校验。

---

## 15. 构建与运行

### 15.1 环境要求

| 工具 | 要求 |
|------|------|
| Android Studio | 推荐最新稳定版（需支持 AGP 8.5.2、Compose BOM 2024.09） |
| JDK | 17（项目 `sourceCompatibility` / `targetCompatibility` / `jvmTarget` 均为 17） |
| Android SDK Platform | API 36（compileSdk / targetSdk） |
| Gradle | 8.7（由 wrapper 自动下载，无需手动安装） |
| 运行设备 | Android 8.0 (API 26) 及以上 |

### 15.2 构建步骤（Android Studio）

1. 克隆 / 打开项目根目录 `/workspace`
2. 等待 Gradle 同步（首次会下载 AGP、Kotlin、依赖；已配置阿里云镜像加速）
3. 连接 Android 设备或启动模拟器（API 26+）
4. 点击 Run 按钮（或 `Shift + F10`）运行 `app` 配置

### 15.3 命令行构建

```bash
# 查看可用任务
./gradlew tasks

# 编译 Debug APK
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 安装到已连接设备
./gradlew :app:installDebug

# 编译 Release APK（当前 isMinifyEnabled = false，未混淆）
./gradlew :app:assembleRelease

# 清理
./gradlew clean
```

> Windows 环境使用 `gradlew.bat` 替代 `./gradlew`。

### 15.4 构建特性

- **Compose**：`buildFeatures.compose = true`，启用 Compose
- **KSP**：`ksp(libs.androidx.room.compiler)` 处理 Room 注解
- **Java 17**：编译目标与 Kotlin jvmTarget 均为 17
- **Release 未混淆**：`isMinifyEnabled = false`（生产发布前建议开启 R8 并完善 `proguard-rules.pro`）
- **nonTransitiveRClass**：`android.nonTransitiveRClass = true`，R 类不传递，减少编译冲突

---

## 16. 配置文件说明

### 16.1 `gradle/libs.versions.toml` — 版本目录

集中管理所有依赖版本号、库坐标与插件坐标，分为三部分：
- `[versions]`：版本号定义（`agp = "8.5.2"`、`kotlin = "2.0.0"` 等）
- `[libraries]`：库坐标，通过 `version.ref` 引用版本
- `[plugins]`：插件坐标

在 `build.gradle.kts` 中通过 `alias(libs.plugins.xxx)` / `implementation(libs.xxx)` 引用。

### 16.2 `settings.gradle.kts` — 项目设置

- `pluginManagement`：配置插件仓库（阿里云 google / central / gradle-plugin + Gradle Plugin Portal 兜底）
- `dependencyResolutionManagement`：`repositoriesMode = FAIL_ON_PROJECT_REPOS`（禁止子项目自定义仓库），配置依赖仓库（阿里云 + mavenCentral）
- `rootProject.name = "BillApp"`
- `include(":app")`：仅包含一个 app 模块

### 16.3 `gradle.properties` — Gradle 全局参数

```properties
org.gradle.jvmargs=-Xmx4096m ...   # Gradle JVM 内存 4G
org.gradle.parallel=true            # 并行构建
org.gradle.caching=true             # 构建缓存
org.gradle.configureondemand=true   # 按需配置
org.gradle.daemon=true              # Gradle 守护进程
kotlin.incremental=true             # Kotlin 增量编译
android.useAndroidX=true            # 使用 AndroidX
android.nonTransitiveRClass=true    # 非传递 R 类
android.suppressUnsupportedCompileSdk=36   # 抑制 compileSdk 36 未正式支持的警告
```

### 16.4 `app/build.gradle.kts` — 模块构建脚本

- 应用 4 个插件：`android.application`、`kotlin.android`、`kotlin.compose`、`ksp`
- `namespace = "com.xuri.billapp"`，`compileSdk = 36`
- `defaultConfig`：`applicationId`、`minSdk = 26`、`targetSdk = 36`、`versionCode = 1`、`versionName = "1.0"`
- `buildTypes.release`：`isMinifyEnabled = false`
- `compileOptions` / `kotlinOptions`：Java 17
- 依赖：Compose BOM + Material3 + 图标扩展 + Room + DataStore + ViewModel + Lifecycle

### 16.5 `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-all.zip
```
Gradle 8.7，使用腾讯云镜像下载（国内加速）。

### 16.6 `AndroidManifest.xml`

- 声明 `MainActivity` 为启动 Activity（`MAIN` / `LAUNCHER`）
- `allowBackup = true`，关联 `@xml/backup_rules`
- 应用主题 `@style/Theme.BillApp`
- `tools:targetApi = "35"`

### 16.7 资源文件

| 文件 | 说明 |
|------|------|
| `res/values/strings.xml` | 字符串资源（`app_name` = "随手记" 等，部分 UI 文案已定义但 UI 层多为硬编码） |
| `res/values/themes.xml` | `Theme.BillApp` 继承 `Theme.Material.Light.NoActionBar`（启动闪屏主题） |
| `res/xml/backup_rules.xml` | 自动备份规则 |
| `res/mipmap-anydpi-v26/ic_launcher.xml` / `ic_launcher_round.xml` | 自适应图标 |
| `res/drawable/ic_launcher_foreground.xml` / `ic_launcher_background.xml` | 图标前景/背景 |

---

## 附录：文件清单速查表

| 文件 | 类型 | 主要职责 |
|------|------|----------|
| `MainActivity.kt` | Activity | 应用入口，装载 Compose 根视图 |
| `data/Bill.kt` | Entity | 账单实体（bills 表） |
| `data/Category.kt` | Entity + Enum | 分类实体 + CategoryType 枚举 |
| `data/CategoryPreset.kt` | Object | 8 个预置支出分类常量 |
| `data/BillDao.kt` | DAO | 账单 CRUD + 范围查询 |
| `data/CategoryDao.kt` | DAO | 分类查询与插入 |
| `data/BillDatabase.kt` | RoomDatabase | 数据库单例 + TypeConverter |
| `data/BillRepository.kt` | Repository | 数据访问抽象层 |
| `data/BudgetManager.kt` | Manager | DataStore 预算读写 |
| `viewmodel/BillViewModel.kt` | ViewModel | 全局状态与业务逻辑核心 |
| `util/NotificationHelper.kt` | Object | 预算预警系统通知 |
| `ui/MainScreen.kt` | Composable | 主屏幕、底部导航、编辑弹窗、我的页 |
| `ui/BillPage.kt` | Composable | 记账页（卡片+快捷入口+列表+批量删除） |
| `ui/AddBillDialog.kt` | Composable | 新增账单底部弹窗 |
| `ui/BillItem.kt` | Composable | 账单列表项（含动画版本） |
| `ui/CalendarPage.kt` | Composable | 日历视图页 |
| `ui/QuickEntryBar.kt` | Composable + fun | 快捷入口栏 + 图标/颜色映射函数 |
| `ui/StatsCard.kt` | Composable | 今日消费卡片 |
| `ui/StatsPage.kt` | Composable + Enum | 统计页 + 统计周期枚举 |
| `ui/components/LineChart.kt` | Composable + 扩展 | Canvas 折线图 + 数据转换 |
| `ui/components/PieChart.kt` | Composable + data class | Canvas 饼图 + 图例 |
| `ui/components/CalendarView.kt` | Composable | 月历热力图组件 |
| `ui/theme/Color.kt` | 颜色常量 + fun | 颜色定义 + 分类颜色映射 |
| `ui/theme/Theme.kt` | Composable | BillAppTheme 深浅色主题 |
| `ui/theme/Type.kt` | Typography | 字体排版定义 |
