"""
路亚活跃度评分计算器（v2.0 - 鱼种精细化版）

6因子评分体系 + 鱼种专属参数：
- 气压: 基础30分（权重可由鱼种配置调整）
- 风速: 基础25分
- 光照(鱼种活跃时段): 20分
- 月相: 15分
- 降水: 10分
- 温度(鱼种最优区间): 10分
- 趋势: ±10分（乘以鱼种气压敏感度）
归一化公式: rawScore * 100 / 120 * 季节系数（鱼种季节加成优先）
"""

import math
from datetime import datetime, date

# 尝试导入 astronomy 模块以获取精确日出日落
try:
    from services.astronomy import sunrise_sunset as _astro_sunrise_sunset
except ImportError:
    _astro_sunrise_sunset = None


# ===== 鱼种参数配置（温度最优区间、活跃时段、因子权重调整）=====
SPECIES_CONFIG = {
    "翘嘴": {
        "optimal_temp": (16, 28),
        "peak_hours": [4, 5, 6, 7, 17, 18, 19, 20],
        "weights": {"pressure": 25, "wind": 30, "light": 20, "moon": 15, "rain": 10, "temperature": 10},
        "pressure_sensitivity": 1.2,
        "seasonal_boost": {3: 0.9, 4: 1.1, 5: 1.15, 6: 1.0, 9: 1.2, 10: 1.15, 11: 0.9},
    },
    "鲈鱼": {
        "optimal_temp": (18, 26),
        "peak_hours": [5, 6, 7, 8, 17, 18, 19, 20],
        "weights": {"pressure": 30, "wind": 20, "light": 20, "moon": 15, "rain": 10, "temperature": 15},
        "pressure_sensitivity": 1.3,
        "seasonal_boost": {4: 1.1, 5: 1.2, 6: 1.0, 9: 1.15, 10: 1.1},
    },
    "鳜鱼": {
        "optimal_temp": (18, 25),
        "peak_hours": [5, 6, 7, 8, 18, 19, 20, 21],
        "weights": {"pressure": 35, "wind": 15, "light": 20, "moon": 15, "rain": 5, "temperature": 15},
        "pressure_sensitivity": 1.5,
        "seasonal_boost": {4: 1.0, 5: 1.15, 6: 1.1, 9: 1.2, 10: 1.15},
    },
    "黑鱼": {
        "optimal_temp": (20, 30),
        "peak_hours": [6, 7, 8, 9, 16, 17, 18, 19],
        "weights": {"pressure": 20, "wind": 20, "light": 25, "moon": 10, "rain": 10, "temperature": 20},
        "pressure_sensitivity": 0.8,
        "seasonal_boost": {5: 1.1, 6: 1.2, 7: 1.15, 8: 1.1, 9: 1.0},
    },
    "马口": {
        "optimal_temp": (14, 22),
        "peak_hours": [6, 7, 8, 9, 16, 17, 18],
        "weights": {"pressure": 25, "wind": 25, "light": 20, "moon": 10, "rain": 15, "temperature": 15},
        "pressure_sensitivity": 1.0,
        "seasonal_boost": {3: 1.0, 4: 1.15, 5: 1.2, 9: 1.1, 10: 1.05},
    },
    "鳡鱼": {
        "optimal_temp": (18, 28),
        "peak_hours": [5, 6, 7, 8, 16, 17, 18, 19],
        "weights": {"pressure": 25, "wind": 30, "light": 20, "moon": 15, "rain": 10, "temperature": 10},
        "pressure_sensitivity": 1.1,
        "seasonal_boost": {4: 1.0, 5: 1.15, 6: 1.1, 9: 1.2, 10: 1.1},
    },
    "军鱼": {
        "optimal_temp": (16, 24),
        "peak_hours": [5, 6, 7, 8, 17, 18, 19, 20],
        "weights": {"pressure": 30, "wind": 25, "light": 20, "moon": 10, "rain": 10, "temperature": 15},
        "pressure_sensitivity": 1.2,
        "seasonal_boost": {4: 1.1, 5: 1.2, 9: 1.15, 10: 1.1},
    },
}

# 默认配置（不选择特定鱼种时使用）
DEFAULT_CONFIG = {
    "optimal_temp": (15, 28),
    "peak_hours": [5, 6, 7, 17, 18, 19],
    "weights": {"pressure": 30, "wind": 25, "light": 20, "moon": 15, "rain": 10, "temperature": 10},
    "pressure_sensitivity": 1.0,
    "seasonal_boost": {},
}


