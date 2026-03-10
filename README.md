# Distributed System - Java TCP Socket

Hệ thống phân tán sử dụng Java Sockets (TCP) thuần túy với Gradle multi-module.

## 📁 Cấu trúc Project

```
distributed-system-root/
├── build.gradle              # Root build file
├── settings.gradle           # Module configuration
├── config.properties.example # Template cấu hình mạng
├── acl.properties            # Auto-generated: Whitelist/Blacklist rules (dynamic)
├── common-lib/               # Shared DTOs, utilities
│   └── src/main/java/org/example/common/
│       ├── PacketUtils.java          # Length-Prefix Framing
│       ├── RequestPayload.java       # Request DTO
│       ├── ResponsePayload.java      # Response DTO
│       ├── UserDTO.java              # User entity DTO
│       ├── Commands.java             # Command constants
│       ├── NetworkConfig.java        # Network configuration loader
│       ├── SSLConfig.java            # SSL/TLS configuration
│       ├── ConnectionAccessManager.java  # 🛡️ Dynamic TCP ACL engine
│       └── PendingConnection.java    # 🛡️ Pending connection model
├── server-node/              # Worker Server
│   └── src/main/java/org/example/
│       ├── WorkerServer.java         # Main server (with ACL integration)
│       ├── ClientHandler.java        # Request handler
│       ├── DatabaseConnection.java   # HikariCP pool
│       └── admin/                    # 🖥️ Server Admin Dashboard
│           ├── ServerAdminLauncher.java
│           ├── ServerAdminApplication.java
│           └── ServerAdminController.java
├── load-balancer/            # Load Balancer (Round Robin)
│   └── src/main/java/org/example/
│       ├── LoadBalancer.java         # Main LB (with ACL integration)
│       └── admin/                    # 🖥️ LB Admin Dashboard
│           ├── LBAdminLauncher.java
│           ├── LBAdminApplication.java
│           └── LBAdminController.java
├── javafx-client/            # Desktop Client
│   └── src/main/java/org/example/
│       ├── ClientApplication.java
│       ├── ClientLauncher.java
│       ├── ClientController.java
│       └── SocketClient.java
└── database/
    └── setup.sql             # Database setup script
```

## 🔧 Technical Requirements

### Protocol: Length-Prefix Framing
```
[4 bytes Integer (length)] + [JSON Payload (UTF-8)]
```
Tránh vấn đề TCP stream fragmentation/packet coalescing.

### Load Balancer
- Algorithm: Round Robin
- Thread Pool: 20 threads
- Port: 8080

### Worker Server
- Connection Pool: HikariCP
- Thread Pool: 10 threads per worker
- Ports: 9001, 9002 (2 instances)

### JavaFX Client
- MVC Architecture
- Async operations với `javafx.concurrent.Task`
- Không block JavaFX Application Thread

## 🚀 Hướng dẫn chạy

### 1. Setup Database (Docker - Khuyến nghị)

```bash
# Khởi động MySQL + phpMyAdmin
docker-compose up -d

# Kiểm tra container đã chạy
docker ps

# Xem logs nếu cần
docker-compose logs -f mysql
```

**Truy cập:**
- MySQL: `localhost:3306` (user: root, pass: password)
- phpMyAdmin: http://localhost:8081

**Dừng Docker:**
```bash
docker-compose down

# Xóa cả data:
docker-compose down -v
```

### 1b. Setup Database (Cài MySQL trực tiếp)

```sql
mysql -u root -p < database/setup.sql
```


### 2. Build Project

**Windows:**
```bash
.\gradlew.bat build
```

**Linux/Mac:**
```bash
./gradlew build
```

### 3. Chạy theo thứ tự

**Terminal 1 - Worker Server 1 (Port 9001):**
```bash
# Windows
.\gradlew.bat :server-node:runWorker1

# Linux/Mac
./gradlew :server-node:runWorker1
```

**Terminal 2 - Worker Server 2 (Port 9002):**
```bash
# Windows
.\gradlew.bat :server-node:runWorker2

# Linux/Mac
./gradlew :server-node:runWorker2
```

**Terminal 3 - Load Balancer (Port 8080):**
```bash
# Windows
.\gradlew.bat :load-balancer:run

# Linux/Mac
./gradlew :load-balancer:run
```

**Terminal 4+ - JavaFX Client(s):**
```bash
# Windows
.\gradlew.bat :javafx-client:run

# Linux/Mac
./gradlew :javafx-client:run
```

## 🛡️ Admin Dashboard — Dynamic TCP Access Control

### Bản chất TCP: Quyền từ chối kết nối

