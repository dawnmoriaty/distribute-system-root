# 🖥️ Distributed System — Nhánh `main`

## 📋 Vai trò: Worker Server + Admin Dashboard + Database (CORE)

Nhánh `main` là **nhánh gốc / nhánh core** của hệ thống phân tán.

### Nhánh này chạy:
- ✅ **Worker Server** — lắng nghe TCP, xử lý CRUD request, kết nối MySQL
- ✅ **Admin Dashboard UI** — quản lý kết nối TCP (ACL, Whitelist/Blacklist, Manual Approve)
- ✅ **MySQL Database** — Docker container (docker-compose)
- ✅ **common-lib** — shared DTOs, protocol, utilities (core cho mọi nhánh)

### Nhánh này KHÔNG build (chỉ giữ source code):
- ❌ `load-balancer/` — chạy ở nhánh `load-balancer`
- ❌ `javafx-client/` — chạy ở nhánh `client-1` hoặc `client-2`

> 💡 Source code của load-balancer và javafx-client vẫn nằm trong repo,
> nhưng `settings.gradle` đã exclude chúng khỏi build.
> Khi switch sang nhánh khác, chỉ cần đổi `settings.gradle` cho đúng vai trò.

---

## 🚀 CÁCH CHẠY

### Bước 1: Setup Database (lần đầu)
```bash
docker-compose up -d
```
- MySQL: `localhost:3306` (root/password)
- phpMyAdmin: http://localhost:8081

### Bước 2: Chạy Worker Server

**Cách 1 — RUN.bat (khuyến nghị):**
```bash
# Worker Server 1 (port 9001) + Admin Dashboard UI
RUN.bat 9001

# Worker Server 2 (port 9002) + Admin Dashboard UI  
RUN.bat 9002
```

**Cách 2 — Gradle trực tiếp:**
```bash
# Worker + Admin Dashboard (UI)
.\gradlew.bat :server-node:run --args="9001"

# Worker chỉ console (không UI)
.\gradlew.bat :server-node:runWorker1
.\gradlew.bat :server-node:runWorker2
```

---

## 🛡️ Admin Dashboard UI — Quản lý kết nối TCP

Khi chạy Server, Admin Dashboard UI tự động mở ra với 4 tabs:

| Tab | Chức năng |
|-----|-----------|
| 🔌 Active Connections | Xem danh sách + Kick kết nối TCP đang hoạt động |
| ⏳ Pending Approvals | Duyệt/từ chối kết nối mới (MANUAL mode) |
| 🛡️ Access Control | Quản lý Whitelist/Blacklist IP, chuyển AUTO/MANUAL mode |
| 📋 Logs | Log realtime tất cả events |

### 2 chế độ TCP Access Control:
- **AUTO**: Tự động cho phép/chặn dựa trên whitelist/blacklist rules
- **MANUAL**: Mỗi kết nối TCP mới phải được Admin duyệt qua UI mới được kết nối

> ⚡ Đây là TCP-level access control — kết nối bị chặn/từ chối sẽ bị close
> ngay tại socket level, trước khi xử lý bất kỳ request nào.

---

## 📊 Kiến trúc hệ thống tổng thể

```
  ┌─────────────┐     ┌──────────────────┐     ┌─────────────────────────────┐
  │  Client 1   │────▶│                  │────▶│  Worker Server 1 (port 9001)│
  │ (nhánh      │     │  Load Balancer   │     │  + Admin Dashboard UI       │
  │  client-1)  │     │  (nhánh          │     │  + MySQL Database           │
  └─────────────┘     │   load-balancer) │     │  ◀── NHÁNH MAIN            │
                      │  Round Robin     │     └─────────────────────────────┘
  ┌─────────────┐     │  Health Check    │     ┌─────────────────────────────┐
  │  Client 2   │────▶│  Port: 8080      │────▶│  Worker Server 2 (port 9002)│
  │ (nhánh      │     │                  │     │  + Admin Dashboard UI       │
  │  client-2)  │     └──────────────────┘     │  ◀── NHÁNH MAIN (máy khác) │
  └─────────────┘                              └─────────────────────────────┘
```

---

## 📂 Tất cả các nhánh

| Nhánh | Vai trò | Module build | Lệnh chạy |
|-------|---------|-------------|------------|
| `main` | **Worker Server + Core** ⬅️ BẠN Ở ĐÂY | common-lib, server-node | `RUN.bat 9001` |
| `load-balancer` | Load Balancer + Monitor UI | common-lib, load-balancer | `RUN.bat` |
| `client-1` | JavaFX Client 1 | common-lib, javafx-client | `RUN.bat` |
| `client-2` | JavaFX Client 2 | common-lib, javafx-client | `RUN.bat` |

---

## 🌐 Chạy trên nhiều máy (Multi-machine)

Sửa `config.properties`:
```properties
WORKER1_HOST=<IP máy này>
WORKER1_PORT=9001
WORKER2_HOST=<IP máy Worker 2>
WORKER2_PORT=9002
DB_URL=jdbc:mysql://<IP máy DB>:3306/distributed_db
DB_USER=root
DB_PASSWORD=password
```