# 鱼种活跃系数映射（中文名/英文名均可匹配，保留向后兼容）
species_activity_map = {
    "鲈鱼": 0.9, "bass": 0.9,
    "翘嘴": 0.85, "topmouth_culter": 0.85,
    "鳜鱼": 0.8, "mandarin_fish": 0.8,
    "黑鱼": 0.75, "snakehead": 0.75,
    "马口": 0.7, "minnow_fish": 0.7,
    "鳡鱼": 0.85, "yellowcheek": 0.85,
    "军鱼": 0.75, "squaliobarbus": 0.75,
}


# ===== 温度评分函数（10分满分）- 支持鱼种专属温度区间 =====
def calculate_temperature_score(temperature: float = None, config: dict = None) -> int:
    """
    温度评分 - 使用鱼种专属最优温度区间
    """
    if temperature is None:
        return 5
    if config is None:
        config = DEFAULT_CONFIG
    opt_min, opt_max = config["optimal_temp"]
    if opt_min <= temperature <= opt_max:
        return 10
    elif (opt_min - 3) <= temperature < opt_min or opt_max < temperature <= (opt_max + 3):
        return 7
    elif (opt_min - 6) <= temperature < (opt_min - 3) or (opt_max + 3) < temperature <= (opt_max + 6):
        return 4
    else:
        return 1


# ===== 光照评分函数（20分满分）- 支持鱼种活跃时段 =====
def calculate_species_light_score(hour: int, config: dict = None) -> int:
    """
    基于鱼种活跃时段的光照评分（满分20）
    peak_hours 内满分，距离越远分数越低
    """
    if config is None:
        config = DEFAULT_CONFIG
    peak_hours = config["peak_hours"]
    if hour in peak_hours:
        return 20  # 满分
    # 距离 peak_hours 最近的距离决定分数
    min_distance = min(abs(hour - ph) for ph in peak_hours)
    if min_distance == 1:
        return 15
    elif min_distance == 2:
        return 10
    elif min_distance <= 4:
        return 6
    else:
        return 3


# ===== 季节乘数系数 =====
SEASONAL_MULTIPLIER = {
    1: 0.5, 2: 0.6, 3: 0.8, 4: 1.0, 5: 1.05, 6: 0.95,
    7: 0.85, 8: 0.9, 9: 1.1, 10: 1.15, 11: 0.9, 12: 0.6
}


def get_seasonal_multiplier(month: int, config: dict = None) -> float:
    """
    获取季节乘数 - 鱼种专属季节加成优先
    """
    if config is None:
        config = DEFAULT_CONFIG
    species_seasonal = config.get("seasonal_boost", {})
    if month in species_seasonal:
        return species_seasonal[month]
    return SEASONAL_MULTIPLIER.get(month, 1.0)


# ===== 动态日出日落窗口 =====
# 中国中纬度地区(~31°N)日出日落估算表
SUNRISE_SUNSET_TABLE = {
    1: (7.5, 18.0), 2: (7.2, 18.3), 3: (6.8, 18.7), 4: (6.3, 19.0),
    5: (6.0, 19.4), 6: (5.8, 19.7), 7: (6.0, 19.6), 8: (6.2, 19.3),
    9: (6.5, 18.8), 10: (6.7, 18.3), 11: (7.0, 17.9), 12: (7.4, 17.8)
}


def _get_sunrise_sunset_hours(month: int, lat: float = None, lon: float = None) -> tuple:
    """
    获取日出日落小时数。
    优先使用 astronomy.py 的 NOAA 精确算法（需要 lat/lon），
    否则回退到查表法（默认纬度 ~31°N）。
    """
    if lat is not None and lon is not None and _astro_sunrise_sunset is not None:
        try:
            today = date.today()
            # 使用当月15日作为代表日期
            d = date(today.year, month, 15)
            sr_str, ss_str = _astro_sunrise_sunset(d, lat, lon)
            if sr_str and ss_str:
                sr_parts = sr_str.split(":")
                ss_parts = ss_str.split(":")
                sr_hour = int(sr_parts[0]) + int(sr_parts[1]) / 60.0
                ss_hour = int(ss_parts[0]) + int(ss_parts[1]) / 60.0
                return (sr_hour, ss_hour)
        except Exception:
            pass
    # 回退到查表法
    return SUNRISE_SUNSET_TABLE.get(month, (6.5, 18.5))


