# Distributed System -- Nhanh `client-2`

## Vai tro: JavaFX Client 2

Nhanh nay chay **JavaFX Client** -- giao dien desktop de tuong tac voi he thong phan tan.
Client gui request TCP den Load Balancer, LB route den Worker Server xu ly.

### Nhanh nay chay:
- JavaFX Client UI -- giao dien CRUD users, ping, health check
- SocketClient -- ket noi TCP den Load Balancer (Length-Prefix Framing)
- common-lib -- shared DTOs, protocol, utilities

### Nhanh nay KHONG build:
- `server-node/` -- chay o nhanh `main`
- `load-balancer/` -- chay o nhanh `load-balancer`

---

## CACH CHAY

### Dieu kien tien quyet:
1. Worker Server da chay (nhanh `main`): `RUN.bat 9001`
2. Load Balancer da chay (nhanh `load-balancer`): `RUN.bat`

### Chay Client 2:

**Cach 1 -- RUN.bat (khuyen nghi):**
```bash
RUN.bat
```

**Cach 2 -- Gradle truc tiep:**
```bash
.\gradlew.bat :javafx-client:run
```

---

## Giao dien Client UI

### Chuc nang:
| Thanh phan | Chuc nang |
|------------|-----------|
| Fetch All Users | Lay danh sach tat ca users tu database |
| Search | Tim kiem user theo keyword (username, email, full name) |
| Get User | Lay user theo ID cu the |
| Ping Server | Kiem tra ket noi TCP den server |
| Health Check | Kiem tra trang thai server |
| Create User | Tao user moi (username, email, full name) |
| Update User | Cap nhat user (chon tu bang hoac nhap ID) |
| Delete User | Xoa user theo ID |

### Status Bar:
- Trang thai ket noi hien tai
- Worker Server nao da xu ly request (round robin)
- Thoi gian xu ly (ms)
- Trang thai SSL

---

## Luong ket noi

```
  +-------------+     +------------------+     +-------------------+
  |  CLIENT 2   |---->|  LOAD BALANCER   |---->|  Worker Server 1  |
  |  (NHANH NAY)|     |  Port: 8080      |     |  Port: 9001       |
  |  JavaFX UI  |     |  Round Robin     |     +-------------------+
  +-------------+     |                  |     +-------------------+
                      |                  |---->|  Worker Server 2  |
                      +------------------+     |  Port: 9002       |
                                               +-------------------+
```

---

## Tat ca cac nhanh

| Nhanh | Vai tro | Module build | Lenh chay |
|-------|---------|-------------|------------|
| `main` | Worker Server + Core | common-lib, server-node | `RUN.bat 9001` |
| `load-balancer` | Load Balancer + Admin UI | common-lib, load-balancer | `RUN.bat` |
| `client-1` | JavaFX Client 1 | common-lib, javafx-client | `RUN.bat` |
| `client-2` | **JavaFX Client 2** <-- BAN O DAY | common-lib, javafx-client | `RUN.bat` |

---

## Chay tren nhieu may (Multi-machine)

Sua `config.properties`:
```properties
# Chi can doi IP cua Load Balancer
LOAD_BALANCER_HOST=<IP may Load Balancer>
LOAD_BALANCER_PORT=8080
```

