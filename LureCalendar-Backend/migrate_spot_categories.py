"""
钓点分类字段扩展 + 收藏表创建
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

# 1. 给 fishing_spots 表添加分类字段（兼容低版本MySQL，无IF NOT EXISTS）
alter_sqls = [
    ("spot_type", "ALTER TABLE fishing_spots ADD COLUMN spot_type VARCHAR(20) DEFAULT '野河' COMMENT '钓场类型: 收费钓场/免费钓场/黑坑/野塘/水库/野河'"),
    ("fee_type", "ALTER TABLE fishing_spots ADD COLUMN fee_type VARCHAR(10) DEFAULT '免费' COMMENT '收费类型: 收费/免费'"),
    ("city", "ALTER TABLE fishing_spots ADD COLUMN city VARCHAR(20) DEFAULT '' COMMENT '所属城市'"),
    ("district", "ALTER TABLE fishing_spots ADD COLUMN district VARCHAR(20) DEFAULT '' COMMENT '所属区县'"),
]

for col_name, sql in alter_sqls:
    try:
        cursor.execute(sql)
        print(f"✓ 添加字段 {col_name}")
    except Exception as e:
        if "Duplicate column" in str(e) or "1060" in str(e):
            print(f"⊘ 字段 {col_name} 已存在，跳过")
        else:
            print(f"✗ 添加字段 {col_name} 错误: {e}")

# 2. 创建用户收藏表
create_favorites = """
CREATE TABLE IF NOT EXISTS user_spot_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL DEFAULT 'default_user' COMMENT '用户ID',
    spot_id BIGINT NOT NULL COMMENT '钓点ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_spot (user_id, spot_id),
    INDEX idx_user_id (user_id),
    INDEX idx_spot_id (spot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏钓点';
"""
try:
    cursor.execute(create_favorites)
    print("✓ 创建 user_spot_favorites 表")
except Exception as e:
    print(f"✗ 创建收藏表错误: {e}")

conn.commit()
conn.close()
print("\n✅ 迁移完成!")