def calculate_dynamic_light_score(hour: int, month: int, weather_desc: str = None,
                                  lat: float = None, lon: float = None) -> int:
    """
    动态光照评分（满分20）- 通用版本（不使用鱼种配置时的回退）。
    根据月份对应的日出日落时间动态计算黄金窗口。
    如果提供 lat/lon 则使用 NOAA 精确计算。
    """
    sunrise, sunset = _get_sunrise_sunset_hours(month, lat, lon)

    morning_start = sunrise - 0.5
    morning_end = sunrise + 1.5
    evening_start = sunset - 1.5
    evening_end = sunset + 0.5
    sub_morning_end = sunrise + 3.0
    sub_evening_start = sunset - 3.0

    h = float(hour)

    if morning_start <= h <= morning_end:
        base = 20
    elif evening_start <= h <= evening_end:
        base = 20
    elif morning_end < h <= sub_morning_end:
        base = 15
    elif sub_evening_start <= h < evening_start:
        base = 15
    elif 20 <= h <= 21 or (4 <= h < morning_start):
        base = 12
    elif h >= 22 or h <= 3:
        base = 8
    else:
        base = 6

    # 阴天正午修正
    is_midday = sub_morning_end < h < sub_evening_start
    is_cloudy = weather_desc and ("云" in weather_desc or "阴" in weather_desc)
    if is_midday and is_cloudy:
        return max(base, 16)

    return base


