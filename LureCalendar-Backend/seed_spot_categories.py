"""
为现有钓点填充分类信息（spot_type, fee_type, city, district）
"""
import os
from dotenv import load_dotenv
import pymysql

load_dotenv()

conn = pymysql.connect(
    host=os.getenv("MYSQL_HOST", "192.168.0.168"),
    port=int(os.getenv("MYSQL_PORT", 3306)),
    user=os.getenv("MYSQL_USER", "luredb"),
    password=os.getenv("MYSQL_PASSWORD", "awerawer"),
    database=os.getenv("MYSQL_DB", "luredb"),
    charset='utf8mb4'
)
cursor = conn.cursor()

# 先查询所有现有钓点
cursor.execute("SELECT id, name, location_detail FROM fishing_spots")
spots = cursor.fetchall()
print(f"共 {len(spots)} 个钓点需要分类")

# 根据名称和地址智能分类
for spot_id, name, address in spots:
    # 判断类型
    spot_type = '野河'  # 默认
    fee_type = '免费'
    
    name_lower = (name or '').lower()
    addr = address or ''
    
    if any(kw in name_lower for kw in ['水库', '湖']):
        spot_type = '水库'
        fee_type = '免费'
    elif any(kw in name_lower for kw in ['黑坑', '塘钓', '计时']):
        spot_type = '黑坑'
        fee_type = '收费'
    elif any(kw in name_lower for kw in ['钓场', '渔场', '垂钓中心', '休闲']):
        spot_type = '收费钓场'
        fee_type = '收费'
    elif any(kw in name_lower for kw in ['野塘', '堰塘', '塘']):
        spot_type = '野塘'
        fee_type = '免费'
    elif any(kw in name_lower for kw in ['河', '江', '溪']):
        spot_type = '野河'
        fee_type = '免费'
    
    # 判断城市和区
    city = ''
    district = ''
    combined = name_lower + addr
    
    # 城市判断
    if any(kw in combined for kw in ['绵阳', '涪城', '游仙', '高新', '安州', '经开']):
        city = '绵阳'
    elif any(kw in combined for kw in ['梓潼']):
        city = '梓潼'
    elif any(kw in combined for kw in ['江油']):
        city = '江油'
    elif any(kw in combined for kw in ['三台']):
        city = '三台'
    elif any(kw in combined for kw in ['盐亭']):
        city = '盐亭'
    elif any(kw in combined for kw in ['德阳', '旌阳', '广汉', '什邡', '绵竹']):
        city = '德阳'
    elif any(kw in combined for kw in ['成都', '锦江', '武侯', '青羊', '金牛', '成华', '龙泉', '双流', '温江', '郫都', '新都', '青白江']):
        city = '成都'
    
    # 绵阳区级判断
    if city == '绵阳':
        if any(kw in combined for kw in ['高新', '普明', '石桥铺', '磨家']):
            district = '高新区'
        elif any(kw in combined for kw in ['游仙', '小枧', '魏城', '仙鹤']):
            district = '游仙区'
        elif any(kw in combined for kw in ['涪城', '城厢', '青义', '石塘', '丰谷']):
            district = '涪城区'
        elif any(kw in combined for kw in ['安州', '花荄', '秀水']):
            district = '安州区'
        elif any(kw in combined for kw in ['经开', '塘汛', '松垭']):
            district = '经开区'
    
    # 更新数据库
    cursor.execute("""
        UPDATE fishing_spots 
        SET spot_type=%s, fee_type=%s, city=%s, district=%s 
        WHERE id=%s
    """, (spot_type, fee_type, city, district, spot_id))
    print(f"  [{spot_type}][{fee_type}][{city}-{district}] {name}")

conn.commit()
conn.close()
print(f"\n✅ 分类完成! 共处理 {len(spots)} 个钓点")
