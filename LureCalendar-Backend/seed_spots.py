"""
绵阳路亚标点种子数据脚本 (v2.0)
运行方式: python seed_spots.py
会向数据库插入绵阳全市范围内的路亚专属标点数据
数据来源: 钓鱼之家、抖音路亚达人分享、江油论坛、实地钓友提供
"""

import uuid
import time
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from main import SessionLocal, FishingSpotModel

# ─── 绵阳路亚标点大全 ──────────────────────────────────────────
# 字段说明:
#   target_species: 目标鱼种（逗号分隔）
#   lure_types:     推荐饵型（逗号分隔）
#   best_season:    最佳作钓季节/时间窗口
#   water_type:     水体类型
#   structure:      标点结构特征

SPOTS = [
    # ============================================================
    #  A. 江河自然标点（涪江流域）
    # ============================================================
    {
        "name": "三江大坝（三江半岛）",
        "latitude": 31.4620,
        "longitude": 104.7530,
        "river": "涪江/安昌河/芙蓉溪交汇",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区三江半岛（涪江、安昌河、芙蓉溪三江交汇处）",
        "water_type": "河流",
        "structure": "坝下急流、洄水湾、乱石底",
        "depth": 4.0,
        "target_species": "鳜鱼,翘嘴,红尾,鲶鱼",
        "lure_types": "铅头钩+T尾,卡罗莱钓组,铁板,VIB,沉水铅笔",
        "best_season": "3-6月,9-11月（鳜鱼窗口期清晨/黄昏）",
        "note": "绵阳最网红的路亚标点！三江交汇处鳜鱼密度高，坝下急流区是鳜鱼黄金标点。注意安全，坝区水流湍急。免费野钓，遵守禁渔期规定。抖音大量路亚视频在此拍摄。"
    },
    {
        "name": "涪江东方红大桥段",
        "latitude": 31.4685,
        "longitude": 104.7480,
        "river": "涪江",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区东方红大桥上下游约500米范围",
        "water_type": "河流",
        "structure": "桥墩结构、深浅交界、缓流区",
        "depth": 3.0,
        "target_species": "翘嘴,鳜鱼,红尾,鲶鱼",
        "lure_types": "亮片,铁板,米诺,铅头钩+T尾",
        "best_season": "4-6月,9-11月（翘嘴窗口期清晨5-8点/傍晚17-20点）",
        "note": "市区最方便的野钓标点。桥墩附近结构复杂，藏鱼多。翘嘴个体最大记录米级。夏季夜钓翘嘴效果好。免费。"
    },
    {
        "name": "安昌河（飞来石-安昌桥段）",
        "latitude": 31.4650,
        "longitude": 104.7380,
        "river": "安昌河",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区安昌桥至飞来石大桥河段",
        "water_type": "河流",
        "structure": "乱石滩、缓流、浅滩",
        "depth": 1.5,
        "target_species": "鲈鱼,马口,桃花鱼,鳜鱼",
        "lure_types": "小亮片,微物米诺,铅头钩+小T尾,飞蝇钩",
        "best_season": "3-5月,9-11月（马口桃花春季发色期最佳）",
        "note": "绵阳微物路亚天堂！安昌河马口、桃花鱼（宽鳍鱲）资源丰富，发色个体非常漂亮。近年有钓友在此钓获斤鲈。上游水质更好。免费。"
    },
    {
        "name": "芙蓉溪（游仙段）",
        "latitude": 31.4750,
        "longitude": 104.7850,
        "river": "芙蓉溪",
        "city": "绵阳市",
        "location_detail": "绵阳市游仙区芙蓉溪游仙镇至科学城段",
        "water_type": "河流",
        "structure": "水草区、倒树、缓流洄湾",
        "depth": 2.0,
        "target_species": "翘嘴,鲈鱼,黑鱼,马口",
        "lure_types": "波爬,之字狗,雷蛙,复合亮片,小胖子",
        "best_season": "4-10月（黑鱼夏季最佳，翘嘴春秋窗口期）",
        "note": "游仙区热门路亚标点。水草区适合打黑鱼，早晚窗口期翘嘴炸水。有钓友在此钓获鲈鱼。禁渔期（3-6月）注意遵守。免费。"
    },
    {
        "name": "涪江（江油城区段）",
        "latitude": 31.7800,
        "longitude": 104.7400,
        "river": "涪江",
        "city": "绵阳市",
        "location_detail": "绵阳市江油市涪江城区段（涪江一桥至三桥）",
        "water_type": "河流",
        "structure": "桥墩、坝下、深浅交接",
        "depth": 3.5,
        "target_species": "翘嘴,鳜鱼,红尾,鲶鱼",
        "lure_types": "铁板,亮片,沉水铅笔,铅头钩+软饵",
        "best_season": "4-6月,9-11月",
        "note": "江油市区主要路亚标点。涪江穿城而过，桥墩和坝下是核心标点。翘嘴、鳜鱼均有钓获记录。红尾个体不错。免费野钓。"
    },
    {
        "name": "梓江（梓潼段）",
        "latitude": 31.6350,
        "longitude": 105.1700,
        "river": "梓江",
        "city": "绵阳市",
        "location_detail": "绵阳市梓潼县梓江流域（文昌镇至长卿镇段）",
        "water_type": "河流",
        "structure": "急流与缓流交界、深潭、岩壁",
        "depth": 3.0,
        "target_species": "翘嘴,鳜鱼,红尾",
        "lure_types": "铁板,亮片,VIB,铅头钩+卷尾蛆",
        "best_season": "9-12月（秋冬米翘季节！天冷大翘嘴靠岸）",
        "note": "梓江是绵阳出'米翘'的著名标点！天冷后大翘嘴靠岸觅食，铁板远投效果拔群。鳜鱼也很多。注意禁渔期。免费野钓。"
    },
    {
        "name": "凯江（三台西平段）",
        "latitude": 31.0200,
        "longitude": 104.9000,
        "river": "凯江",
        "city": "绵阳市",
        "location_detail": "绵阳市三台县西平镇与中江县交界凯江河段（虾子沟附近）",
        "water_type": "河流",
        "structure": "深浅交接、洄水湾、乱石底",
        "depth": 2.5,
        "target_species": "红尾,翘嘴,鳜鱼",
        "lure_types": "亮片,铁板,VIB,铅头钩+T尾",
        "best_season": "5-10月（红尾活性最高）",
        "note": "凯江红尾资源极好！抖音钓友称'红尾爆拉'。翘嘴和鳜鱼也有。西平镇虾子沟附近是核心标点。免费野钓。"
    },
    {
        "name": "盐亭梓江/弥江",
        "latitude": 31.2100,
        "longitude": 105.3900,
        "river": "梓江/弥江",
        "city": "绵阳市",
        "location_detail": "绵阳市盐亭县梓江与弥江交汇区域",
        "water_type": "河流",
        "structure": "两河交汇、陡坡、岩壁结构",
        "depth": 3.0,
        "target_species": "翘嘴,鳜鱼,红尾,鲈鱼",
        "lure_types": "铁板,铅头钩+软饵,亮片,深潜米诺",
        "best_season": "3-5月,9-11月",
        "note": "盐亭是四川鳜鱼养殖大县（年产1600余吨），野生鳜鱼资源丰富！梓江和弥江两河交汇处是核心标点。翘嘴红尾鳜鱼均有。免费野钓。"
    },
    {
        "name": "潼江（梓潼段）",
        "latitude": 31.6100,
        "longitude": 105.1900,
        "river": "潼江",
        "city": "绵阳市",
        "location_detail": "绵阳市梓潼县潼江流域（宏仁镇至自强镇段）",
        "water_type": "河流",
        "structure": "岩壁陡坡、深潭、倒树",
        "depth": 3.0,
        "target_species": "翘嘴,鳜鱼,鲶鱼",
        "lure_types": "铅头钩+卷尾蛆,VIB,沉水铅笔,铁板",
        "best_season": "4-6月,9-11月",
        "note": "梓潼另一条重要路亚河流。岩壁结构多，是鳜鱼喜欢的藏身之处。翘嘴个体尚可。免费野钓。"
    },
    {
        "name": "武都水库下游涪江段",
        "latitude": 31.8500,
        "longitude": 104.7800,
        "river": "涪江",
        "city": "绵阳市",
        "location_detail": "绵阳市江油市武都镇武都水库大坝下游至中坝镇涪江段",
        "water_type": "河流",
        "structure": "坝下急流、乱石滩、洄水湾",
        "depth": 4.0,
        "target_species": "翘嘴,红尾,鳜鱼,鲶鱼",
        "lure_types": "铁板,亮片,铅头钩+大T尾,沉水铅笔",
        "best_season": "5-10月（红尾夏季最活跃）",
        "note": "武都水库大坝下游是江油路亚热门标点。坝下急流区含氧量高，红尾成群。翘嘴、鳜鱼也有。注意安全，泄洪期勿近。免费。"
    },
    # ============================================================
    #  B. 湖库标点
    # ============================================================
    {
        "name": "鲁班水库（路亚）",
        "latitude": 30.9952,
        "longitude": 105.0823,
        "river": "鲁班水库",
        "city": "绵阳市",
        "location_detail": "绵阳市三台县鲁班镇鲁班水库（四川第三大人工湖）",
        "water_type": "水库",
        "structure": "深浅变化大、水下暗岛、陡坡、淹没树林",
        "depth": 15.0,
        "target_species": "鲈鱼,翘嘴,鳜鱼,黑鱼,鲶鱼",
        "lure_types": "德州钓组,倒吊钓组,复合亮片,深潜米诺,铁板,波爬",
        "best_season": "3-6月,9-11月（春产卵季鲈鱼靠岸，秋翘嘴窗口期）",
        "note": "绵阳头号路亚圣地！面积大、结构复杂、鱼种丰富。鲈鱼种群稳定，翘嘴有米级。推荐船钓覆盖更多标点。白天收费约40元，过夜60元。钓获过5kg+巨鲈。"
    },
    {
        "name": "仙海湖（沉抗水库）路亚标点",
        "latitude": 31.4389,
        "longitude": 104.8152,
        "river": "沉抗水库",
        "city": "绵阳市",
        "location_detail": "绵阳市游仙区沉抗镇仙海水利风景区",
        "water_type": "水库",
        "structure": "陡坡结构、岩壁、水下台地、码头",
        "depth": 12.0,
        "target_species": "翘嘴,鲈鱼,鳜鱼,黑鱼",
        "lure_types": "铁板,深潜米诺,复合亮片,铅头钩+软饵,波爬",
        "best_season": "4-6月,9-11月（清晨/黄昏窗口期）",
        "note": "国家级水利风景区，环境一流。翘嘴资源好，码头附近和陡坡处是核心标点。部分区域收费。景区内注意文明作钓。距绵阳市区仅15km。"
    },
    {
        "name": "武都水库（涪江六峡）",
        "latitude": 31.9100,
        "longitude": 104.8200,
        "river": "武都水库",
        "city": "绵阳市",
        "location_detail": "绵阳市江油市武都镇武都水库（涪江六峡景区内）",
        "water_type": "水库",
        "structure": "峡谷型水库、陡峭岩壁、深水、水下暗礁",
        "depth": 30.0,
        "target_species": "翘嘴,红尾,鲈鱼,鳜鱼",
        "lure_types": "铁板,深潜米诺,沉水铅笔,VIB,铅头钩+大软饵",
        "best_season": "5-10月（峡谷水温回升后鱼口好）",
        "note": "峡谷型水库，水深30米+，是路亚大翘嘴的绝佳标点。红尾资源丰富。岩壁结构适合搜鳜鱼。需要远投装备。注意安全，峡谷地势险要。"
    },
    {
        "name": "燕儿河水库",
        "latitude": 31.4000,
        "longitude": 104.6500,
        "river": "燕儿河",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区吴家镇燕儿河水库",
        "water_type": "水库",
        "structure": "土坎、淹没草区、浅滩",
        "depth": 5.0,
        "target_species": "鲈鱼,黑鱼,翘嘴",
        "lure_types": "德州钓组,雷蛙,复合亮片,水面系",
        "best_season": "4-10月",
        "note": "涪城区较近的水库标点。有野化鲈鱼种群，黑鱼在草区出没。适合岸钓路亚。免费或低费。具体收费请现场确认。"
    },
    {
        "name": "西五八水库（蝶湖）路亚",
        "latitude": 31.4762,
        "longitude": 104.9134,
        "river": "西五八水库",
        "city": "绵阳市",
        "location_detail": "绵阳市游仙区云凤镇晒石板西五八水库",
        "water_type": "水库",
        "structure": "结构复杂、草区、深浅交替、水下障碍",
        "depth": 3.0,
        "target_species": "翘嘴,鳜鱼,马口,黑鱼",
        "lure_types": "铁板,亮片,米诺,铅头钩+软饵,雷蛙",
        "best_season": "3-6月,9-11月",
        "note": "占地400亩的综合钓场。路亚对象鱼多：翘嘴、鳜鱼、马口。配套露营烧烤。收费60元/5小时。环境优美，周末休闲好去处。"
    },
    # ============================================================
    #  C. 商业路亚基地
    # ============================================================
    {
        "name": "蟠龙路亚基地",
        "latitude": 31.4551,
        "longitude": 104.7422,
        "river": "蟠龙路亚基地",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区二环路外侧约500米（高德导航'蟠龙路亚基地'）",
        "water_type": "黑坑",
        "structure": "人造结构丰富、深浅区、障碍区",
        "depth": 1.5,
        "target_species": "鲈鱼,鳜鱼,太阳鱼,大口鲶",
        "lure_types": "德州钓组,倒吊,铅头钩+T尾,小型复合亮片",
        "best_season": "全年营业（冬季鲈鱼活性稍低）",
        "note": "绵阳老牌路亚基地，面积10亩。鱼种多：太阳鱼、鲈鱼、鳜鱼、大口鲶等。约100元/天。电话：18144364717。适合新手练竿和老手过瘾。"
    },
    {
        "name": "七星渔乐路亚基地",
        "latitude": 31.3721,
        "longitude": 104.8257,
        "river": "七星渔乐路亚基地",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区丰谷镇杨家湾（导航'七星漁樂路亚基地'）",
        "water_type": "黑坑",
        "structure": "障碍区、水草、倒树模拟",
        "depth": 2.0,
        "target_species": "鲈鱼",
        "lure_types": "精细钓组,德州,倒吊,无铅,小型Swimbait",
        "best_season": "全年营业",
        "note": "专业路亚基地，面积4亩，水深1-3米。鲈鱼为主，水质达养殖标准。约288元/天。配套餐饮齐全。老板口碑好。"
    },
    {
        "name": "九九路亚基地",
        "latitude": 31.4480,
        "longitude": 104.7350,
        "river": "九九路亚基地",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区长虹大道南段172号（二九基地内）",
        "water_type": "黑坑",
        "structure": "障碍区、深坑、水草",
        "depth": 2.0,
        "target_species": "鳜鱼,鲈鱼",
        "lure_types": "铅头钩+T尾,内德钓组,卡罗莱钓组",
        "best_season": "全年营业（定期放标鳜）",
        "note": "绵阳知名黑坑路亚基地。定期放鳜鱼，如11月放标鳜1100斤。抖音热度高。鳜鱼为主，适合喜欢搏鳜鱼的钓友。具体收费请电话咨询。"
    },
    {
        "name": "山澜水韵路亚基地",
        "latitude": 31.4700,
        "longitude": 104.8700,
        "river": "山澜水韵路亚基地",
        "city": "绵阳市",
        "location_detail": "绵阳市游仙区（四川山澜水韵乡村旅游有限公司，导航搜索'山澜水韵路亚基地'）",
        "water_type": "黑坑",
        "structure": "深浅滩、水草区、障碍区（还原野钓场景）",
        "depth": 3.0,
        "target_species": "鲈鱼,鳜鱼,翘嘴",
        "lure_types": "全套路亚饵适用（老板称'抛竿就有口'）",
        "best_season": "全年营业",
        "note": "'路亚人的快乐星球'！超大面积水域，标点丰富，还原野钓场景。鲈鱼、鳜鱼、翘嘴密度在线，活性爆棚。抖音热度高。新开业，设施新。"
    },
    {
        "name": "山中湖路亚基地",
        "latitude": 31.4200,
        "longitude": 104.6700,
        "river": "山中湖",
        "city": "绵阳市",
        "location_detail": "绵阳市涪城区汉蓉高速旁（距绵阳主城区约25km，导航'山中湖'）",
        "water_type": "黑坑",
        "structure": "山地水库型、深浅交替",
        "depth": 4.0,
        "target_species": "鲈鱼,鳜鱼",
        "lure_types": "德州钓组,铅头钩+软饵,复合亮片",
        "best_season": "全年营业",
        "note": "地理位置优越，坐落在汉蓉高速旁。距成都约100km，绵阳25km，德阳也近。山地水库环境，风景好。具体收费请电话咨询。"
    },
    {
        "name": "绵阳九岭路亚钓场",
        "latitude": 31.5374,
        "longitude": 104.6923,
        "river": "九岭",
        "city": "绵阳市",
        "location_detail": "绵阳市江油市青莲镇肖家老房子",
        "water_type": "黑坑",
        "structure": "障碍区、深浅结合",
        "depth": 2.0,
        "target_species": "鲶鱼,鲈鱼,鳜鱼",
        "lure_types": "铅头钩+软饵,VIB（限10g以内）,德州钓组",
        "best_season": "全年营业（定期放鱼）",
        "note": "江油方向知名路亚钓场，7亩。鲶鱼、鲈鱼等。约300-500元/天（按场次）。电话：13778166668。禁用铁板、阿拉巴马钓组。"
    },
    {
        "name": "大林湾江湖钓场",
        "latitude": 31.4683,
        "longitude": 104.8685,
        "river": "大林湾",
        "city": "绵阳市",
        "location_detail": "绵阳市游仙区遇仙村一组冻库下坡300米",
        "water_type": "黑坑",
        "structure": "深浅结合、草区、开阔水面",
        "depth": 4.0,
        "target_species": "鲈鱼,翘嘴,黑鱼",
        "lure_types": "德州钓组,复合亮片,水面系,铅头钩+软饵",
        "best_season": "全年营业",
        "note": "新开钓场，面积30亩，十几年没干过水，原塘有野生鱼。放鲈鱼2000斤等。路亚100元/天。配套农家乐柴火鸡。适合休闲路亚+家庭出游。"
    },
    {
        "name": "菩提庄园垂钓基地（路亚）",
        "latitude": 31.5021,
        "longitude": 104.8812,
        "river": "菩提庄园",
        "city": "绵阳市",
        "location_detail": "绵阳市游仙区小永路仙鹤镇洛水村19号",
        "water_type": "野塘",
        "structure": "草区、深潭、老塘结构",
        "depth": 3.0,
        "target_species": "鲈鱼,翘嘴,黑鱼,鲶鱼",
        "lure_types": "雷蛙,复合亮片,深潜米诺,铅头钩+大软饵",
        "best_season": "4-10月",
        "note": "十几年清水老塘，鱼自然生长，巨物多。花鲢10斤+、草鱼20斤+、青鱼40-50斤。路亚收费25元/小时。鱼质媲美野生。"
    },
    {
        "name": "王大器鲈鱼养殖生态园",
        "latitude": 31.0800,
        "longitude": 104.9500,
        "river": "王大器鲈鱼养殖生态园",
        "city": "绵阳市",
        "location_detail": "绵阳市三台县平镇朱君红粱村9组037号（导航'王大器鲈鱼养殖生态园'）",
        "water_type": "黑坑",
        "structure": "养殖塘、均匀水深",
        "depth": 2.0,
        "target_species": "鲈鱼",
        "lure_types": "德州钓组,倒吊,无铅,小型硬饵",
        "best_season": "全年营业",
        "note": "三台县专业鲈鱼养殖/路亚塘。面积6亩，水深2米。按斤收费约25元/斤。适合想带鱼回家的钓友。交通便利，环境优美。"
    },
    # ============================================================
    #  D. 溪流微物标点
    # ============================================================
    {
        "name": "安昌河上游（安州段）微物标点",
        "latitude": 31.5200,
        "longitude": 104.5000,
        "river": "安昌河",
        "city": "绵阳市",
        "location_detail": "绵阳市安州区安昌河上游（塔水镇至桑枣镇段）",
        "water_type": "河流",
        "structure": "浅滩、鹅卵石底、清澈溪流",
        "depth": 0.8,
        "target_species": "马口,桃花鱼(宽鳍鱲),溪哥",
        "lure_types": "微型亮片(1-3g),飞蝇钩,小型米诺,微型铅头钩",
        "best_season": "3-6月（春季发色期！马口桃花色彩最艳丽）",
        "note": "绵阳微物路亚爱好者的天堂！安昌河上游水质清澈，马口和桃花鱼（宽鳍鱲）资源丰富。发色期雄鱼体色极其艳丽，是微物路亚和飞蝇钓的绝佳标点。"
    },
]


