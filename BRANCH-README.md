# 🖥️ Distributed System — Nhánh `main` (Server Worker)

## 📋 Vai trò: Worker Server + Admin Dashboard

Nhánh này chạy **Worker Server** — xử lý request từ client, kết nối database, và cung cấp **Admin Dashboard UI** để quản lý kết nối TCP.

## 🚀 CÁCH CHẠY (1 lệnh duy nhất)

### Bước 1: Setup Database (lần đầu)
```bash
docker-compose up -d
```

### Bước 2: Chạy Server

**Cách 1 — Dùng RUN.bat:**
```bash
# Worker Server 1 (port 9001) + Admin Dashboard UI
RUN.bat 9001

# Worker Server 2 (port 9002) + Admin Dashboard UI
RUN.bat 9002
```

**Cách 2 — Dùng Gradle trực tiếp:**
```bash
# Worker 1 + Admin Dashboard
.\gradlew.bat :server-node:run --args="9001"

# Worker 2 + Admin Dashboard
.\gradlew.bat :server-node:run --args="9002"
```

## 🛡️ Admin Dashboard UI

Khi chạy, giao diện Admin Dashboard sẽ mở ra với 4 tabs:

| Tab | Chức năng |
|-----|-----------|
| 🔌 Active Connections | Xem + Kick kết nối TCP đang hoạt động |
| ⏳ Pending Approvals | Duyệt/từ chối kết nối (MANUAL mode) |
| 🛡️ Access Control | Quản lý Whitelist/Blacklist IP, chuyển AUTO/MANUAL |
| 📋 Logs | Log realtime tất cả events |

### 2 chế độ hoạt động:
- **AUTO**: Tự động cho phép/chặn dựa trên whitelist/blacklist
- **MANUAL**: Admin duyệt từng kết nối TCP qua UI

## 🌐 Khi chạy trên máy khác (Multi-machine)

Sửa file `config.properties`:
```properties
LOAD_BALANCER_HOST=<IP máy Load Balancer>
LOAD_BALANCER_PORT=8080
WORKER1_HOST=<IP máy này>
WORKER1_PORT=9001
WORKER2_HOST=<IP máy Worker 2>
WORKER2_PORT=9002
DB_URL=jdbc:mysql://<IP máy DB>:3306/distributed_db
DB_USER=root
DB_PASSWORD=password
```

## 📊 Sơ đồ hệ thống

```
                    ┌─────────────────────────────┐
                    │     NHÁNH NÀY (main)        │
                    │  Worker Server + Admin UI    │
                    │  Port: 9001 hoặc 9002       │
                    └──────────┬──────────────────┘
                               │
  Client 1 ──► Load Balancer ──┤
  Client 2 ──► (nhánh lb)   ──┤──► MySQL Database
                               │
                    ┌──────────┴──────────────────┐
                    │     Worker Server khác       │
                    │  (máy khác, cùng nhánh main) │
                    └─────────────────────────────┘
```

## 📂 Các nhánh trong hệ thống

| Nhánh | Vai trò | Lệnh chạy |
|-------|---------|------------|
| `main` | **Worker Server** + Admin Dashboard ⬅️ BẠN Ở ĐÂY | `RUN.bat 9001` |
| `load-balancer` | Load Balancer + Monitor UI | `RUN.bat` |
| `client-1` | Client 1 | `RUN.bat` |
| `client-2` | Client 2 | `RUN.bat` |

