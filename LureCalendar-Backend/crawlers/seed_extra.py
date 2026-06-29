# -*- coding: utf-8 -*-
"""扩充内容种子（鱼种 + 路亚饵 + 教程）
===================================
不联网，直接写入 MySQL 数据库。原因：search_replace 工具对大段中文有
字符污染风险，create_file 写入更稳。

执行：
    cd LureCalendar-Backend
    python -m crawlers.seed_extra
"""
from __future__ import annotations

import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from main import (  # noqa: E402
    SessionLocal,
    FishEncyclopediaModel,
    LureLibraryModel,
    FishingGuideModel,
)


# ===================================================================
# 一、扩充鱼种（8 种新增对象鱼）
# ===================================================================
EXTRA_FISH = [
    {
        "name": "大口黑鲈",
        "alias": "美洲鲈,Largemouth Bass",
        "scientific_name": "Micropterus salmoides",
        "family": "太阳鱼科 黑鲈属",
        "category": "路亚对象鱼",
        "distribution": "原产北美，国内广东、江苏、云南黑坑大量养殖。",
        "habitat": "中上层伏击型，喜水草、沉木、桥墩等结构。",
        "feeding_habit": "纯肉食，捕食小鱼、虾、青蛙。",
        "body_size": "30-55cm / 0.5-3kg",
        "best_season": "4-11 月",
        "best_hours": "日出后 1 小时 / 日落前 2 小时",
        "optimal_temp": "18-26",
        "recommended_lures": "软虫德州,波趴,雷蛙,Spinnerbait,Crankbait",
        "technique_tips": "路亚黄金对象鱼；高抛德州配水面系交替；攻击凶猛。",
    },
    {
        "name": "黄颡鱼",
        "alias": "黄辣丁,黄骨鱼",
        "scientific_name": "Pelteobagrus fulvidraco",
        "family": "鲿科 黄颡鱼属",
        "category": "路亚对象鱼",
        "distribution": "全国各大江河湖泊均有分布，野生资源丰富。",
        "habitat": "底层鱼，夜间活动较多，喜阴暗结构。",
        "feeding_habit": "杂食偏肉，喜食蚯蚓、小虾、昆虫。",
        "body_size": "15-25cm / 100-300g",
        "best_season": "4-10 月",
        "best_hours": "黄昏后 2 小时 / 阴雨天全天",
        "optimal_temp": "18-28",
        "recommended_lures": "Ned 软虫,腰挂软虫,超轻铅头钩",
        "technique_tips": "适合 UL 微物钓；硬棘有毒，摘钩时小心刺手。",
    },
    {
        "name": "白斑狗鱼",
        "alias": "北方狗鱼,Pike",
        "scientific_name": "Esox lucius",
        "family": "狗鱼科 狗鱼属",
        "category": "路亚对象鱼",
        "distribution": "东北黑龙江流域、新疆额尔齐斯河有分布。",
        "habitat": "中上层伏击型，藏于水草、芦苇根部。",
        "feeding_habit": "凶猛肉食，捕小鱼、青蛙、水鸟雏鸟。",
        "body_size": "50-100cm / 1-8kg",
        "best_season": "5-10 月",
        "best_hours": "上午 / 黄昏",
        "optimal_temp": "12-22",
        "recommended_lures": "大型 Swimbait,Jerkbait,大亮片,雷蛙",
        "technique_tips": "需用钢丝前导防咬线；抽停节奏停顿 2-3 秒常有奇效。",
    },
    {
        "name": "海鲈",
        "alias": "七星鲈,花鲈",
        "scientific_name": "Lateolabrax japonicus",
        "family": "花鲈科",
        "category": "海钓路亚对象鱼",
        "distribution": "中国沿海、黄渤海、东海、南海均有分布。",
        "habitat": "近岸入海口，咸淡交界为关键水域。",
        "feeding_habit": "肉食，追击捕食小鱼、虾。",
        "body_size": "30-80cm / 0.5-5kg",
        "best_season": "3-5 月、9-12 月",
        "best_hours": "涨落潮交替期",
        "optimal_temp": "15-22",
        "recommended_lures": "海鲈专用米诺,Swimbait,铁板,腰挂虾型",
        "technique_tips": "潮汐流动是黄金信号；昼夜与潮表共同决定钓法选择。",
    },
    {
        "name": "黑鲷",
        "alias": "黑加吉,海鲋",
        "scientific_name": "Acanthopagrus schlegelii",
        "family": "鲷科 棘鲷属",
        "category": "海钓路亚对象鱼",
        "distribution": "中国东南沿海、台海、日本、朝鲜半岛。",
        "habitat": "近底礁石区、沙泥混合底、港口码头。",
        "feeding_habit": "杂食偏荤，虾、蟹、贝类、小鱼。",
        "body_size": "20-40cm / 0.3-1.5kg",
        "best_season": "4-6 月、10-12 月",
        "best_hours": "涨落潮初期",
        "optimal_temp": "15-22",
        "recommended_lures": "软虫倒钓,微型 Crankbait,VIB,腰挂铅头钩",
        "technique_tips": "黑鲷警惕高，用深色饵更稳；超轻铅 1-3g 慢跳底效果佳。",
    },
    {
        "name": "竹梭鱼",
        "alias": "魣鱼,Barracuda",
        "scientific_name": "Sphyraena pinguis",
        "family": "魣科",
        "category": "海钓路亚对象鱼",
        "distribution": "中国东南海、台湾周边海域。",
        "habitat": "中上层集群，凶猛追击小鱼群。",
        "feeding_habit": "肉食，追击受惊鱼群表层炸水。",
        "body_size": "40-80cm / 0.5-2kg",
        "best_season": "5-10 月",
        "best_hours": "上午 / 太阳上山后",
        "optimal_temp": "20-28",
        "recommended_lures": "远投金属振饵,Stickbait,大米诺",
        "technique_tips": "鸟群是钓点指示；高速摇收触发追击反射。",
    },
    {
        "name": "江鳕",
        "alias": "山鳕,上鳕",
        "scientific_name": "Lota lota",
        "family": "鳕科",
        "category": "路亚对象鱼",
        "distribution": "东北黑龙江、乌苏里江流域。",
        "habitat": "冷水底层，喜低温清水。",
        "feeding_habit": "肉食，夜行性，捕小鱼、底栖虾蟹。",
        "body_size": "40-80cm / 1-5kg",
        "best_season": "冬春季 (11-3 月)",
        "best_hours": "夜间 / 清晨",
        "optimal_temp": "4-12",
        "recommended_lures": "重型腰挂软虫,VIB,金属铁板",
        "technique_tips": "低温期主攻，冰钓也可；动作必须极慢。",
    },
    {
        "name": "草鱼",
        "alias": "鲩鱼,白鲩",
        "scientific_name": "Ctenopharyngodon idella",
        "family": "鲤科 草鱼属",
        "category": "拓展路亚对象鱼",
        "distribution": "全国分布，大型水库、河流常见。",
        "habitat": "中上层，喜栖于水草丰茂区域。",
        "feeding_habit": "草食为主，偶食昆虫、小鱼。",
        "body_size": "50-100cm / 2-15kg",
        "best_season": "5-10 月",
        "best_hours": "清晨、傍晚",
        "optimal_temp": "20-30",
        "recommended_lures": "玉米软饵,水面青蛙(误食),浮水颗粒型软饵",
        "technique_tips": "路亚草鱼是非主流玩法；以模仿落水草料、果实为思路。",
    },
]


