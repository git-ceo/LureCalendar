# LureCalendar 后端服务 (FastAPI)

这是一个专为 LureCalendar App 设计的极简同步后端，基于 Python FastAPI 和 SQLite 驱动。

## 核心功能
*   **全量同步**：接收来自 App 的 JSON 格式钓点和鱼获数据。
*   **UPSERT 逻辑**：自动判断数据是否存在，存在即更新，不存在即插入。
*   **数据持久化**：使用 SQLite 数据库，方便迁移和备份。

## 本地环境配置 (Windows)
1.  确保已安装 Python 3.8+。
2.  进入项目目录：`cd LureCalendar-Backend`
3.  安装依赖：
    ```bash
    pip install fastapi uvicorn sqlalchemy
    ```
4.  启动服务：
    ```bash
    python main.py
    ```
    服务默认运行在：`http://localhost:8080`

## Linux 服务器部署步骤

### 1. 上传代码
将 `main.py` 上传到服务器的某个目录下（如 `/opt/lurecalendar-api`）。

### 2. 环境安装 (以 Ubuntu 为例)
```bash
# 更新系统包
sudo apt update
# 安装 python3 和 pip
sudo apt install python3-pip
# 安装后端依赖
pip3 install fastapi uvicorn sqlalchemy
```

### 3. 使用 Systemd 实现后台运行 (推荐)
为了让服务在后台常驻并在断电后自动启动，建议配置 Systemd。

创建一个服务文件：
```bash
sudo nano /etc/systemd/system/lurecalendar.service
```

写入以下内容（注意修改 User 和 WorkingDirectory）：
```ini
[Unit]
Description=LureCalendar Backend Service
After=network.target

[Service]
User=root
WorkingDirectory=/opt/lurecalendar-api
ExecStart=/usr/bin/python3 main.py
Restart=always

[Install]
WantedBy=multi-user.target
```

启动并使能服务：
```bash
sudo systemctl start lurecalendar
sudo systemctl enable lurecalendar
```

### 4. 检查状态
```bash
sudo systemctl status lurecalendar
```

## 注意事项
*   **端口安全**：请确保你的服务器防火墙（如腾讯云/阿里云的安全组）已开放 `8080` 端口。
*   **数据库文件**：同步开始后，目录下会生成 `lurecalendar.db`。你可以随时把这个文件下载到本地用 SQLite 查看工具打开。
