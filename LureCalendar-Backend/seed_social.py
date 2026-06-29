"""
钓友圈虚拟动态种子脚本
运行方式: python seed_social.py
会向 users + moments + likes + comments 表插入虚拟社交数据
"""

import uuid
import time
import random
import os
import sys
from datetime import datetime, timedelta

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from main import SessionLocal, UserModel, MomentModel, LikeModel, CommentModel

# ─── 虚拟用户 ──────────────────────────────────────────
VIRTUAL_USERS = [
    {"phone": "13800000001", "username": "路亚老王",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=wang",     "signature": "涪江常客，翘嘴杀手"},
    {"phone": "13800000002", "username": "鳜鱼猎手",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=gui",      "signature": "专攻鳜鱼，德州钓组忠实粉"},
    {"phone": "13800000003", "username": "马口杀手",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=makou",    "signature": "溪流微物，享受自然"},
    {"phone": "13800000004", "username": "夜钓达人",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=night",    "signature": "月光下的路亚人"},
    {"phone": "13800000005", "username": "绵阳钓王",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=mianyang", "signature": "绵阳各大水域都有我的足迹"},
    {"phone": "13800000006", "username": "黑鱼终结者",   "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=heiyu",    "signature": "雷蛙爱好者，炸水成瘾"},
    {"phone": "13800000007", "username": "新手路亚",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=newbie",   "signature": "刚入坑，请多指教"},
    {"phone": "13800000008", "username": "装备党小李",   "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=li",       "signature": "买装备比钓鱼还开心"},
    {"phone": "13800000009", "username": "涪江钓客",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=fujiang",  "signature": "涪江是我的主场"},
    {"phone": "13800000010", "username": "科学钓鱼",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=science",  "signature": "用数据和科学指导路亚"},
    {"phone": "13800000011", "username": "结构区猎人",   "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=struct",   "signature": "找结构，找鱼窝"},
    {"phone": "13800000012", "username": "清晨路亚",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=morning",  "signature": "早起的钓鱼人有鱼钓"},
    {"phone": "13800000013", "username": "周末钓手",     "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=weekend",  "signature": "工作日搬砖，周末路亚"},
    {"phone": "13800000014", "username": "鲈鱼发烧友",   "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=bass",     "signature": "鲈鱼是路亚的灵魂"},
    {"phone": "13800000015", "username": "细线搏大鱼",   "avatar_url": "https://api.dicebear.com/7.x/avataaars/png?seed=thin",     "signature": "0.8PE照样拉大物"},
]

# ─── 虚拟动态内容 ──────────────────────────────────────
POSTS = [
    {"phone": "13800000001", "content": "今天在涪江用米诺打了3条翘嘴，最大的有2斤！气压1015hPa，果然气压回升鱼口就好", "target_likes": 23, "target_comments": ["米诺什么牌子？求推荐", "气压回升确实好钓，验证了", "恭喜老王又爆护了", "翘嘴真漂亮，羡慕", "涪江哪个位置？"]},
    {"phone": "13800000002", "content": "德克萨斯钓组搞鳜鱼，软虫拖底慢收，中了一条大板鳜！结构区果然是王道", "target_likes": 45, "target_comments": ["大板鳜太帅了！", "德州钓组yyds", "软虫用什么颜色？", "结构区怎么找？新手求教", "拖底速度多快？", "这鳜鱼至少3斤吧", "羡慕，我搞了一天空军", "鳜鱼真的只认结构区", "学到了，下次试试德州", "太厉害了老哥", "求带", "这得有4斤吧"]},
    {"phone": "13800000003", "content": "溪流亮片抽马口太爽了，2号亮片连竿！清晨5-7点是黄金时段", "target_likes": 18, "target_comments": ["马口发色了吗？", "2号亮片是多少克的？", "安昌河上游吗？"]},
    {"phone": "13800000004", "content": "月圆之夜用荧光米诺搞翘嘴，水面炸裂！月相满月时翘嘴活性确实高", "target_likes": 67, "target_comments": ["满月夜钓太刺激了", "荧光米诺什么牌子？", "翘嘴夜间攻表层吗？", "月圆之夜确实出鱼", "炸水的感觉太爽了", "我也试过，满月确实好钓", "水面系晚上也能用？", "羡慕能夜钓的", "老婆不让出门夜钓", "多大的翘嘴？", "涪江还是水库？", "夜钓注意安全啊", "月相理论实锤了", "下次满月约一波", "配什么线组？"]},
    {"phone": "13800000005", "content": "仙海水库今天白条闹窝严重，换大号VIB直接干翘嘴，过滤小鱼效果拉满", "target_likes": 31, "target_comments": ["VIB多大的？", "仙海最近出鱼吗？", "白条闹窝太烦了", "大号VIB是什么思路？学到了", "仙海收费多少？", "翘嘴多大？", "绵阳钓王果然名不虚传", "下次也去仙海试试"]},
    {"phone": "13800000006", "content": "雷蛙炸黑鱼！草洞里拉出来一条3斤多的，太刺激了。夏天就该玩雷蛙", "target_likes": 89, "target_comments": ["炸水瞬间太爽了！", "雷蛙用什么颜色？", "草洞怎么找的？", "3斤多黑鱼真不小", "黑鱼吃雷蛙的动作太帅了", "夏天打黑最过瘾", "雷蛙要等几秒再抽？", "MH竿还是H竿？", "PE线用多粗？", "草区路亚防挂怎么办？", "这条真漂亮", "哪里打的？", "黑鱼炸水视频有吗？", "太猛了", "雷蛙入门推荐什么？", "我的雷蛙老被切线", "换钢丝前导试试", "芙蓉溪有黑鱼吗？", "有的，草区多", "求组队打黑", "夏天打黑最好的季节", "暴力美学"]},
    {"phone": "13800000007", "content": "第一次用铅头钩钓到鱼！虽然只是一条小鳜鱼，但真的很开心，路亚入坑了", "target_likes": 56, "target_comments": ["恭喜开竿！", "路亚入坑第一条鱼最难忘", "铅头钩是最好的入门方式", "小鳜鱼也很漂亮", "欢迎入坑，从此钱包不保", "哈哈哈前面说得对", "多大的铅头钩？", "加油，以后越钓越大", "新手建议从铅头钩开始练", "第一条鱼的感觉最珍贵", "恭喜恭喜！", "我第一条也是小鳜鱼", "路亚的魅力就在这里", "开心就好！", "继续加油", "很快就会上瘾的", "欢迎加入路亚大家庭", "记得买个好一点的竿子"]},
    {"phone": "13800000008", "content": "新入了一根UL竿，搭配1000号纺车轮抽马口，手感绝了！2克亮片抛投距离惊人", "target_likes": 12, "target_comments": ["什么牌子的UL竿？", "1000号轮搭配UL竿确实舒服", "2克能抛多远？", "装备党快乐就完事了"]},
    {"phone": "13800000009", "content": "涪江三桥下面出鱼了！用7cm米诺匀收，连续中了5条翘嘴。傍晚6-7点最佳", "target_likes": 42, "target_comments": ["三桥哪个位置？上游还是下游？", "米诺什么颜色？", "匀收速度怎么把握？", "5条翘嘴爆护了", "傍晚窗口期确实好", "涪江最近水位怎么样？", "7cm米诺够用了", "翘嘴最大多少？", "我去了空军，可能位置不对", "明天也去试试", "涪江钓客又出手了"]},
    {"phone": "13800000010", "content": "验证了气压骤降后24小时内鱼口关闭的理论，今天气压从1020降到1005，果然空军", "target_likes": 28, "target_comments": ["科学钓鱼实锤了", "气压降15hPa确实太猛了", "所以出门前一定要看气压", "LureCalendar的钓鱼指数准吗？", "配合APP看气压趋势很有用", "空军也是一种验证", "下次气压回升再去", "学到了，以后出门先看气压", "气压变化比绝对值更重要"]},
    {"phone": "13800000011", "content": "找到一个水下暗桩群，用倒钓钓组慢拖，连续3天都出鳜鱼。结构区就是鱼窝", "target_likes": 73, "target_comments": ["暗桩群怎么找到的？", "倒钓钓组太好用了", "连续3天出鱼说明是鱼窝", "结构区猎人名不虚传", "鳜鱼最大多大？", "倒钓用什么软饵？", "求坐标", "保密，自己找才有意思", "学到了，找结构很重要", "声呐探的还是盲找？", "观察水面动静就行", "鳜鱼就爱躲结构里", "太厉害了", "带我去看看呗", "哈哈不带，但可以教你找", "暗桩群用探鱼器能看到吗？"]},
    {"phone": "13800000012", "content": "5点起床值了！铅笔水面系狗走，翘嘴疯狂追饵。低光照时段表层系最好用", "target_likes": 34, "target_comments": ["狗走手法怎么练？", "早起果然有回报", "低光照+水面系绝配", "翘嘴追铅笔的画面太帅了", "5点太早了起不来", "我4点半就去了", "水面系的乐趣没玩过的不懂"]},
    {"phone": "13800000013", "content": "带儿子第一次路亚，他自己抛竿中了一条小翘嘴，高兴坏了！亲子路亚真不错", "target_likes": 95, "target_comments": ["太温馨了！", "亲子路亚最有意义", "小朋友多大？", "7岁，刚好能抛竿了", "培养下一代钓鱼人", "这才是正确的周末打开方式", "我也想带孩子去", "小翘嘴也是翘嘴！", "孩子开心最重要", "羡慕有个会钓鱼的爸爸", "以后父子同台钓鱼", "太有爱了", "周末钓手带娃模式", "哈哈哈说不定以后比你厉害", "很有可能", "下次带他去鲁班水库", "亲子时光最珍贵", "用什么竿？小朋友能抛吗？", "UL竿，很轻的", "学到了，也给儿子买一根", "赞！好爸爸", "路亚从娃娃抓起", "下次组个亲子路亚团", "好主意！", "算我一个"]},
    {"phone": "13800000014", "content": "水库深水区用深潜米诺搜底，中了一条40cm+的鲈鱼！秋季鲈鱼确实开始靠边了", "target_likes": 61, "target_comments": ["40cm鲈鱼不小了", "深潜米诺什么型号？", "秋季鲈鱼确实靠边", "鲁班水库吗？", "水库鲈鱼手感怎么样？", "深水区搜底要注意挂底", "深潜米诺下潜多深？", "鲈鱼体色真漂亮", "秋天是鲈鱼的季节", "下次也去水库试试", "搜底速度怎么把握？", "慢收加停顿", "学到了", "鲈鱼太帅了"]},
    {"phone": "13800000015", "content": "0.8PE线搏了20分钟拉上来一条4斤翘嘴，手都在抖！路亚就是这种肾上腺素飙升的感觉", "target_likes": 108, "target_comments": ["0.8PE搏4斤太刺激了", "手抖是正常的哈哈", "泄力调好了吗？", "20分钟搏鱼太爽了", "细线搏大鱼名不虚传", "0.8PE极限能拉多大？", "看技术和泄力", "4斤翘嘴在涪江算大物了", "肾上腺素飙升的感觉没错", "路亚的魅力就在这里", "太猛了老哥", "我0.6PE断过3斤的", "0.8安全一些", "搏鱼过程有视频吗？", "忘录了，光顾着拉鱼了", "哈哈哈下次一定录", "这才是路亚的灵魂", "PE线质量也很重要", "用的什么牌子？", "YGK的", "好线", "细线搏大鱼的快感无与伦比", "4斤翘嘴值了！", "20分钟手臂废了吧", "确实酸了一天", "太猛了", "恭喜恭喜", "下次目标米翘！", "加油！", "细线搏大鱼的艺术"]},
]


def _random_time_within_7_days():
    """生成最近7天内的随机时间戳（毫秒）"""
    now = datetime.now()
    delta = timedelta(
        days=random.randint(0, 6),
        hours=random.randint(5, 21),
        minutes=random.randint(0, 59),
        seconds=random.randint(0, 59),
    )
    dt = now - delta
    return int(dt.timestamp() * 1000)


def seed():
    db = SessionLocal()
    try:
        # ── 1. 创建虚拟用户 ──
        user_created = 0
        user_updated = 0
        for u in VIRTUAL_USERS:
            existing = db.query(UserModel).filter(UserModel.phone == u["phone"]).first()
            if existing:
                existing.username = u["username"]
                existing.avatar_url = u["avatar_url"]
                existing.signature = u["signature"]
                user_updated += 1
                print(f"  🔄 用户已存在，更新: {u['username']} ({u['phone']})")
            else:
                db.add(UserModel(
                    phone=u["phone"],
                    password="123456",
                    username=u["username"],
                    signature=u["signature"],
                    avatar_url=u["avatar_url"],
                    create_time=int(time.time() * 1000),
                ))
                user_created += 1
                print(f"  ✅ 新增用户: {u['username']} ({u['phone']})")
        db.flush()

        # ── 2. 插入动态 ──
        moment_created = 0
        all_phones = [u["phone"] for u in VIRTUAL_USERS]

        for idx, p in enumerate(POSTS):
            # 用内容前20字做去重
            content_prefix = p["content"][:20]
            existing_moment = db.query(MomentModel).filter(
                MomentModel.user_phone == p["phone"],
                MomentModel.content.like(f"{content_prefix}%"),
            ).first()
            if existing_moment:
                print(f"  ⏭️  动态已存在，跳过: {content_prefix}...")
                continue

            moment_id = str(uuid.uuid4())
            create_time = _random_time_within_7_days()

            db.add(MomentModel(
                id=moment_id,
                user_phone=p["phone"],
                content=p["content"],
                photos="[]",
                visibility="public",
                create_time=create_time,
            ))
            db.flush()

            # ── 3. 模拟点赞 ──
            like_count = min(p["target_likes"], len(all_phones))
            likers = random.sample(all_phones, like_count) if like_count <= len(all_phones) else all_phones
            for phone in likers:
                db.add(LikeModel(moment_id=moment_id, user_phone=phone))

            # ── 4. 模拟评论 ──
            comments_text = p.get("target_comments", [])
            for c_text in comments_text:
                commenter = random.choice([ph for ph in all_phones if ph != p["phone"]])
                comment_time = create_time + random.randint(60_000, 3_600_000)
                db.add(CommentModel(
                    moment_id=moment_id,
                    user_phone=commenter,
                    content=c_text,
                    create_time=comment_time,
                ))

            moment_created += 1
            print(f"  ✅ 动态 #{idx+1}: {content_prefix}...  👍{p['target_likes']} 💬{len(comments_text)}")

        db.commit()

        # ── 汇总 ──
        total_likes = sum(min(p["target_likes"], len(all_phones)) for p in POSTS)
        total_comments = sum(len(p.get("target_comments", [])) for p in POSTS)
        print(f"\n{'='*60}")
        print(f"  🎣 钓友圈种子数据完成！")
        print(f"  用户: 新增 {user_created} / 更新 {user_updated}")
        print(f"  动态: 新增 {moment_created} 条")
        print(f"  点赞: ~{total_likes} 条")
        print(f"  评论: ~{total_comments} 条")
        print(f"{'='*60}")

    except Exception as e:
        db.rollback()
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    print("=" * 60)
    print("  LureCalendar - 钓友圈虚拟数据种子 v1.0")
    print("=" * 60)
    print(f"  虚拟用户: {len(VIRTUAL_USERS)} 个")
    print(f"  虚拟动态: {len(POSTS)} 条")
    print("=" * 60)
    seed()
