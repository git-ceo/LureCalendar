"""
路亚饵推荐 & 鱼种活跃度预测服务

规则体系:
- 清晨+微风+气压稳 → 水面系
- 中午+高温 → 德州软虫
- 气压骤降后 → VIB/亮片快搜
- 低温深水 → 铅头钩慢拖
- 按鱼种特化推荐
"""

from datetime import datetime, timedelta
import math

# ─── 鱼种配置表 ───
species_config = {
    "翘嘴": {
        "peak_hours": [(4, 7), (17, 20)],
        "temp_range": (16, 28),
        "lures": ["米诺", "亮片", "VIB"],
        "technique": "快抽停顿",
        "technique_detail": "抽2-3下后停顿1-2秒，利用反应打击",
    },
    "鲈鱼": {
        "peak_hours": [(5, 8), (17, 20)],
        "temp_range": (18, 26),
        "lures": ["米诺", "德州", "VIB"],
        "technique": "慢摇跳底",
        "technique_detail": "底层慢拖后轻抽跳起，模拟受伤饵鱼",
    },
    "鳜鱼": {
        "peak_hours": [(5, 8), (18, 21)],
        "temp_range": (18, 25),
        "lures": ["德州", "铅头钩", "软虫"],
        "technique": "底层慢拖",
        "technique_detail": "铅头钩贴底慢拖，在障碍区逐层搜索",
    },
    "黑鱼": {
        "peak_hours": [(6, 9), (16, 19)],
        "temp_range": (20, 30),
        "lures": ["雷蛙", "德州重钓组"],
        "technique": "水面炸水",
        "technique_detail": "雷蛙落点后停顿3秒再抽动，引诱攻击",
    },
    "马口": {
        "peak_hours": [(6, 9), (16, 18)],
        "temp_range": (14, 22),
        "lures": ["微物亮片", "小米诺"],
        "technique": "匀速收线",
        "technique_detail": "小克数亮片匀速收线，偶尔变速触发追咬",
    },
    "鳡鱼": {
        "peak_hours": [(5, 8), (16, 19)],
        "temp_range": (18, 28),
        "lures": ["大米诺", "铁板", "VIB"],
        "technique": "高速平收",
        "technique_detail": "大饵高速收线，鳡鱼追击速度快需快节奏",
    },
    "军鱼": {
        "peak_hours": [(5, 8), (17, 20)],
        "temp_range": (16, 24),
        "lures": ["亮片", "米诺"],
        "technique": "中速抽停",
        "technique_detail": "中速匀收间歇停顿，模拟溪流小鱼",
    },
}

# ─── 饵料详细描述映射 ───
lure_detail = {
    "米诺": {"type": "米诺（Minnow）", "desc": "全泳层搜索利器，模拟小鱼游姿", "icon": "minnow"},
    "小米诺": {"type": "小米诺", "desc": "微物作钓，针对溪流小型鱼种", "icon": "minnow"},
    "大米诺": {"type": "大米诺（12cm+）", "desc": "远投搜索大水面，诱钓体型鱼种", "icon": "minnow"},
    "亮片": {"type": "亮片（Spoon）", "desc": "金属反光远投，快速搜索大面积水域", "icon": "spoon"},
    "微物亮片": {"type": "微物亮片（1-5g）", "desc": "溪流微物利器，小巧灵动", "icon": "spoon"},
    "VIB": {"type": "VIB", "desc": "全泳层震动搜索，快速定位鱼群", "icon": "vib"},
    "德州": {"type": "德州钓组（Texas Rig）", "desc": "底层障碍区防挂搜索", "icon": "soft"},
    "德州重钓组": {"type": "德州重钓组", "desc": "重铅穿越密草区直达标点", "icon": "soft"},
    "铅头钩": {"type": "铅头钩（Jig Head）", "desc": "底层慢拖精搜，贴底诱鱼", "icon": "jig"},
    "软虫": {"type": "软虫（Worm）", "desc": "模拟底栖生物，缓慢诱食", "icon": "soft"},
    "雷蛙": {"type": "雷蛙（Frog）", "desc": "水面系重草区炸水利器", "icon": "frog"},
    "铁板": {"type": "铁板（Jig）", "desc": "远投深水快搜，沉底跳收", "icon": "spoon"},
    "水面铅笔": {"type": "水面铅笔（Pencil）", "desc": "水面之字走行，清晨黄昏炸水首选", "icon": "pencil"},
    "波爬": {"type": "波爬（Popper）", "desc": "水面啵啵声诱鱼上浮攻击", "icon": "popper"},
}


