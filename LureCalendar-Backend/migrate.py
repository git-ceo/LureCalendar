import sqlite3
import os

db_path = "./lurecalendar.db"

def migrate():
    if not os.path.exists(db_path):
        print(f"数据库文件 {db_path} 不存在，无需迁移。启动后端时会自动创建。")
        return

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # 检查 users 表的列
    cursor.execute("PRAGMA table_info(users)")
    columns = [column[1] for column in cursor.fetchall()]

    # 需要添加的列
    new_columns = [
        ("signature", "TEXT DEFAULT '这个钓鱼佬很懒，什么都没留下'"),
        ("avatar_url", "TEXT"),
        ("background_url", "TEXT"),
        ("is_admin", "INTEGER DEFAULT 0"),
        ("last_latitude", "REAL"),
        ("last_longitude", "REAL"),
        ("last_location_time", "INTEGER"),
        ("share_location", "INTEGER DEFAULT 0")
    ]

    for col_name, col_def in new_columns:
        if col_name not in columns:
            print(f"正在向 users 表添加列: {col_name}")
            try:
                cursor.execute(f"ALTER TABLE users ADD COLUMN {col_name} {col_def}")
            except Exception as e:
                print(f"添加 {col_name} 失败: {e}")
        else:
            print(f"列 {col_name} 已存在")

    # 检查 catch_records 表的列
    cursor.execute("PRAGMA table_info(catch_records)")
    catch_columns = [column[1] for column in cursor.fetchall()]
    if "user_phone" not in catch_columns:
        print("正在向 catch_records 表添加列: user_phone")
        cursor.execute("ALTER TABLE catch_records ADD COLUMN user_phone TEXT")

    # 检查 fishing_spots 表的路亚专属列
    cursor.execute("PRAGMA table_info(fishing_spots)")
    spot_columns = [column[1] for column in cursor.fetchall()]

    lure_columns = [
        ("target_species", "TEXT"),
        ("lure_types", "TEXT"),
        ("best_season", "TEXT"),
    ]
    for col_name, col_def in lure_columns:
        if col_name not in spot_columns:
            print(f"正在向 fishing_spots 表添加列: {col_name}")
            try:
                cursor.execute(f"ALTER TABLE fishing_spots ADD COLUMN {col_name} {col_def}")
            except Exception as e:
                print(f"添加 {col_name} 失败: {e}")
        else:
            print(f"列 {col_name} 已存在")

    conn.commit()
    conn.close()
    print("数据库迁移完成！")

if __name__ == "__main__":
    migrate()