class FishingIndexCalculator:
    def __init__(self, config=None):
        # 6因子默认权重配置（向后兼容）
        self.weights = config or {
            'pressure': 30,
            'wind': 25,
            'light': 20,
            'moon': 15,
            'rain': 10,
            'temperature': 10
        }

    def calculate(self, temp=20, humidity=50, pressure=1013, wind_speed=2,
                  water_temp=18, fish_species='default', hour=12,
                  pressure_change=0, water_type='river',
                  moon_phase=None, pressure_history=None,
                  weather_text=None, precipitation_mm=None,
                  temperature=None, month=None,
                  lat=None, lon=None):
        """
        计算路亚活跃度评分 (0-100) - v2.0 鱼种精细化版

        根据 fish_species 选择对应鱼种配置，影响：
        - 温度评分区间
        - 光照/活跃时段评分
        - 气压趋势敏感度
        - 季节乘数
        - 各因子权重比例

        :param temp: 气温(℃)，保留兼容
        :param humidity: 湿度(%)，保留兼容
        :param pressure: 当前气压(hPa)
        :param wind_speed: 风速(m/s)
        :param water_temp: 水温(℃)，保留兼容
        :param fish_species: 目标鱼种（如"翘嘴"/"鳜鱼"/"default"）
        :param hour: 当前小时(0-23)
        :param pressure_change: 气压变化(hPa/h)，保留兼容
        :param water_type: 水域类型，保留兼容
        :param moon_phase: 月相名称
        :param pressure_history: 过去几小时气压列表(从旧到新)
        :param weather_text: 天气描述文本
        :param precipitation_mm: 降水量(mm)
        :param temperature: 气温(℃)，用于温度因子评分
        :param month: 月份(1-12)
        :param lat: 纬度
        :param lon: 经度
        """

        # 处理默认值
        if month is None:
            month = datetime.now().month
        # temperature 优先使用显式传入，回退到 temp 参数
        actual_temp = temperature if temperature is not None else temp

        # 获取鱼种配置
        species_config = SPECIES_CONFIG.get(fish_species, DEFAULT_CONFIG)
        weights = species_config["weights"]

        # === 1. 气压评分（满分30分）===
        pressure_score = self._calc_pressure_score(pressure)

        # === 2. 风速评分（满分25分）===
        wind_score = self._calc_wind_score(wind_speed)

        # === 3. 光照/时间段评分（满分20分，使用鱼种活跃时段）===
        light_score = calculate_species_light_score(hour, species_config)

        # === 4. 月相评分（满分15分）===
        moon_score = self._calc_moon_score(moon_phase)

        # === 5. 降水评分（满分10分）===
        rain_score = self._calc_rain_score(precipitation_mm)

        # === 6. 温度评分（满分10分，鱼种专属温度区间）===
        temp_score = calculate_temperature_score(actual_temp, species_config)

        # === 气压趋势加成（±10分 × 鱼种气压敏感度）===
        base_trend_score = self._calc_pressure_trend_bonus(pressure_history, pressure, pressure_change)
        pressure_sensitivity = species_config.get("pressure_sensitivity", 1.0)
        trend_score = base_trend_score * pressure_sensitivity
        # 限制在 -10 到 +10 范围
        trend_score = max(-10, min(10, trend_score))

        # === 权重归一化 - 使用鱼种专属权重 ===
        # 默认满分: pressure=30, wind=25, light=20, moon=15, rain=10, temperature=10
        # 各因子按鱼种权重/默认权重 比例调整
        weighted_pressure = pressure_score * weights["pressure"] / 30
        weighted_wind = wind_score * weights["wind"] / 25
        weighted_light = light_score * weights["light"] / 20
        weighted_moon = moon_score * weights["moon"] / 15
        weighted_rain = rain_score * weights["rain"] / 10
        weighted_temp = temp_score * weights["temperature"] / 10

        raw_score = (weighted_pressure + weighted_wind + weighted_light +
                     weighted_moon + weighted_rain + weighted_temp + trend_score)

        # 归一化: rawScore * 100 / 120 * 季节系数（鱼种季节加成优先）
        seasonal = get_seasonal_multiplier(month, species_config)
        total_weight = sum(weights.values())  # 动态总权重
        normalized = raw_score * 100.0 / (total_weight + 10) * seasonal
        final_score = min(100, max(0, round(normalized)))

        return final_score

    def calculate_with_details(self, temp=20, humidity=50, pressure=1013, wind_speed=2,
                               water_temp=18, fish_species='default', hour=12,
                               pressure_change=0, water_type='river',
                               moon_phase=None, pressure_history=None,
                               weather_text=None, precipitation_mm=None,
                               temperature=None, month=None,
                               lat=None, lon=None):
        """
        计算路亚活跃度评分并返回详细信息（供API使用）
        返回: (score, species_config)
        """
        if month is None:
            month = datetime.now().month
        actual_temp = temperature if temperature is not None else temp

        species_config = SPECIES_CONFIG.get(fish_species, DEFAULT_CONFIG)
        score = self.calculate(
            temp=temp, humidity=humidity, pressure=pressure, wind_speed=wind_speed,
            water_temp=water_temp, fish_species=fish_species, hour=hour,
            pressure_change=pressure_change, water_type=water_type,
            moon_phase=moon_phase, pressure_history=pressure_history,
            weather_text=weather_text, precipitation_mm=precipitation_mm,
            temperature=temperature, month=month, lat=lat, lon=lon
        )
        return score, species_config

    def get_description(self, score):
        """
        根据评分返回标签描述
        """
        if score >= 85:
            return "爆护期"
        elif score >= 70:
            return "活跃"
        elif score >= 55:
            return "一般"
        elif score >= 40:
            return "低迷"
        else:
            return "不建议出钓"

    def _calc_pressure_score(self, pressure):
        """气压评分（满分30）"""
        if pressure is None:
            return 15  # 无数据给中间分
        if 1010 <= pressure <= 1025:
            return 30
        elif 1000 <= pressure < 1010 or 1025 < pressure <= 1030:
            return 22
        elif 995 <= pressure < 1000 or 1030 < pressure <= 1035:
            return 14
        else:
            return 6

    def _calc_wind_score(self, wind_speed):
        """风速评分（满分25）"""
        if wind_speed is None:
            return 12  # 无数据给中间分
        if 0.3 <= wind_speed <= 5.4:
            return 25
        elif 5.4 < wind_speed <= 7.9:
            return 12.5
        elif wind_speed < 0.3:
            return 15  # 几乎无风
        else:
            return 0  # 5级以上

    def _calc_light_score(self, hour, weather_text=None):
        """光照/时间段评分（满分20）- 保留向后兼容"""
        is_overcast = weather_text and ('阴' in weather_text or '多云' in weather_text)
        is_low_cloud = weather_text and '云' in weather_text

        if hour is None:
            if is_overcast:
                return 16
            elif is_low_cloud:
                return 14
            else:
                return 10

        # 清晨(5-7)/黄昏(17-19)满分
        if 5 <= hour <= 7:
            return 20
        elif 17 <= hour <= 19:
            return 20
        elif hour in (8, 9, 15, 16):
            return 15  # 次优时段
        elif hour in (4, 20, 21):
            return 12  # 夜钓前/深晨
        elif 10 <= hour <= 14:
            # 正午时段，检查是否阴天
            if is_overcast:
                return 16
            elif is_low_cloud:
                return 14
            else:
                return 6  # 正午烈日
        else:
            return 8  # 深夜

    def _calc_moon_score(self, moon_phase):
        """月相评分（满分15）"""
        if moon_phase is None:
            return 7  # 无数据给中间分
        if any(k in moon_phase for k in ['新月', '满月', '朔', '望']):
            return 15
        elif any(k in moon_phase for k in ['上弦', '下弦', '半月']):
            return 7
        else:
            return 3

    def _calc_rain_score(self, precipitation_mm):
        """降水评分（满分10）"""
        if precipitation_mm is None:
            return 10  # 无降水数据视为无雨
        if precipitation_mm < 1:
            return 10  # 无雨或毛毛雨
        elif precipitation_mm < 5:
            return 7   # 小雨
        elif precipitation_mm < 15:
            return 3   # 中雨
        else:
            return 0   # 大雨以上

    def _calc_pressure_trend_bonus(self, pressure_history, current_pressure, pressure_change=0):
        """
        气压趋势加成（原始值，不含鱼种敏感度修正）：
        - 稳定（3h变化<2hPa）：不加不减
        - 缓升（3h升2-5hPa）：+3分
        - 骤降（3h降>5hPa）：-10分
        - 持续高压后缓降：+5分（窗口期）
        """
        # 优先使用 pressure_history 列表
        if pressure_history and len(pressure_history) >= 2:
            oldest = pressure_history[0]
            newest = pressure_history[-1]
            delta = newest - oldest
            avg = sum(pressure_history) / len(pressure_history)

            if delta < -5:
                return -10  # 骤降
            elif -5 <= delta <= -2 and avg > 1020:
                return 5   # 持续高压后缓降（窗口期）
            elif 2 <= delta <= 5:
                return 3   # 缓升
            elif abs(delta) < 2:
                return 0   # 稳定
            else:
                return 0

        # 兼容旧的 pressure_change 参数（每小时变化，乘3估算3h变化）
        if pressure_change != 0:
            delta_3h = pressure_change * 3
            if delta_3h < -5:
                return -10
            elif 2 <= delta_3h <= 5:
                return 3
            elif abs(delta_3h) < 2:
                return 0

        return 0