TCP cho phép server **accept hoặc refuse** bất kỳ kết nối nào. Hệ thống này hiện thực hóa quyền đó thông qua **Admin Dashboard UI** — không phải cấu hình cứng trong code!

### Cách chạy Server VỚI Admin Dashboard

Thay vì chạy `runWorker1`/`runWorker2` (console only), chạy `runAdmin1`/`runAdmin2`:

```bash
# Worker Server 1 + Admin Dashboard
.\gradlew.bat :server-node:runAdmin1

# Worker Server 2 + Admin Dashboard
.\gradlew.bat :server-node:runAdmin2

# Load Balancer + Admin Dashboard
.\gradlew.bat :load-balancer:runAdmin
```

### Dashboard có 4 tab chính:

#### 🔌 Tab 1: Active Connections
- Xem tất cả kết nối TCP đang hoạt động (IP, port, thời gian)
- **Kick**: Ngắt kết nối bất kỳ client nào
- **Block IP & Kick**: Thêm IP vào blacklist + ngắt ngay

#### ⏳ Tab 2: Pending Approvals (MANUAL mode)
- Khi chế độ MANUAL bật, mỗi kết nối TCP mới sẽ **chờ admin duyệt**
- **✅ Approve**: Cho phép kết nối
- **❌ Reject**: Từ chối kết nối
- **✅ Approve & Whitelist**: Cho phép + thêm IP vào whitelist luôn
- Nếu không duyệt trong timeout (mặc định 30s) → tự động reject

#### 🛡️ Tab 3: Access Control
- **AUTO mode**: Dùng whitelist/blacklist tự động
  - Whitelist trống = cho phép tất cả
  - Whitelist có IP = chỉ cho phép IP đó
  - Blacklist = luôn chặn
- **MANUAL mode**: Admin duyệt từng kết nối qua Tab 2
- Thêm/xóa IP whitelist/blacklist từ UI
- Cấu hình timeout cho manual approval
- Tất cả thay đổi được **lưu tự động** vào `acl.properties`

#### 📋 Tab 4: Logs
- Log realtime tất cả events: accept, reject, kick, thay đổi ACL

### Luồng hoạt động TCP Access Control

```
Client gửi kết nối TCP
         │
         ▼
   serverSocket.accept()
         │
         ▼
┌─────────────────────────┐
│ ConnectionAccessManager │
│   evaluateConnection()  │
└────────┬────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
 Blacklist? ──YES──► REJECT (close socket)
    │NO
    ▼
 Mode = AUTO?
    │YES              │NO (MANUAL)
    ▼                 ▼
 Whitelist empty?   Đưa vào PendingQueue
    │YES → ALLOW     Hiển thị trên UI
    │NO              Admin Approve/Reject
    ▼                 │
 IP in whitelist?    Timeout → Auto Reject
    │YES → ALLOW
    │NO → REJECT
```

### File `acl.properties` (auto-generated)

```properties
# Connection Access Control List — Auto-generated, editable from Admin UI
whitelist=192.168.1.10,192.168.1.20
blacklist=10.0.0.5
approval_mode=AUTO
manual_timeout_ms=30000
```

## 📊 Luồng hoạt động

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│   Client    │────►│ Load Balancer│────►│ Worker Server │
│  (JavaFX)   │◄────│   (Port 8080)│◄────│  (9001/9002)  │
└─────────────┘     └──────────────┘     └───────┬───────┘
                                                  │
                                                  ▼
                                          ┌──────────────┐
                                          │    MySQL     │
                                          │ (distributed_db)
                                          └──────────────┘
```

## 🌐 MULTI-MACHINE SETUP (Chạy trên nhiều máy)

### Sơ đồ mạng thực tế

```
┌─────────────────┐
│  MÁY 1 (Client) │  IP: 192.168.1.10
│  JavaFX Client  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  MÁY 2 (LB)     │  IP: 192.168.1.100
│  Load Balancer  │  Port: 8080
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│ MÁY 3  │ │ MÁY 4  │
│Worker 1│ │Worker 2│
│ :9001  │ │ :9002  │
└────┬───┘ └───┬────┘
     │         │
     └────┬────┘
          ▼
    ┌──────────┐
    │  MySQL   │  (Có thể trên máy riêng hoặc Worker)
    └──────────┘
```

### Bước 1: Cấu hình mạng

**Trên MỖI máy**, tạo file `config.properties` trong thư mục project:

```bash
# Copy từ template
cp config.properties.example config.properties
```

**Chỉnh sửa `config.properties`:**

```properties
# ============== LOAD BALANCER ==============
# IP máy chạy Load Balancer (Máy 2)
LOAD_BALANCER_HOST=192.168.1.100
LOAD_BALANCER_PORT=8080

