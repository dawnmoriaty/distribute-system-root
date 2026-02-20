Báo cáo Nghiên cứu: Thiết kế và Triển khai Hệ thống Phân tán Đa Server Tích hợp Client Desktop JavaFX Quy mô Lớn
1. Tổng quan và Cơ sở Lý luận về Hệ thống Phân tán Phi Web
   1.1 Đặt vấn đề và Phạm vi Nghiên cứu
   Trong bối cảnh phát triển phần mềm hiện đại, xu hướng chuyển dịch sang các ứng dụng nền web (Web-based applications) và kiến trúc vi dịch vụ (Microservices) đang chiếm ưu thế nhờ khả năng triển khai linh hoạt và tính sẵn sàng cao. Tuy nhiên, tồn tại một phân khúc quan trọng các hệ thống nghiệp vụ yêu cầu hiệu năng xử lý thời gian thực, độ trễ thấp, và khả năng tương tác phần cứng sâu mà chỉ các ứng dụng Desktop (Native Applications) mới có thể đáp ứng tối ưu.1 Đề bài đặt ra thách thức xây dựng một hệ thống phân tán bao gồm 4 máy chủ (Server) và cơ sở dữ liệu lớn, phục vụ Client là ứng dụng Desktop viết bằng Java (JavaFX), hoàn toàn không sử dụng công nghệ Web (HTTP/REST), mà dựa trên giao thức giao vận mức thấp (Socket).
   Việc loại bỏ các tầng trừu tượng của giao thức HTTP đặt ra yêu cầu thiết kế lại toàn bộ cơ chế giao tiếp, cân bằng tải, và đồng bộ dữ liệu thủ công, vốn thường được các Web Server (như Apache, Nginx) hoặc Application Server (như Tomcat, Jetty) xử lý tự động.3 Báo cáo này sẽ phân tích sâu các khía cạnh lý thuyết và thực tiễn để xây dựng hệ thống này, đảm bảo các tiêu chí về tính toàn vẹn dữ liệu (Consistency), tính sẵn sàng (Availability) và khả năng chịu lỗi (Fault Tolerance) theo định lý CAP trong hệ phân tán.
   1.2 Kiến trúc Hệ thống Phân tán và Mô hình Client-Server
   Hệ thống phân tán được định nghĩa là một tập hợp các máy tính độc lập xuất hiện đối với người dùng như một hệ thống nhất quán duy nhất.2 Trong phạm vi dự án này, mô hình kiến trúc được lựa chọn là Client-Server đa tầng (Multi-tier Client-Server), mở rộng từ mô hình 2 tầng truyền thống.
   Sự khác biệt cốt lõi nằm ở tầng giữa (Middleware/Application Layer). Thay vì một máy chủ đơn lẻ xử lý toàn bộ logic nghiệp vụ, hệ thống sử dụng một cụm (cluster) gồm 4 máy chủ. Điều này đòi hỏi giải quyết ba vấn đề kỹ thuật lớn:
   Phân phối yêu cầu (Request Dispatching): Làm thế nào Client biết gửi yêu cầu đến Server nào trong 4 Server?
   Đồng bộ trạng thái (State Synchronization): Dữ liệu được cập nhật tại Server A có được phản ánh tức thì tại Server B không?
   Quản lý giao dịch phân tán (Distributed Transaction): Đảm bảo tính nhất quán của dữ liệu lớn khi có nhiều luồng ghi đồng thời.5
   Kiến trúc đề xuất bao gồm:
   Tầng Client (Presentation Layer): Ứng dụng JavaFX chạy trên máy trạm, chịu trách nhiệm hiển thị và tương tác người dùng.6
   Tầng Cân bằng tải (Load Balancing Layer): Một thành phần phần mềm (Software Load Balancer) đóng vai trò cổng vào (Gateway), tiếp nhận kết nối TCP từ Client và phân phối sang các Worker Node.7
   Tầng Xử lý (Processing Layer): Gồm 3 Worker Servers thực thi logic nghiệp vụ.
   Tầng Dữ liệu (Data Layer): Hệ quản trị cơ sở dữ liệu MySQL được cấu hình theo mô hình Replication hoặc Sharding để xử lý dữ liệu lớn.9
   1.3 Giao thức Giao tiếp: Socket vs RMI vs Web Services
   Trong yêu cầu "không web", lựa chọn giao thức giao tiếp là quyết định quan trọng nhất ảnh hưởng đến hiệu năng.

Đặc điểm
Java Sockets (TCP)
Java RMI (Remote Method Invocation)
Web Services (HTTP/SOAP/REST)
Mô hình
Stream-based, mức thấp
Object-based, RPC
Document-based, mức cao
Hiệu năng
Rất cao, độ trễ thấp nhất do ít overhead
Trung bình, overhead do serialization Java
Thấp hơn do overhead của HTTP headers và XML/JSON
Kiểm soát
Kiểm soát toàn diện từng byte dữ liệu
Che giấu chi tiết mạng, khó tùy biến cân bằng tải
Stateless, khó duy trì kết nối dài (long-lived)
Triển khai
Phức tạp, phải tự định nghĩa Protocol
Dễ dàng trong môi trường Java thuần nhất
Phổ biến, dễ tích hợp đa nền tảng
Tương thích
11
13
3

