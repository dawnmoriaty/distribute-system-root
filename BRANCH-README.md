# ⚖️ Distributed System — Nhánh `load-balancer`

## 📋 Vai trò: Load Balancer + Admin Dashboard UI

Nhánh này chạy **Load Balancer** — điểm vào duy nhất của hệ thống.
Client kết nối TCP đến LB, LB route request đến Worker Servers theo Round Robin.

### Nhánh này chạy:
- ✅ **Load Balancer** — lắng nghe TCP port 8080, route đến Workers (Round Robin)
- ✅ **Admin Dashboard UI** — quản lý kết nối TCP, xem Worker Health, ACL
- ✅ **Health Check** — tự động kiểm tra Worker Servers mỗi 10 giây
- ✅ **common-lib** — shared DTOs, protocol, utilities

### Nhánh này KHÔNG build:
- ❌ `server-node/` — chạy ở nhánh `main`
- ❌ `javafx-client/` — chạy ở nhánh `client-1` hoặc `client-2`

---

## 🚀 CÁCH CHẠY

### Bước 1: Đảm bảo Worker Servers đang chạy (nhánh `main`)
```bash
# Trên máy chạy nhánh main:
RUN.bat 9001   # Worker Server 1
RUN.bat 9002   # Worker Server 2
```

### Bước 2: Chạy Load Balancer

**Cách 1 — RUN.bat (khuyến nghị):**
```bash
RUN.bat
```

**Cách 2 — Gradle trực tiếp:**
```bash
# LB + Admin Dashboard (UI) — mặc định
.\gradlew.bat :load-balancer:run

# LB chỉ console (không UI)
.\gradlew.bat :load-balancer:runConsole
```

---

## 🛡️ Admin Dashboard UI — 5 Tabs

| Tab | Chức năng |
|-----|-----------|
| 🔌 **Active Connections** | Xem danh sách Client đang kết nối qua LB, Kick/Block |
| ⏳ **Pending Approvals** | Duyệt/từ chối kết nối TCP mới (MANUAL mode) |
| 🛡️ **Access Control** | Quản lý Whitelist/Blacklist IP, chuyển AUTO/MANUAL mode |
| 💓 **Worker Health** | Xem trạng thái HEALTHY/UNHEALTHY của từng Worker Server |
| 📋 **Logs** | Log realtime tất cả events kết nối |

### TCP Access Control (ACL):
- **AUTO mode**: Tự động cho phép/chặn dựa trên whitelist/blacklist
- **MANUAL mode**: Mỗi kết nối TCP mới phải được Admin duyệt qua UI

> ⚡ Load Balancer là **entry point đầu tiên** — chặn ở đây = Client không bao giờ
> đến được Worker Server. Đây là TCP-level firewall của hệ thống.

### Worker Health Check:
- LB tự động PING đến mỗi Worker mỗi 10 giây
- Worker không phản hồi → đánh dấu UNHEALTHY → không route traffic đến
- Worker phục hồi → tự động HEALTHY lại → nhận traffic trở lại

---

## 📊 Luồng kết nối

```
  Client 1 ──┐                              ┌──▶ Worker 1 (port 9001) ✅
              │     ┌──────────────────┐     │
              ├────▶│  LOAD BALANCER   │─────┤   Round Robin routing
              │     │  Port: 8080      │     │   + Health Check
  Client 2 ──┘     │  ◀── NHÁNH NÀY   │     └──▶ Worker 2 (port 9002) ✅
                    │                  │
                    │  TCP ACL:        │
                    │  • Whitelist     │
                    │  • Blacklist     │
                    │  • Manual Approve│
                    └──────────────────┘
```

---

## 📂 Tất cả các nhánh

| Nhánh | Vai trò | Module build | Lệnh chạy |
|-------|---------|-------------|------------|
| `main` | Worker Server + Core | common-lib, server-node | `RUN.bat 9001` |
| `load-balancer` | **Load Balancer + Admin UI** ⬅️ BẠN Ở ĐÂY | common-lib, load-balancer | `RUN.bat` |
| `client-1` | JavaFX Client 1 | common-lib, javafx-client | `RUN.bat` |
| `client-2` | JavaFX Client 2 | common-lib, javafx-client | `RUN.bat` |

---

## 🌐 Chạy trên nhiều máy (Multi-machine)

Sửa `config.properties`:
```properties
LOAD_BALANCER_HOST=<IP máy này>
LOAD_BALANCER_PORT=8080
WORKER1_HOST=<IP máy Worker 1>
WORKER1_PORT=9001
WORKER2_HOST=<IP máy Worker 2>
WORKER2_PORT=9002
```

