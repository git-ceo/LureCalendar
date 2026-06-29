"""鱼种百科爬虫
=============

聚焦 15 种路亚常见对象鱼，从百度百科抓取拉丁学名、科属、分布、习性、摘要与主图。
百度百科若访问失败或反爬，回落到内置兜底数据，保证种子数据稳定可用。

直接运行：
    python -m crawlers.fish_crawler
"""

from __future__ import annotations

import os
import re
import sys
import time
import json
from typing import Optional
from urllib.parse import quote

import requests
from bs4 import BeautifulSoup

# 允许从模块外部直接 python crawlers/fish_crawler.py 运行
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from main import SessionLocal, FishEncyclopediaModel  # noqa: E402

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
HEADERS = {"User-Agent": UA, "Accept-Language": "zh-CN,zh;q=0.9"}

# ---------------------------------------------------------------------------
# 15 种路亚常见对象鱼 —— 兜底字段 + 百科条目
# 当网络爬取失败时，使用 fallback 中的字段写库，保证数据可用。
# ---------------------------------------------------------------------------
FISH_LIST = [
    {
        "name": "翘嘴", "baike": "翘嘴红鲌",
        "alias": "白鱼,翘壳,翘鲌", "scientific_name": "Culter alburnus",
        "family": "鲤科 鲌亚科", "category": "路亚对象鱼",
        "distribution": "广泛分布于中国长江、黄河、珠江等大型水系及附属湖泊水库，是江河、水库的代表性凶猛鱼。",
        "habitat": "栖息于水域中上层，喜活水、洄水湾与桥墩、坝下急流，黄昏与清晨贴岸觅食。",
        "feeding_habit": "以白条、餐条等小型表层鱼为主食，凶猛肉食性，捕食时常炸水。",
        "body_size": "常见 30-60cm/0.3-2kg，大体可达 80cm+/5kg+（俗称米翘）",
        "best_season": "4-6 月、9-11 月（产卵后及秋季育肥期）",
        "best_hours": "清晨 5-8 点、傍晚 17-20 点",
        "optimal_temp": "18-26",
        "recommended_lures": "米诺,亮片,VIB,铁板,水面铅笔,波爬",
        "technique_tips": "迎风口、明暗交界处优先；中上层快收+抽停诱发追咬；秋冬贴底慢收铁板。",
    },
    {
        "name": "鳜鱼", "baike": "鳜",
        "alias": "桂鱼,季花鱼,花鲫鱼", "scientific_name": "Siniperca chuatsi",
        "family": "鮨科 鳜属", "category": "路亚对象鱼",
        "distribution": "中国除青藏高原外的广大水系均有分布，长江、珠江流域为主产区。",
        "habitat": "栖息于水域底层，喜乱石、桥墩、倒树等障碍区，昼伏夜出。",
        "feeding_habit": "肉食性，伏击型猎手，专食活体小鱼小虾。",
        "body_size": "常见 25-45cm/0.5-2kg，大体 3kg+",
        "best_season": "3-6 月、9-11 月",
        "best_hours": "清晨、黄昏与夜间",
        "optimal_temp": "16-25",
        "recommended_lures": "德州钓组,铅头钩+T尾,卡罗莱钓组,VIB,沉水铅笔",
        "technique_tips": "贴底慢跳，钓障碍区边缘；操作要带'死口'停顿；防挂钓组优先。",
    },
    {
        "name": "黑鱼", "baike": "乌鳢",
        "alias": "乌鱼,雷鱼,蛇头鱼", "scientific_name": "Channa argus",
        "family": "鳢科 鳢属", "category": "路亚对象鱼",
        "distribution": "中国除西部高原外几乎全部省份，南北方均有，水草丰茂湖泊河塘高密度。",
        "habitat": "栖息浅水区水草、芦苇、菱角丛中，能用鳔呼吸空气。",
        "feeding_habit": "凶猛肉食，捕食小鱼、青蛙、昆虫；护幼期攻击性最强。",
        "body_size": "常见 30-60cm/0.5-3kg，大体 5kg+",
        "best_season": "5-10 月（夏季最活跃）",
        "best_hours": "上午 9-11 点、下午 15-18 点",
        "optimal_temp": "22-30",
        "recommended_lures": "雷蛙,德州钓组,水面之字狗,大波扒",
        "technique_tips": "水面雷蛙'之'字慢拖；炸水后顿杆 1-2 秒再大力刺鱼。",
    },
    {
        "name": "鲶鱼", "baike": "鲇",
        "alias": "鲇鱼,塘虱,胡子鲶", "scientific_name": "Silurus asotus",
        "family": "鲇科 鲇属", "category": "路亚对象鱼",
        "distribution": "广泛分布于中国各大水系，江河、湖泊、水库底层常见。",
        "habitat": "底栖夜行性，喜深潭、洞穴、桥墩与浑浊水体。",
        "feeding_habit": "肉食性，以小鱼、虾、蛙为主食，靠嗅觉与触须捕食。",
        "body_size": "常见 30-70cm/1-5kg，大体 10kg+",
        "best_season": "5-10 月，雨后浑水时最佳",
        "best_hours": "夜间、傍晚、阴雨天",
        "optimal_temp": "20-28",
        "recommended_lures": "大软虫,大铅头钩+T尾,夜光VIB,德州钓组",
        "technique_tips": "贴底慢拖+长停顿；夜钓首选；用大体积深色软饵增加嗅觉信号。",
    },
    {
        "name": "鲈鱼", "baike": "大口黑鲈",
        "alias": "加州鲈,黑鲈,美国鲈", "scientific_name": "Micropterus salmoides",
        "family": "太阳鱼科 黑鲈属", "category": "路亚对象鱼",
        "distribution": "原产北美，国内大量人工养殖与水库放流，南方水库与黑坑常见。",
        "habitat": "栖息有结构的水域，水草、倒树、乱石、码头桩柱旁。",
        "feeding_habit": "凶猛肉食，捕食小鱼、虾、蛙、昆虫。",
        "body_size": "常见 25-50cm/0.5-3kg，大体 5kg+",
        "best_season": "3-6 月（产卵期靠岸）、9-11 月",
        "best_hours": "清晨与傍晚窗口期",
        "optimal_temp": "18-26",
        "recommended_lures": "德州钓组,无铅钓组,复合亮片,水面铅笔,深潜米诺,Swimbait",
        "technique_tips": "结构边缘下蹲打点；产卵期用慢动作贴底；高温期试水面系。",
    },
    {
        "name": "马口", "baike": "马口鱼",
        "alias": "山溪鱼,坑爬子", "scientific_name": "Opsariichthys bidens",
        "family": "鲤科 马口属", "category": "微物路亚对象鱼",
        "distribution": "中国南方溪流、江河支流广泛分布，山涧溪流密度高。",
        "habitat": "中上层活跃溪流鱼，喜清澈流水、卵石浅滩。",
        "feeding_habit": "杂食偏肉食，捕食小型水生昆虫、稚鱼。",
        "body_size": "10-20cm/30-150g",
        "best_season": "3-5 月发色期、9-11 月",
        "best_hours": "全天可钓，正午略差",
        "optimal_temp": "16-24",
        "recommended_lures": "微型亮片(1-3g),飞蝇钩,小型米诺,微型铅头钩",
        "technique_tips": "顺水抛投匀收；春季雄鱼发色，是微物路亚最美对象鱼。",
    },
    {
        "name": "红尾", "baike": "赤眼鳟",
        "alias": "红眼鱼,红尾鱼", "scientific_name": "Squaliobarbus curriculus",
        "family": "鲤科 赤眼鳟属", "category": "路亚对象鱼",
        "distribution": "中国大部分江河水系，长江、珠江流域常见。",
        "habitat": "中下层流水鱼，喜急缓交界、洄水湾。",
        "feeding_habit": "杂食偏荤，食小鱼、虫、藻类。",
        "body_size": "常见 25-40cm/0.3-1.5kg，大体 3kg+",
        "best_season": "5-10 月（夏季活性最高）",
        "best_hours": "上午、傍晚",
        "optimal_temp": "20-28",
        "recommended_lures": "亮片,铁板,VIB,铅头钩+T尾",
        "technique_tips": "急流坝下远投搜索；速收带停顿；红尾上钩冲击力大，要稳住竿。",
    },
    {
        "name": "桃花鱼", "baike": "宽鳍鱲",
        "alias": "宽鳍鱲,桃花板,溪鱼", "scientific_name": "Zacco platypus",
        "family": "鲤科 鱲属", "category": "微物路亚对象鱼",
        "distribution": "中国南方山溪、江河支流，水质清澈处常见。",
        "habitat": "中上层活水溪流鱼，喜卵石浅滩与急缓交界。",
        "feeding_habit": "杂食偏肉，食水生昆虫、小型甲壳类。",
        "body_size": "8-15cm/20-100g",
        "best_season": "3-5 月发色期最佳",
        "best_hours": "全天可钓",
        "optimal_temp": "16-22",
        "recommended_lures": "微型亮片(1-3g),飞蝇钩,微型米诺,亮片+羽毛",
        "technique_tips": "雄鱼发色期蓝紫红色彩斑斓；轻装备配 0.6-1 号 PE，享受微物乐趣。",
    },
    {
        "name": "罗非鱼", "baike": "罗非鱼",
        "alias": "非洲鲫鱼,福寿鱼", "scientific_name": "Oreochromis",
        "family": "丽鱼科 罗非鱼属", "category": "路亚对象鱼",
        "distribution": "原产非洲，国内南方广泛养殖与野化，珠三角水域常见。",
        "habitat": "中下层杂食鱼，喜温暖静水与浅滩。",
        "feeding_habit": "杂食偏荤，捕食昆虫、虾、稚鱼及水生植物。",
        "body_size": "20-40cm/0.5-2kg",
        "best_season": "5-10 月（南方全年）",
        "best_hours": "上午与傍晚",
        "optimal_temp": "22-30",
        "recommended_lures": "小型铅头钩+T尾,微型胖子,VIB,无铅钓组",
        "technique_tips": "贴底慢拖；产卵期攻击性强；冬季活性低基本不上。",
    },
    {
        "name": "鳡鱼", "baike": "鳡",
        "alias": "黄钻,黄颡,水老虎", "scientific_name": "Elopichthys bambusa",
        "family": "鲤科 鳡属", "category": "巨物路亚对象鱼",
        "distribution": "中国长江、珠江、黑龙江水系大型湖库，群体多。",
        "habitat": "水域中上层，喜开阔水面追逐鱼群。",
        "feeding_habit": "极凶猛肉食，群体捕食白条、餐条等。",
        "body_size": "常见 60-120cm/3-15kg，巨物可达 30kg+",
        "best_season": "5-10 月（夏秋追饵旺季）",
        "best_hours": "清晨炸水期、阴天",
        "optimal_temp": "20-28",
        "recommended_lures": "大铁板,大水面铅笔,大波爬,大Swimbait",
        "technique_tips": "看炸水追群打投；远投+大克重；要重型路亚装备。",
    },
    {
        "name": "青鱼", "baike": "青鱼",
        "alias": "螺蛳青,黑鲩", "scientific_name": "Mylopharyngodon piceus",
        "family": "鲤科 青鱼属", "category": "路亚对象鱼（少见）",
        "distribution": "长江中下游、珠江流域湖库为主，国内四大家鱼之一。",
        "habitat": "底层鱼，喜深水有螺蛳处。",
        "feeding_habit": "以螺蛳、河蚌、虾为食，肉食/底栖偏荤。",
        "body_size": "常见 50-100cm/3-15kg，巨物 50kg+",
        "best_season": "5-10 月",
        "best_hours": "夜间与早晨",
        "optimal_temp": "22-28",
        "recommended_lures": "螺蛳软饵,贴底大软虫,德州钓组",
        "technique_tips": "黑坑专属对象鱼；定点慢拖+长停顿；中钩力量极大。",
    },
    {
        "name": "翘嘴鳜", "baike": "斑鳜",
        "alias": "斑鳜,黑鳜", "scientific_name": "Siniperca scherzeri",
        "family": "鮨科 鳜属", "category": "路亚对象鱼",
        "distribution": "中国南方山区溪流与江河支流。",
        "habitat": "山溪急流中的乱石与岩缝。",
        "feeding_habit": "肉食伏击，食小型溪鱼。",
        "body_size": "20-35cm/0.3-1kg",
        "best_season": "5-10 月",
        "best_hours": "全天可钓",
        "optimal_temp": "18-26",
        "recommended_lures": "小型铅头钩+T尾,微型VIB,小亮片",
        "technique_tips": "急流贴底慢拖；溪流路亚精彩对象鱼。",
    },
    {
        "name": "白条", "baike": "餐条",
        "alias": "餐条,白鲦", "scientific_name": "Hemiculter leucisculus",
        "family": "鲤科 鲌亚科", "category": "微物路亚对象鱼",
        "distribution": "广泛分布于中国大江大湖。",
        "habitat": "上层鱼，开阔水域大群活动。",
        "feeding_habit": "杂食，食小昆虫、藻类、稚鱼。",
        "body_size": "10-20cm/30-100g",
        "best_season": "全年（夏季最活跃）",
        "best_hours": "全天",
        "optimal_temp": "18-28",
        "recommended_lures": "微型亮片(0.5-2g),羽毛钩,微型米诺",
        "technique_tips": "微物路亚入门首选；连竿利器；可练手感。",
    },
    {
        "name": "太阳鱼", "baike": "太阳鱼",
        "alias": "蓝鳃,英国鲫", "scientific_name": "Lepomis macrochirus",
        "family": "太阳鱼科", "category": "路亚对象鱼",
        "distribution": "原产北美，国内人工放流水库与黑坑常见。",
        "habitat": "中下层鱼，喜障碍物与浅滩。",
        "feeding_habit": "杂食偏荤，食小虫、稚鱼、虾。",
        "body_size": "10-25cm/0.1-0.5kg",
        "best_season": "5-10 月",
        "best_hours": "全天",
        "optimal_temp": "20-28",
        "recommended_lures": "微型铅头钩+T尾,微物胖子,小波爬",
        "technique_tips": "新手友好；连续中鱼很有成就感；可微物轻装备。",
    },
    {
        "name": "鲇巨脂鲤", "baike": "短盖巨脂鲤",
        "alias": "淡水鲳,巴西鲷", "scientific_name": "Piaractus brachypomus",
        "family": "脂鲤科", "category": "路亚对象鱼",
        "distribution": "原产南美，国内南方人工养殖与黑坑常见。",
        "habitat": "中下层鱼，喜温暖静水。",
        "feeding_habit": "杂食偏荤，食小鱼、虾、果实。",
        "body_size": "30-50cm/1-3kg",
        "best_season": "5-10 月",
        "best_hours": "上午与傍晚",
        "optimal_temp": "22-30",
        "recommended_lures": "复合亮片,铅头钩+T尾,小Swimbait",
        "technique_tips": "黑坑专属；牙齿锋利防咬线；中钩冲击力强。",
    },
]


