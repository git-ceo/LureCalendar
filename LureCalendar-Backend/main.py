from fastapi import FastAPI, Body, Query, File, UploadFile, Depends
from fastapi.staticfiles import StaticFiles
from sqlalchemy import create_engine, Column, String, Float, Integer, BigInteger, Text, ForeignKey
from sqlalchemy.orm import declarative_base, sessionmaker, relationship, Session
from typing import List, Optional
import json
import time
import os
import shutil
import os
from urllib.parse import quote_plus
from services.fishing_index import FishingIndexCalculator, SPECIES_CONFIG, DEFAULT_CONFIG
from services.recommendation import (
    LureRecommendationService,
    SpeciesActivityService,
    CalendarScoreService,
    PressureTrendService,
)

# --- PDF 导出相关 ---
try:
    from reportlab.pdfgen import canvas
    from reportlab.lib.pagesizes import A4
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    # 如果服务器有中文字体，请指向它。这里先用默认
except ImportError:
    pass

# --- 文件夹配置 ---
UPLOAD_DIR = "uploads"
if not os.path.exists(UPLOAD_DIR):
    os.makedirs(UPLOAD_DIR)

# --- 自动加载 .env 文件（使用 stdlib，无需 python-dotenv）---
def _load_dotenv(path: str = ".env") -> None:
    env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), path)
    if not os.path.exists(env_path):
        return
    try:
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, _, value = line.partition("=")
                key = key.strip()
                value = value.strip().strip('"').strip("'")
                # 已存在的环境变量优先，不覆盖
                if key and key not in os.environ:
                    os.environ[key] = value
    except Exception as e:
        print(f".env 加载失败（忽略）: {e}")

_load_dotenv()

# --- 数据库配置 ---
def build_database_url() -> str:
    explicit = os.getenv("DATABASE_URL") or os.getenv("LURECALENDAR_DATABASE_URL")
    if explicit:
        return explicit

    # MySQL 默认配置（优先从环境变量读取，否则使用本地默认值）
    mysql_host = (os.getenv("MYSQL_HOST") or os.getenv("DB_HOST") or "localhost").strip()
    mysql_db = (os.getenv("MYSQL_DB") or os.getenv("MYSQL_DATABASE") or os.getenv("DB_NAME") or "lurecalendar").strip()
    mysql_user = (os.getenv("MYSQL_USER") or os.getenv("MYSQL_USERNAME") or os.getenv("DB_USER") or "root").strip()
    mysql_password = os.getenv("MYSQL_PASSWORD") or os.getenv("MYSQL_PASS") or os.getenv("MYSQL_PWD") or os.getenv("DB_PASSWORD") or ""
    mysql_port = (os.getenv("MYSQL_PORT") or os.getenv("DB_PORT") or "3306").strip()

    user = quote_plus(mysql_user)
    pwd = quote_plus(mysql_password)
    return f"mysql+pymysql://{user}:{pwd}@{mysql_host}:{mysql_port}/{mysql_db}?charset=utf8mb4"


DATABASE_URL = build_database_url()
engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {},
    pool_pre_ping=True
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# --- 数据库模型 ---

class FishingSpotModel(Base):
    __tablename__ = "fishing_spots"
    id = Column(String(64), primary_key=True, index=True)
    name = Column(String(128))
    latitude = Column(Float)
    longitude = Column(Float)
    river = Column(String(128), nullable=True)
    city = Column(String(64), nullable=True)
    location_detail = Column(String(255), nullable=True)
    q_weather_location_id = Column(String(32), nullable=True)
    water_type = Column(String(32))
    structure = Column(String(32))
    depth = Column(Float, nullable=True)
    target_species = Column(String(255), nullable=True)
    lure_types = Column(String(255), nullable=True)
    best_season = Column(String(128), nullable=True)
    note = Column(Text, nullable=True)
    photos = Column(Text)  # JSON String
    create_time = Column(BigInteger)
    update_time = Column(BigInteger)
    # v3.2 分类字段
    spot_type = Column(String(20), default='野河', nullable=True)
    fee_type = Column(String(10), default='免费', nullable=True)
    district = Column(String(20), default='', nullable=True)

class UserSpotFavoriteModel(Base):
    """用户收藏钓点"""
    __tablename__ = "user_spot_favorites"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(String(64), default='default_user', index=True)
    spot_id = Column(String(64), index=True)
    created_at = Column(BigInteger)

class CatchRecordModel(Base):
    __tablename__ = "catch_records"
    id = Column(String(64), primary_key=True, index=True)
    user_phone = Column(String(32), index=True) # 关联用户
    spot_id = Column(String(64))
    species = Column(String(64))
    length = Column(Float, nullable=True)
    weight = Column(Float, nullable=True)
    photo_uris = Column(Text) # JSON String
    weather_key = Column(String(64), nullable=True)
    catch_time = Column(BigInteger)
    bait = Column(String(128), nullable=True)
    rod = Column(String(128), nullable=True)
    note = Column(Text, nullable=True)
    released = Column(Integer)
    river = Column(String(128), nullable=True)
    city = Column(String(64), nullable=True)
    location_detail = Column(String(255), nullable=True)
    count = Column(Integer, default=1)
    temperature = Column(Float, nullable=True)
    humidity = Column(Integer, nullable=True)
    pressure = Column(Float, nullable=True)
    fishing_index = Column(Integer, nullable=True)
    lure_type = Column(String(100), nullable=True)
    rig_type = Column(String(100), nullable=True)
    structure_zone = Column(String(100), nullable=True)
    water_clarity = Column(String(50), nullable=True)
    wind_shore_relation = Column(String(50), nullable=True)

class WeatherCacheModel(Base):
    __tablename__ = "weather_cache"
    location_key = Column(String(64), primary_key=True, index=True)
    data_json = Column(Text)
    timestamp = Column(BigInteger)

class WaterLevelCacheModel(Base):
    __tablename__ = "water_level_cache"
    station_id = Column(String(64), primary_key=True, index=True)
    data_json = Column(Text)
    timestamp = Column(BigInteger)

# === 内容知识库表（v3.0 新增） ===
class FishEncyclopediaModel(Base):
    """鱼种百科：分布、习性、最佳作钓季节、对应饵型等。"""
    __tablename__ = "fish_encyclopedia"
    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(64), unique=True, index=True)           # 中文名（如：翘嘴）
    alias = Column(String(255), nullable=True)                    # 别名（逗号分隔）
    scientific_name = Column(String(128), nullable=True)          # 拉丁学名
    family = Column(String(64), nullable=True)                    # 科属
    category = Column(String(32), nullable=True)                  # 路亚对象鱼/台钓常见鱼/海钓鱼
    distribution = Column(Text, nullable=True)                    # 分布范围
    habitat = Column(Text, nullable=True)                         # 栖息环境
    feeding_habit = Column(Text, nullable=True)                   # 食性
    body_size = Column(String(128), nullable=True)                # 体型范围（如 30-80cm/0.5-5kg）
    best_season = Column(String(128), nullable=True)              # 最佳作钓季节
    best_hours = Column(String(128), nullable=True)               # 最佳时段
    optimal_temp = Column(String(64), nullable=True)              # 适宜水温（如 18-26）
    recommended_lures = Column(String(255), nullable=True)        # 推荐饵型（逗号）
    technique_tips = Column(Text, nullable=True)                  # 钓法要点
    image_url = Column(String(512), nullable=True)                # 主图
    description = Column(Text, nullable=True)                     # 综合描述
    source = Column(String(128), nullable=True)                   # 数据源
    update_time = Column(BigInteger)

