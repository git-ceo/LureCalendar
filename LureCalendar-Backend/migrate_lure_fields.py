"""
Migration: Add lure-specific columns to catch_records table.
Run once: python migrate_lure_fields.py
"""
import pymysql
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Load .env
env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
if os.path.exists(env_path):
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = value

host = os.getenv("MYSQL_HOST", "192.168.0.168")
port = int(os.getenv("MYSQL_PORT", "3306"))
user = os.getenv("MYSQL_USER", "luredb")
password = os.getenv("MYSQL_PASSWORD", "awerawer")
database = os.getenv("MYSQL_DB", "luredb")

conn = pymysql.connect(host=host, port=port, user=user, password=password, database=database, charset="utf8mb4")
cursor = conn.cursor()

columns_to_add = [
    ("lure_type", "VARCHAR(100)"),
    ("rig_type", "VARCHAR(100)"),
    ("structure_zone", "VARCHAR(100)"),
    ("water_clarity", "VARCHAR(50)"),
    ("wind_shore_relation", "VARCHAR(50)"),
]

# Check existing columns
cursor.execute("SHOW COLUMNS FROM catch_records")
existing = {row[0] for row in cursor.fetchall()}

for col_name, col_type in columns_to_add:
    if col_name not in existing:
        sql = f"ALTER TABLE catch_records ADD COLUMN {col_name} {col_type} NULL"
        print(f"Executing: {sql}")
        cursor.execute(sql)
    else:
        print(f"Column {col_name} already exists, skipping.")

conn.commit()
conn.close()
print("Migration complete!")