Dựa trên bảng so sánh và yêu cầu dự án, Java Sockets (TCP) là lựa chọn tối ưu. RMI mặc dù hỗ trợ Java tốt nhưng gặp khó khăn lớn trong việc cấu hình qua tường lửa và cân bằng tải tùy biến (RMI Load Balancing rất phức tạp và thường yêu cầu CORBA hoặc Jini).14 Web Services bị loại trừ theo yêu cầu đề bài. Socket cho phép xây dựng giao thức truyền tin tin cậy, duy trì kết nối liên tục (Persistent Connection) giữa Desktop Client và Server, hỗ trợ tốt cho các ứng dụng cần cập nhật thời gian thực (Real-time updates).11
2. Thiết kế Kiến trúc Hệ thống Chi tiết
   2.1 Sơ đồ Topo Mạng và Phân bố Server
   Để triển khai mô hình 4 Server hiệu quả, chúng ta cần phân định rõ vai trò của từng Server vật lý (hoặc máy ảo). Một cấu hình tham chiếu tối ưu cho khả năng mở rộng và chịu lỗi bao gồm:
   Server 1 (Gateway/Load Balancer Node): Chạy ứng dụng Load Balancer tự phát triển bằng Java. Đây là điểm truy cập duy nhất (Single Point of Entry) mà Client biết. Server này không thực hiện nghiệp vụ xử lý dữ liệu nặng mà chỉ chuyển tiếp gói tin (Packet Forwarding).8
   Server 2 & Server 3 (Worker Nodes): Chạy ứng dụng Server xử lý nghiệp vụ (Application Server). Tại đây diễn ra quá trình phân tích dữ liệu, tính toán logic và tạo truy vấn SQL.
   Server 4 (Database Node): Chứa MySQL Server. Đối với yêu cầu "cơ sở dữ liệu lớn", Server 4 có thể là một máy chủ vật lý mạnh, hoặc trong môi trường mở rộng, Server 2 và 3 cũng có thể chứa các bản sao (Replica) của Database để chia tải đọc.9 Tuy nhiên, để tách biệt mối quan tâm (Separation of Concerns), việc dành riêng Server 4 cho DB Master là kiến trúc chuẩn.
   2.2 Thiết kế Load Balancer Mềm (Software Load Balancer)
   Trong môi trường không có các thiết bị cân bằng tải phần cứng (F5, Cisco) hoặc các giải pháp cloud (AWS ELB), việc tự xây dựng một Load Balancer bằng Java Socket là một bài toán kỹ thuật phức tạp nhưng thú vị. Load Balancer này sẽ hoạt động ở Lớp 4 (Transport Layer) hoặc Lớp 7 (Application Layer) của mô hình OSI.
   2.2.1 Chiến lược Cân bằng tải
   Chúng ta sẽ triển khai thuật toán Round Robin (Vòng tròn) hoặc Weighted Round Robin (Vòng tròn có trọng số).7
   Cơ chế hoạt động:
   Load Balancer (LB) mở một ServerSocket lắng nghe tại cổng công khai (ví dụ: 8080).
   Duy trì một danh sách các địa chỉ IP:Port của Worker Nodes (ví dụ: 192.168.1.101:9000, 192.168.1.102:9000).
   Khi Client kết nối đến, LB chấp nhận kết nối (accept()) tạo ra clientSocket.
   LB chọn một Worker Node theo thuật toán Round Robin.
   LB mở một kết nối socket mới đến Worker Node đó (workerSocket).
   LB tạo hai luồng (Threads) để bơm dữ liệu (Pipe) qua lại giữa clientSocket và workerSocket.21
   2.2.2 Xử lý Đa luồng và Hiệu năng tại Load Balancer
   Vì LB phải duy trì kết nối cho toàn bộ Client, việc quản lý luồng là sống còn. Mô hình One-Thread-Per-Connection truyền thống của Java (Java IO/BIO) sẽ nhanh chóng làm cạn kiệt tài nguyên nếu số lượng Client lớn. Giải pháp đề xuất là sử dụng Java NIO (Non-blocking IO) với thư viện Netty hoặc sử dụng Selector của Java để một luồng có thể quản lý nhiều kênh (Channel) kết nối.22 Tuy nhiên, trong phạm vi bài nghiên cứu sử dụng Java cơ bản, mô hình Thread Pool (ExecutorService) với kích thước cố định là phương án khả thi và dễ cài đặt để minh họa nguyên lý.23
   2.3 Thiết kế Giao thức Ứng dụng (Application Protocol)
   Vì không sử dụng HTTP, hệ thống cần một giao thức định nghĩa cấu trúc gói tin để Client và Server "hiểu" nhau. Giao thức này cần giải quyết vấn đề phân mảnh gói tin (Fragmentation) và dính gói tin (Packet Coalescing/Stickiness) đặc thù của TCP stream.21
   Cấu trúc Gói tin (Packet Structure):
   Giao thức đề xuất sử dụng mô hình Length-Prefix Framing (Khung độ dài tiền tố):
   Header (4 bytes): Một số nguyên (Integer) biểu thị độ dài của phần thân dữ liệu (Payload).
   Payload (N bytes): Dữ liệu thực tế, được mã hóa dưới dạng JSON hoặc Java Object Serialization.
   Lý do chọn JSON over Serialization: Mặc dù Java Serialization tích hợp sẵn 25, nhưng nó gặp vấn đề về bảo mật, hiệu năng thấp và khó debug (dữ liệu nhị phân không đọc được). JSON (sử dụng thư viện Jackson hoặc Gson) cho phép dễ dàng kiểm tra nội dung gói tin, linh hoạt thay đổi cấu trúc dữ liệu mà không làm gãy giao thức, và hiệu năng chấp nhận được với các hệ thống nghiệp vụ thông thường.26
   Ví dụ nội dung Payload (JSON):