class LureLibraryModel(Base):
    """路亚饵知识库：分类、参数、适用鱼种与水温。"""
    __tablename__ = "lure_library"
    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(64), index=True)                         # 米诺/亮片/VIB/铅头钩/雷蛙...
    category = Column(String(32))                                 # 硬饵/软饵/金属饵/水面系
    sub_type = Column(String(64), nullable=True)                  # 浮水米诺/沉水米诺/铅笔等
    swim_layer = Column(String(32), nullable=True)                # 上层/中层/底层/水面
    weight_range = Column(String(64), nullable=True)              # 5-15g
    length_range = Column(String(64), nullable=True)              # 50-90mm
    diving_depth = Column(String(64), nullable=True)              # 0-1.5m
    target_species = Column(String(255), nullable=True)           # 翘嘴,鲈鱼
    suitable_water_temp = Column(String(64), nullable=True)       # 18-28
    suitable_water_type = Column(String(128), nullable=True)      # 河流,水库,黑坑
    technique = Column(Text, nullable=True)                       # 操作手法（抽停/匀收/twitch）
    color_tip = Column(String(255), nullable=True)                # 配色建议
    pros = Column(Text, nullable=True)                            # 优势
    cons = Column(Text, nullable=True)                            # 局限
    icon = Column(String(64), nullable=True)
    image_url = Column(String(512), nullable=True)
    description = Column(Text, nullable=True)
    source = Column(String(128), nullable=True)
    update_time = Column(BigInteger)

class FishingGuideModel(Base):
    """钓法教程/季节攻略：图文知识。"""
    __tablename__ = "fishing_guides"
    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(255), index=True)
    category = Column(String(32))                                 # 入门/进阶/季节/钓法/装备
    target_species = Column(String(128), nullable=True)
    season = Column(String(64), nullable=True)                    # 春/夏/秋/冬/全年
    water_type = Column(String(64), nullable=True)
    summary = Column(String(512), nullable=True)
    content = Column(Text)                                        # markdown
    cover_url = Column(String(512), nullable=True)
    tags = Column(String(255), nullable=True)                     # 逗号
    source = Column(String(128), nullable=True)                   # 来源
    source_url = Column(String(512), nullable=True)
    view_count = Column(Integer, default=0)
    create_time = Column(BigInteger)
    update_time = Column(BigInteger)

class AstronomyCacheModel(Base):
    """日出日落 + 月相 + 农历 + 禁渔期标记，按日期+地区缓存。"""
    __tablename__ = "astronomy_cache"
    id = Column(Integer, primary_key=True, autoincrement=True)
    region_key = Column(String(64), index=True)                   # 经纬度hash或省市
    date = Column(String(16), index=True)                         # YYYY-MM-DD
    sunrise = Column(String(8), nullable=True)                    # HH:MM
    sunset = Column(String(8), nullable=True)
    moonrise = Column(String(8), nullable=True)
    moonset = Column(String(8), nullable=True)
    moon_phase = Column(String(32), nullable=True)                # 朔/上弦/望/下弦
    moon_illumination = Column(Float, nullable=True)              # 0-1
    lunar_date = Column(String(32), nullable=True)                # 农历
    is_closed_season = Column(Integer, default=0)                 # 是否禁渔期
    closed_season_note = Column(String(255), nullable=True)
    update_time = Column(BigInteger)

class UserModel(Base):
    __tablename__ = "users"
    phone = Column(String(32), primary_key=True, index=True)
    password = Column(String(128))
    username = Column(String(64))
    signature = Column(String(255), default="这个钓鱼佬很懒，什么都没留下")
    avatar_url = Column(String(512), nullable=True)
    background_url = Column(String(512), nullable=True)
    create_time = Column(BigInteger)
    is_admin = Column(Integer, default=0) # 0:普通用户, 1:管理员
    add_friend_confirm = Column(Integer, default=0) # 0: 直接关注, 1: 需确认
    
    # 实时位置字段
    last_latitude = Column(Float, nullable=True)
    last_longitude = Column(Float, nullable=True)
    last_location_time = Column(BigInteger, nullable=True)
    share_location = Column(Integer, default=0) # 是否开启位置分享

class MomentModel(Base):
    __tablename__ = "moments"
    id = Column(String(64), primary_key=True, index=True)
    user_phone = Column(String(32), ForeignKey("users.phone"))
    content = Column(Text)
    photos = Column(Text) # JSON String
    catch_id = Column(String(64), nullable=True)
    visibility = Column(String(16), default="public") # public, friends
    create_time = Column(BigInteger)
    
    user = relationship("UserModel")
    likes = relationship("LikeModel", back_populates="moment", cascade="all, delete-orphan")
    comments = relationship("CommentModel", back_populates="moment", cascade="all, delete-orphan")

class LikeModel(Base):
    __tablename__ = "likes"
    id = Column(Integer, primary_key=True, autoincrement=True)
    moment_id = Column(String(64), ForeignKey("moments.id"))
    user_phone = Column(String(32), ForeignKey("users.phone"))
    
    moment = relationship("MomentModel", back_populates="likes")

class CommentModel(Base):
    __tablename__ = "comments"
    id = Column(Integer, primary_key=True, autoincrement=True)
    moment_id = Column(String(64), ForeignKey("moments.id"))
    user_phone = Column(String(32), ForeignKey("users.phone"))
    parent_id = Column(Integer, nullable=True) # 二级回复
    content = Column(Text)
    create_time = Column(BigInteger)
    
    moment = relationship("MomentModel", back_populates="comments")
    user = relationship("UserModel")

class FollowModel(Base):
    __tablename__ = "follows"
    id = Column(Integer, primary_key=True, autoincrement=True)
    follower_phone = Column(String(32), ForeignKey("users.phone"))
    followed_phone = Column(String(32), ForeignKey("users.phone"))
    create_time = Column(BigInteger)

class FriendRequestModel(Base):
    __tablename__ = "friend_requests"
    id = Column(Integer, primary_key=True, autoincrement=True)
    from_phone = Column(String(32), ForeignKey("users.phone"))
    to_phone = Column(String(32), ForeignKey("users.phone"))
    status = Column(String(16), default="pending") # pending, accepted, rejected
    create_time = Column(BigInteger)

def get_or_create_user(db, phone: str) -> UserModel:
    user = db.query(UserModel).filter(UserModel.phone == phone).first()
    if user:
        return user
    username = f"钓鱼佬_{phone[-4:]}" if phone and len(phone) >= 4 else "钓鱼佬"
    user = UserModel(phone=phone, password="", username=username, create_time=int(time.time() * 1000))
    db.add(user)
    db.flush()
    return user

# 初始化数据库
try:
    dialect = engine.url.get_backend_name()
    database = engine.url.database
    host = getattr(engine.url, "host", None)
    port = getattr(engine.url, "port", None)
    if dialect.startswith("mysql"):
        print(f"DB: mysql host={host}:{port} db={database}")
    else:
        print(f"DB: {dialect} db={database}")
    Base.metadata.create_all(bind=engine)
except Exception as e:
    print(f"数据库初始化失败：{e}")
    raise 

# 初始化钓鱼指数计算器
index_calculator = FishingIndexCalculator()

# 初始化推荐服务
lure_recommender = LureRecommendationService()
species_activity_service = SpeciesActivityService()
calendar_score_service = CalendarScoreService()
pressure_trend_service = PressureTrendService()