class LureRecommendationService:
    """路亚饵推荐服务"""

    def recommend(self, temp: float, wind_speed: float, pressure: float,
                  hour: int, species: str = None,
                  pressure_history: list = None,
                  weather_text: str = None) -> dict:
        """
        基于实时天气条件推荐路亚饵
        返回 {"recommendations": [...], "condition_summary": "..."}
        """
        recommendations = []
        conditions = []

        is_morning = 4 <= hour <= 8
        is_evening = 16 <= hour <= 20
        is_noon = 10 <= hour <= 14
        is_low_light = is_morning or is_evening
        is_gentle_wind = 0.3 <= (wind_speed or 2) <= 5.4
        is_high_temp = (temp or 20) > 28
        is_low_temp = (temp or 20) < 15

        # 气压趋势判断
        pressure_dropping = False
        pressure_stable = True
        if pressure_history and len(pressure_history) >= 2:
            delta = pressure_history[-1] - pressure_history[0]
            if delta < -5:
                pressure_dropping = True
                pressure_stable = False
            elif abs(delta) > 2:
                pressure_stable = False

        # ─── 规则1: 清晨+微风+气压稳 → 水面系 ───
        if is_low_light and is_gentle_wind and pressure_stable:
            recommendations.append({
                "lure_type": "水面系（波爬）",
                "description": "清晨微风低光，水面系最佳",
                "technique": "快抽停顿",
                "technique_detail": "抽2-3下后停顿1-2秒",
                "confidence": 90,
            })
            recommendations.append({
                "lure_type": "水面铅笔",
                "description": "水面之字走行，低光时段炸水率高",
                "technique": "连续抽动",
                "technique_detail": "竿尖朝下连续小抽，制造之字行进",
                "confidence": 85,
            })
            conditions.append("低光时段+微风+气压稳定，水面系活性窗口")

        # ─── 规则2: 中午+高温 → 德州软虫 ───
        if is_noon and is_high_temp:
            recommendations.append({
                "lure_type": "德州钓组（软虫）",
                "description": "中午高温鱼沉底，德州软虫慢搜底层",
                "technique": "底层慢拖",
                "technique_detail": "铅坠触底后缓慢拖拽，停顿等口",
                "confidence": 85,
            })
            conditions.append("正午高温，鱼群下沉至底层")

        # ─── 规则3: 气压骤降后 → VIB/亮片快搜 ───
        if pressure_dropping:
            recommendations.append({
                "lure_type": "VIB / 亮片快搜",
                "description": "气压骤降后窗口期，快速搜索反应鱼",
                "technique": "高速平收+抽停",
                "technique_detail": "快速收线3-4圈后急停1秒，利用反应打击",
                "confidence": 88,
            })
            conditions.append("气压骤降，鱼群躁动期适合快速搜索")

        # ─── 规则4: 低温深水 → 铅头钩慢拖 ───
        if is_low_temp:
            recommendations.append({
                "lure_type": "铅头钩+卷尾蛆",
                "description": "低温期鱼活性低，铅头钩贴底慢诱",
                "technique": "跳底慢收",
                "technique_detail": "铅头钩触底后轻挑竿尖，让饵跳起后自然下落",
                "confidence": 82,
            })
            conditions.append("低温期鱼活性低，需慢节奏底层搜索")

        # ─── 规则5: 按鱼种特化 ───
        if species and species in species_config:
            cfg = species_config[species]
            temp_lo, temp_hi = cfg["temp_range"]
            in_peak = any(s <= hour <= e for s, e in cfg["peak_hours"])
            in_temp = temp_lo <= (temp or 20) <= temp_hi

            for lure_name in cfg["lures"][:2]:
                detail = lure_detail.get(lure_name, {})
                conf = 70
                if in_peak:
                    conf += 15
                if in_temp:
                    conf += 10
                conf = min(conf, 98)

                # 去重
                existing_types = {r["lure_type"] for r in recommendations}
                display_type = detail.get("type", lure_name)
                if display_type not in existing_types:
                    recommendations.append({
                        "lure_type": display_type,
                        "description": f"针对{species}特化推荐 - {detail.get('desc', '')}",
                        "technique": cfg["technique"],
                        "technique_detail": cfg["technique_detail"],
                        "confidence": conf,
                    })

            peak_str = "/".join(f"{s}:00-{e}:00" for s, e in cfg["peak_hours"])
            conditions.append(f"{species}活跃时段: {peak_str}，适温{temp_lo}-{temp_hi}℃")

        # 如果没有任何规则命中，给出通用推荐
        if not recommendations:
            recommendations = [
                {
                    "lure_type": "米诺（Minnow）",
                    "description": "万能通用型假饵，适合大多数水域和鱼种",
                    "technique": "匀速收线+抽停",
                    "technique_detail": "匀速收2-3圈后抽一下竿尖再停顿",
                    "confidence": 70,
                },
                {
                    "lure_type": "VIB",
                    "description": "全泳层覆盖，快速找鱼利器",
                    "technique": "快收搜索",
                    "technique_detail": "中快速匀收，感觉到震动即正常工作",
                    "confidence": 65,
                },
            ]
            conditions.append("当前条件一般，建议通用搜索型饵料")

        # 按置信度排序，最多返回5条
        recommendations.sort(key=lambda x: x["confidence"], reverse=True)
        recommendations = recommendations[:5]

        summary = "；".join(conditions) if conditions else "天气条件适中，建议多种饵型轮换尝试"

        return {
            "recommendations": recommendations,
            "condition_summary": summary,
        }


