"""天文服务：日出日落 / 月相 / 月光 / 农历 / 禁渔期标记

本模块不依赖外部网络，全部基于本地数学公式计算，保证离线可用。
- 日出日落：使用 NOAA 太阳位置算法
- 月相：使用合朔参考点 + 朔望月长度推算
- 农历：仅在安装了 zhdate 库时启用，否则返回 None
- 禁渔期：内置 4 大主要流域规则（可按需扩展）
"""
from __future__ import annotations

import math
from datetime import datetime, date, timedelta
from typing import Optional

# ===== 月相计算（基于已知合朔时刻） =====
# 已知 2000-01-06 18:14 UTC 为新月（朔），朔望月平均 29.530589 天
SYNODIC_MONTH = 29.530588853
KNOWN_NEW_MOON = datetime(2000, 1, 6, 18, 14)

PHASE_LABELS = ["朔(新月)", "娥眉月", "上弦月", "盈凸月", "望(满月)", "亏凸月", "下弦月", "残月"]


def moon_phase(d: date) -> tuple[str, float]:
    """返回（月相名称, 月光照率 0-1）。"""
    target = datetime(d.year, d.month, d.day, 12, 0)
    delta_days = (target - KNOWN_NEW_MOON).total_seconds() / 86400
    age = delta_days % SYNODIC_MONTH  # 月龄（0-29.5）
    # 月光照率：1 - cos(2π * age / synodic_month) 的归一化
    illumination = (1 - math.cos(2 * math.pi * age / SYNODIC_MONTH)) / 2
    idx = int((age / SYNODIC_MONTH) * 8) % 8
    return PHASE_LABELS[idx], round(illumination, 3)


# ===== 日出日落计算（NOAA 简化版） =====

def _julian_day(d: date) -> float:
    y, m = d.year, d.month
    if m <= 2:
        y -= 1
        m += 12
    a = y // 100
    b = 2 - a + a // 4
    return math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d.day + b - 1524.5


def _sun_decl_eqtime(jd: float) -> tuple[float, float]:
    n = jd - 2451545.0
    L = (280.460 + 0.9856474 * n) % 360
    g = math.radians((357.528 + 0.9856003 * n) % 360)
    lam = math.radians(L + 1.915 * math.sin(g) + 0.020 * math.sin(2 * g))
    eps = math.radians(23.439 - 0.0000004 * n)
    decl = math.asin(math.sin(eps) * math.sin(lam))
    eqtime = 4 * math.degrees(math.atan2(
        math.tan(eps / 2) ** 2 * math.sin(2 * lam) -
        2 * 0.0167 * math.sin(g) +
        4 * 0.0167 * math.tan(eps / 2) ** 2 * math.sin(g) * math.cos(2 * lam) -
        0.5 * math.tan(eps / 2) ** 4 * math.sin(4 * lam) -
        1.25 * 0.0167 ** 2 * math.sin(2 * g),
        1
    ))
    return decl, eqtime


def sunrise_sunset(d: date, lat: float, lon: float, tz_offset: float = 8.0) -> tuple[Optional[str], Optional[str]]:
    """计算日出日落 HH:MM（北京时间默认 +8）。"""
    try:
        jd = _julian_day(d)
        decl, eqtime = _sun_decl_eqtime(jd)
        lat_rad = math.radians(lat)
        cos_h = (math.cos(math.radians(90.833)) - math.sin(lat_rad) * math.sin(decl)) / (math.cos(lat_rad) * math.cos(decl))
        if cos_h > 1 or cos_h < -1:
            return None, None
        h = math.degrees(math.acos(cos_h))
        sunrise_min = 720 - 4 * (lon + h) - eqtime + tz_offset * 60
        sunset_min = 720 - 4 * (lon - h) - eqtime + tz_offset * 60
        return _to_hhmm(sunrise_min), _to_hhmm(sunset_min)
    except Exception:
        return None, None


def _to_hhmm(minutes: float) -> Optional[str]:
    if minutes is None:
        return None
    minutes = minutes % (24 * 60)
    h = int(minutes // 60)
    m = int(round(minutes % 60))
    if m == 60:
        h += 1
        m = 0
    return f"{h:02d}:{m:02d}"


# ===== 农历（可选） =====

def lunar_string(d: date) -> Optional[str]:
    try:
        from zhdate import ZhDate  # type: ignore
        z = ZhDate.from_datetime(datetime(d.year, d.month, d.day))
        return z.chinese()
    except Exception:
        return None


# ===== 禁渔期判定（核心流域简化版） =====
# 数据来源：农业农村部公告
CLOSED_SEASONS = [
    {"region": "长江流域", "start": (3, 1), "end": (6, 30),
     "lat_range": (24.0, 35.0), "lon_range": (95.0, 122.5),
     "note": "长江流域十年禁渔期（年度内 3-6 月特别强化）"},
    {"region": "黄河流域", "start": (4, 1), "end": (6, 30),
     "lat_range": (32.0, 42.0), "lon_range": (95.0, 119.0),
     "note": "黄河禁渔期"},
    {"region": "珠江流域", "start": (3, 1), "end": (6, 30),
     "lat_range": (21.0, 27.0), "lon_range": (102.0, 116.0),
     "note": "珠江禁渔期"},
    {"region": "海洋伏季", "start": (5, 1), "end": (9, 16),
     "lat_range": (3.0, 41.0), "lon_range": (108.0, 130.0),
     "note": "海洋伏季休渔（具体海区时间略有差异）"},
]


def closed_season_info(d: date, lat: float, lon: float) -> tuple[int, Optional[str]]:
    md = (d.month, d.day)
    for rule in CLOSED_SEASONS:
        if rule["lat_range"][0] <= lat <= rule["lat_range"][1] and \
           rule["lon_range"][0] <= lon <= rule["lon_range"][1]:
            if rule["start"] <= md <= rule["end"]:
                return 1, f"{rule['region']}禁渔期：{rule['note']}"
    return 0, None


# ===== 对外统一入口 =====

def compute(lat: float, lon: float, date_str: str) -> dict:
    d = datetime.strptime(date_str, "%Y-%m-%d").date()
    sunrise, sunset = sunrise_sunset(d, lat, lon)
    phase, illum = moon_phase(d)
    is_closed, note = closed_season_info(d, lat, lon)
    return {
        "date": date_str,
        "sunrise": sunrise,
        "sunset": sunset,
        "moonrise": None,  # 月升月落计算复杂，留扩展位
        "moonset": None,
        "moon_phase": phase,
        "moon_illumination": illum,
        "lunar_date": lunar_string(d),
        "is_closed_season": is_closed,
        "closed_season_note": note,
    }


if __name__ == "__main__":
    # 自测：绵阳坐标
    import json as _json
    print(_json.dumps(compute(31.4620, 104.7530, datetime.now().strftime("%Y-%m-%d")),
                      ensure_ascii=False, indent=2))