# 依赖注入: 数据库会话
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 自动执行迁移逻辑（SQLite 专用，补齐缺失字段；MySQL 通过 create_all 自动建表）
if DATABASE_URL.startswith("sqlite"):
    try:
        from migrate import migrate
        migrate()
    except Exception as e:
        print(f"自动迁移失败 (可能是首次启动): {e}")

# --- FastAPI 接口 ---
app = FastAPI(title="LureCalendar API")

# 挂载静态文件目录，使上传的图片可以通过浏览器访问
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")

@app.get("/")
def read_root():
    return {"status": "running", "message": "LureCalendar API is ready!"}

# --- 同步接口 ---
@app.get("/api/spots")
async def get_all_spots(
    spot_type: Optional[str] = None,
    fee_type: Optional[str] = None,
    city: Optional[str] = None,
    district: Optional[str] = None
):
    db = SessionLocal()
    try:
        query = db.query(FishingSpotModel)
        if spot_type:
            query = query.filter(FishingSpotModel.spot_type == spot_type)
        if fee_type:
            query = query.filter(FishingSpotModel.fee_type == fee_type)
        if city:
            query = query.filter(FishingSpotModel.city == city)
        if district:
            query = query.filter(FishingSpotModel.district == district)
        spots = query.all()
        result = []
        for s in spots:
            result.append({
                "id": s.id,
                "name": s.name,
                "latitude": s.latitude,
                "longitude": s.longitude,
                "river": s.river,
                "city": s.city,
                "locationDetail": s.location_detail,
                "qWeatherLocationId": s.q_weather_location_id,
                "waterType": s.water_type,
                "structure": s.structure,
                "depth": s.depth,
                "targetSpecies": s.target_species,
                "lureTypes": s.lure_types,
                "bestSeason": s.best_season,
                "note": s.note,
                "photos": json.loads(s.photos) if s.photos else [],
                "createTime": s.create_time,
                "updateTime": s.update_time,
                "spotType": s.spot_type,
                "feeType": s.fee_type,
                "district": s.district
            })
        return result
    finally:
        db.close()

# --- 收藏接口 ---
@app.post("/api/spots/favorite")
async def add_favorite(data: dict = Body(...)):
    db = SessionLocal()
    try:
        spot_id = data.get('spot_id') or data.get('spotId')
        user_id = data.get('user_id') or data.get('userId') or 'default_user'
        existing = db.query(UserSpotFavoriteModel).filter(
            UserSpotFavoriteModel.user_id == user_id,
            UserSpotFavoriteModel.spot_id == str(spot_id)
        ).first()
        if existing:
            return {"success": True, "message": "已收藏"}
        fav = UserSpotFavoriteModel(
            user_id=user_id,
            spot_id=str(spot_id),
            created_at=int(time.time() * 1000)
        )
        db.add(fav)
        db.commit()
        return {"success": True, "message": "收藏成功"}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

@app.delete("/api/spots/favorite")
async def remove_favorite(spot_id: str = Query(...), user_id: str = Query(default="default_user")):
    db = SessionLocal()
    try:
        fav = db.query(UserSpotFavoriteModel).filter(
            UserSpotFavoriteModel.user_id == user_id,
            UserSpotFavoriteModel.spot_id == spot_id
        ).first()
        if fav:
            db.delete(fav)
            db.commit()
            return {"success": True, "message": "取消收藏"}
        return {"success": True, "message": "未收藏"}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

@app.get("/api/spots/favorites")
async def get_favorites(user_id: str = Query(default="default_user")):
    db = SessionLocal()
    try:
        favs = db.query(UserSpotFavoriteModel).filter(
            UserSpotFavoriteModel.user_id == user_id
        ).all()
        spot_ids = [f.spot_id for f in favs]
        if not spot_ids:
            return []
        spots = db.query(FishingSpotModel).filter(FishingSpotModel.id.in_(spot_ids)).all()
        result = []
        for s in spots:
            result.append({
                "id": s.id,
                "name": s.name,
                "latitude": s.latitude,
                "longitude": s.longitude,
                "river": s.river,
                "city": s.city,
                "locationDetail": s.location_detail,
                "waterType": s.water_type,
                "structure": s.structure,
                "depth": s.depth,
                "targetSpecies": s.target_species,
                "lureTypes": s.lure_types,
                "bestSeason": s.best_season,
                "note": s.note,
                "photos": json.loads(s.photos) if s.photos else [],
                "createTime": s.create_time,
                "updateTime": s.update_time,
                "spotType": s.spot_type,
                "feeType": s.fee_type,
                "district": s.district
            })
        return result
    finally:
        db.close()

@app.post("/api/spots/sync")
async def sync_spots(spots: List[dict] = Body(...)):
    db = SessionLocal()
    try:
        for s in spots:
            data = {
                "id": s['id'], "name": s['name'], "latitude": s['latitude'], "longitude": s['longitude'],
                "river": s.get('river'), "city": s.get('city'), "location_detail": s.get('locationDetail'),
                "q_weather_location_id": s.get('qWeatherLocationId'), "water_type": s['waterType'],
                "structure": s['structure'], "depth": s.get('depth'),
                "target_species": s.get('targetSpecies'), "lure_types": s.get('lureTypes'),
                "best_season": s.get('bestSeason'),
                "note": s.get('note'),
                "photos": json.dumps(s.get('photos', [])), "create_time": s['createTime'], "update_time": s['updateTime']
            }
            existing = db.query(FishingSpotModel).filter(FishingSpotModel.id == data['id']).first()
            if existing:
                for key, value in data.items(): setattr(existing, key, value)
            else:
                db.add(FishingSpotModel(**data))
        db.commit()
        return {"success": True, "syncedCount": len(spots)}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

@app.post("/api/catches/sync")
async def sync_catches(catches: List[dict] = Body(...)):
    db = SessionLocal()
    try:
        for c in catches:
            released_val = 1 if c.get("released", False) else 0
            data = {
                "id": c['id'], "user_phone": c.get('userPhone'), "spot_id": c['spotId'], "species": c['species'], "length": c.get('length'),
                "weight": c.get('weight'), "photo_uris": json.dumps(c.get("photoUris", [])),
                "weather_key": c.get('weatherKey'), "catch_time": c['catchTime'], "bait": c.get('bait'),
                "rod": c.get('rod'), "note": c.get('note'), "released": released_val,
                "river": c.get('river'), "city": c.get('city'), "location_detail": c.get('locationDetail'),
                "count": c.get('count', 1), "temperature": c.get('temperature'), "humidity": c.get('humidity'),
                "pressure": c.get('pressure'), "fishing_index": c.get('fishingIndex'),
                "lure_type": c.get('lureType'), "rig_type": c.get('rigType'),
                "structure_zone": c.get('structureZone'), "water_clarity": c.get('waterClarity'),
                "wind_shore_relation": c.get('windShoreRelation')
            }
            existing = db.query(CatchRecordModel).filter(CatchRecordModel.id == data['id']).first()
            if existing:
                for key, value in data.items(): setattr(existing, key, value)
            else:
                db.add(CatchRecordModel(**data))
        db.commit()
        return {"success": True, "syncedCount": len(catches)}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

# --- 用户与社交接口 ---

@app.post("/api/auth/register")
async def register(data: dict = Body(...)):
    db = SessionLocal()
    try:
        phone, password = data['phone'], data['password']
        username = data.get('username', f"钓鱼佬_{phone[-4:]}")
        if db.query(UserModel).filter(UserModel.phone == phone).first():
            return {"success": False, "message": "该手机号已注册"}
        new_user = UserModel(phone=phone, password=password, username=username, create_time=int(time.time() * 1000))
        db.add(new_user)
        db.commit()
        return {"success": True, "userId": phone, "username": username}
    finally:
        db.close()

