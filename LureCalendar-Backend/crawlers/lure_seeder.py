"""路亚饵知识库种子脚本
=====================

按硬饵/软饵/金属饵/水面系四大类，覆盖 25 款主流路亚饵的参数、操作手法与适用鱼种。
数据由路亚领域知识结构化整理（参考钓鱼之家、大江户钓具评测、抖音路亚达人分享）。
图片优先尝试从必应图片接口抓取，失败时使用 unsplash 兜底。

直接运行：
    python -m crawlers.lure_seeder
"""
from __future__ import annotations

import os
import sys
import time
import re
from urllib.parse import quote

import requests

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from main import SessionLocal, LureLibraryModel  # noqa: E402

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
HEADERS = {"User-Agent": UA, "Accept-Language": "zh-CN,zh;q=0.9"}

LURES = [
    # ---------- 硬饵 ----------
    {
        "name": "浮水米诺", "category": "硬饵", "sub_type": "浮水米诺(Floating Minnow)",
        "swim_layer": "上层", "weight_range": "5-12g", "length_range": "50-90mm",
        "diving_depth": "0.3-1.5m",
        "target_species": "翘嘴,鲈鱼,马口,白条",
        "suitable_water_temp": "18-28", "suitable_water_type": "河流,水库,黑坑",
        "technique": "匀收+轻抽停顿(twitch)，抽 1-2 下停 0.5-1 秒，模仿受伤小鱼。",
        "color_tip": "晴天透明/反光银，阴天荧光黄/火虎，浑水黑金",
        "pros": "通用性极强，新手友好，全水域可用。",
        "cons": "深水穿透力差，大风远投困难。",
        "icon": "minnow",
        "description": "路亚入门首选，模仿受伤小鱼形态，通过抽停产生左右晃动诱发攻击。",
    },
    {
        "name": "沉水米诺", "category": "硬饵", "sub_type": "沉水米诺(Sinking Minnow)",
        "swim_layer": "中层", "weight_range": "8-25g", "length_range": "60-110mm",
        "diving_depth": "1-3m",
        "target_species": "翘嘴,鳜鱼,鲈鱼,红尾",
        "suitable_water_temp": "12-26", "suitable_water_type": "河流,水库",
        "technique": "等饵下沉到位再匀收+轻抽，深水搜索利器；带流水抛上游摆动收。",
        "color_tip": "深水域金黑/紫黑，浅水蓝白/银白",
        "pros": "可达中下层，远投能力优于浮水米诺。",
        "cons": "易挂底，需配合软调竿减少切线。",
        "icon": "minnow",
        "description": "沉水米诺适合中层搜索，水温较低或鱼离底时性能优于浮水米诺。",
    },
    {
        "name": "深潜米诺", "category": "硬饵", "sub_type": "Deep Diver",
        "swim_layer": "底层", "weight_range": "10-30g", "length_range": "70-130mm",
        "diving_depth": "2-5m",
        "target_species": "翘嘴,鲈鱼,鳜鱼",
        "suitable_water_temp": "10-24", "suitable_water_type": "水库,深水河流",
        "technique": "高速匀收使其潜到位，再缓收带停顿，撞击底部障碍物诱发攻击。",
        "color_tip": "深水首选金黑、火虎；冬季银黑反光",
        "pros": "深水搜鱼神器，撞底动作诱鱼力强。",
        "cons": "极易挂底，建议配三本钩防挂改装。",
        "icon": "deep_minnow",
        "description": "拥有大舌板的深潜硬饵，针对水库深水区与冬季离底鱼。",
    },
    {
        "name": "VIB", "category": "硬饵", "sub_type": "Lipless Crankbait",
        "swim_layer": "全泳层", "weight_range": "7-20g", "length_range": "50-80mm",
        "diving_depth": "0.5-3m",
        "target_species": "翘嘴,鳜鱼,鲈鱼,红尾,鲶鱼",
        "suitable_water_temp": "10-28", "suitable_water_type": "河流,水库,黑坑",
        "technique": "抛投后让其下沉到目标层，匀速收线产生高频震动；冬季'抖动+长停顿'。",
        "color_tip": "经典火虎、银黑、金红；浑水夜光",
        "pros": "全层覆盖，远投能力顶级，'快速找鱼'神器。",
        "cons": "震动持续容易钝化警觉鱼，连续使用需轮换。",
        "icon": "vib",
        "description": "VIB(Vibration)无舌板硬饵，靠自身重量产生震动，是水库远投搜索的王者。",
    },
    {
        "name": "波爬", "category": "硬饵", "sub_type": "Popper(波扒)",
        "swim_layer": "水面", "weight_range": "8-20g", "length_range": "55-100mm",
        "diving_depth": "0m",
        "target_species": "翘嘴,鲈鱼,黑鱼",
        "suitable_water_temp": "20-30", "suitable_water_type": "黑坑,水库,水草边",
        "technique": "猛抽+长停顿，让饵嘴部喷溅出水花与噪音，鱼来攻击不要立刻刺鱼。",
        "color_tip": "白腹+背部荧光，浑水黑色，清水透明",
        "pros": "炸水视觉刺激强，爆护时刻最爽。",
        "cons": "仅夏季高水温有效，鱼必须活性高。",
        "icon": "popper",
        "description": "嘴部呈杯状的水面硬饵，靠喷溅水花和'啵'的声音诱鱼。",
    },
    {
        "name": "水面铅笔", "category": "硬饵", "sub_type": "Pencil(铅笔)",
        "swim_layer": "水面", "weight_range": "7-25g", "length_range": "70-130mm",
        "diving_depth": "0m",
        "target_species": "翘嘴,鲈鱼,鳡鱼",
        "suitable_water_temp": "20-30", "suitable_water_type": "水库,大型湖泊",
        "technique": "高频抽竿走'之字步(walking the dog)'，左右摆动逃逸感强。",
        "color_tip": "晴天反光银，阴天炫彩；夜晚发光体",
        "pros": "炸水视觉冲击+巨物率高，搜大水面利器。",
        "cons": "操作要诀难，新手不易玩出效果。",
        "icon": "pencil",
        "description": "无嘴部喷溅装置，靠竿尖左右抽动产生'之'字游姿，模仿逃窜小鱼。",
    },
    {
        "name": "之字狗", "category": "硬饵", "sub_type": "Walking Dog",
        "swim_layer": "水面", "weight_range": "8-18g", "length_range": "70-120mm",
        "diving_depth": "0m",
        "target_species": "翘嘴,黑鱼,鲈鱼",
        "suitable_water_temp": "22-30", "suitable_water_type": "黑坑,水草区,水库",
        "technique": "短促抽竿+收线节奏一致，让饵呈'之'字摆动；遇攻击不立刻刺鱼。",
        "color_tip": "白腹炫彩、青蛙绿、橙红",
        "pros": "黑鱼/翘嘴双效，夏季水面爆护神器。",
        "cons": "仅夏季有效，水温低于 20℃基本无口。",
        "icon": "walker",
        "description": "无嘴喷溅水面饵，靠节奏抽竿在水面 'S' 形摆动诱鱼炸水。",
    },
    {
        "name": "雷蛙", "category": "硬饵", "sub_type": "Hollow Frog(空心蛙)",
        "swim_layer": "水面", "weight_range": "10-20g", "length_range": "50-70mm",
        "diving_depth": "0m",
        "target_species": "黑鱼,鲈鱼",
        "suitable_water_temp": "22-32", "suitable_water_type": "水草区,芦苇荡,荷叶塘",
        "technique": "草缝中慢拖+点停，黑鱼炸水后顿杆 1-2 秒再大力刺鱼。",
        "color_tip": "青蛙绿、白腹、黑色为主，浑水夜光",
        "pros": "防挂草性能好，是黑鱼路亚的'大杀器'。",
        "cons": "中钩率不高，要练'死口'技巧。",
        "icon": "frog",
        "description": "中空硅胶+双钩内嵌设计的水面雷蛙，专攻水草区黑鱼。",
    },
    {
        "name": "复合亮片", "category": "硬饵", "sub_type": "Spinnerbait",
        "swim_layer": "中层", "weight_range": "7-21g", "length_range": "60-100mm",
        "diving_depth": "0.5-2m",
        "target_species": "鲈鱼,翘嘴,鳜鱼",
        "suitable_water_temp": "15-26", "suitable_water_type": "黑坑,水草区,障碍区",
        "technique": "匀速收线+偶尔大幅抽竿；可下沉到位后慢速贴底拖。",
        "color_tip": "亮片金/银+裙摆白红/黄黑",
        "pros": "防挂效果好，可在重障碍区放心搜鱼。",
        "cons": "重量大对杆要求高，不适合微物。",
        "icon": "spinner",
        "description": "由旋转叶片+裙摆+铅头钩组成的复合饵，是结构区的安全选择。",
    },
    {
        "name": "胖子", "category": "硬饵", "sub_type": "Crankbait",
        "swim_layer": "中层", "weight_range": "5-15g", "length_range": "30-70mm",
        "diving_depth": "0.5-2.5m",
        "target_species": "鲈鱼,翘嘴,马口",
        "suitable_water_temp": "15-26", "suitable_water_type": "河流,水库",
        "technique": "匀收即可，舌板让饵自然下潜与晃动；撞底/障碍后停顿吃口高发。",
        "color_tip": "黄绿火虎、黑橙、银白",
        "pros": "新手友好，匀收就有效果。",
        "cons": "深度变化小，不适合深水。",
        "icon": "crank",
        "description": "短胖体型+大舌板的硬饵，靠匀收产生大幅左右晃动。",
    },
    # ---------- 软饵 ----------
    {
        "name": "T尾软虫", "category": "软饵", "sub_type": "T-Tail Grub",
        "swim_layer": "全泳层", "weight_range": "3-15g(含铅头)", "length_range": "50-120mm",
        "diving_depth": "0-5m",
        "target_species": "鳜鱼,鲈鱼,翘嘴,鲶鱼",
        "suitable_water_temp": "10-30", "suitable_water_type": "河流,水库,黑坑",
        "technique": "配铅头钩贴底跳动；'抽-停-跳'三部曲；停顿期是高发吃口期。",
        "color_tip": "浑水深色(黑/紫)，清水自然色(白/银)",
        "pros": "通用百搭，是路亚最稳定的选择，不挂底版本可破障碍。",
        "cons": "需要配铅头钩或专用钓组，新手装配略麻烦。",
        "icon": "soft_worm",
        "description": "T 形尾巴的软质饵，匀收时尾部高频抖动，是软饵的常青树。",
    },
    {
        "name": "卷尾蛆", "category": "软饵", "sub_type": "Curl Tail Grub",
        "swim_layer": "中底层", "weight_range": "3-10g", "length_range": "40-80mm",
        "diving_depth": "0.5-3m",
        "target_species": "鳜鱼,鲈鱼,马口",
        "suitable_water_temp": "12-26", "suitable_water_type": "河流,溪流",
        "technique": "贴底慢拖；'卷尾'在缓动中也持续摆动，比 T 尾更显安静。",
        "color_tip": "白色、粉色、黄色火虎",
        "pros": "动作细腻，警觉鱼专属。",
        "cons": "尾部易被小鱼咬掉。",
        "icon": "curly",
        "description": "尾部呈螺旋卷曲的软饵，慢动作下也有持续摆动。",
    },
    {
        "name": "虾型软饵", "category": "软饵", "sub_type": "Shrimp",
        "swim_layer": "底层", "weight_range": "3-8g", "length_range": "40-100mm",
        "diving_depth": "0-3m",
        "target_species": "鳜鱼,鲈鱼,罗非鱼",
        "suitable_water_temp": "15-28", "suitable_water_type": "黑坑,水库底",
        "technique": "贴底拖+轻抽，模仿虾类倒退游动；遇障碍轻抖。",
        "color_tip": "透明、橙红、棕色自然色",
        "pros": "鱼对虾的接受度高，黑坑专属。",
        "cons": "结实度一般，连续中鱼易破损。",
        "icon": "shrimp",
        "description": "外形仿真虾的软饵，是鳜鱼、鲈鱼、罗非鱼的最爱口粮。",
    },
    {
        "name": "蠕虫软饵", "category": "软饵", "sub_type": "Senko/Stick Worm",
        "swim_layer": "中下层", "weight_range": "无铅3-10g/带铅", "length_range": "80-150mm",
        "diving_depth": "0-2m",
        "target_species": "鲈鱼",
        "suitable_water_temp": "15-26", "suitable_water_type": "黑坑,水草区",
        "technique": "无铅钓组慢沉+轻抽，水中扭动幅度大；产卵期鲈鱼最爱。",
        "color_tip": "西瓜色、黑紫、自然棕",
        "pros": "无铅自由下落动作极慢，警觉鱼难拒绝。",
        "cons": "速度慢，搜鱼效率低。",
        "icon": "worm",
        "description": "棒状软饵，无铅自由下沉时左右扭动模仿蠕虫，是 Senko 钓法核心。",
    },
    {
        "name": "Swimbait", "category": "软饵", "sub_type": "Soft Swimbait",
        "swim_layer": "中层", "weight_range": "10-30g", "length_range": "80-200mm",
        "diving_depth": "1-3m",
        "target_species": "鲈鱼,翘嘴,鳡鱼",
        "suitable_water_temp": "18-28", "suitable_water_type": "水库,黑坑",
        "technique": "匀速收线即可；模仿真实小鱼游动。",
        "color_tip": "白银反光、火虎、紫斑",
        "pros": "巨物率高，是搏大鲈鱼的利器。",
        "cons": "价格高，新手心理压力大。",
        "icon": "swimbait",
        "description": "仿真鱼形软饵+预埋铅头与三本钩，是巨物路亚的常见武器。",
    },
    # ---------- 金属饵 ----------
    {
        "name": "亮片", "category": "金属饵", "sub_type": "Spoon",
        "swim_layer": "中层", "weight_range": "3-15g", "length_range": "30-70mm",
        "diving_depth": "0.5-3m",
        "target_species": "翘嘴,马口,红尾,白条,桃花鱼",
        "suitable_water_temp": "12-28", "suitable_water_type": "河流,溪流,水库",
        "technique": "匀速收线即可；摇摆幅度自带，可加抽停产生顿停诱发攻击。",
        "color_tip": "晴天银/金，阴天荧光，浑水深色",
        "pros": "操作简单，性价比之王，远投能力好。",
        "cons": "动作单一，警觉鱼会拒口。",
        "icon": "spoon",
        "description": "经典金属饵，靠勺形外壳的反光和水阻产生摆动。",
    },
    {
        "name": "铁板", "category": "金属饵", "sub_type": "Jig",
        "swim_layer": "全泳层", "weight_range": "10-60g", "length_range": "60-120mm",
        "diving_depth": "0-15m",
        "target_species": "翘嘴,红尾,鳡鱼,海鲈",
        "suitable_water_temp": "10-26", "suitable_water_type": "河流坝下,深水库,海钓",
        "technique": "下沉到位+大幅抽竿+回收，'抽抽停'诱鱼追咬；冬季大翘必备。",
        "color_tip": "晴天反光银，浑水金红，深水夜光",
        "pros": "远投王者，深水穿透极强，秋冬米翘神器。",
        "cons": "重量大对竿要求高，挂底损失大。",
        "icon": "jig",
        "description": "高密度金属棒状饵，下沉速度快，是远投与深水搜鱼利器。",
    },
    {
        "name": "复合金属", "category": "金属饵", "sub_type": "Chatterbait",
        "swim_layer": "中层", "weight_range": "10-21g", "length_range": "60-90mm",
        "diving_depth": "0.5-2.5m",
        "target_species": "鲈鱼,翘嘴",
        "suitable_water_temp": "15-26", "suitable_water_type": "黑坑,水草区",
        "technique": "匀速收线产生强烈震动+'颤抖音'。",
        "color_tip": "黑红、白红、火虎",
        "pros": "震动+裙摆双重诱鱼，浑水利器。",
        "cons": "重量大，操作疲劳。",
        "icon": "chatter",
        "description": "顾名思义会发出'咔咔'颤动声的复合金属饵。",
    },
    # ---------- 钓组 ----------
    {
        "name": "德州钓组", "category": "钓组", "sub_type": "Texas Rig",
        "swim_layer": "底层", "weight_range": "3-15g(子弹铅)", "length_range": "依据软饵",
        "diving_depth": "0.5-5m",
        "target_species": "鲈鱼,鳜鱼,鲶鱼",
        "suitable_water_temp": "10-30", "suitable_water_type": "黑坑,水草区,重障碍区",
        "technique": "贴底拖+轻抽+长停顿，子弹铅前置可使饵贴底贯穿障碍。",
        "color_tip": "搭配软饵颜色策略",
        "pros": "防挂能力顶级，可破任何障碍区。",
        "cons": "装配略复杂，新手要练习。",
        "icon": "texas",
        "description": "由子弹铅+无倒刺钩+软饵组成的高防挂钓组，是黑坑/水草区的标配。",
    },
    {
        "name": "卡罗莱钓组", "category": "钓组", "sub_type": "Carolina Rig",
        "swim_layer": "底层", "weight_range": "5-30g(铅+珠+转环)", "length_range": "依据软饵",
        "diving_depth": "1-6m",
        "target_species": "鳜鱼,鲈鱼",
        "suitable_water_temp": "10-26", "suitable_water_type": "深水库,大江大河",
        "technique": "缓拖底，'撞击-停顿'诱鱼；前导线让饵悬浮自然下落。",
        "color_tip": "搭配软饵颜色策略",
        "pros": "搜大面积深水利器，可探水深变化。",
        "cons": "钓组长，抛投难度略高。",
        "icon": "carolina",
        "description": "由铅+玻璃珠+转环+前导线+软饵组成的远投搜底钓组。",
    },
    {
        "name": "无铅钓组", "category": "钓组", "sub_type": "Wacky/No-Sinker Rig",
        "swim_layer": "中下层", "weight_range": "0(纯软饵)", "length_range": "依据软饵",
        "diving_depth": "0-2m",
        "target_species": "鲈鱼",
        "suitable_water_temp": "16-26", "suitable_water_type": "黑坑,水草边",
        "technique": "靠软饵自重自然下沉+扭动，是产卵期鲈鱼利器。",
        "color_tip": "西瓜色、黑紫、自然棕",
        "pros": "自然下沉动作'警觉鱼克星'，产卵期专用。",
        "cons": "搜鱼效率低，需要精准点位。",
        "icon": "wacky",
        "description": "无铅且钩穿软饵中部的自由下落钓组。",
    },
    {
        "name": "倒吊钓组", "category": "钓组", "sub_type": "Drop Shot",
        "swim_layer": "中下层", "weight_range": "3-15g(咬铅)", "length_range": "依据软饵",
        "diving_depth": "1-4m",
        "target_species": "鲈鱼,鳜鱼,罗非鱼",
        "suitable_water_temp": "10-26", "suitable_water_type": "黑坑,深水库",
        "technique": "铅在下饵在上，定点抖竿让饵悬空抖动；钓警觉鱼利器。",
        "color_tip": "搭配软饵自然色为主",
        "pros": "可定点持久搜索一个标点。",
        "cons": "搜鱼效率低，对装备精度要求高。",
        "icon": "dropshot",
        "description": "倒置型钓组，铅在底，钩+软饵悬浮在上方，是精细化钓法的代表。",
    },
    {
        "name": "铅头钩+T尾", "category": "钓组", "sub_type": "Jig Head + T-Tail",
        "swim_layer": "中下层", "weight_range": "3-15g", "length_range": "50-120mm",
        "diving_depth": "0.5-4m",
        "target_species": "鳜鱼,鲈鱼,翘嘴,鲶鱼",
        "suitable_water_temp": "10-30", "suitable_water_type": "全部水域",
        "technique": "贴底跳动'抽-停-跳'；停顿吃口高发；万能搭配。",
        "color_tip": "浑水深色，清水自然色",
        "pros": "万能配置，新手老手都常用。",
        "cons": "易挂底，预备多套。",
        "icon": "jighead",
        "description": "铅头钩+T尾软虫的经典组合，路亚最常见的'万能搭配'。",
    },
    {
        "name": "内德钓组", "category": "钓组", "sub_type": "Ned Rig",
        "swim_layer": "底层", "weight_range": "2-7g(蘑菇头)", "length_range": "50-80mm",
        "diving_depth": "0-3m",
        "target_species": "鲈鱼",
        "suitable_water_temp": "8-22", "suitable_water_type": "黑坑,冷水区",
        "technique": "贴底慢拖+轻抖，软饵尾部漂浮，是冷水低活性鱼克星。",
        "color_tip": "白色、粉色、自然棕",
        "pros": "冷天低活性鱼专属武器。",
        "cons": "夏季效果一般。",
        "icon": "ned",
        "description": "蘑菇头铅+短软饵+悬浮设计的内德钓组，专攻低活性鲈鱼。",
    },
    {
        "name": "微物亮片", "category": "金属饵", "sub_type": "Micro Spoon",
        "swim_layer": "上层", "weight_range": "0.5-3g", "length_range": "20-40mm",
        "diving_depth": "0.1-1m",
        "target_species": "马口,白条,桃花鱼,溪哥",
        "suitable_water_temp": "12-26", "suitable_water_type": "溪流,小河",
        "technique": "顺水抛投+匀速回收，配 UL 微物竿享受连竿。",
        "color_tip": "金色、银色、橙红色",
        "pros": "微物路亚入门首选，连竿乐趣。",
        "cons": "不适合大鱼。",
        "icon": "micro",
        "description": "针对溪流微物路亚的小克重亮片。",
    },
]