# ===== 验证测试：相同条件下不同鱼种的评分差异 =====
# 测试条件: 25℃、1015hPa、3m/s风速、5月、7:00、无降水、无月相数据
#
# if __name__ == "__main__":
#     calc = FishingIndexCalculator()
#     test_params = {
#         "temperature": 25, "pressure": 1015, "wind_speed": 3,
#         "month": 5, "hour": 7, "precipitation_mm": 0,
#         "moon_phase": None, "pressure_history": None
#     }
#     print("=== 鱼种精细化评分对比 ===")
#     print(f"条件: 25℃, 1015hPa, 3m/s风速, 5月, 07:00")
#     print("-" * 40)
#     for species in ["翘嘴", "黑鱼", "鳜鱼", "鲈鱼", "马口", "鳡鱼", "军鱼", "default"]:
#         score = calc.calculate(fish_species=species, **test_params)
#         desc = calc.get_description(score)
#         config = SPECIES_CONFIG.get(species, DEFAULT_CONFIG)
#         print(f"  {species:6s} -> {score:3d}分 [{desc}] "
#               f"(最优温区{config['optimal_temp']}, 敏感度{config['pressure_sensitivity']})")
#     print("-" * 40)
#     # 预期结果：
#     # 翘嘴: 25℃在(16,28)区间内满分, 7:00在peak_hours, 5月季节加成1.15 → 高分
#     # 黑鱼: 25℃在(20,30)区间内满分, 7:00在peak_hours, 5月季节加成1.1 → 高分但略低
#     # 鳜鱼: 25℃在(18,25)区间边缘满分, 7:00在peak_hours, 5月季节加成1.15 → 高分
#     # 马口: 25℃超出(14,22)区间, 7:00在peak_hours内, 5月加成1.2 → 温度拉低
#     # 不同鱼种应有明显分数差异，体现鱼种精细化效果