@app.post("/api/auth/login")
async def login(data: dict = Body(...)):
    db = SessionLocal()
    try:
        user = db.query(UserModel).filter(UserModel.phone == data['phone'], UserModel.password == data['password']).first()
        if not user: return {"success": False, "message": "手机号或密码错误"}
        return {"success": True, "userId": user.phone, "username": user.username}
    finally:
        db.close()

@app.get("/api/user/profile")
async def get_profile(phone: str):
    db = SessionLocal()
    try:
        user = get_or_create_user(db, phone)
        db.commit()
        return {
            "success": True,
            "username": user.username,
            "signature": user.signature,
            "avatarUrl": user.avatar_url,
            "backgroundUrl": user.background_url,
            "createTime": user.create_time
        }
    finally:
        db.close()

@app.post("/api/user/update")
async def update_profile(data: dict = Body(...)):
    db = SessionLocal()
    try:
        user = get_or_create_user(db, data['phone'])
        if 'username' in data: user.username = data['username']
        if 'signature' in data: user.signature = data['signature']
        if 'avatarUrl' in data: user.avatar_url = data['avatarUrl']
        if 'backgroundUrl' in data: user.background_url = data['backgroundUrl']
        if 'shareLocation' in data: user.share_location = 1 if data['shareLocation'] else 0
        db.commit()
        return {"success": True}
    finally:
        db.close()

# --- 位置共享接口 ---
@app.post("/api/user/location")
async def update_location(data: dict = Body(...)):
    db = SessionLocal()
    try:
        phone = data.get('phone')
        user = get_or_create_user(db, phone)
        
        user.last_latitude = data.get('latitude')
        user.last_longitude = data.get('longitude')
        user.last_location_time = int(time.time() * 1000)
        db.commit()
        return {"success": True}
    finally:
        db.close()

@app.get("/api/users/locations")
async def get_all_locations(viewer_phone: str):
    db = SessionLocal()
    try:
        viewer = db.query(UserModel).filter(UserModel.phone == viewer_phone).first()
        if not viewer: return []
        
        # 逻辑：只有管理员能看所有人；普通用户只能看开启了 share_location 且 2小时内活跃的用户
        now = int(time.time() * 1000)
        active_threshold = now - (2 * 60 * 60 * 1000)
        
        query = db.query(UserModel).filter(
            UserModel.last_latitude != None,
            UserModel.last_location_time > active_threshold
        )
        
        if viewer.is_admin != 1:
            # 普通用户过滤掉未开启分享的
            query = query.filter(UserModel.share_location == 1)
            
        users = query.all()
        result = []
        for u in users:
            if u.phone == viewer_phone: continue
            result.append({
                "phone": u.phone,
                "username": u.username,
                "avatarUrl": u.avatar_url,
                "latitude": u.last_latitude,
                "longitude": u.last_longitude,
                "lastActive": u.last_location_time
            })
        return result
    finally:
        db.close()

@app.get("/api/moments")
async def get_moments(type: str = "square", phone: Optional[str] = None, page: int = 0, size: int = 20):
    db = SessionLocal()
    try:
        query = db.query(MomentModel)
        if type == "friends" and phone:
            # 获取关注列表
            following = db.query(FollowModel.followed_phone).filter(FollowModel.follower_phone == phone).all()
            following_phones = [f[0] for f in following] + [phone]
            query = query.filter(MomentModel.user_phone.in_(following_phones))
        else:
            query = query.filter(MomentModel.visibility == "public")
            
        moments = query.order_by(MomentModel.create_time.desc()).offset(page*size).limit(size).all()
        result = []
        for m in moments:
            is_liked = False
            if phone:
                is_liked = db.query(LikeModel).filter(LikeModel.moment_id == m.id, LikeModel.user_phone == phone).first() is not None
            
            result.append({
                "id": m.id,
                "userId": m.user_phone,
                "username": m.user.username,
                "avatarUrl": m.user.avatar_url,
                "content": m.content,
                "photos": json.loads(m.photos),
                "visibility": m.visibility,
                "createTime": m.create_time,
                "likeCount": len(m.likes),
                "commentCount": len(m.comments),
                "isLiked": is_liked
            })
        return result
    finally:
        db.close()

@app.post("/api/moments")
async def create_moment(data: dict = Body(...)):
    db = SessionLocal()
    try:
        phone = data['phone']
        user = get_or_create_user(db, phone)
        new_m = MomentModel(
            id=str(int(time.time()*1000)),
            user_phone=phone,
            content=data['content'],
            photos=json.dumps(data.get('photos', [])),
            visibility=data.get('visibility', 'public'),
            create_time=int(time.time()*1000)
        )
        db.add(new_m)
        db.commit()
        db.refresh(new_m)
        
        return {
            "success": True,
            "id": new_m.id,
            "userId": phone,
            "username": user.username,
            "avatarUrl": user.avatar_url,
            "content": new_m.content,
            "photos": json.loads(new_m.photos),
            "visibility": new_m.visibility,
            "createTime": new_m.create_time,
            "likeCount": 0,
            "commentCount": 0,
            "isLiked": False
        }
    finally:
        db.close()

@app.post("/api/moments/{id}/like")
async def toggle_like(id: str, phone: str = Query(...)):
    db = SessionLocal()
    try:
        get_or_create_user(db, phone)
        existing = db.query(LikeModel).filter(LikeModel.moment_id == id, LikeModel.user_phone == phone).first()
        if existing:
            db.delete(existing)
            liked = False
        else:
            db.add(LikeModel(moment_id=id, user_phone=phone))
            liked = True
        db.commit()
        count = db.query(LikeModel).filter(LikeModel.moment_id == id).count()
        return {"liked": liked, "like_count": count}
    finally:
        db.close()

@app.get("/api/moments/{id}/comments")
async def get_comments(id: str):
    db = SessionLocal()
    try:
        # 获取所有评论
        all_comments = db.query(CommentModel).filter(CommentModel.moment_id == id).order_by(CommentModel.create_time.asc()).all()
        # 构建评论树
        comment_map = {}
        roots = []
        for c in all_comments:
            c_data = {
                "id": c.id,
                "userId": c.user_phone,
                "username": c.user.username,
                "avatarUrl": c.user.avatar_url,
                "content": c.content,
                "createTime": c.create_time,
                "replies": []
            }
            comment_map[c.id] = c_data
            if c.parent_id is None:
                roots.append(c_data)
            else:
                parent = comment_map.get(c.parent_id)
                if parent:
                    parent["replies"].append(c_data)
        return roots
    finally:
        db.close()

@app.post("/api/moments/{id}/comment")
async def create_comment(id: str, data: dict = Body(...)):
    db = SessionLocal()
    try:
        get_or_create_user(db, data['phone'])
        moment = db.query(MomentModel).filter(MomentModel.id == id).first()
        if not moment:
            return {"success": False, "message": "动态不存在"}
        new_c = CommentModel(
            moment_id=id,
            user_phone=data['phone'],
            parent_id=data.get('parent_id'),
            content=data['content'],
            create_time=int(time.time()*1000)
        )
        db.add(new_c)
        db.commit()
        return {"success": True}
    finally:
        db.close()

