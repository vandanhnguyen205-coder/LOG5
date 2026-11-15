import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

// Đổi thành http://10.0.2.2:8085/api nếu chạy Android Emulator
const String baseUrl = 'http://localhost:8085/api';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LOG-05 Tài Xế Hàng Đặc Biệt',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      home: const DriverLoginScreen(),
    );
  }
}

class DriverLoginScreen extends StatefulWidget {
  const DriverLoginScreen({super.key});

  @override
  State<DriverLoginScreen> createState() => _DriverLoginScreenState();
}

class _DriverLoginScreenState extends State<DriverLoginScreen> {
  final TextEditingController _emailCtrl = TextEditingController();
  final TextEditingController _passCtrl = TextEditingController();
  bool _loading = false;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    final String email = _emailCtrl.text.trim();
    final String password = _passCtrl.text.trim();
    if (email.isEmpty || password.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng nhập đầy đủ Email và Mật khẩu!'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }
    setState(() => _loading = true);
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
      final data = jsonDecode(utf8.decode(res.bodyBytes));
      if (data['success'] == true && mounted) {
        final Map<String, dynamic>? userMap = data['user'] is Map
            ? Map<String, dynamic>.from(data['user'])
            : null;
        final String driverName = userMap?['hoTen']?.toString().trim() ?? '';
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (_) =>
                DriverOrderScreen(driverEmail: email, driverName: driverName),
          ),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(data['message'] ?? 'Đăng nhập thất bại'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi kết nối server: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Đăng Nhập Tài Xế LOG-05')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.local_shipping, size: 64, color: Colors.teal),
            const SizedBox(height: 16),
            TextField(
              controller: _emailCtrl,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(
                labelText: 'Email Tài Xế',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _passCtrl,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Mật khẩu',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: _loading ? null : _login,
                child: _loading
                    ? const CircularProgressIndicator()
                    : const Text('Đăng Nhập Vào Ca Vận Chuyển'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class DriverOrderScreen extends StatefulWidget {
  final String driverEmail;
  final String driverName;

  const DriverOrderScreen({
    super.key,
    required this.driverEmail,
    this.driverName = '',
  });

  @override
  State<DriverOrderScreen> createState() => _DriverOrderScreenState();
}

class _DriverOrderScreenState extends State<DriverOrderScreen> {
  List<dynamic> orders = [];
  bool isLoading = true;
  final Map<String, TextEditingController> _tempControllers = {};

  String get _displayDriverName {
    if (widget.driverName.isNotEmpty) return widget.driverName;
    if (orders.isNotEmpty && orders[0]['taiXeTen'] != null) {
      return orders[0]['taiXeTen'].toString();
    }
    return widget.driverEmail;
  }

  @override
  void initState() {
    super.initState();
    fetchOrders();
  }

  @override
  void dispose() {
    for (final ctrl in _tempControllers.values) {
      ctrl.dispose();
    }
    super.dispose();
  }

  TextEditingController _getTempController(Map<String, dynamic> order) {
    final String id = order['id'].toString();
    if (!_tempControllers.containsKey(id)) {
      final double minT = (order['minTemp'] as num?)?.toDouble() ?? -25.0;
      final double maxT = (order['maxTemp'] as num?)?.toDouble() ?? -15.0;
      final double midT = double.parse(
        ((minT + maxT) / 2.0).toStringAsFixed(1),
      );
      _tempControllers[id] = TextEditingController(text: midT.toString());
    }
    return _tempControllers[id]!;
  }

  Future<void> fetchOrders() async {
    setState(() => isLoading = true);
    try {
      final res = await http.get(
        Uri.parse(
          '$baseUrl/orders?driverEmail=${Uri.encodeComponent(widget.driverEmail)}',
        ),
      );
      if (res.statusCode == 200) {
        setState(() {
          orders = jsonDecode(utf8.decode(res.bodyBytes));
        });
      }
    } catch (e) {
      _showWarningDialog(
        'Lỗi kết nối',
        'Không thể kết nối tới máy chủ Spring Boot: $e',
      );
    } finally {
      setState(() => isLoading = false);
    }
  }

  Future<void> sendTemperature(String orderId, double temp) async {
    Map<String, dynamic>? existingOrder;
    for (final item in orders) {
      if (item is Map && item['id'].toString() == orderId) {
        existingOrder = Map<String, dynamic>.from(item);
        break;
      }
    }

    if (existingOrder != null) {
      final List cps = existingOrder['tuyenDuong'] ?? [];
      final int passed = cps.where((c) => c['daQua'] == true).length;
      if (existingOrder['trangThai'] == 'Đã giao hàng' || passed >= 3) {
        _showWarningDialog(
          'Không thể cập nhật nhiệt độ',
          'Đơn hàng đã qua đủ 3/3 trạm hoặc đã giao xong, không thể gửi thêm nhiệt độ.',
        );
        await fetchOrders();
        return;
      }
    }

    try {
      final res = await http.post(
        Uri.parse('$baseUrl/orders/update-temp'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'orderId': orderId, 'newTemp': temp}),
      );
      final data = jsonDecode(utf8.decode(res.bodyBytes));
      if (data['blocked'] == true) {
        _showWarningDialog(
          'CẢNH BÁO VI PHẠM NHIỆT ĐỘ',
          data['message'] ?? 'Nhiệt độ vượt ngưỡng yêu cầu!',
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(data['message'] ?? 'Đã cập nhật nhiệt độ'),
            backgroundColor: Colors.green,
          ),
        );
      }
      await fetchOrders();
    } catch (e) {
      _showWarningDialog('Lỗi', 'Không gửi được nhiệt độ: $e');
    }
  }

  Future<void> confirmPassCheckpoint(String orderId, int tramId) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/orders/pass-checkpoint'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'orderId': orderId, 'tramId': tramId}),
      );
      final data = jsonDecode(utf8.decode(res.bodyBytes));
      if (data['success'] == true) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(data['message']),
              backgroundColor: Colors.teal,
            ),
          );
        }
        await fetchOrders();
      } else {
        _showWarningDialog('CẢNH BÁO KIỂM TRA TRẠM $tramId', data['message']);
      }
    } catch (e) {
      _showWarningDialog('Lỗi', 'Không thể xác nhận qua trạm: $e');
    }
  }

  Future<void> completeDelivery(String orderId) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/orders/complete-delivery'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'orderId': orderId,
          'podFileName': 'bien_ban_pod_flutter.pdf',
        }),
      );
      final data = jsonDecode(utf8.decode(res.bodyBytes));
      if (data['success'] == true) {
        _showWarningDialog('BÀN GIAO THÀNH CÔNG', data['message']);
      } else {
        _showWarningDialog('CHẶN BÀN GIAO', data['message']);
      }
      await fetchOrders();
    } catch (e) {
      _showWarningDialog('Lỗi', 'Không thể hoàn tất bàn giao: $e');
    }
  }

  void _showWarningDialog(String title, String message) {
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Đã hiểu'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Tài xế: $_displayDriverName',
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
            ),
            Text(
              'LOG-05 • ${widget.driverEmail}',
              style: const TextStyle(fontSize: 12, color: Colors.black54),
            ),
          ],
        ),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: fetchOrders),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => Navigator.pushReplacement(
              context,
              MaterialPageRoute(builder: (_) => const DriverLoginScreen()),
            ),
          ),
        ],
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : orders.isEmpty
          ? const Center(
              child: Text('Chưa có đơn hàng nào được phân công cho bạn.'),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: orders.length,
              itemBuilder: (context, index) {
                final Map<String, dynamic> order = Map<String, dynamic>.from(
                  orders[index],
                );
                final List checkpoints = order['tuyenDuong'] ?? [];
                final int passedCount = checkpoints
                    .where((c) => c['daQua'] == true)
                    .length;
                final bool duBaTram = passedCount >= 3;
                final bool isDelivered = order['trangThai'] == 'Đã giao hàng';
                final TextEditingController tempCtrl = _getTempController(
                  order,
                );

                return Card(
                  margin: const EdgeInsets.only(bottom: 16),
                  child: Padding(
                    padding: const EdgeInsets.all(14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '${order['id']} - ${order['tenHang']} (${order['loaiHang'] ?? 'Hàng Đặc Biệt'})',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Lộ trình: ${order['diemDi']} ➔ ${order['diemDen']}',
                        ),
                        Text(
                          'Ngưỡng yêu cầu: ${order['minTemp']}°C đến ${order['maxTemp']}°C',
                          style: const TextStyle(
                            color: Colors.teal,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Text(
                          'Trạng thái: ${order['trangThai']} | Nhiệt độ hiện tại: ${order['nhietDoThucTe']}°C',
                        ),
                        const Divider(),
                        if (!isDelivered && !duBaTram) ...[
                          const Text(
                            'Bước 1: Kiểm tra & gửi nhiệt độ trước mỗi trạm:',
                            style: TextStyle(fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Expanded(
                                child: TextField(
                                  controller: tempCtrl,
                                  keyboardType:
                                      const TextInputType.numberWithOptions(
                                        signed: true,
                                        decimal: true,
                                      ),
                                  decoration: const InputDecoration(
                                    labelText: 'Nhiệt độ thùng xe (°C)',
                                    border: OutlineInputBorder(),
                                    isDense: true,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              ElevatedButton.icon(
                                onPressed: () {
                                  final double? val = double.tryParse(
                                    tempCtrl.text.trim().replaceAll(',', '.'),
                                  );
                                  if (val == null) {
                                    _showWarningDialog(
                                      'Dữ liệu không hợp lệ',
                                      'Vui lòng nhập giá trị nhiệt độ hợp lệ (ví dụ: -18.5 hoặc 20.0).',
                                    );
                                    return;
                                  }
                                  sendTemperature(order['id'].toString(), val);
                                },
                                icon: const Icon(Icons.thermostat),
                                label: const Text('Gửi Nhiệt Độ'),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                        ] else if (!isDelivered && duBaTram) ...[
                          const Text(
                            'Đã hoàn tất kiểm tra nhiệt độ qua 3/3 trạm. Hãy bấm Xác Nhận Bàn Giao POD bên dưới.',
                            style: TextStyle(
                              color: Colors.green,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 10),
                        ],
                        Text(
                          'Bước 2: Xác nhận qua 3 trạm kiểm tra ($passedCount/3 trạm):',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 6),
                        ...checkpoints.map((tram) {
                          final bool daQua = tram['daQua'] == true;
                          return ListTile(
                            dense: true,
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(
                              daQua
                                  ? Icons.check_circle
                                  : Icons.radio_button_unchecked,
                              color: daQua ? Colors.green : Colors.grey,
                            ),
                            title: Text(tram['tenTram']),
                            trailing: daQua
                                ? Text(
                                    'Đã qua (${tram['nhietDoXacNhan'] ?? ''}°C)',
                                    style: const TextStyle(
                                      color: Colors.green,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  )
                                : ElevatedButton(
                                    onPressed: isDelivered
                                        ? null
                                        : () => confirmPassCheckpoint(
                                            order['id'].toString(),
                                            (tram['id'] as num).toInt(),
                                          ),
                                    child: const Text('Qua Trạm'),
                                  ),
                          );
                        }),
                        const Divider(),
                        if (!isDelivered)
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: duBaTram
                                    ? Colors.green
                                    : Colors.grey,
                                foregroundColor: Colors.white,
                              ),
                              onPressed: duBaTram
                                  ? () =>
                                        completeDelivery(order['id'].toString())
                                  : () => _showWarningDialog(
                                      'Chưa đủ điều kiện bàn giao',
                                      'Bạn mới qua $passedCount/3 trạm kiểm tra. Phải cập nhật nhiệt độ và đi qua đủ 3 trạm mới có thể bàn giao!',
                                    ),
                              icon: const Icon(Icons.assignment_turned_in),
                              label: Text(
                                duBaTram
                                    ? 'Xác Nhận Bàn Giao POD (Đủ 3/3 trạm)'
                                    : 'Khóa Bàn Giao (Cần đủ 3/3 trạm)',
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}