class SpeciesActivityService:
    """鱼种活跃度预测服务"""

    def predict(self, species: str, temp: float, pressure: float,
                wind_speed: float, hour: int,
                weather_text: str = None,
                moon_phase: str = None) -> dict:
        """
        预测指定鱼种当前活跃度
        返回完整的活跃度报告
        """
        cfg = species_config.get(species)
        if not cfg:
            return self._default_response(species)

        temp_lo, temp_hi = cfg["temp_range"]
        current_temp = temp or 20
        current_hour = hour or 12

        # ─── 综合评分 ───
        score = 50  # 基础分

        # 时段评分 (+0~25)
        in_peak = False
        for s, e in cfg["peak_hours"]:
            if s <= current_hour <= e:
                in_peak = True
                break
        if in_peak:
            score += 25
        elif any(abs(current_hour - s) <= 1 or abs(current_hour - e) <= 1
                 for s, e in cfg["peak_hours"]):
            score += 12  # 接近窗口

        # 温度评分 (+0~20)
        if temp_lo <= current_temp <= temp_hi:
            # 越接近中心越高分
            mid = (temp_lo + temp_hi) / 2
            dist = abs(current_temp - mid) / ((temp_hi - temp_lo) / 2)
            score += int(20 * (1 - dist * 0.5))
        elif current_temp < temp_lo:
            gap = temp_lo - current_temp
            score -= min(int(gap * 2), 15)
        else:
            gap = current_temp - temp_hi
            score -= min(int(gap * 2), 15)

        # 气压评分 (+0~10)
        if pressure:
            if 1010 <= pressure <= 1025:
                score += 10
            elif 1000 <= pressure <= 1030:
                score += 5

        # 风速评分 (+0~5)
        if wind_speed is not None:
            if 0.3 <= wind_speed <= 5.4:
                score += 5
            elif wind_speed > 7.9:
                score -= 5

        score = min(100, max(0, score))

        # ─── 活跃时段窗口 ───
        activity_windows = []
        for s, e in cfg["peak_hours"]:
            label = "清晨" if s < 10 else "傍晚"
            activity_windows.append({
                "start": f"{s:02d}:00",
                "end": f"{e:02d}:00",
                "label": label,
                "is_current": s <= current_hour <= e,
            })

        # ─── 推荐饵料 ───
        recommended_lures = []
        for lure_name in cfg["lures"]:
            detail = lure_detail.get(lure_name, {})
            recommended_lures.append({
                "name": detail.get("type", lure_name),
                "description": detail.get("desc", ""),
                "icon": detail.get("icon", "lure"),
            })

        # ─── 智能洞察 ───
        insight = self._generate_insight(species, score, in_peak, current_temp, cfg)

        return {
            "species": species,
            "overall_score": score,
            "activity_windows": activity_windows,
            "recommended_lures": recommended_lures,
            "recommended_technique": f"{cfg['technique']} - {cfg['technique_detail']}",
            "insight": insight,
        }

    def _generate_insight(self, species, score, in_peak, temp, cfg):
        """生成人性化洞察文本"""
        temp_lo, temp_hi = cfg["temp_range"]
        parts = []

        if score >= 80:
            parts.append(f"当前{species}活跃度极高，强烈建议出钓！")
        elif score >= 60:
            parts.append(f"{species}活跃度良好，可以尝试作钓")
        elif score >= 40:
            parts.append(f"{species}活跃度一般，需要耐心等待窗口")
        else:
            parts.append(f"当前条件不利于{species}活动，建议等待更好时机")

        if in_peak:
            parts.append("正处于活跃时段窗口内")
        else:
            # 找到最近的窗口
            now_h = int(temp)  # 这里复用temp作为近似，实际用hour
            next_windows = []
            for s, e in cfg["peak_hours"]:
                next_windows.append(f"{s:02d}:00-{e:02d}:00")
            parts.append(f"下一个活跃窗口: {'/'.join(next_windows)}")

        if temp < temp_lo:
            parts.append(f"水温偏低（适温{temp_lo}-{temp_hi}℃），可尝试慢节奏底层搜索")
        elif temp > temp_hi:
            parts.append(f"水温偏高（适温{temp_lo}-{temp_hi}℃），建议选择深水阴凉处")

        return "。".join(parts) + "。"

    def _default_response(self, species):
        """未知鱼种返回通用结果"""
        return {
            "species": species,
            "overall_score": 50,
            "activity_windows": [
                {"start": "05:00", "end": "08:00", "label": "清晨", "is_current": False},
                {"start": "17:00", "end": "20:00", "label": "傍晚", "is_current": False},
            ],
            "recommended_lures": [
                {"name": "米诺（Minnow）", "description": "通用型假饵", "icon": "minnow"},
                {"name": "VIB", "description": "全泳层搜索", "icon": "vib"},
            ],
            "recommended_technique": "匀速收线+抽停 - 匀速收2-3圈后轻抽停顿",
            "insight": f"暂无{species}的专属数据，建议使用通用饵型探索。",
        }