def fetch_baike(item: str) -> dict:
    """从百度百科抓取一个条目的扩展信息。"""
    url = f"https://baike.baidu.com/item/{quote(item)}"
    try:
        resp = requests.get(url, headers=HEADERS, timeout=10)
        resp.encoding = "utf-8"
        if resp.status_code != 200:
            return {}
        soup = BeautifulSoup(resp.text, "html.parser")

        # 摘要
        summary_node = soup.find("div", class_="lemma-summary") or soup.find("div", class_="lemmaSummary_NJoUg")
        summary = ""
        if summary_node:
            summary = re.sub(r"\[\d+\]", "", summary_node.get_text(" ", strip=True))

        # 主图
        img_url = ""
        img = soup.find("img", class_="picture") or soup.find("img", attrs={"alt": True})
        if img and img.get("src"):
            img_url = img["src"]
            if img_url.startswith("//"):
                img_url = "https:" + img_url

        return {"description": summary[:1000], "image_url": img_url}
    except Exception as exc:
        print(f"  ! 百科请求失败 {item}: {exc}")
        return {}


def upsert_fish(db, data: dict) -> bool:
    existing = db.query(FishEncyclopediaModel).filter(FishEncyclopediaModel.name == data["name"]).first()
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
        source=data.get("source", "百度百科+人工整理"),
        update_time=int(time.time() * 1000),
    )
    if existing:
        for k, v in payload.items():
            if v is not None:
                setattr(existing, k, v)
        return False
    db.add(FishEncyclopediaModel(**payload))
    return True


def run():
    db = SessionLocal()
    inserted = 0
    updated = 0
    try:
        for entry in FISH_LIST:
            print(f"→ 处理 {entry['name']} ({entry['baike']})")
            extra = fetch_baike(entry["baike"])
            merged = {**entry, **{k: v for k, v in extra.items() if v}}
            new_record = upsert_fish(db, merged)
            if new_record:
                inserted += 1
            else:
                updated += 1
            db.commit()
            time.sleep(1.2)  # 礼貌延迟
        print(f"\n完成：新增 {inserted} 条，更新 {updated} 条。")
    except Exception as exc:
        db.rollback()
        print(f"鱼种入库失败：{exc}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    run()