def seed():
    db = SessionLocal()
    try:
        now_ms = int(time.time() * 1000)
        inserted = 0
        skipped = 0

        for s in SPOTS:
            # 按名称+城市判断是否已存在
            existing = db.query(FishingSpotModel).filter(
                FishingSpotModel.name == s["name"],
                FishingSpotModel.city == s["city"]
            ).first()
            if existing:
                # 更新已有记录的路亚专属字段
                existing.target_species = s.get("target_species")
                existing.lure_types = s.get("lure_types")
                existing.best_season = s.get("best_season")
                existing.update_time = now_ms
                print(f"  🔄 更新: {s['name']}")
                skipped += 1
                continue

            spot = FishingSpotModel(
                id=str(uuid.uuid4()),
                name=s["name"],
                latitude=s["latitude"],
                longitude=s["longitude"],
                river=s.get("river"),
                city=s.get("city"),
                location_detail=s.get("location_detail"),
                q_weather_location_id=None,
                water_type=s["water_type"],
                structure=s["structure"],
                depth=s.get("depth"),
                target_species=s.get("target_species"),
                lure_types=s.get("lure_types"),
                best_season=s.get("best_season"),
                note=s.get("note"),
                photos="[]",
                create_time=now_ms,
                update_time=now_ms,
            )
            db.add(spot)
            print(f"  ✅ 新增: {s['name']} | 目标鱼: {s.get('target_species','-')}")
            inserted += 1

        db.commit()
        print(f"\n{'='*60}")
        print(f"  🎣 路亚标点种子完成！")
        print(f"  新增: {inserted} 个标点")
        print(f"  更新: {skipped} 个标点（已存在，更新了路亚专属字段）")
        print(f"{'='*60}")
    except Exception as e:
        db.rollback()
        print(f"❌ 错误: {e}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    print("=" * 60)
    print("  LureCalendar - 绵阳路亚标点种子数据 v2.0")
    print("=" * 60)
    print(f"  总计: {len(SPOTS)} 个标点")
    print(f"  覆盖: 涪城区、游仙区、安州区、江油市、")
    print(f"        三台县、盐亭县、梓潼县")
    print(f"  类型: 江河野钓({sum(1 for s in SPOTS if s['water_type']=='河流')})个 + ")
    print(f"        水库({sum(1 for s in SPOTS if s['water_type']=='水库')})个 + ")
    print(f"        商业基地({sum(1 for s in SPOTS if s['water_type'] in ('黑坑','野塘'))})个")
    print("=" * 60)
    seed()