JSON


{
"request_id": "uuid-v4",
"command": "QUERY_LARGE_DATA",
"parameters": {
"start_date": "2023-01-01",
"end_date": "2023-12-31"
}
}


3. Chiến lược Cơ sở dữ liệu Quy mô Lớn (Large-Scale Database Strategy)
   3.1 Thách thức của Dữ liệu Lớn trong Hệ Phân tán
   Yêu cầu "truy xuất cơ sở dữ liệu lớn" ngụ ý rằng một máy chủ MySQL đơn lẻ có thể trở thành điểm nghẽn (bottleneck) về I/O đĩa hoặc CPU. Để giải quyết, chúng ta cần áp dụng các kỹ thuật mở rộng cơ sở dữ liệu (Database Scaling).
   Có hai hướng tiếp cận chính:
   Vertical Scaling (Mở rộng dọc): Nâng cấp phần cứng (RAM, CPU, SSD) cho Server 4. Đây là giải pháp tạm thời và có giới hạn vật lý.
   Horizontal Scaling (Mở rộng ngang): Phân tán dữ liệu ra nhiều máy chủ. Đây là giải pháp bền vững cho hệ thống lớn.10
   3.2 Replication (Nhân bản Dữ liệu)
   Đối với các ứng dụng có tỷ lệ Đọc/Ghi chênh lệch lớn (thường là Đọc nhiều hơn Ghi - Read Heavy), mô hình Master-Slave Replication là phù hợp nhất.9
   Master Node: Chịu trách nhiệm cho tất cả các thao tác ghi dữ liệu (INSERT, UPDATE, DELETE). Dữ liệu thay đổi được ghi vào Binary Log (binlog).
   Slave Nodes: Sao chép dữ liệu từ Master thông qua binlog và chỉ phục vụ các thao tác đọc (SELECT).
   Trong kiến trúc 4 Server của chúng ta:
   Nếu Server 4 là Master, ta có thể cài đặt thêm các instance MySQL Slave trên Server 2 và Server 3 (cùng với ứng dụng Worker).
   Ứng dụng Client/Server sẽ được cấu hình để gửi lệnh Ghi tới IP của Master và lệnh Đọc tới IP của các Slave, giúp cân bằng tải ngay tại tầng cơ sở dữ liệu.9
   3.3 Sharding (Phân mảnh Dữ liệu)
   Nếu dữ liệu quá lớn đến mức một Server không thể chứa hết (ví dụ: hàng tỷ bản ghi), cần áp dụng Sharding.10
   Horizontal Sharding: Chia bảng dữ liệu thành các phần nhỏ dựa trên Shard Key (ví dụ: UserID).
   Shard 1 (Server DB A): UserID 1 - 1.000.000
   Shard 2 (Server DB B): UserID 1.000.001 - 2.000.000
   Triển khai trong Java: Tầng ứng dụng (Application Layer) phải có logic định tuyến (Routing Logic). Khi cần tìm UserID 1.500.000, ứng dụng phải biết kết nối tới Shard 2. Kỹ thuật này phức tạp hơn Replication và thường yêu cầu thay đổi lớn trong code truy xuất dữ liệu (DAO Layer).30
   Kết luận Chiến lược DB: Với quy mô bài toán triển khai 4 Server, mô hình Replication là khả thi và hiệu quả nhất để minh họa khả năng truy xuất dữ liệu lớn mà không làm tăng quá mức độ phức tạp quản lý Shard.
