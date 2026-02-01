# Distributed System - Java TCP Socket

Hệ thống phân tán sử dụng Java Sockets (TCP) thuần túy với Gradle multi-module.

## 📁 Cấu trúc Project

```
distributed-system-root/
├── build.gradle              # Root build file
├── settings.gradle           # Module configuration
├── config.properties.example # Template cấu hình mạng
├── common-lib/               # Shared DTOs, utilities
│   └── src/main/java/org/example/common/
│       ├── PacketUtils.java      # Length-Prefix Framing
│       ├── RequestPayload.java   # Request DTO
│       ├── ResponsePayload.java  # Response DTO
│       ├── UserDTO.java          # User entity DTO
│       ├── Commands.java         # Command constants
│       └── NetworkConfig.java    # Network configuration loader
├── server-node/              # Worker Server
│   └── src/main/java/org/example/
│       ├── WorkerServer.java     # Main server
│       ├── ClientHandler.java    # Request handler
│       └── DatabaseConnection.java # HikariCP pool
├── load-balancer/            # Load Balancer (Round Robin)
│   └── src/main/java/org/example/
│       └── LoadBalancer.java     # Main LB
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