# --- 用户搜索与好友系统 ---
@app.get("/api/users/search")
async def search_users(keyword: str, phone: str):
    db = SessionLocal()
    try:
        # 搜索手机号或昵称，排除自己
        users = db.query(UserModel).filter(
            UserModel.phone != phone,
            (UserModel.phone.like(f"%{keyword}%")) | (UserModel.username.like(f"%{keyword}%"))
        ).all()
        
        # 检查关注状态
        following = db.query(FollowModel.followed_phone).filter(FollowModel.follower_phone == phone).all()
        following_phones = [f[0] for f in following]
        
        result = []
        for u in users:
            result.append({
                "phone": u.phone,
                "username": u.username,
                "avatarUrl": u.avatar_url,
                "signature": u.signature,
                "isFollowing": u.phone in following_phones,
                "addConfirm": u.add_friend_confirm == 1
            })
        return result
    finally:
        db.close()

@app.post("/api/follow/{target_phone}")
async def follow_user(target_phone: str, phone: str = Query(...)):
    db = SessionLocal()
    try:
        target = db.query(UserModel).filter(UserModel.phone == target_phone).first()
        if not target: return {"success": False, "message": "用户不存在"}
        get_or_create_user(db, phone)
        
        if target.add_friend_confirm == 1:
            # 创建好友申请
            req = db.query(FriendRequestModel).filter(
                FriendRequestModel.from_phone == phone, 
                FriendRequestModel.to_phone == target_phone,
                FriendRequestModel.status == "pending"
            ).first()
            if not req:
                db.add(FriendRequestModel(from_phone=phone, to_phone=target_phone, create_time=int(time.time()*1000)))
                db.commit()
            return {"status": "pending"}
        else:
            # 直接关注
            existing = db.query(FollowModel).filter(FollowModel.follower_phone == phone, FollowModel.followed_phone == target_phone).first()
            if not existing:
                db.add(FollowModel(follower_phone=phone, followed_phone=target_phone, create_time=int(time.time()*1000)))
                db.commit()
            return {"status": "accepted"}
    finally:
        db.close()

@app.get("/api/friend-requests")
async def get_friend_requests(phone: str):
    db = SessionLocal()
    try:
        reqs = db.query(FriendRequestModel, UserModel).join(
            UserModel, FriendRequestModel.from_phone == UserModel.phone
        ).filter(FriendRequestModel.to_phone == phone, FriendRequestModel.status == "pending").all()
        
        result = []
        for r, u in reqs:
            result.append({
                "id": r.id,
                "fromPhone": u.phone,
                "username": u.username,
                "avatarUrl": u.avatar_url,
                "createTime": r.create_time
            })
        return result
    finally:
        db.close()

@app.put("/api/friend-requests/{id}")
async def handle_friend_request(id: int, action: str = Query(...)):
    db = SessionLocal()
    try:
        req = db.query(FriendRequestModel).filter(FriendRequestModel.id == id).first()
        if not req: return {"success": False}
        
        if action == "accept":
            req.status = "accepted"
            # 建立关注关系
            db.add(FollowModel(follower_phone=req.from_phone, followed_phone=req.to_phone, create_time=int(time.time()*1000)))
            db.add(FollowModel(follower_phone=req.to_phone, followed_phone=req.from_phone, create_time=int(time.time()*1000)))
        else:
            req.status = "rejected"
        db.commit()
        return {"success": True}
    finally:
        db.close()

# --- 雷达面对面添加 (内存简易版) ---
RADAR_CODES = {} # {code: {"phone": phone, "expire": timestamp}}

@app.post("/api/users/radar/start")
async def start_radar(phone: str = Query(...)):
    import random
    code = "".join([str(random.randint(0, 9)) for _ in range(6)])
    RADAR_CODES[code] = {"phone": phone, "expire": time.time() + 60}
    return {"code": code, "expires_in": 60}

@app.post("/api/users/radar/add")
async def add_radar(code: str = Body(embed=True), phone: str = Query(...)):
    db = SessionLocal()
    try:
        entry = RADAR_CODES.get(code)
        if not entry or entry["expire"] < time.time():
            return {"success": False, "message": "识别码无效或已过期"}
        
        target_phone = entry["phone"]
        if target_phone == phone: return {"success": False, "message": "不能添加自己"}
        
        # 互相关注
        for f, t in [(phone, target_phone), (target_phone, phone)]:
            if not db.query(FollowModel).filter(FollowModel.follower_phone == f, FollowModel.followed_phone == t).first():
                db.add(FollowModel(follower_phone=f, followed_phone=t, create_time=int(time.time()*1000)))
        db.commit()
        return {"success": True}
    finally:
        db.close()

# --- 文件上传接口 ---
@app.post("/api/upload")
async def upload_file(file: UploadFile = File(...)):
    try:
        file_ext = file.filename.split(".")[-1]
        file_name = f"{int(time.time()*1000)}.{file_ext}"
        file_path = os.path.join(UPLOAD_DIR, file_name)
        
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # 这里返回你的服务器公网 IP 或域名
        base_url = "http://125.67.191.50:8001" 
        return {
            "success": True, 
            "url": f"{base_url}/uploads/{file_name}",
            "message": "上传成功"
        }
    except Exception as e:
        return {"success": False, "message": str(e)}

# --- 钓鱼指数接口 ---
@app.post("/api/weather/index")
async def get_fishing_index(data: dict = Body(...)):
    try:
        fish_species = data.get('fish_species', 'default')
        score = index_calculator.calculate(
            temp=data.get('temp', 20),
            humidity=data.get('humidity', 50),
            pressure=data.get('pressure', 1013),
            wind_speed=data.get('wind_speed', 2),
            water_temp=data.get('water_temp', 18),
            fish_species=fish_species,
            hour=data.get('hour', 12),
            pressure_change=data.get('pressure_change', 0),
            moon_phase=data.get('moon_phase'),
            pressure_history=data.get('pressure_history'),
            weather_text=data.get('weather_text'),
            precipitation_mm=data.get('precipitation_mm'),
            temperature=data.get('temperature', data.get('temp')),
            month=data.get('month'),
            lat=data.get('lat'),
            lon=data.get('lon')
        )
        description = index_calculator.get_description(score)

        # 获取当前鱼种配置
        species_config = SPECIES_CONFIG.get(fish_species, DEFAULT_CONFIG)
        
        # 联动：路亚饵建议逻辑
        lure_suggestions = []
        species = fish_species.lower() if fish_species else 'default'
        temp = data.get('temp', 20)
        
        if "翘嘴" in species or "topmouth" in species:
            if temp > 25: lure_suggestions = [
                {"name": "水面铅笔", "desc": "高水温期翘嘴活性高，适合水面系搜索", "icon": "pencil"},
                {"name": "米诺", "desc": "全泳层搜索，针对中上层活跃鱼群", "icon": "minnow"}
            ]
            else: lure_suggestions = [
                {"name": "铁板/亮片", "desc": "低水温期鱼群在深水，适合远投沉底搜索", "icon": "spoon"},
                {"name": "VIB", "desc": "全层全速搜索，利用震动诱鱼", "icon": "vib"}
            ]
        elif "鳜鱼" in species or "mandarin" in species:
            lure_suggestions = [
                {"name": "德州钓组", "desc": "底层障碍区搜索，防挂效果好", "icon": "soft"},
                {"name": "大波扒", "desc": "针对结构区边缘的领地意识打击", "icon": "popper"}
            ]
        elif "黑鱼" in species or "snakehead" in species:
            lure_suggestions = [
                {"name": "雷蛙", "desc": "水面系攻击性饵，针对黑鱼领地意识", "icon": "frog"},
                {"name": "德州钓组", "desc": "重障碍区底层搜索", "icon": "soft"}
            ]
        elif "马口" in species or "minnow" in species:
            lure_suggestions = [
                {"name": "微物亮片", "desc": "1-3g小亮片，溪流马口首选", "icon": "spoon"},
                {"name": "微物米诺", "desc": "3-5cm小米诺，模拟小鱼", "icon": "minnow"}
            ]
        elif "鳡鱼" in species or "yellowcheek" in species:
            lure_suggestions = [
                {"name": "大铁板", "desc": "远投重饵，追击高速掠食鱼", "icon": "spoon"},
                {"name": "大号米诺", "desc": "12-15cm米诺，匹配鳡鱼猎物体型", "icon": "minnow"}
            ]
        elif "军鱼" in species or "squaliobarbus" in species:
            lure_suggestions = [
                {"name": "亮片", "desc": "中层匀收搜索，激发攻击欲", "icon": "spoon"},
                {"name": "米诺", "desc": "溪流抽停手法，模拟受伤小鱼", "icon": "minnow"}
            ]
        elif "鲈鱼" in species or "bass" in species:
            lure_suggestions = [
                {"name": "卷尾蛆", "desc": "底层慢搜，经典鲈鱼软饵", "icon": "soft"},
                {"name": "曲柄钩+面条虫", "desc": "障碍区wacky钓法", "icon": "soft"}
            ]
        else:
            lure_suggestions = [
                {"name": "米诺", "desc": "路亚入门必备，万能通用型假饵", "icon": "minnow"},
                {"name": "VIB", "desc": "全泳层覆盖，快速找鱼利器", "icon": "vib"}
            ]

        return {
            "success": True, 
            "score": score, 
            "description": description,
            "target_species": fish_species,
            "species_factors": {
                "optimal_temp_range": list(species_config["optimal_temp"]),
                "peak_activity_hours": species_config["peak_hours"],
                "pressure_sensitivity": species_config["pressure_sensitivity"]
            },
            "lure_suggestions": lure_suggestions
        }
    except Exception as e:
        return {"success": False, "message": str(e)}