4. Hiện thực hóa Hệ thống: Triển khai Chi tiết
   4.1 Triển khai Worker Server (Java Socket & Multi-threading)
   Worker Server là trái tim của hệ thống xử lý. Mã nguồn cần được thiết kế theo mô hình đa luồng để phục vụ nhiều kết nối từ Load Balancer.
   4.1.1 Quản lý Kết nối và Luồng
   Sử dụng java.net.ServerSocket để lắng nghe cổng. Tuy nhiên, thay vì tạo new Thread() cho mỗi kết nối (dễ dẫn đến lỗi OutOfMemoryError nếu quá tải), ta sử dụng ExecutorService (Thread Pool).23

Java


// Snippet mô phỏng kiến trúc Server
public class WorkerServer {
private static final int PORT = 9001;
private static final int THREAD_POOL_SIZE = 100; // Giới hạn số luồng xử lý đồng thời

    public void start() {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Giao việc xử lý kết nối cho Thread Pool
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


4.1.2 Xử lý Database Connection Pooling
Việc mở kết nối đến MySQL cho mỗi request là cực kỳ tốn kém (chi phí bắt tay TCP, xác thực). Worker Server phải sử dụng Connection Pooling (như HikariCP hoặc C3P0) để tái sử dụng các kết nối DB.29 Điều này đặc biệt quan trọng khi hệ thống phải chịu tải cao.
Cấu hình Pool: Cần tính toán số lượng kết nối tối đa dựa trên khả năng của MySQL Server. Công thức tham khảo: Connections = ((core_count * 2) + effective_spindle_count).
4.2 Triển khai Client Desktop (JavaFX)
Ứng dụng Client JavaFX cần đảm bảo trải nghiệm người dùng mượt mà (Responsive UI), ngay cả khi đang thực hiện truy vấn dữ liệu lớn qua mạng.
4.2.1 Kiến trúc MVC/MVP cho JavaFX
Sử dụng mẫu thiết kế Model-View-Controller (MVC) để tách biệt logic giao diện và logic dữ liệu.32
View (FXML): Định nghĩa giao diện người dùng (Layout, Controls).
Controller: Xử lý sự kiện từ người dùng, nhưng không trực tiếp thực hiện gọi mạng (Network calls).
Service/Model: Lớp chịu trách nhiệm giao tiếp Socket và xử lý dữ liệu.
4.2.2 Xử lý Bất đồng bộ (Asynchronous Handling)
Một lỗi phổ biến là thực hiện I/O (đọc/ghi socket) ngay trên luồng giao diện chính (JavaFX Application Thread), dẫn đến ứng dụng bị "đóng băng" (Not Responding). Giải pháp bắt buộc là sử dụng javafx.concurrent.Task hoặc Service để đẩy các tác vụ mạng xuống luồng nền (Background Thread).34

Java


// Snippet xử lý cập nhật UI từ luồng nền
Task<List<Data>> dataLoadTask = new Task<>() {
@Override
protected List<Data> call() throws Exception {
// Thực hiện giao tiếp mạng ở đây (Blocking I/O)
return socketClient.queryLargeData();
}
};

dataLoadTask.setOnSucceeded(event -> {
// Cập nhật UI ở đây (JavaFX Thread)
List<Data> result = dataLoadTask.getValue();
tableView.getItems().setAll(result);
});

new Thread(dataLoadTask).start();


Trong trường hợp cần cập nhật UI từ một luồng Socket lắng nghe liên tục (ví dụ: nhận thông báo real-time), phải bọc lệnh cập nhật trong Platform.runLater().36
4.3 Kỹ thuật Đồng bộ Dữ liệu và Trạng thái
Trong môi trường đa Server, việc đồng bộ dữ liệu (Data Synchronization) là thách thức lớn nhất để đảm bảo tính nhất quán (Consistency).
4.3.1 Đồng bộ Cache (Cache Coherency)
Để tăng tốc truy xuất, mỗi Worker Server thường cache dữ liệu cục bộ. Khi Server A cập nhật dữ liệu vào DB, cache tại Server B sẽ bị cũ (stale).
Giải pháp:
Không dùng Cache cục bộ (No Local Cache): Luôn đọc từ DB. Đảm bảo đúng đắn nhưng chậm.
Distributed Cache (Redis/Hazelcast): Sử dụng một server cache chung.
Cơ chế Pub/Sub nội bộ: Khi Server A ghi dữ liệu, nó gửi một thông điệp "INVALIDATE_CACHE" tới tất cả các Server khác (thông qua một kênh multicast hoặc qua Load Balancer) để các Server khác xóa cache cũ.37
Đối với yêu cầu "Server xử lý đồng bộ dữ liệu", phương án Write-Through kết hợp với cơ chế thông báo là tối ưu. Khi có lệnh cập nhật từ Client:
Server xử lý ghi vào MySQL.
Nếu ghi thành công, Server gửi tín hiệu broadcast tới các Server còn lại.
Các Server nhận tín hiệu sẽ tải lại dữ liệu mới từ DB hoặc cập nhật cache của mình.39
4.3.2 Khóa Lạc quan (Optimistic Locking)
Khi nhiều Client cùng sửa một bản ghi trên 2 Server khác nhau, cần cơ chế khóa để tránh ghi đè dữ liệu (Lost Update Problem). Sử dụng cột version trong bảng cơ sở dữ liệu.
Lệnh Update: UPDATE table SET value = new_value, version = version + 1 WHERE id = x AND version = current_version.
Nếu số dòng cập nhật = 0, nghĩa là dữ liệu đã bị thay đổi bởi người khác, Server trả lỗi về Client để người dùng tải lại dữ liệu.41
5. Quy trình Vận hành và Kịch bản Triển khai
   5.1 Kịch bản Khởi động Hệ thống
   Để vận hành hệ thống gồm 4 Server logic trên môi trường giả lập hoặc thực tế, cần tuân thủ quy trình khởi động nghiêm ngặt để đảm bảo các kết nối được thiết lập đúng.
   Bước 1: Khởi động Tầng Dữ liệu (Server 4). Đảm bảo MySQL Service đã chạy và sẵn sàng chấp nhận kết nối tại port 3306.
   Bước 2: Khởi động các Worker Node (Server 2 & 3). Các ứng dụng Java Server khởi chạy, khởi tạo Connection Pool kết nối tới DB. Nếu không kết nối được DB, Server phải tự tắt hoặc chờ (retry mechanism).
   Bước 3: Khởi động Load Balancer (Server 1). Load Balancer khởi động, đọc file cấu hình chứa danh sách IP của Worker Node. LB bắt đầu lắng nghe tại cổng 8080.
   Bước 4: Khởi chạy Client. Người dùng mở ứng dụng JavaFX, ứng dụng kết nối tới Load Balancer (Server 1:8080).
   5.2 Giám sát và Kiểm tra Sức khỏe (Health Checks)
   Hệ thống phân tán cần khả năng tự phát hiện lỗi. Load Balancer cần tích hợp module Health Check.42
   Active Check: LB định kỳ (ví dụ 5 giây) gửi một gói tin "PING" đặc biệt tới từng Worker. Nếu Worker không phản hồi "PONG" trong thời gian timeout, LB đánh dấu Worker đó là "Dead" và ngừng gửi request tới đó.
   Passive Check: Nếu LB gửi request của Client tới Worker và gặp lỗi kết nối (Connection Refused/Reset), nó tự động thử lại (Retry) sang Worker khác và đánh dấu Worker hiện tại là nghi ngờ.
   5.3 Triển khai Mã nguồn và Cấu trúc Thư mục
   Để quản lý dự án phức tạp này, mã nguồn nên được tổ chức thành mô hình Multi-module (ví dụ sử dụng Maven hoặc Gradle).44
   Cấu trúc Project tham khảo:
   distributed-system-root
   common-lib: Chứa các class DTO, Protocol Constant, Utility dùng chung.
   server-node: Mã nguồn của Worker Server (xử lý nghiệp vụ, DB).
   load-balancer: Mã nguồn của Load Balancer (routing, health check).
   javafx-client: Mã nguồn ứng dụng Desktop.
   5.4 Kỹ thuật Triển khai trên Một máy (Localhost Simulation)
   Trong giai đoạn phát triển và báo cáo, thường chúng ta triển khai giả lập trên một máy. Để làm điều này, mỗi Server phải lắng nghe trên một cổng khác nhau.46
   Load Balancer: localhost:8080
   Worker 1: localhost:9001
   Worker 2: localhost:9002
   Worker 3: localhost:9003
   Database: localhost:3306
   File cấu hình của Load Balancer sẽ trỏ tới các cổng 9001, 9002, 9003 thay vì các IP khác nhau.
6. Phân tích Các Vấn đề Kỹ thuật Chuyên sâu
   6.1 So sánh Hiệu năng: JSON vs Binary Serialization
   Trong phần thiết kế giao thức, việc chọn định dạng dữ liệu ảnh hưởng lớn đến băng thông và CPU.
   Tiêu chí
   JSON (Text-based)
   Java Serialization
   Protobuf/MessagePack
   Kích thước
   Lớn (do lặp lại tên trường)
   Rất lớn (chứa meta-data của class)
   Rất nhỏ (Binary packed)
   CPU Cost
   Cao (Parsing text tốn kém)
   Trung bình
   Thấp
   Độ linh hoạt
   Rất cao (Schema-less)
   Thấp (Strict typing)
   Trung bình (Schema evolution)
   Khả năng debug
   Dễ (Đọc được bằng mắt thường)
   Khó
   Khó (Cần tool decode)

Khuyến nghị: Với dự án quy mô vừa và mục tiêu nghiên cứu, JSON kết hợp với GZIP compression (nếu gói tin lớn) là điểm cân bằng tốt nhất giữa hiệu năng và khả năng bảo trì.27
6.2 Chiến lược Xử lý Lỗi Mạng (Network Failure Handling)
Hệ thống phân tán không thể giả định mạng luôn ổn định (Fallacies of distributed computing).
Tại Client: Cần xử lý ngoại lệ SocketException. Khi mất kết nối, Client nên hiển thị trạng thái "Offline" và thử kết nối lại ngầm (Exponential Backoff Strategy).48
Tại Load Balancer: Nếu đang truyền dữ liệu mà Worker chết, LB phải đóng kết nối với Client và báo lỗi, hoặc tốt hơn là (nếu chưa gửi dữ liệu) thử lại sang Worker khác (Transparent Retry).
6.3 Bảo mật trong Giao tiếp Socket
Giao tiếp Socket thuần túy là văn bản rõ (Clear text). Để bảo mật, cần nâng cấp lên SSL/TLS Sockets (SSLSocket trong Java).
Cần tạo Keystore và Truststore chứa chứng chỉ số (Certificate).
Server sử dụng SSLServerSocketFactory.
Client sử dụng SSLSocketFactory.
Điều này đảm bảo dữ liệu (đặc biệt là thông tin đăng nhập và dữ liệu nhạy cảm từ DB) được mã hóa trên đường truyền.
7. Kết luận và Đánh giá
   Việc xây dựng một hệ thống phân tán đa Server với Client JavaFX Desktop là một bài toán tổng hợp kiến thức sâu rộng từ Lập trình mạng (Socket), Lập trình đa luồng (Concurrency), Thiết kế cơ sở dữ liệu (Database Replication/Sharding) đến Kiến trúc phần mềm (MVC/MVP, Layered Architecture).
   Hệ thống được thiết kế trong báo cáo này đáp ứng đầy đủ các yêu cầu khắt khe:
   Tính phân tán: Sử dụng 4 Server với vai trò rõ ràng (Gateway, Worker, DB).
   Hiệu năng cao: Sử dụng giao thức TCP Socket tùy biến, loại bỏ overhead của HTTP, tối ưu cho ứng dụng Desktop thời gian thực.
   Khả năng mở rộng: Kiến trúc Load Balancer cho phép thêm Worker Node mới mà không cần sửa đổi Client.
   Xử lý dữ liệu lớn: Tích hợp chiến lược MySQL Replication để phân tải truy vấn.
   Mặc dù độ phức tạp trong việc triển khai (tự viết Load Balancer, tự quản lý Protocol) cao hơn nhiều so với việc sử dụng các Web Framework có sẵn, nhưng kết quả mang lại là một hệ thống có độ trễ cực thấp, khả năng kiểm soát tài nguyên tối đa và nền tảng kiến thức vững chắc về nguyên lý hoạt động của các hệ thống máy tính hiện đại. Đây là mô hình tham chiếu lý tưởng cho các hệ thống giao dịch tài chính, giám sát thời gian thực, hoặc các ứng dụng nội bộ doanh nghiệp yêu cầu độ tin cậy cao.
   (Hết báo cáo)
   Lưu ý: Báo cáo này đã tích hợp các kiến thức từ các tài liệu nghiên cứu 1 đến 49 để đảm bảo tính chính xác và đầy đủ theo yêu cầu.
   Works cited
   Unlocking the Power of Java: Building High-Performance Applications with Distributed System Architecture - Ways and Means Technology, accessed January 26, 2026, https://waysandmeanstechnology.com/blog/unlocking-the-power-of-java-building-high-performance-applications-with-distributed-system-architecture
   What Is a Distributed Application? - JRebel, accessed January 26, 2026, https://www.jrebel.com/blog/distributed-application
   Web-Service vs Client-Server Distributed Computing Technology - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/10152115/web-service-vs-client-server-distributed-computing-technology
   Distributed Systems: An Introduction - Confluent, accessed January 26, 2026, https://www.confluent.io/learn/distributed-systems/
   Synchronization in Distributed Systems - GeeksforGeeks, accessed January 26, 2026, https://www.geeksforgeeks.org/distributed-systems/synchronization-in-distributed-systems/
   Building Serious JavaFX 2 Applications - YouTube, accessed January 26, 2026, https://www.youtube.com/watch?v=gKU7ZeCNbqU
   Java load balancing design tips, accessed January 26, 2026, http://www.javaperformancetuning.com/tips/loadbalance.shtml
   How to create a software load balancer for socket connections in java - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/16141975/how-to-create-a-software-load-balancer-for-socket-connections-in-java
   MySQL 8.4 Reference Manual :: 19 Replication, accessed January 26, 2026, https://dev.mysql.com/doc/en/replication.html
   Sharding vs. partitioning: What's the difference? - PlanetScale, accessed January 26, 2026, https://planetscale.com/blog/sharding-vs-partitioning-whats-the-difference
   Java Socket Programming - Socket Server, Client example - DigitalOcean, accessed January 26, 2026, https://www.digitalocean.com/community/tutorials/java-socket-programming-server-client
   JavaFX, Sockets and Threading: Lessons Learned - DZone, accessed January 26, 2026, https://dzone.com/articles/javafx-sockets-and-threading
   Developing RMI Applications for Oracle WebLogic Server, accessed January 26, 2026, https://docs.oracle.com/en/middleware/fusion-middleware/weblogic-server/12.2.1.4/wlrmi/developing-rmi-applications-oracle-weblogic-server.pdf
   Is it possible to use RMI with a round-robin load balancer in a distributed environment?, accessed January 26, 2026, https://stackoverflow.com/questions/42150878/is-it-possible-to-use-rmi-with-a-round-robin-load-balancer-in-a-distributed-envi
   Distributed/Network application development that is user focused but NOT web application development [closed] - Software Engineering Stack Exchange, accessed January 26, 2026, https://softwareengineering.stackexchange.com/questions/76760/distributed-network-application-development-that-is-user-focused-but-not-web-app
   Load Balancing Java RMI Requests - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/19046560/load-balancing-java-rmi-requests
   Scaling Java Web Socket Applications - Medium, accessed January 26, 2026, https://medium.com/@AlexanderObregon/scaling-java-web-socket-applications-f3bbd2de8866
   Client-Server architecture with multiple servers - java - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/9204463/client-server-architecture-with-multiple-servers
   How To Set Up MySQL Master-Master Replication - DigitalOcean, accessed January 26, 2026, https://www.digitalocean.com/community/tutorials/how-to-set-up-mysql-master-master-replication
   Weight based round robin pattern - java - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/28002792/weight-based-round-robin-pattern
   How to make your own L4(TCP) Load Balancer? | thoughts, projects, interests, accessed January 26, 2026, https://koksalmis.github.io/jekyll/update/2021/05/23/L4-Load-Balancer.html
   Building a Production-Grade TCP Network Load Balancer in Java with Netty — End-to-End Guide | by Deysouvik - Medium, accessed January 26, 2026, https://medium.com/@deysouvik700/building-a-production-grade-tcp-network-load-balancer-in-java-with-netty-end-to-end-guide-a41254c13dae
   Multithreaded Servers in Java - GeeksforGeeks, accessed January 26, 2026, https://www.geeksforgeeks.org/java/multithreaded-servers-in-java/
   Multithreaded Java proxy server - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/33525045/multithreaded-java-proxy-server
   Java Serialization vs JSON vs XML - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/11102645/java-serialization-vs-json-vs-xml
   Serialization Strategies for Low-Latency Systems: JSON vs Protobuf vs SBE | by Aditya Kale, accessed January 26, 2026, https://medium.com/@adikale123/serialization-strategies-for-low-latency-systems-json-vs-protobuf-vs-sbe-ccf9730c8655
   Benchmarking Data Serialization: JSON vs. Protobuf vs. Flatbuffers | by Harshil Jani, accessed January 26, 2026, https://medium.com/@harshiljani2002/benchmarking-data-serialization-json-vs-protobuf-vs-flatbuffers-3218eecdba77
   A Beginner's Guide to Database Sharding: How to Scale Your Database Effectively, accessed January 26, 2026, https://proxysql.com/blog/database-sharding/
   Connectors and APIs Manual :: 3.8.5 Advanced Load-balancing and Failover Configuration, accessed January 26, 2026, https://dev.mysql.com/doc/connectors/en/connector-j-usagenotes-j2ee-concepts-load-balancing-failover.html
   Sharding pattern - Azure Architecture Center - Microsoft Learn, accessed January 26, 2026, https://learn.microsoft.com/en-us/azure/architecture/patterns/sharding
   Sharding with SpringBoot. Understanding Database Sharding | by Raj Kundalia | Medium, accessed January 26, 2026, https://medium.com/@rajkundalia/sharding-with-springboot-c9530e6af929
   JavaFX Notepad App Tutorial: Build a Functional Desktop App - Kite Metric, accessed January 26, 2026, https://kitemetric.com/blogs/javafx-notepad-app-tutorial-build-a-functional-desktop-app
   MVP, JavaFx and components references - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/30554694/mvp-javafx-and-components-references
   Platform.runLater and Task in JavaFX - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/13784333/platform-runlater-and-task-in-javafx
   Concurrency in JavaFX - by Matthew Glover - Medium, accessed January 26, 2026, https://medium.com/@mglover/concurrency-in-javafx-32a5f6133d
   When should I be using Platform.runLater() in javafx? - Reddit, accessed January 26, 2026, https://www.reddit.com/r/JavaFX/comments/11ery89/when_should_i_be_using_platformrunlater_in_javafx/
   Best way to synchronize cache data between two servers [closed] - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/16585798/best-way-to-synchronize-cache-data-between-two-servers
   A simple architecture for cache or web-socket layers | by Brian Mayer | Nagoya Foundation, accessed January 26, 2026, https://medium.com/nagoya-foundation/a-simple-architecture-for-cache-or-web-socket-layers-96571cffe85b
   Read Through Vs Write Through Cache | System Design - AlgoMaster.io, accessed January 26, 2026, https://algomaster.io/learn/system-design/read-through-vs-write-through-cache
   Why Use a Write-Through Cache in Distributed Systems (in Real World) - Reddit, accessed January 26, 2026, https://www.reddit.com/r/AskProgramming/comments/16bkua0/why_use_a_writethrough_cache_in_distributed/
   data transaction synchronization in multiple server - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/34658538/data-transaction-synchronization-in-multiple-server
   Implementing health checks - AWS - Amazon.com, accessed January 26, 2026, https://aws.amazon.com/builders-library/implementing-health-checks/
   Health Checking Best Practices - Carl Mastrangelo, accessed January 26, 2026, https://carlmastrangelo.com/blog/health-checking-best-practices
   Regarding multi-module project, can I set each port? - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/74201708/regarding-multi-module-project-can-i-set-each-port
   JavaFX Project Structure - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/24948397/javafx-project-structure
   Java Server - Multiple ports? - Stack Overflow, accessed January 26, 2026, https://stackoverflow.com/questions/5079172/java-server-multiple-ports
   Run and Configure Multiple Instances in a Single Tomcat Server - DZone, accessed January 26, 2026, https://dzone.com/articles/run-configure-multiple-instance-in-a-single-tomcat
   Best practices for JavaFX desktop application communicating with a remote server [closed], accessed January 26, 2026, https://stackoverflow.com/questions/41608158/best-practices-for-javafx-desktop-application-communicating-with-a-remote-server
   Using Java's Project Loom to build more reliable distributed systems - James Baker, accessed January 26, 2026, https://jbaker.io/2022/05/09/project-loom-for-distributed-systems/
   CHƯƠNG 1: TỔNG QUAN & CÔNG NGHỆ SỬ DỤNG
   Mục tiêu: Giới thiệu đề tài, lý do chọn đề tài và các công nghệ lõi.
   Đã có (Trong file Word):
   Đặt vấn đề: Mục 1.1 "Đặt vấn đề và Phạm vi Nghiên cứu" đã viết rất tốt về bối cảnh chuyển dịch sang Web nhưng vẫn cần Desktop App cho hiệu năng cao.
   Mục tiêu: Xây dựng hệ thống 4 server, không dùng HTTP, dùng Socket.
   Cần làm thêm/Bổ sung:
   Liệt kê chi tiết công nghệ (lấy từ README.md): Java 17/21, Gradle, JavaFX, MySQL, thư viện Jackson (JSON), HikariCP.
   Giải thích ngắn gọn tại sao chọn bộ stack này (Ví dụ: Tại sao JavaFX mà không phải Swing? Tại sao Gradle mà không phải Maven? - Code bạn dùng Gradle).
   CHƯƠNG 2: CƠ SỞ LÝ THUYẾT
   Mục tiêu: Chứng minh kiến thức nền tảng.
   Trong file Word của bạn đã có các ý chính, nhưng đang viết dưới dạng "Kết luận/Giải pháp" nhiều hơn là "Lý thuyết căn bản". Bạn cần đảo ngược lại cách viết: từ Lý thuyết -> Dẫn đến lựa chọn.
   👤 Người 1: Mạng & Kiến trúc (Network Architect)
   Nội dung cần viết:
   Lý thuyết: Mô hình Client-Server truyền thống vs Đa tầng (Multi-tier).
   Lý thuyết: Các giao thức tầng Transport (TCP vs UDP). So sánh Socket vs HTTP (Mục 1.3 và bảng so sánh trong file Word rất đắt giá, hãy bưng vào đây).
   Cần làm rõ thêm: Định nghĩa thuật toán Round Robin là gì (lý thuyết thuần túy), chưa cần nói là mình áp dụng thế nào.
   👤 Người 2: Đa luồng & Database (Backend Engineer)
   Nội dung cần viết:
   Lý thuyết: Các mô hình lập trình song song (Concurrency) trong Java (Thread, Thread Pool, Future). Tham khảo mục 4.1.1 trong Word.
   Lý thuyết: Định lý CAP trong hệ phân tán (Mục 3.1 file Word đã nhắc đến, cần mở rộng định nghĩa).
   Cần làm rõ thêm: Lý thuyết về Connection Pooling (tại sao mở kết nối DB lại tốn kém?). Lý thuyết về Database Replication (Master-Slave).
   👤 Người 3: Giao thức & Client (Protocol Engineer)
   Nội dung cần viết:
   Lý thuyết: Các phương pháp đóng gói dữ liệu (Serialization). So sánh JSON vs Binary (Mục 6.1 trong Word).
   Lý thuyết: Vấn đề TCP Fragmentation (Phân mảnh gói tin) và các kỹ thuật Framing (Length-Prefix vs Delimiter). Code của bạn dùng Length-Prefix, nên cần viết kỹ lý thuyết phần này.
   Lý thuyết: Kiến trúc MVC trong JavaFX (Mục 4.2.1 file Word)