class CalendarScoreService:
    """日历月度评分服务"""

    def compute_month(self, lat: float, lon: float, year: int, month: int,
                      index_calculator) -> dict:
        """
        计算指定月份每天的钓鱼评分
        基于季节+位置+平均气象条件估算
        """
        import calendar
        days_in_month = calendar.monthrange(year, month)[1]

        scores = []
        for day in range(1, days_in_month + 1):
            date_str = f"{year}-{month:02d}-{day:02d}"
            dt = datetime(year, month, day)

            # 基于日期估算基础条件
            estimated = self._estimate_conditions(dt, lat, lon)

            # 计算每日最佳窗口得分
            best_score = 0
            best_hour = 6
            for h in [5, 6, 7, 17, 18, 19]:
                s = index_calculator.calculate(
                    temp=estimated["temp"],
                    pressure=estimated["pressure"],
                    wind_speed=estimated["wind_speed"],
                    hour=h,
                    weather_text=estimated.get("weather_text"),
                )
                if s > best_score:
                    best_score = s
                    best_hour = h

            # 也计算全天平均(取几个代表时段)
            sample_hours = [6, 9, 12, 15, 18]
            day_scores = []
            for h in sample_hours:
                s = index_calculator.calculate(
                    temp=estimated["temp"],
                    pressure=estimated["pressure"],
                    wind_speed=estimated["wind_speed"],
                    hour=h,
                    weather_text=estimated.get("weather_text"),
                )
                day_scores.append(s)
            avg_score = round(sum(day_scores) / len(day_scores))

            label = self._score_label(avg_score)
            window_start = f"{best_hour:02d}:00"
            window_end = f"{best_hour + 2:02d}:00"

            scores.append({
                "date": date_str,
                "score": avg_score,
                "label": label,
                "best_window": f"{window_start}-{window_end}",
            })

        return {
            "year": year,
            "month": month,
            "scores": scores,
        }

    def _estimate_conditions(self, dt: datetime, lat: float, lon: float) -> dict:
        """
        根据日期和纬度估算典型气象条件
        （实际项目中应查询天气预报API缓存，这里用季节模型估算兜底）
        """
        month = dt.month
        # 根据月份估算温度（以四川盆地为参考基准，再按纬度微调）
        base_temps = {
            1: 6, 2: 8, 3: 14, 4: 20, 5: 24, 6: 27,
            7: 30, 8: 29, 9: 25, 10: 19, 11: 13, 12: 8
        }
        temp = base_temps.get(month, 20)
        # 纬度修正: 每偏离30°北纬1°，温度变化约0.7℃
        lat_offset = (lat - 30.0) * (-0.7)
        temp = temp + lat_offset

        # 气压: 季节性波动
        pressure_base = 1013
        if month in (12, 1, 2):
            pressure_base = 1020  # 冬季高压
        elif month in (6, 7, 8):
            pressure_base = 1008  # 夏季低压

        # 日变化随机性用日期hash模拟
        day_hash = (dt.day * 7 + month * 13) % 10
        pressure = pressure_base + (day_hash - 5) * 0.8

        wind_speed = 1.5 + (day_hash % 4) * 0.8

        weather_options = ["晴", "多云", "阴", "多云"]
        weather_text = weather_options[day_hash % len(weather_options)]

        return {
            "temp": round(temp, 1),
            "pressure": round(pressure, 1),
            "wind_speed": round(wind_speed, 1),
            "weather_text": weather_text,
        }

    def _score_label(self, score: int) -> str:
        if score >= 85:
            return "爆护期"
        elif score >= 70:
            return "活跃"
        elif score >= 55:
            return "一般"
        elif score >= 40:
            return "低迷"
        else:
            return "不宜"


