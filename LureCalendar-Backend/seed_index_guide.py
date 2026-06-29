"""
seed_index_guide.py - 指数解读知识数据初始化脚本
创建 index_guide 和 species_guide 表并插入完整数据
支持重复运行（先清空再插入）
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from main import SessionLocal, engine
from sqlalchemy import text
import json


def create_tables():
    """创建 index_guide 和 species_guide 表"""
    ddl_index_guide = """
    CREATE TABLE IF NOT EXISTS index_guide (
        id INT AUTO_INCREMENT PRIMARY KEY,
        category VARCHAR(50) NOT NULL COMMENT '分类：score/time_period/species/trend/calculation/faq',
        title VARCHAR(100) NOT NULL COMMENT '标题',
        subtitle VARCHAR(200) COMMENT '副标题',
        content TEXT NOT NULL COMMENT '详细内容',
        icon VARCHAR(50) COMMENT '图标标识',
        sort_order INT DEFAULT 0 COMMENT '排序',
        extra_data JSON COMMENT '额外数据（如分数区间、鱼种参数等）',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

    ddl_species_guide = """
    CREATE TABLE IF NOT EXISTS species_guide (
        id INT AUTO_INCREMENT PRIMARY KEY,
        species_name VARCHAR(50) NOT NULL COMMENT '鱼种名称',
        key_factors VARCHAR(200) COMMENT '关键影响因素',
        best_time VARCHAR(200) COMMENT '最佳时段描述',
        care_about VARCHAR(500) COMMENT '它更在意什么',
        how_to_understand VARCHAR(500) COMMENT '怎么理解',
        preferred_conditions JSON COMMENT '偏好条件（温度/气压/风速等）',
        lure_tips VARCHAR(500) COMMENT '路亚饵料建议',
        sort_order INT DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

    with engine.connect() as conn:
        conn.execute(text(ddl_index_guide))
        conn.execute(text(ddl_species_guide))
        conn.commit()
    print("[OK] 表 index_guide 和 species_guide 创建/确认完成")


def seed_index_guide():
    """插入指数解读知识数据"""

    # === category = 'score'（分数代表什么）===
    score_data = [
        {
            "category": "score",
            "title": "分数代表什么",
            "subtitle": "0-100 分",
            "content": "分数越高，说明外部条件越适合鱼活动；分数低，不代表完全不能钓，只是预期要放低。",
            "icon": "score",
            "sort_order": 1,
            "extra_data": json.dumps({
                "ranges": [
                    {"min": 0, "max": 39, "label": "不太建议", "icon": "close", "description": "天气、风雨或风险不占优，更适合练竿、短试，别专门跑远路。", "color": "#F44336"},
                    {"min": 40, "max": 59, "label": "一般", "icon": "approximate", "description": "能钓，但别从早守到晚。优先选熟悉钓点，没口就及时调整。", "color": "#FF9800"},
                    {"min": 60, "max": 79, "label": "可以去", "icon": "check", "description": "条件还不错，重点看推荐时段和目标鱼种，不要只盯总分。", "color": "#4CAF50"},
                    {"min": 80, "max": 100, "label": "很适合", "icon": "star", "description": "值得安排出钓，但钓位、饵料、钓法和现场水情仍然很关键。", "color": "#FFD700"}
                ]
            }, ensure_ascii=False)
        }
    ]

    # === category = 'time_period'（黄金时段）===
    time_data = [
        {
            "category": "time_period",
            "title": "黄金时段怎么用",
            "subtitle": "看窗口",
            "content": "一天里鱼口不是平均分布的。好时段的意思是：那段时间更值得认真钓。",
            "icon": "time",
            "sort_order": 1,
            "extra_data": json.dumps({
                "periods": [
                    {"type": "推荐时段", "example": "15:00-22:00", "level": "高", "tip": "提前30-60分钟到钓点，先找水层、看风向、水色和鱼活动。"},
                    {"type": "普通时段", "example": "清晨/上午", "level": "中", "tip": "可以试，但如果一直没口，不建议在低效时间硬守。"}
                ]
            }, ensure_ascii=False)
        }
    ]

    # === category = 'trend'（走势）===
    trend_data = [
        {
            "category": "trend",
            "title": "走势比单个分数更有用",
            "subtitle": "示例",
            "content": "看曲线是为了知道鱼口大概什么时候变好，决定早点去还是晚点去。",
            "icon": "trend",
            "sort_order": 1,
            "extra_data": json.dumps({
                "example_hours": [0, 3, 6, 9, 12, 15, 18, 21, 24],
                "example_scores": [35, 40, 55, 60, 58, 65, 72, 75, 78],
                "species": "翘嘴"
            }, ensure_ascii=False)
        }
    ]

    # === category = 'calculation'（怎么算）===
    calc_data = [
        {
            "category": "calculation",
            "title": "指数大概怎么算",
            "subtitle": "说明",
            "content": "不用记公式，核心就是下面几类因素一起看。",
            "icon": "calculator",
            "sort_order": 1,
            "extra_data": json.dumps({
                "factors": [
                    {"name": "天气和水情", "weight": 30, "description": "气压、温度、风、雨、水温、水位变化，都会影响鱼开不开口。"},
                    {"name": "时间和季节", "weight": 25, "description": "清晨、傍晚、夜间、季节变化，会影响鱼活动的高峰时间。"},
                    {"name": "鱼种习性", "weight": 20, "description": "翘嘴、鳜鱼、鲈鱼、黑鱼关注的条件不同，所以分数会按鱼种调整。"},
                    {"name": "光照条件", "weight": 15, "description": "光照强度影响饵鱼活动和掠食鱼视觉捕食效率。"},
                    {"name": "月相周期", "weight": 10, "description": "新月满月前后鱼类活动频繁，与潮汐和光照周期相关。"}
                ],
                "summary": "简单记：天气别太差、时间赶得上、目标鱼正合适，指数就会高。遇到雷雨、大风、涨水，指数会更保守。"
            }, ensure_ascii=False)
        }
    ]

    # === category = 'faq'（容易误会的点）===
    faq_data = [
        {
            "category": "faq",
            "title": "几个容易误会的点",
            "subtitle": "指数是辅助判断，不是鱼获保证。",
            "content": "",
            "icon": "faq",
            "sort_order": 1,
            "extra_data": json.dumps({
                "items": [
                    {"title": "高分也可能钓不到", "content": "钓点有没有鱼、钓位对不对、饵料和钓法合不合适，都会影响结果。"},
                    {"title": "低分也不是完全不能钓", "content": "低分只是说明外部条件不占优，更适合短试、练竿或降低预期。"},
                    {"title": "安全永远优先", "content": "雷雨、大风、涨水、冰面不稳时，不管分数多高都不建议冒险。"}
                ],
                "disclaimer": "钓鱼指数仅用于辅助判断，不构成鱼获承诺。请结合当地法规、禁渔期和现场安全情况出钓。"
            }, ensure_ascii=False)
        }
    ]

    all_guide_data = score_data + time_data + trend_data + calc_data + faq_data

    # 清空并插入
    with engine.connect() as conn:
        conn.execute(text("DELETE FROM index_guide"))
        conn.commit()

    db = SessionLocal()
    try:
        for item in all_guide_data:
            db.execute(text("""
                INSERT INTO index_guide (category, title, subtitle, content, icon, sort_order, extra_data)
                VALUES (:category, :title, :subtitle, :content, :icon, :sort_order, :extra_data)
            """), item)
        db.commit()
        print(f"[OK] index_guide 插入 {len(all_guide_data)} 条记录")
    finally:
        db.close()


def seed_species_guide():
    """插入鱼种指南数据"""
    species_data = [
        {
            "species_name": "翘嘴",
            "key_factors": "更看风、光照和水面动静",
            "best_time": "傍晚、夜间、风浪适中时更容易有机会",
            "care_about": "水面活性、明暗变化、水流和小鱼聚集",
            "how_to_understand": "综合分高，不代表每种鱼都高；切到目标鱼后再看更准。",
            "preferred_conditions": json.dumps({"temp_range": "18-28", "pressure": "1005-1020", "wind": "2-4级", "best_lures": ["米诺", "铅笔", "VIB", "亮片"]}, ensure_ascii=False),
            "lure_tips": "低光照用荧光色米诺，白天用自然色VIB搜索，风浪天用亮片远投",
            "sort_order": 1
        },
        {
            "species_name": "鳜鱼",
            "key_factors": "更看水温、结构区和底层活动",
            "best_time": "清晨、黄昏，水温20-26°C时最活跃",
            "care_about": "水底结构（石堆/暗桩/坎沿）、水温稳定性、水流缓急",
            "how_to_understand": "鳜鱼是伏击型选手，找对结构比看天气更重要。",
            "preferred_conditions": json.dumps({"temp_range": "20-26", "pressure": "1008-1018", "wind": "1-3级", "best_lures": ["软虫", "卷尾蛆", "德克萨斯钓组", "铅头钩"]}, ensure_ascii=False),
            "lure_tips": "德克萨斯钓组+软虫拖底慢收，倒钓钓组在障碍区精细作钓",
            "sort_order": 2
        },
        {
            "species_name": "鲈鱼",
            "key_factors": "更看水温、风向和饵鱼活动",
            "best_time": "秋季靠边期、清晨和傍晚黄金窗口",
            "care_about": "水温骤变方向、岸边饵鱼密度、水下结构",
            "how_to_understand": "鲈鱼跟着饵鱼走，找到饵鱼群就找到了鲈鱼。",
            "preferred_conditions": json.dumps({"temp_range": "16-25", "pressure": "1010-1025", "wind": "2-4级", "best_lures": ["深潜米诺", "摇滚虫", "铅头钩+卷尾", "spinnerbait"]}, ensure_ascii=False),
            "lure_tips": "深水用深潜米诺搜底，浅水用spinnerbait快速搜索覆盖水层",
            "sort_order": 3
        },
        {
            "species_name": "黑鱼",
            "key_factors": "更看水温、草区密度和光照",
            "best_time": "夏季高温期、中午前后、草洞边",
            "care_about": "水草覆盖率、水温(25°C+)、繁殖期护仔行为",
            "how_to_understand": "黑鱼领地性强，找到草洞就等于找到鱼。",
            "preferred_conditions": json.dumps({"temp_range": "25-32", "pressure": "1000-1015", "wind": "0-2级", "best_lures": ["雷蛙", "蛙形软饵", "德克萨斯钓组"]}, ensure_ascii=False),
            "lure_tips": "雷蛙打草洞最刺激，护仔期黑鱼攻击性极强，点射草边等炸水",
            "sort_order": 4
        },
        {
            "species_name": "马口",
            "key_factors": "更看水质、溪流流速和光照",
            "best_time": "清晨5-7点、傍晚日落前，溪流浅滩",
            "care_about": "水质清澈度、水流速度、水温(15-22°C为佳)",
            "how_to_understand": "马口喜欢流水和清澈环境，找到溪流急流缓流交界就有鱼。",
            "preferred_conditions": json.dumps({"temp_range": "15-22", "pressure": "1005-1025", "wind": "0-2级", "best_lures": ["1-3g亮片", "微型米诺", "飞蝇钩"]}, ensure_ascii=False),
            "lure_tips": "UL竿+1-3克亮片顺流抛逆流收，微型米诺在缓流区匀收",
            "sort_order": 5
        },
        {
            "species_name": "鳡鱼",
            "key_factors": "更看水流、饵鱼密度和季节",
            "best_time": "春末秋初、江河大水面、洄水区",
            "care_about": "水流强度、饵鱼聚集区、深浅交界处",
            "how_to_understand": "鳡鱼是开放水域的顶级掠食者，追着鱼群走。",
            "preferred_conditions": json.dumps({"temp_range": "20-28", "pressure": "1005-1020", "wind": "2-5级", "best_lures": ["大号VIB", "金属片", "15cm+米诺"]}, ensure_ascii=False),
            "lure_tips": "重型装备远投大号VIB快速搜索，洄水区定点等待鱼群经过",
            "sort_order": 6
        },
        {
            "species_name": "军鱼",
            "key_factors": "更看水质、水温和觅食规律",
            "best_time": "清晨、雨后溪流涨水时",
            "care_about": "水质清澈、果实掉落区、缓流深潭",
            "how_to_understand": "军鱼杂食偏素，雨后冲刷带来食物时最活跃。",
            "preferred_conditions": json.dumps({"temp_range": "18-26", "pressure": "1005-1020", "wind": "0-3级", "best_lures": ["小型米诺", "软虫", "仿生果实饵"]}, ensure_ascii=False),
            "lure_tips": "小型自然色米诺在深潭口慢收，雨后涨水是绝佳窗口期",
            "sort_order": 7
        }
    ]

    # 清空并插入
    with engine.connect() as conn:
        conn.execute(text("DELETE FROM species_guide"))
        conn.commit()

    db = SessionLocal()
    try:
        for item in species_data:
            db.execute(text("""
                INSERT INTO species_guide (species_name, key_factors, best_time, care_about, how_to_understand, preferred_conditions, lure_tips, sort_order)
                VALUES (:species_name, :key_factors, :best_time, :care_about, :how_to_understand, :preferred_conditions, :lure_tips, :sort_order)
            """), item)
        db.commit()
        print(f"[OK] species_guide 插入 {len(species_data)} 条记录")
    finally:
        db.close()


if __name__ == "__main__":
    print("=" * 50)
    print("指数解读知识数据初始化")
    print("=" * 50)
    create_tables()
    seed_index_guide()
    seed_species_guide()
    print("=" * 50)
    print("全部完成！")
