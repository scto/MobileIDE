
###  前置准备：统一回调处理器
对于所有带有 `callbackId` 的异步方法，原生代码会通过 Base64 编码将结果传回。请在 JS 中全局定义以下函数：

```javascript
/**
 * 原生响应统一回调
 * @param {string} callbackId 调用时传入的唯一标识
 * @param {string} base64Data Base64编码的JSON字符串 {success: bool, data: string}
 */
window.onAndroidResponse = function(callbackId, base64Data) {
    const jsonStr = decodeURIComponent(escape(window.atob(base64Data)));
    const result = JSON.parse(jsonStr);
    console.log(`收到回调 [${callbackId}]:`, result);
    // 处理逻辑...
};
```

---

### 一、 文件系统与配置 (7个)
*注意：readFile/writeFile 等方法通常由子类实现具体逻辑。*

**setBackKeyInterceptor(enabled)**: 设置是否拦截物理返回键。

```javascript
window.Android.setBackKeyInterceptor(true);
```

**readFile(path)**: 读取指定路径文件的文本。

```javascript
let content = window.Android.readFile("/sdcard/test.txt");
```

**writeFile(path, content)**: 将文本写入指定文件。

```javascript
let success = window.Android.writeFile("/sdcard/test.txt", "Hello World");
```

**listFiles(directory)**: 获取目录下文件列表。

```javascript
let files = JSON.parse(window.Android.listFiles("/sdcard/Documents"));
```

**fileExists(path)**: 判断文件是否存在。

```javascript
if(window.Android.fileExists("/sdcard/test.txt")) { ... }
```

**deleteFile(path)**: 删除指定文件。

```javascript
window.Android.deleteFile("/sdcard/test.txt");
```

**getAppConfig()**: 获取 WebApp 的内部配置信息。

```javascript
let config = JSON.parse(window.Android.getAppConfig());
```

---

### 二、 网络(3个)

**saveToDownloads(filename, content, mimeType)**: 将内容保存到系统的“下载”目录。

```javascript
window.Android.saveToDownloads("report.txt", "这是文件内容", "text/plain");
```

**httpRequest(method, url, headers, body, callbackId)**: 发送原生 HTTP 请求（绕过跨域）。

```javascript
let headers = JSON.stringify({"Authorization": "Bearer token"});
window.Android.httpRequest("POST", "https://api.test.com/data", headers, '{"id":1}', "req_001");
```

**下载接口**

**自动被setDownloadListener捕获**

```html
<a href="https://example.com/file.zip">点击下载</a>
```

**主动js请求**

```javascript
// 简单下载
Android.triggerDownload("https://example.com/image.png");

// 完整控制
// Android.downloadFile(url, userAgent, contentDisposition, mimeType);
```

---

### 三、 UI 交互与通知 (3个)

**showToast(message)**: 显示原生气泡提示。

```javascript
window.Android.showToast("保存成功！");
```

**showDialog(title, message, posText, negText, callbackId)**: 显示原生确认对话框。

```javascript
window.Android.showDialog("提示", "确认删除吗？", "确定", "取消", "dialog_delete");
// 回调 data 将返回 "positive" 或 "negative"
```

**showNotification(id, title, content)**: 发送系统状态栏通知。

```javascript
window.Android.showNotification(100, "新消息", "您有一条未读提醒");
```

---

### 四、 设备信息与剪贴板 (3个)

**getDeviceInfo()**: 获取手机型号、系统版本、屏幕宽高、WIFI等信息。

```javascript
let info = JSON.parse(window.Android.getDeviceInfo());
console.log(info.model, info.androidVersion);
```

**copyToClipboard(text)**: 将文字复制到剪贴板。

```javascript
window.Android.copyToClipboard("这是要复制的文字");
```

**getFromClipboard(callbackId)**: 获取剪贴板中的文字。

```javascript
window.Android.getFromClipboard("paste_001");
```

---

### 五、 数据存储 - SharedPreferences (5个)

**saveStorage(key, value)**: 永久存储字符串。

```javascript
window.Android.saveStorage("username", "jack");
```

**getStorage(key)**: 读取存储的值。

```javascript
let val = window.Android.getStorage("username");
```

**removeStorage(key)**: 删除某个键值对。

```javascript
window.Android.removeStorage("username");
```

**clearStorage()**: 清空所有存储。

```javascript
window.Android.clearStorage();
```

**getAllStorage()**: 获取所有存储的数据。

```javascript
let all = JSON.parse(window.Android.getAllStorage());
```

---

### 六、 系统功能跳转 (6个)

**openBrowser(url)**: 使用系统浏览器打开网址。

```javascript
window.Android.openBrowser("https://google.com");
```

**shareText(text)**: 调用系统原生分享。

```javascript
window.Android.shareText("这是要分享的文字内容");
```

**callPhone(number)**: 跳转到拨号盘。

```javascript
window.Android.callPhone("10086");
```

**sendSMS(number, message)**: 跳转到短信发送界面。

```javascript
window.Android.sendSMS("10086", "查询余额");
```

**sendEmail(email, subject, body)**: 调用邮件客户端。

```javascript
window.Android.sendEmail("test@me.com", "反馈", "内容如下...");
```

**openMap(lat, lng, label)**: 打开地图并标记坐标。

```javascript
window.Android.openMap(39.9042, 116.4074, "北京天安门");
```

---

### 七、 硬件控制 (4个)