class PressureTrendService:
    """气压趋势服务"""

    def get_trend(self, lat: float, lon: float, weather_cache_data: dict = None) -> dict:
        """
        返回气压趋势数据
        优先使用 weather_cache 中的真实数据，没有则生成估算数据
        """
        now = datetime.now()

        if weather_cache_data:
            return self._extract_from_cache(weather_cache_data, lat, lon)

        # 没有缓存数据时，生成基于季节的估算气压序列
        history = []
        forecast = []
        base_pressure = 1013.0
        month = now.month
        if month in (12, 1, 2):
            base_pressure = 1020.0
        elif month in (6, 7, 8):
            base_pressure = 1008.0

        for i in range(24, 0, -1):
            t = now - timedelta(hours=i)
            # 日变化模拟: 气压在凌晨最高、午后最低
            hour_offset = math.sin((t.hour - 4) * math.pi / 12) * 2
            p = base_pressure - hour_offset + ((t.hour * 3 + t.day) % 5 - 2) * 0.3
            history.append({
                "time": t.strftime("%Y-%m-%d %H:%M"),
                "pressure": round(p, 1),
            })

        current_p = history[-1]["pressure"] if history else base_pressure
        for i in range(1, 25):
            t = now + timedelta(hours=i)
            hour_offset = math.sin((t.hour - 4) * math.pi / 12) * 2
            p = current_p - hour_offset * 0.5 + ((t.hour * 3 + t.day) % 5 - 2) * 0.2
            forecast.append({
                "time": t.strftime("%Y-%m-%d %H:%M"),
                "pressure": round(p, 1),
            })

        return {
            "location": {"lat": lat, "lon": lon},
            "history": history,
            "forecast": forecast,
        }

    def _extract_from_cache(self, cache_data: dict, lat: float, lon: float) -> dict:
        """从 weather_cache 的 data_json 中提取气压序列"""
        history = []
        forecast = []

        # 尝试从缓存结构中提取（兼容和风天气 JSON 格式）
        if isinstance(cache_data, dict):
            # hourly 数据
            hourly = cache_data.get("hourly", [])
            now = datetime.now()
            for item in hourly:
                try:
                    fx_time = item.get("fxTime", "")
                    p = float(item.get("pressure", 0))
                    if not p:
                        continue
                    entry = {"time": fx_time, "pressure": p}
                    # 简单按时间字符串比较判断历史/未来
                    if fx_time <= now.strftime("%Y-%m-%dT%H:%M"):
                        history.append(entry)
                    else:
                        forecast.append(entry)
                except (ValueError, TypeError):
                    continue

        # 如果提取到的数据太少，补充估算
        if len(history) < 6 and len(forecast) < 6:
            fallback = self.get_trend(lat, lon, None)
            if not history:
                history = fallback["history"]
            if not forecast:
                forecast = fallback["forecast"]

        return {
            "location": {"lat": lat, "lon": lon},
            "history": history[-24:],
            "forecast": forecast[:24],
        }