# ===================================================================
# 二、扩充路亚饵库（8 款补充）
# ===================================================================
EXTRA_LURES = [
    {
        "name": "克邦 SK Pop",
        "category": "硬饵",
        "sub_type": "波趴",
        "swim_layer": "水面",
        "weight_range": "8-14g",
        "length_range": "60-90mm",
        "diving_depth": "0m",
        "target_species": "翘嘴,黑鱼,大口黑鲈",
        "suitable_water_temp": "20-30",
        "suitable_water_type": "湖泊,水库,黑坑",
        "technique": "撞水手法：竿尖向下短促抽线，让杯口炸出水花；间隔 1-2 秒。",
        "color_tip": "晴天用本色或反光色；浑水用红头白身高对比。",
        "pros": "声响诱鱼力强，远距离即可发现；夏季表层猎食活跃时神器。",
        "cons": "对鱼活性要求高，低温/低活性期效果差。",
        "icon": "popper",
        "description": "经典水面系硬饵，盛夏高温清晨/傍晚专用。",
    },
    {
        "name": "MEGABASS 铅笔 Giant Dog-X",
        "category": "硬饵",
        "sub_type": "铅笔",
        "swim_layer": "水面",
        "weight_range": "11g",
        "length_range": "98mm",
        "diving_depth": "0m",
        "target_species": "翘嘴,大口黑鲈,海鲈",
        "suitable_water_temp": "18-30",
        "suitable_water_type": "湖泊,水库,海钓",
        "technique": "Walk-the-dog 左右摆头：节奏抽竿配合放线，展现鱼受惊蛇形。",
        "color_tip": "晴天银白、阴天金黄、清晨/傍晚萤光黄。",
        "pros": "大鱼专杀；摆头幅度大，水面传播视觉信号优秀。",
        "cons": "需要练习 Walk-the-dog 节奏，新手不易掌握。",
        "icon": "pencil",
        "description": "国际公认顶级铅笔代表，路亚老炮儿心头好。",
    },
    {
        "name": "DUO Realis Crank M65",
        "category": "硬饵",
        "sub_type": "Crankbait",
        "swim_layer": "中层",
        "weight_range": "12g",
        "length_range": "65mm",
        "diving_depth": "1.5-2.5m",
        "target_species": "翘嘴,鳜鱼,大口黑鲈",
        "suitable_water_temp": "15-26",
        "suitable_water_type": "水库,河流,黑坑",
        "technique": "匀速摇收，配合偶尔停顿；遇到结构故意撞击诱发反射攻击。",
        "color_tip": "清水用半透明natural色；浑水用萤光火虎纹。",
        "pros": "撞击声+震动+反光三位一体；搜索效率极高。",
        "cons": "钓深固定，需多备不同潜深款式。",
        "icon": "crank",
        "description": "中层搜索王者，水库探鱼效率最高。",
    },
    {
        "name": "Keitech Easy Shiner 4",
        "category": "软饵",
        "sub_type": "T 尾",
        "swim_layer": "中下层",
        "weight_range": "5-12g(配铅头钩)",
        "length_range": "100mm",
        "diving_depth": "1-3m",
        "target_species": "翘嘴,鳜鱼,大口黑鲈,海鲈",
        "suitable_water_temp": "10-28",
        "suitable_water_type": "全水型",
        "technique": "匀速摇收即可成型；活性低时加慢提抽诱导追击。",
        "color_tip": "Pro Blue Red Pearl/Sight Flash 双色经典，清浊水通用。",
        "pros": "全球使用率最高的 T 尾；自带盐+诱食剂；摆动节奏完美。",
        "cons": "材质较软，大鱼或牙齿利的鱼一咬就废。",
        "icon": "shad",
        "description": "全球路亚 T 尾经典款，高级钓手默认选择。",
    },
    {
        "name": "Berkley PowerBait Maxscent The General",
        "category": "软饵",
        "sub_type": "直插式 Senko",
        "swim_layer": "全泳层",
        "weight_range": "0-10g(无铅或微铅)",
        "length_range": "127mm",
        "diving_depth": "依配铅而定",
        "target_species": "大口黑鲈,海鲈",
        "suitable_water_temp": "12-28",
        "suitable_water_type": "湖泊,水库,黑坑",
        "technique": "无铅 Wacky 钓法：将钩穿过中段，落水自然抖动。",
        "color_tip": "Green Pumpkin/Watermelon Red 通杀。",
        "pros": "Maxscent 强诱食剂，无铅落水自然抖动击发反射攻击。",
        "cons": "不适合远投，主打中近距离精准搜索。",
        "icon": "stick",
        "description": "Wacky 钓法神器，黑鲈杀伤力顶级。",
    },
    {
        "name": "Strike King Bitsy Bug 1/8oz",
        "category": "钓组",
        "sub_type": "橡皮裙铅头钩 (Jig)",
        "swim_layer": "贴底",
        "weight_range": "3.5g/7g",
        "length_range": "—",
        "diving_depth": "贴底",
        "target_species": "鳜鱼,大口黑鲈",
        "suitable_water_temp": "10-22",
        "suitable_water_type": "水库,黑坑,河流",
        "technique": "贴底慢拖配合短抽；遇结构刻意撞击。",
        "color_tip": "黑紫(紫黑)、棕黑配橙花。",
        "pros": "全年通用；冷水期王者钓组；搭配软饵尾部触感丰富。",
        "cons": "新手难以分辨咬口，需要专心控竿。",
        "icon": "jig",
        "description": "美式 Jig 钓组代表，配上 Trailer 软饵贴底搜索。",
    },
    {
        "name": "Daiwa SaltigaTG Bait 60g",
        "category": "金属饵",
        "sub_type": "钨钢铁板",
        "swim_layer": "全水层垂直",
        "weight_range": "60g",
        "length_range": "65mm",
        "diving_depth": "可达 30m+",
        "target_species": "海鲈,竹梭鱼,鲣鱼",
        "suitable_water_temp": "16-28",
        "suitable_water_type": "海钓",
        "technique": "Slow Pitch Jigging：竿尖打挑+收线一格，让铁板水中翻转。",
        "color_tip": "蓝粉、夜光绿、紫红根据水深换色。",
        "pros": "钨钢密度高体积小，下沉快、阻力小；深场神器。",
        "cons": "价格昂贵；脱钩后回收成本高。",
        "icon": "jig",
        "description": "钨钢铁板顶级款，深场垂钓王者。",
    },
    {
        "name": "Z-Man ChatterBait Jack Hammer",
        "category": "硬饵",
        "sub_type": "Spinnerbait/ChatterBait",
        "swim_layer": "中层",
        "weight_range": "10-21g",
        "length_range": "—",
        "diving_depth": "0.5-2m",
        "target_species": "大口黑鲈,翘嘴",
        "suitable_water_temp": "16-28",
        "suitable_water_type": "湖泊,水库",
        "technique": "匀速摇收配偶尔抽停；草洞、水草边缘最佳。",
        "color_tip": "白闪通杀；浑水用查特酱黄。",
        "pros": "震动+闪光+刀片相撞声三重诱鱼；穿草性极佳。",
        "cons": "刀片损耗后需更换；配套 Trailer 软饵。",
        "icon": "spinnerbait",
        "description": "国际 Bass 路亚锦标赛获奖之神，钓组优中之选。",
    },
]