**keepScreenOn(enabled)**: 设置屏幕是否保持常亮。

```javascript
window.Android.keepScreenOn(true);
```

**setScreenBrightness(brightness)**: 设置屏幕亮度 (0.0 - 1.0)。

```javascript
window.Android.setScreenBrightness(0.8);
```

**getScreenBrightness()**: 获取当前亮度。

```javascript
let b = window.Android.getScreenBrightness();
```

**setVolume(volume)**: 设置媒体音量。

```javascript
window.Android.setVolume(10);
```

---

### 八、 传感器 (2个)

**startSensor(type, callbackId)**: 开启传感器。

*   类型: `accelerometer`, `gyroscope`, `magnetometer`, `light`, `proximity`

```javascript
window.Android.startSensor("accelerometer", "sensor_acc");
// 数据会通过 "sensor_acc_data" 持续回调返回数组 [x, y, z]
```

**stopSensor()**: 停止所有传感器监听。

```javascript
window.Android.stopSensor();
```

---

### 九、 权限与设置 (3个)

**requestPermission(permission, callbackId)**: 请求 Android 权限。

```javascript
window.Android.requestPermission("android.permission.CAMERA", "perm_camera");
```

**hasPermission(permission)**: 检查是否已有权限。

```javascript
let has = window.Android.hasPermission("android.permission.READ_EXTERNAL_STORAGE");
```

**openAppSettings()**: 打开本应用的系统设置页面。

```javascript
window.Android.openAppSettings();
```

---

### 十、 高级跳转与应用控制 (6个)

**startActivity(jsonStr, callbackId)**: 高级 Intent 跳转（支持参数、Action、Flags等）。

```javascript
let config = {
    action: "android.intent.action.VIEW",
    uri: "https://youtube.com",
    package: "com.google.android.youtube"
};
window.Android.startActivity(JSON.stringify(config), "jump_yt");
```

**openSystemSetting(type)**: 快捷打开系统设置页。

*   类型: `wifi`, `bluetooth`, `display`, `battery`, `location`, `nfc`, `date`

```javascript
window.Android.openSystemSetting("wifi");
```

**reloadApp()**: 重新加载原生 Activity（类似于刷新应用）。

```javascript
window.Android.reloadApp();
```

**exitApp()**: 彻底退出应用。

```javascript
window.Android.exitApp();
```

**getCurrentTimeMillis()**: 获取系统当前时间戳（毫秒）。

```javascript
let ts = window.Android.getCurrentTimeMillis();
```

**formatDate(timestamp, format)**: 格式化时间戳。

```javascript
let timeStr = window.Android.formatDate(Date.now(), "yyyy-MM-dd HH:mm:ss");
// 输出示例: 2023-10-27 10:30:00
```

---

### 注意事项：
*   **JSON 传输**: 像 `startActivity` 和 `httpRequest` 这样接收对象的接口，必须在 JS 端先用 `JSON.stringify()` 转换为字符串。
*   **Context 要求**: 涉及 UI 操作（Dialog/Toast/Permission）的方法通常需要原生侧传入的是 `Activity` 类型的 Context，否则可能会失效。
*   **权限声明**: 即使调用了 `requestPermission`，您的 AndroidManifest.xml 中仍需预先声明对应的权限。

---

### 十一、 蓝牙功能 (Beta)
*注意：需在 AndroidManifest.xml 声明蓝牙相关权限，并动态申请权限。*

**btIsAvailable()**: 检查设备是否支持蓝牙。
```javascript
if(window.Android.btIsAvailable()) { ... }
```

**btIsEnabled()**: 检查蓝牙是否已开启。
```javascript
if(window.Android.btIsEnabled()) { ... }
```

**btEnable()**: 请求开启蓝牙。
```javascript
window.Android.btEnable();
```

**btStartScan(callbackId)**: 开始扫描 BLE 设备。
```javascript
window.Android.btStartScan("scan_callback");
// 回调数据: {name: "Device", address: "00:11...", rssi: -50}
```

**btStopScan()**: 停止扫描。
```javascript
window.Android.btStopScan();
```

**btConnect(address, callbackId)**: 连接设备。
```javascript
window.Android.btConnect("00:11:22:33:44:55", "conn_callback");
// 回调数据: {status: "connected"|"disconnected", address: "..."}
```

**btDisconnect(address, callbackId)**: 断开连接。
```javascript
window.Android.btDisconnect("00:11:22:33:44:55", "dis_callback");
```

**btGetServices(address, callbackId)**: 获取服务列表。
```javascript
window.Android.btGetServices("00:11:22:33:44:55", "svc_callback");
// 回调返回 JSON 数组
```

**btRead(address, serviceUuid, charUuid, callbackId)**: 读取特征值。
```javascript
window.Android.btRead("00:11...", "00001800...", "00002a00...", "read_cb");
// 回调数据: Base64 字符串
```

**btWrite(address, serviceUuid, charUuid, dataBase64, callbackId)**: 写入特征值。
```javascript
window.Android.btWrite("00:11...", "00001800...", "00002a00...", "SGVsbG8=", "write_cb");
```

**btNotify(address, serviceUuid, charUuid, enable, callbackId)**: 开启/关闭通知。
```javascript
window.Android.btNotify("00:11...", "00001800...", "00002a00...", true, "notify_cb");
// 回调数据: {uuid: "...", data: "Base64..."}
```
