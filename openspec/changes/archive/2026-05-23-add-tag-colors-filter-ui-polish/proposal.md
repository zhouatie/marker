## Why

当前时间线里标签只作为文字展示，多个活动混在列表中时不容易快速扫描；列表也缺少按标签聚焦的入口，用户回顾一天时需要反复浏览无关时间段。现有手机端整体 UI 视觉层级偏弱，需要让每日回顾主视图更清爽、可读、可操作。

## What Changes

- 为每个活动标签提供稳定、可区分的颜色，并在时间段列表、标签选择入口和筛选入口中一致展示。
- 在时间线列表增加标签筛选能力，用户可以查看全部时间段，也可以只查看某一个标签对应的时间段。
- 未标记时间段保留明确的“未标记”视觉状态，并支持作为筛选项被单独查看。
- 优化手机端每日回顾 UI 的视觉层级，包括顶部日期/视图切换、筛选控件、时间段行、标签 chip、选中态和空状态。
- 保持现有标记同步、时间段推导、标签归属和删除后标签清理语义不变。

## Capabilities

### New Capabilities

- 无

### Modified Capabilities

- `daily-activity-timeline`: 增加标签颜色展示、按标签筛选时间段列表，以及每日回顾主视图的视觉层级要求。

## Impact

- 影响手机端 Compose UI：`app-phone/src/main/java/com/atie/marker/phone/MainActivity.kt` 中的每日回顾页面、时间段行、标签编辑弹窗和相关控件。
- 可能影响手机端 UI 状态管理：`app-phone/src/main/java/com/atie/marker/phone/ui/TimelineViewModel.kt` 或页面本地状态需要跟踪当前标签筛选条件。
- 可能需要在共享或手机端 UI 层新增标签颜色映射工具，确保同一标签在不同位置颜色稳定。
- 不改变 Room schema、Wear OS Data Layer 同步协议、标记事件模型、时间段推导逻辑或标签持久化模型。