# ===================================================================
# 三、扩充教程（6 篇新增专题）
# ===================================================================
EXTRA_GUIDES = [
    {
        "title": "大口黑鲈黑坑实战：德州钓组与水面系切换",
        "category": "钓法",
        "target_species": "大口黑鲈",
        "season": "夏秋",
        "water_type": "黑坑",
        "summary": "国内黑坑大口黑鲈最实用的德州+水面双武器组合策略。",
        "tags": "大口黑鲈,德州,水面系,黑坑",
        "content": (
            "## 一、黑坑环境特点\n\n"
            "黑坑大口黑鲈密度高、警惕性中等，对饵的反应快慢取决于刚补鱼时间和气压。\n\n"
            "## 二、武器选择\n\n"
            "1. **德州钓组**：4-7g 子弹铅 + 4/0 子弹钩 + 4 寸虫型软饵；主攻草边、浮岛。\n"
            "2. **波趴/铅笔**：8-14g 水面系；高活性时段（清晨/傍晚/雨前）使用。\n"
            "3. **Spinnerbait**：阴天/浑水时震动+闪光双重刺激。\n\n"
            "## 三、节奏建议\n\n"
            "1. 入塘先用 Spinnerbait 大范围搜索 30 分钟；\n"
            "2. 锁定区域改德州慢拖 + 抽停；\n"
            "3. 早晚切换水面波趴诱发表层炸水。\n\n"
            "## 四、常见误区\n\n"
            "- 一直死磕一种饵；\n"
            "- 全程匀速无停顿；\n"
            "- 忽略风向与气压变化。"
        ),
    },
    {
        "title": "海鲈路亚入门：潮汐表读法与钓点选择",
        "category": "入门",
        "target_species": "海鲈",
        "season": "春秋",
        "water_type": "海钓",
        "summary": "解读潮汐表三大要素，定位海鲈黄金窗口与必杀钓点。",
        "tags": "海鲈,潮汐,海钓,入门",
        "content": (
            "## 一、潮汐三要素\n\n"
            "**涨潮、落潮、平潮**——海鲈最活跃的是涨潮中后段与落潮初段。\n\n"
            "## 二、钓点定位\n\n"
            "1. **入海口**：江海交界处饵料丰富；\n"
            "2. **港口防波堤**：水流被打乱形成回流区；\n"
            "3. **礁石突出处**：水流冲击产生流影；\n"
            "4. **桥墩水柱后**：天然伏击点。\n\n"
            "## 三、装备建议\n\n"
            "- 海钓竿 ML-M 调 2.4-2.7m\n"
            "- 4000-5000 型纺车轮，PE 1.0-1.5\n"
            "- 前导氟碳 2.5-4 号 1.5-2m\n\n"
            "## 四、常见信号\n\n"
            "海鸟密集俯冲水面 = 鱼群聚集；岸边小鱼跳出水面 = 大鲈追击。看到信号立刻投入战斗。"
        ),
    },
    {
        "title": "黄颡鱼 UL 微物：城市河道夜钓乐趣",
        "category": "钓法",
        "target_species": "黄颡鱼",
        "season": "春夏秋",
        "water_type": "河流,水库",
        "summary": "用 UL 装备钓黄颡鱼，享受高频中鱼的微物乐趣。",
        "tags": "黄颡鱼,UL,微物,夜钓",
        "content": (
            "## 一、装备配置\n\n"
            "- UL 路亚竿 1.68-1.83m\n"
            "- 1000-2000 纺车轮\n"
            "- 主线 PE 0.4-0.6 + 前导 0.8 号氟碳\n"
            "- 1-3g 铅头钩 + 1-2 寸卷尾蛆/Ned 软虫\n\n"
            "## 二、钓点选择\n\n"
            "1. 城市河道桥墩下\n"
            "2. 水库泄洪出水口缓流处\n"
            "3. 阴影遮蔽的码头边缘\n\n"
            "## 三、技术细节\n\n"
            "- 慢沉到底→短抽 2-3 下→停 5 秒\n"
            "- 黄颡鱼咬口为短促抖动，发现就抽竿\n"
            "- 摘钩戴手套，硬棘有毒\n\n"
            "## 四、最佳时段\n\n"
            "黄昏到午夜是黄金窗口；阴雨天全天可钓。"
        ),
    },
    {
        "title": "看懂气压与鱼情：路亚出钓决策手册",
        "category": "进阶",
        "target_species": "通用",
        "season": "全年",
        "water_type": "通用",
        "summary": "气压走势是路亚出钓最重要的隐藏变量，本文教你如何解读。",
        "tags": "气压,鱼情,出钓决策",
        "content": (
            "## 一、为什么气压重要？\n\n"
            "鱼鳔受气压变化影响平衡感，气压剧烈波动会导致鱼活性骤变。\n\n"
            "## 二、气压区间速查\n\n"
            "- **>1020 hPa**：鱼活性中下，靠底找鱼；\n"
            "- **1010-1020 hPa**：黄金区间，全水层活跃；\n"
            "- **1000-1010 hPa**：鱼上浮，水面系大放异彩；\n"
            "- **<1000 hPa**：闷热低压，鱼活性极差。\n\n"
            "## 三、变化趋势比绝对值更重要\n\n"
            "- 上升 3hPa/24h：鱼食欲恢复；\n"
            "- 下降 3hPa/24h：通常出钓窗口最佳（暴雨前 2-3 小时）；\n"
            "- 持平：稳定鱼情。\n\n"
            "## 四、配合风向\n\n"
            "南风+低压通常导致黑坑闭口；北风+高压秋冬出鱼概率最高。"
        ),
    },
    {
        "title": "PE 线全攻略：编号、颜色、前导线选择",
        "category": "装备",
        "target_species": "通用",
        "season": "全年",
        "water_type": "通用",
        "summary": "PE 线深度知识：8 编/12 编、染色方法、前导线连接全解。",
        "tags": "PE 线,前导,装备",
        "content": (
            "## 一、PE 线编数\n\n"
            "- 4 编：粗糙、便宜，适合新手；\n"
            "- 8 编：主流，圆度好、抛投远；\n"
            "- 12 编：高端，丝滑顶级，但价格翻倍。\n\n"
            "## 二、号数选择\n\n"
            "- UL 微物：0.4-0.6 号；\n"
            "- L/ML 通用：0.8-1.2 号；\n"
            "- M 翘嘴/鳜鱼：1.0-1.5 号；\n"
            "- MH/H 黑鱼/大物：2.0-3.0 号。\n\n"
            "## 三、颜色考量\n\n"
            "- 多色分段：方便估测距离；\n"
            "- 萤光黄：白天看线找咬口最清楚；\n"
            "- 灰绿/迷彩：警觉度高鱼种使用。\n\n"
            "## 四、前导线\n\n"
            "PE 不耐磨擦底，必须接 1-1.5m 氟碳前导。FG 结过竿圈不卡，新手可先用 8 字结+巨人结过渡。"
        ),
    },
    {
        "title": "雨季路亚专题：暴雨前后的黄金钓点",
        "category": "季节",
        "target_species": "通用",
        "season": "夏秋",
        "water_type": "通用",
        "summary": "暴雨是路亚最棒的天气信号，掌握 3 个时间窗口连竿不停。",
        "tags": "雨季,暴雨,出钓",
        "content": (
            "## 一、暴雨前 2 小时\n\n"
            "气压急速下降，鱼群预感天气变化进入疯狂进食期。**水面系最佳窗口**。\n\n"
            "## 二、暴雨期间\n\n"
            "雨水搅浑水体，鱼依靠侧线感知。**改用 VIB/Spinnerbait 等震动+闪光饵**。\n\n"
            "## 三、暴雨后 1-3 天\n\n"
            "1. 水温下降，水位上涨；\n"
            "2. 入水口冲入大量饵料；\n"
            "3. **入水口、新淹没的草丛是金矿**。\n\n"
            "## 四、安全提示\n\n"
            "- 雷暴期间禁止外出钓鱼，碳素竿是天然避雷针；\n"
            "- 山溪暴涨时撤离低洼河滩；\n"
            "- 提前查看天气雷达图。"
        ),
    },
]