# ============== WORKER SERVERS ==============
# IP máy chạy Worker 1 (Máy 3)
WORKER1_HOST=192.168.1.101
WORKER1_PORT=9001

# IP máy chạy Worker 2 (Máy 4)
WORKER2_HOST=192.168.1.102
WORKER2_PORT=9002

# ============== DATABASE ==============
DB_URL=jdbc:mysql://192.168.1.103:3306/distributed_db
DB_USER=root
DB_PASSWORD=yourpassword
```

### Bước 2: Mở Firewall

**Trên mỗi máy**, mở port tương ứng:

```powershell
# Máy Load Balancer (Máy 2) - mở port 8080
netsh advfirewall firewall add rule name="LoadBalancer" dir=in action=allow protocol=tcp localport=8080

# Máy Worker 1 (Máy 3) - mở port 9001
netsh advfirewall firewall add rule name="Worker1" dir=in action=allow protocol=tcp localport=9001

# Máy Worker 2 (Máy 4) - mở port 9002
netsh advfirewall firewall add rule name="Worker2" dir=in action=allow protocol=tcp localport=9002

# Máy MySQL - mở port 3306
netsh advfirewall firewall add rule name="MySQL" dir=in action=allow protocol=tcp localport=3306
```

### Bước 3: Kiểm tra IP

```powershell
# Xem IP của máy
ipconfig
```

Tìm dòng `IPv4 Address` trong phần `Wireless LAN adapter Wi-Fi` hoặc `Ethernet adapter`.

### Bước 4: Chạy trên từng máy

**Máy 3 - Worker 1:**
```bash
.\gradlew.bat :server-node:runWorker1
```

**Máy 4 - Worker 2:**
```bash
.\gradlew.bat :server-node:runWorker2
```

**Máy 2 - Load Balancer:**
```bash
.\gradlew.bat :load-balancer:run
```

**Máy 1 - Client (nhiều người có thể chạy):**
```bash
.\gradlew.bat :javafx-client:run
```

### Cách khác: Dùng Environment Variables

Thay vì file config.properties, có thể set biến môi trường:

```powershell
# Windows - Set tạm thời
$env:LOAD_BALANCER_HOST = "192.168.1.100"
$env:WORKER1_HOST = "192.168.1.101"
$env:WORKER2_HOST = "192.168.1.102"

# Rồi chạy
.\gradlew.bat :javafx-client:run
```

## 🔐 Database Sharding Logic

```java
// ID chẵn -> SHARD_A, ID lẻ -> SHARD_B
String shard = (userId % 2 == 0) ? "SHARD_A" : "SHARD_B";
```

## ⚙️ Configuration

Hệ thống đọc config theo thứ tự ưu tiên:
1. **Environment Variables** (cao nhất)
2. **config.properties** file
3. **Default values** (localhost)

### Các biến cấu hình

| Variable | Default | Description |
|----------|---------|-------------|
| `LOAD_BALANCER_HOST` | localhost | IP của Load Balancer |
| `LOAD_BALANCER_PORT` | 8080 | Port của Load Balancer |
| `WORKER1_HOST` | localhost | IP của Worker 1 |
| `WORKER1_PORT` | 9001 | Port của Worker 1 |
| `WORKER2_HOST` | localhost | IP của Worker 2 |
| `WORKER2_PORT` | 9002 | Port của Worker 2 |
| `DB_URL` | jdbc:mysql://localhost:3306/distributed_db | JDBC URL |
| `DB_USER` | root | Database username |
| `DB_PASSWORD` | password | Database password |
| `SSL_ENABLED` | false | Enable SSL/TLS encryption |
| `ACL_APPROVAL_MODE` | AUTO | TCP approval mode (AUTO/MANUAL) |
| `ACL_MANUAL_TIMEOUT_MS` | 30000 | Manual approval timeout (ms) |

## 📝 Available Commands

| Command | Description |
|---------|-------------|
| `GET_USER` | Get user by ID |
| `GET_ALL_USERS` | Get all users (limit 100) |
| `SEARCH_USERS` | Search by keyword |
| `CREATE_USER` | Create new user |
| `PING` | Check server connectivity |
| `HEALTH_CHECK` | Check database connection |
| `GET_LARGE_DATA` | Simulate large data query |

## 🧪 Testing

Mở nhiều JavaFX Client instances để test load balancing:
```bash
# Terminal 4
./gradlew :javafx-client:run

# Terminal 5
./gradlew :javafx-client:run

# Terminal 6
./gradlew :javafx-client:run
```

Quan sát console của Load Balancer để thấy requests được phân phối đều cho các Worker.