def fetch_image(query: str) -> str:
    """优先尝试必应图片搜索（无需 key），失败回落 unsplash 占位图。
    返回 URL 被截断到 480 字符，以适配数据库 VARCHAR(500) 限制。"""
    fallback = f"https://source.unsplash.com/featured/?{quote(query)}"
    try:
        url = f"https://www.bing.com/images/search?q={quote(query)}&form=HDRSC2"
        resp = requests.get(url, headers=HEADERS, timeout=8)
        if resp.status_code == 200:
            match = re.search(r'murl&quot;:&quot;(https?://[^&]+?)&quot;', resp.text)
            if match:
                u = match.group(1).replace("\\u002F", "/")
                # 超长签名 URL 丢弃，返回后备图
                return u if len(u) <= 480 else fallback
    except Exception:
        pass
    return fallback


def upsert_lure(db, data: dict) -> bool:
    existing = db.query(LureLibraryModel).filter(
        LureLibraryModel.name == data["name"],
        LureLibraryModel.sub_type == data.get("sub_type")
    ).first()
    payload = dict(
        name=data["name"], category=data["category"],
        sub_type=data.get("sub_type"), swim_layer=data.get("swim_layer"),
        weight_range=data.get("weight_range"), length_range=data.get("length_range"),
        diving_depth=data.get("diving_depth"),
        target_species=data.get("target_species"),
        suitable_water_temp=data.get("suitable_water_temp"),
        suitable_water_type=data.get("suitable_water_type"),
        technique=data.get("technique"), color_tip=data.get("color_tip"),
        pros=data.get("pros"), cons=data.get("cons"),
        icon=data.get("icon"), image_url=data.get("image_url"),
        description=data.get("description"),
        source=data.get("source", "知识结构化整理"),
        update_time=int(time.time() * 1000),
    )
    if existing:
        for k, v in payload.items():
            if v is not None:
                setattr(existing, k, v)
        return False
    db.add(LureLibraryModel(**payload))
    return True


def run(skip_image: bool = False):
    db = SessionLocal()
    inserted = updated = 0
    try:
        for entry in LURES:
            print(f"→ 处理 {entry['name']} ({entry.get('sub_type')})")
            if not skip_image and not entry.get("image_url"):
                entry["image_url"] = fetch_image(f"路亚 {entry['name']}")
                time.sleep(0.6)
            new_record = upsert_lure(db, entry)
            if new_record:
                inserted += 1
            else:
                updated += 1
            db.commit()
        print(f"\n完成：新增 {inserted} 条，更新 {updated} 条。")
    except Exception as exc:
        db.rollback()
        print(f"路亚饵入库失败：{exc}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    run(skip_image="--no-image" in sys.argv)