# ===================================================================
# 入库逻辑
# ===================================================================
def upsert_fish(db, data: dict) -> bool:
    existing = db.query(FishEncyclopediaModel).filter(
        FishEncyclopediaModel.name == data["name"]
    ).first()
    payload = dict(
        name=data["name"], alias=data.get("alias"),
        scientific_name=data.get("scientific_name"),
        family=data.get("family"), category=data.get("category"),
        distribution=data.get("distribution"), habitat=data.get("habitat"),
        feeding_habit=data.get("feeding_habit"), body_size=data.get("body_size"),
        best_season=data.get("best_season"), best_hours=data.get("best_hours"),
        optimal_temp=data.get("optimal_temp"),
        recommended_lures=data.get("recommended_lures"),
        technique_tips=data.get("technique_tips"),
        image_url=data.get("image_url"), description=data.get("description"),
        source=data.get("source", "LureCalendar 内容团队"),
        update_time=int(time.time() * 1000),
    )
    if existing:
        for k, v in payload.items():
            if v is not None:
                setattr(existing, k, v)
        return False
    db.add(FishEncyclopediaModel(**payload))
    return True


def upsert_lure(db, data: dict) -> bool:
    existing = db.query(LureLibraryModel).filter(
        LureLibraryModel.name == data["name"]
    ).first()
    now = int(time.time() * 1000)
    payload = dict(
        name=data["name"], category=data.get("category"),
        sub_type=data.get("sub_type"), swim_layer=data.get("swim_layer"),
        weight_range=data.get("weight_range"),
        length_range=data.get("length_range"),
        diving_depth=data.get("diving_depth"),
        target_species=data.get("target_species"),
        suitable_water_temp=data.get("suitable_water_temp"),
        suitable_water_type=data.get("suitable_water_type"),
        technique=data.get("technique"), color_tip=data.get("color_tip"),
        pros=data.get("pros"), cons=data.get("cons"),
        icon=data.get("icon"), image_url=data.get("image_url"),
        description=data.get("description"),
        source=data.get("source", "LureCalendar 内容团队"),
        update_time=now,
    )
    if existing:
        for k, v in payload.items():
            if v is not None:
                setattr(existing, k, v)
        return False
    db.add(LureLibraryModel(**payload))
    return True