# --- 鱼获统计接口 ---
@app.get("/api/catches/stats")
async def get_catch_stats(phone: str):
    # ... 原有代码保持不变 (此处为了节省空间省略，实际操作会保留)
    pass

# --- PDF 报告生成接口 ---
@app.get("/api/catches/report")
async def generate_catch_report(phone: str):
    db = SessionLocal()
    try:
        user = db.query(UserModel).filter(UserModel.phone == phone).first()
        catches = db.query(CatchRecordModel).filter(CatchRecordModel.user_phone == phone).all()
        
        report_filename = f"report_{phone}_{int(time.time())}.pdf"
        report_path = os.path.join(UPLOAD_DIR, report_filename)
        
        c = canvas.Canvas(report_path, pagesize=A4)
        width, height = A4
        
        # 写入标题
        c.setFont("Helvetica-Bold", 24)
        c.drawString(100, height - 80, f"LureCalendar Fishing Report")
        
        c.setFont("Helvetica", 14)
        c.drawString(100, height - 120, f"User: {user.username if user else phone}")
        c.drawString(100, height - 140, f"Total Catches: {len(catches)}")
        c.drawString(100, height - 160, f"Generated at: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 简单的统计列表
        y = height - 200
        c.drawString(100, y, "Recent Records:")
        y -= 30
        
        for catch in catches[-10:]: # 只取最后10条
            text = f"- {catch.species} | {catch.weight}g | {time.strftime('%Y-%m-%d', time.localtime(catch.catch_time/1000))}"
            c.drawString(120, y, text)
            y -= 25
            if y < 50: break
            
        c.showPage()
        c.save()
        
        base_url = "http://125.67.191.50:8001" 
        return {
            "success": True, 
            "url": f"{base_url}/uploads/{report_filename}",
            "message": "报告生成成功"
        }
    except Exception as e:
        return {"success": False, "message": str(e)}
    finally:
        db.close()


# --- 钓点排行榜接口 ---
@app.get("/api/spots/leaderboard")
async def get_spot_leaderboard(spot_id: str):
    db = SessionLocal()
    try:
        results = db.query(CatchRecordModel, UserModel).join(
            UserModel, CatchRecordModel.user_phone == UserModel.phone
        ).filter(
            CatchRecordModel.spot_id == spot_id
        ).order_by(CatchRecordModel.weight.desc()).limit(10).all()
        leaderboard = []
        for catch, user in results:
            photos = json.loads(catch.photo_uris)
            leaderboard.append({
                "username": user.username, "avatarUrl": user.avatar_url,
                "species": catch.species, "weight": catch.weight,
                "length": catch.length,
                "photo": photos[0] if photos else None,
                "catchTime": catch.catch_time
            })
        return leaderboard
    finally:
        db.close()

# --- 成就系统接口 ---
@app.get("/api/user/achievements")
async def get_user_achievements(phone: str):
    db = SessionLocal()
    try:
        get_or_create_user(db, phone)
        db.commit()
        catches = db.query(CatchRecordModel).filter(CatchRecordModel.user_phone == phone).all()
        total_count = len(catches)
        max_weight = max([c.weight or 0 for c in catches], default=0)
        days = set()
        for c in catches:
            try:
                days.add(time.strftime('%Y-%m-%d', time.localtime((c.catch_time or 0) / 1000)))
            except Exception:
                pass
        fishing_days = len(days)
        bait_types = set()
        for c in catches:
            b = (c.bait or "").strip()
            if b:
                bait_types.add(b)
        bait_type_count = len(bait_types)
        spot_ids = {c.spot_id for c in catches if c.spot_id}
        structure_count = 0
        if spot_ids:
            spots = db.query(FishingSpotModel).filter(FishingSpotModel.id.in_(list(spot_ids))).all()
            structures = {(s.structure or "").strip() for s in spots if (s.structure or "").strip()}
            structure_count = len(structures)
        achievements = [
            {"id": "1", "name": "路亚萌新", "desc": "成功记录1条鱼获", "unlocked": total_count >= 1, "progress": f"{min(total_count, 1)}/1"},
            {"id": "2", "name": "结构大师", "desc": "在3个不同结构的钓点作钓", "unlocked": structure_count >= 3, "progress": f"{min(structure_count, 3)}/3"},
            {"id": "3", "name": "巨物捕手", "desc": "捕获一条超过5kg的巨物", "unlocked": max_weight >= 5, "progress": f"{int(max_weight >= 5)}/1"},
            {"id": "4", "name": "打卡达人", "desc": "累计打卡7天", "unlocked": fishing_days >= 7, "progress": f"{min(fishing_days, 7)}/7"},
            {"id": "5", "name": "全能选手", "desc": "使用5种不同类型的路亚饵获鱼", "unlocked": bait_type_count >= 5, "progress": f"{min(bait_type_count, 5)}/5"},
        ]
        return achievements
    finally:
        db.close()

# --- 技巧视频接口 ---
@app.get("/api/videos")
async def get_technique_videos():
    return [
        {"id": "1", "title": "米诺假饵操控教学", "platform": "Bilibili", "videoUrl": "https://player.bilibili.com/player.html?bvid=BV1gj411x7u7&page=1&high_quality=1", "thumbnail": "https://i0.hdslb.com/bfs/archive/minnow_thumb.jpg", "author": "路亚大师"},
        {"id": "2", "title": "水面系铅笔炸水瞬间", "platform": "TikTok", "videoUrl": "https://www.douyin.com/player/7258932312312312", "thumbnail": "https://p3.douyinpic.com/thumb/pencil_thumb.jpg", "author": "探鱼者"}
    ]

# --- 装备统计接口 ---
@app.get("/api/gear/stats")
async def get_gear_stats(phone: str, rod_name: str):
    db = SessionLocal()
    try:
        catches = db.query(CatchRecordModel).filter(CatchRecordModel.user_phone == phone, CatchRecordModel.rod == rod_name).order_by(CatchRecordModel.catch_time.asc()).all()
        if not catches:
            return {"species_dist": [], "weight_trend": []}
        species_count = {}
        for c in catches:
            species_count[c.species] = species_count.get(c.species, 0) + 1
        species_dist = [{"species": k, "count": v} for k, v in species_count.items()]
        weight_trend = [{"time": time.strftime('%m-%d', time.localtime(c.catch_time / 1000)), "weight": c.weight or 0} for c in catches]
        return {"rod_name": rod_name, "total_count": len(catches), "species_dist": species_dist, "weight_trend": weight_trend}
    finally:
        db.close()

# --- 缓存接口 ---
@app.get("/api/weather/cache")
async def get_weather_cache(location_key: str):
    db = SessionLocal()
    try:
        cache = db.query(WeatherCacheModel).filter(WeatherCacheModel.location_key == location_key).first()
        if cache: return {"success": True, "data": json.loads(cache.data_json), "timestamp": cache.timestamp}
        return {"success": False}
    finally:
        db.close()

@app.post("/api/weather/cache")
async def save_weather_cache(data: dict = Body(...)):
    db = SessionLocal()
    try:
        loc_key = data['location_key']
        cache = db.query(WeatherCacheModel).filter(WeatherCacheModel.location_key == loc_key).first()
        if cache:
            cache.data_json, cache.timestamp = json.dumps(data['weather_data']), data['timestamp']
        else:
            db.add(WeatherCacheModel(location_key=loc_key, data_json=json.dumps(data['weather_data']), timestamp=data['timestamp']))
        db.commit()
        return {"success": True}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

@app.get("/api/water-level/cache")
async def get_water_level_cache(station_id: str):
    db = SessionLocal()
    try:
        cache = db.query(WaterLevelCacheModel).filter(WaterLevelCacheModel.station_id == station_id).first()
        if cache: return {"success": True, "data": json.loads(cache.data_json), "timestamp": cache.timestamp}
        return {"success": False}
    finally:
        db.close()

@app.post("/api/water-level/cache")
async def save_water_level_cache(data: dict = Body(...)):
    db = SessionLocal()
    try:
        sid = data['station_id']
        cache = db.query(WaterLevelCacheModel).filter(WaterLevelCacheModel.station_id == sid).first()
        if cache:
            cache.data_json, cache.timestamp = json.dumps(data['water_level_data']), data['timestamp']
        else:
            db.add(WaterLevelCacheModel(station_id=sid, data_json=json.dumps(data['water_level_data']), timestamp=data['timestamp']))
        db.commit()
        return {"success": True}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

# === 内容知识库接口（v3.0）===
def _fish_to_dict(f: "FishEncyclopediaModel") -> dict:
    return {"id": f.id, "name": f.name, "alias": f.alias, "scientificName": f.scientific_name, "family": f.family, "category": f.category, "distribution": f.distribution, "habitat": f.habitat, "feedingHabit": f.feeding_habit, "bodySize": f.body_size, "bestSeason": f.best_season, "bestHours": f.best_hours, "optimalTemp": f.optimal_temp, "recommendedLures": f.recommended_lures, "techniqueTips": f.technique_tips, "imageUrl": f.image_url, "description": f.description, "source": f.source, "updateTime": f.update_time}

def _lure_to_dict(l: "LureLibraryModel") -> dict:
    return {"id": l.id, "name": l.name, "category": l.category, "subType": l.sub_type, "swimLayer": l.swim_layer, "weightRange": l.weight_range, "lengthRange": l.length_range, "divingDepth": l.diving_depth, "targetSpecies": l.target_species, "suitableWaterTemp": l.suitable_water_temp, "suitableWaterType": l.suitable_water_type, "technique": l.technique, "colorTip": l.color_tip, "pros": l.pros, "cons": l.cons, "icon": l.icon, "imageUrl": l.image_url, "description": l.description, "source": l.source, "updateTime": l.update_time}

def _guide_to_dict(g: "FishingGuideModel") -> dict:
    return {"id": g.id, "title": g.title, "category": g.category, "targetSpecies": g.target_species, "season": g.season, "waterType": g.water_type, "summary": g.summary, "content": g.content, "coverUrl": g.cover_url, "tags": g.tags, "source": g.source, "sourceUrl": g.source_url, "viewCount": g.view_count or 0, "createTime": g.create_time, "updateTime": g.update_time}

@app.get("/api/encyclopedia/fish")
async def list_fish_encyclopedia(category: Optional[str] = None, q: Optional[str] = None):
    db = SessionLocal()
    try:
        query = db.query(FishEncyclopediaModel)
        if category: query = query.filter(FishEncyclopediaModel.category == category)
        if q:
            like = f"%{q}%"
            query = query.filter((FishEncyclopediaModel.name.like(like)) | (FishEncyclopediaModel.alias.like(like)) | (FishEncyclopediaModel.description.like(like)))
        return [_fish_to_dict(f) for f in query.all()]
    finally:
        db.close()

@app.get("/api/encyclopedia/fish/{name}")
async def get_fish_detail(name: str):
    db = SessionLocal()
    try:
        f = db.query(FishEncyclopediaModel).filter(FishEncyclopediaModel.name == name).first()
        if not f: return {"success": False, "message": "鱼种未收录"}
        return {"success": True, "data": _fish_to_dict(f)}
    finally:
        db.close()

@app.get("/api/encyclopedia/lures")
async def list_lures(category: Optional[str] = None, target: Optional[str] = None, swim_layer: Optional[str] = None):
    db = SessionLocal()
    try:
        query = db.query(LureLibraryModel)
        if category: query = query.filter(LureLibraryModel.category == category)
        if swim_layer: query = query.filter(LureLibraryModel.swim_layer == swim_layer)
        if target: query = query.filter(LureLibraryModel.target_species.like(f"%{target}%"))
        return [_lure_to_dict(l) for l in query.all()]
    finally:
        db.close()

@app.get("/api/encyclopedia/lures/match")
async def match_lures(species: str, water_temp: Optional[float] = None, water_type: Optional[str] = None, hour: Optional[int] = None):
    db = SessionLocal()
    try:
        all_lures = db.query(LureLibraryModel).filter(LureLibraryModel.target_species.like(f"%{species}%")).all()
        if not all_lures: all_lures = db.query(LureLibraryModel).all()
        scored = []
        for l in all_lures:
            score = 0
            if l.target_species and species in l.target_species: score += 50
            if water_temp is not None and l.suitable_water_temp:
                try:
                    rng = l.suitable_water_temp.replace("℃", "").replace(" ", "")
                    if "-" in rng:
                        lo, hi = rng.split("-", 1)
                        if float(lo) <= water_temp <= float(hi): score += 25
                except Exception: pass
            if water_type and l.suitable_water_type and water_type in l.suitable_water_type: score += 15
            if hour is not None and l.swim_layer:
                if (4 <= hour < 9 or 17 <= hour < 21) and l.swim_layer in ("上层", "水面"): score += 10
                elif 11 <= hour < 16 and l.swim_layer in ("底层", "中层"): score += 10
            scored.append((score, l))
        scored.sort(key=lambda x: x[0], reverse=True)
        top = [_lure_to_dict(l) | {"matchScore": s} for s, l in scored[:5] if s > 0]
        return {"success": True, "species": species, "recommendations": top}
    finally:
        db.close()

@app.get("/api/encyclopedia/guides")
async def list_guides(category: Optional[str] = None, season: Optional[str] = None, target: Optional[str] = None, page: int = 0, size: int = 20):
    db = SessionLocal()
    try:
        query = db.query(FishingGuideModel)
        if category: query = query.filter(FishingGuideModel.category == category)
        if season: query = query.filter(FishingGuideModel.season.like(f"%{season}%"))
        if target: query = query.filter(FishingGuideModel.target_species.like(f"%{target}%"))
        query = query.order_by(FishingGuideModel.update_time.desc())
        return [_guide_to_dict(g) for g in query.offset(page * size).limit(size).all()]
    finally:
        db.close()

@app.get("/api/encyclopedia/guides/{guide_id}")
async def get_guide_detail(guide_id: int):
    db = SessionLocal()
    try:
        g = db.query(FishingGuideModel).filter(FishingGuideModel.id == guide_id).first()
        if not g: return {"success": False, "message": "教程不存在"}
        g.view_count = (g.view_count or 0) + 1
        db.commit()
        return {"success": True, "data": _guide_to_dict(g)}
    finally:
        db.close()

@app.get("/api/astronomy")
async def get_astronomy(lat: float, lon: float, date: Optional[str] = None):
    """返回指定地点与日期的日出日落/月相/农历/禁渔期。"""
    from services import astronomy as astro_service
    today = date or time.strftime("%Y-%m-%d")
    region_key = f"{round(lat, 2)},{round(lon, 2)}"
    db = SessionLocal()
    try:
        cache = db.query(AstronomyCacheModel).filter(AstronomyCacheModel.region_key == region_key, AstronomyCacheModel.date == today).first()
        if cache and (time.time() * 1000 - (cache.update_time or 0)) < 24 * 3600 * 1000:
            return {"success": True, "data": _astronomy_to_dict(cache)}
        result = astro_service.compute(lat, lon, today)
        if cache:
            for k in ("sunrise", "sunset"): setattr(cache, k, result[k])
            for k in ("moonrise", "moonset", "lunar_date", "closed_season_note"): setattr(cache, k, result.get(k))
            cache.moon_phase = result["moon_phase"]; cache.moon_illumination = result["moon_illumination"]
            cache.is_closed_season = result.get("is_closed_season", 0); cache.update_time = int(time.time() * 1000)
        else:
            db.add(AstronomyCacheModel(region_key=region_key, date=today, sunrise=result["sunrise"], sunset=result["sunset"], moonrise=result.get("moonrise"), moonset=result.get("moonset"), moon_phase=result["moon_phase"], moon_illumination=result["moon_illumination"], lunar_date=result.get("lunar_date"), is_closed_season=result.get("is_closed_season", 0), closed_season_note=result.get("closed_season_note"), update_time=int(time.time() * 1000)))
        db.commit()
        return {"success": True, "data": result}
    except Exception as e:
        db.rollback()
        return {"success": False, "message": str(e)}
    finally:
        db.close()

def _astronomy_to_dict(a: "AstronomyCacheModel") -> dict:
    return {"date": a.date, "sunrise": a.sunrise, "sunset": a.sunset, "moonrise": a.moonrise, "moonset": a.moonset, "moon_phase": a.moon_phase, "moon_illumination": a.moon_illumination, "lunar_date": a.lunar_date, "is_closed_season": a.is_closed_season, "closed_season_note": a.closed_season_note}


# === 新增接口（v3.1）：气压趋势 / 路亚推荐 / 日历评分 / 鱼种活跃度 ===

@app.get("/api/weather/pressure-trend")
def get_pressure_trend(lat: float, lon: float, db: Session = Depends(get_db)):
    """返回过去24h + 未来24h 的气压序列数据"""
    location_key = f"{round(lat, 2)},{round(lon, 2)}"
    cache = db.query(WeatherCacheModel).filter(WeatherCacheModel.location_key == location_key).first()
    cache_data = None
    if cache and cache.data_json:
        try:
            cache_data = json.loads(cache.data_json)
        except Exception:
            pass
    return pressure_trend_service.get_trend(lat, lon, cache_data)


@app.get("/api/recommendations/lure")
def get_lure_recommendations(lat: float, lon: float, species: str = None, db: Session = Depends(get_db)):
    """路亚饵料推荐接口"""
    location_key = f"{round(lat, 2)},{round(lon, 2)}"
    cache = db.query(WeatherCacheModel).filter(WeatherCacheModel.location_key == location_key).first()
    temp, wind_speed, pressure = 20.0, 2.0, 1013.0
    hour = int(time.strftime("%H"))
    pressure_history, weather_text = None, None
    if cache and cache.data_json:
        try:
            data = json.loads(cache.data_json)
            now_data = data.get("now", {})
            temp = float(now_data.get("temp", temp))
            wind_speed = float(now_data.get("windSpeed", wind_speed)) / 3.6
            pressure = float(now_data.get("pressure", pressure))
            weather_text = now_data.get("text", None)
            hourly = data.get("hourly", [])
            if hourly:
                pressure_history = []
                for h in hourly[:12]:
                    try: pressure_history.append(float(h.get("pressure", 0)))
                    except (ValueError, TypeError): pass
        except Exception:
            pass
    return lure_recommender.recommend(temp=temp, wind_speed=wind_speed, pressure=pressure, hour=hour, species=species, pressure_history=pressure_history if pressure_history else None, weather_text=weather_text)


@app.get("/api/calendar/scores")
def get_calendar_scores(lat: float, lon: float, year: int, month: int, db: Session = Depends(get_db)):
    """日历月度评分数据"""
    return calendar_score_service.compute_month(lat, lon, year, month, index_calculator)


@app.get("/api/species/activity")
def get_species_activity(lat: float, lon: float, species: str, db: Session = Depends(get_db)):
    """鱼种活跃时段接口"""
    location_key = f"{round(lat, 2)},{round(lon, 2)}"
    cache = db.query(WeatherCacheModel).filter(WeatherCacheModel.location_key == location_key).first()
    temp, pressure, wind_speed = 20.0, 1013.0, 2.0
    hour = int(time.strftime("%H"))
    weather_text, moon_phase = None, None
    if cache and cache.data_json:
        try:
            data = json.loads(cache.data_json)
            now_data = data.get("now", {})
            temp = float(now_data.get("temp", temp))
            pressure = float(now_data.get("pressure", pressure))
            wind_speed = float(now_data.get("windSpeed", wind_speed)) / 3.6
            weather_text = now_data.get("text", None)
        except Exception:
            pass
    region_key = f"{round(lat, 2)},{round(lon, 2)}"
    today = time.strftime("%Y-%m-%d")
    astro_cache = db.query(AstronomyCacheModel).filter(AstronomyCacheModel.region_key == region_key, AstronomyCacheModel.date == today).first()
    if astro_cache:
        moon_phase = astro_cache.moon_phase
    return species_activity_service.predict(species=species, temp=temp, pressure=pressure, wind_speed=wind_speed, hour=hour, weather_text=weather_text, moon_phase=moon_phase)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