def upsert_guide(db, data: dict) -> bool:
    existing = db.query(FishingGuideModel).filter(
        FishingGuideModel.title == data["title"]
    ).first()
    now = int(time.time() * 1000)
    payload = dict(
        title=data["title"], category=data.get("category"),
        target_species=data.get("target_species"), season=data.get("season"),
        water_type=data.get("water_type"), summary=data.get("summary"),
        content=data.get("content"), cover_url=data.get("cover_url"),
        tags=data.get("tags"),
        source=data.get("source", "LureCalendar 内容团队"),
        source_url=data.get("source_url"),
        update_time=now,
    )
    if existing:
        for k, v in payload.items():
            if v is not None:
                setattr(existing, k, v)
        return False
    payload["create_time"] = now
    db.add(FishingGuideModel(**payload))
    return True


def run() -> None:
    db = SessionLocal()
    fi = fu = li = lu = gi = gu = 0
    try:
        for f in EXTRA_FISH:
            if upsert_fish(db, f):
                fi += 1
            else:
                fu += 1
        db.commit()
        print(f"鱼种：新增 {fi} 条，更新 {fu} 条")

        for l in EXTRA_LURES:
            if upsert_lure(db, l):
                li += 1
            else:
                lu += 1
        db.commit()
        print(f"路亚饵：新增 {li} 条，更新 {lu} 条")

        for g in EXTRA_GUIDES:
            if upsert_guide(db, g):
                gi += 1
            else:
                gu += 1
        db.commit()
        print(f"教程：新增 {gi} 篇，更新 {gu} 篇")

        print("\n全部完成。")
    except Exception as exc:
        db.rollback()
        print(f"入库失败：{exc}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    run()
