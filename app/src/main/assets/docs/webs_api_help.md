# WebS Api 帮助文档

## 简介与注意事项
1. `AndroidManifest.xml` 是配置清单，用于打包配置
2. `websApp` 是浏览器内置框架，用于调用安卓 Api
3. 运行环境为 Js, 项目必须是 APK 类型:
```html
<script>
    websApp.toast("你好");
</script>
```
4. 路径相关简化
    - `@` 代表 assets 目录: `@index.html` 代表 `assets/index.html`
    - `%` 代表 sdcard 目录: `%a.txt` 代表 `sdcard/a.txt`
    - `$` 代表内部存储目录: `$a.txt` 代表 `/data/data/com.webs.app/files/a.txt` (打包后生效)
    - `#` 代表下载目录: `#a.txt` 代表 `/storage/emulated/0/Download/a.txt`

# WebS Api 六大类代码示例

## 一、浏览器相关

### evalJavaCode
执行 Beanshell Java 代码, Beanshell Java 代码是 Java 代码的简化版，无面向对象。
已内置变量 `activity`, `thisWebView`。

```javascript
var javaCode = `
import android.widget.Toast;
int a = 123; a ++;
Toast.makeText(activity, "结果"+ a, Toast.LENGTH_LONG).show();
`;
websApp.evalJavaCode(javaCode);
```

### openInApp
开启打开其他应用请求

```javascript
websApp.openInApp();
```

### closeOpenInApp
关闭打开其他应用请求

```javascript
websApp.closeOpenInApp();
```

### openLongClickImage
开启长按图片功能

```javascript
websApp.openLongClickImage();
```

### closeLongClickImage
关闭长按图片功能

```javascript
websApp.closeLongClickImage();
```

### openNoImageMode
开启无图浏览模式

```javascript
websApp.openNoImageMode();
```

### closeNoImageMode
关闭无图浏览模式

```javascript
websApp.closeNoImageMode();
```

### cleanCache
清空 WebView 的缓存

```javascript
websApp.cleanCache();
```

### evalJsCode
执行 JS 代码

```javascript
websApp.evalJsCode("alert('Hello World');");
```

### addJScode
添加 js 注入代码。此函数用于向指定的 URL 注入 JavaScript 代码。

```javascript
var url = "http://m.baidu.com";
window.location.href = url;
websApp.addJScode(url , "alert(123)");
```

## 二、UI 交互相关

### toast
弹出一个短时间显示的消息提示

```javascript
websApp.toast("你好");
```

### showDialog
弹出一个自定义对话框

```javascript
websApp.showDialog("标题", "内容你好", "确定", "取消",
 "alert('点击了确定')", "alert('点击了取消')");
websApp.showDialog("标题","内容你好");
```

### sendNotification
发送通知

```javascript
//标题，内容，通知分类Id，通知分类名，点击通知后执行的Js代码
websApp.sendNotification("标题", "内容", "ch_id", "聊天消息类", "alert(123)");
//标题，内容，点击通知后执行的Js代码
websApp.sendNotification("标题", "内容", "alert(TZ_message)");
//标题，内容
websApp.sendNotification("标题", "内容");
```
> ps: JS 执行代码中，可快速调用下列变量:
> - `TZ_title`: 通知的标题
> - `TZ_message`: 通知的内容
> - `TZ_CHANNEL_ID`: 通知分类 ID
> - `TZ_CHANNEL_NAME`: 通知分类名称

### shareText
分享文本内容

```javascript
websApp.shareText("这是要分享的文本内容");
```

### openActivity
打开一个新的 Activity

```javascript
websApp.openActivity("com.webs.app.manager.mine.ActivitySettings");
```

### setStatuColor
设置状态栏颜色

```javascript
websApp.setStatuColor("#FF0000"); // 设置状态栏颜色为红色
```

### setNavColor
设置导航栏颜色

```javascript
websApp.setNavColor("#0000FF"); // 设置导航栏颜色为蓝色
```

### joinQFriend
弹出添加 QQ 好友的界面

```javascript
websApp.joinQFriend("123456789"); // 弹出添加QQ号为123456789的好友界面
```

### joinQun
弹出加入 QQ 群的界面

```javascript
websApp.joinQun("123456789"); // 弹出加入QQ群号为123456789的界面
```

### openFlashLight
打开手电筒

```javascript
websApp.openFlashLight();
```

### closeFlashLight
关闭手电筒

```javascript
websApp.closeFlashLight();
```

### setWallpaper
设置壁纸

```javascript
websApp.setWallpaper("/sdcard/a.jpg");
```

### isVpnActive
检测 VPN

```javascript
var isHaveVpn = websApp.isVpnActive();
```

### isHook
检测是否被 HOOK

```javascript
var isHooked = websApp.isHook();
```

### checkFloatPermission
检查悬浮窗权限

```javascript
var isHaveFloatPermisson = websApp.checkFloatPermission();
```

### requestFloatPermission
请求悬浮窗权限

```javascript
websApp.requestFloatPermission();
```

### showFloatView
开启悬浮窗
> 悬浮窗的 websApp 暂时使用不能

```javascript
//参数: 目标url，宽度，高度，x，y
websApp.showFloatView("http://m.baidu.com", -1,800,  0, 0);
```

### getCpuTemperature
获取 CPU 温度

```javascript
var cpu = websApp.getCpuTemperature();
```

### getBatteryTemperature
获取电池温度

```javascript
var bat = websApp.getBatteryTemperature();
```

### openWifi
开启 WIFI

```javascript
websApp.openWifi();
```

### closeWifi
关闭 WIFI

```javascript
websApp.closeWifi();
```

### setLandscape
当前界面横屏

```javascript
websApp.setLandscape();
```

### setPortrait
当前界面竖屏

```javascript
websApp.setPortrait();
```

### setFullScreen
设置全屏

```javascript
websApp.setFullScreen();
```

### cancelFullScreen
取消全屏

```javascript
websApp.cancelFullScreen();
```

### setUserAgent
设置 UA

```javascript
websApp.setUserAgent("xxxx");
```

## 三、文件系统相关

### getStoragePermission
检查应用是否有存储权限

```javascript
var hasPermission = websApp.getStoragePermission();
```

### requestStoragePermissions
请求存储权限

```javascript
websApp.requestStoragePermissions();
```

### listFiles
获取指定文件夹下所有文件的名称，文件名之间用 `[FG]` 分隔

```javascript
var files = websApp.listFiles("/sdcard/Download/");
```

### deletFiles
删除文件或文件夹

```javascript
websApp.deletFiles("/sdcard/Download/file.txt");
```

### writeFile
写入文件内容

```javascript
websApp.writeFile("/sdcard/Download/file.txt", "你好");
```

### readFile
读取文件内容

```javascript
var content = websApp.readFile("/sdcard/Download/file.txt");
```

### renameFile
重命名文件

```javascript
websApp.renameFile("/sdcard/Download/file.txt", "newfile.txt");
```

### openFile
打开文件

```javascript
websApp.openFile("file:///android_asset/index.html");
```

### fileExists
检查文件或文件夹是否存在

```javascript
var exists = websApp.fileExists("/sdcard/Download/file.txt");
```

### getFileByBase64
以 Base64 编码读取文件

```javascript
var content = websApp.getFileByBase64("/sdcard/Download/file.txt");
```

### saveFileByBase64
以 Base64 编码保存文件

```javascript
var success = websApp.saveFileByBase64("/sdcard/Download/file.txt", "dGVzdCBkYXRh");
```

### getSDdir
获取设备存储路径/文件绝对路径

```javascript
var sdDir = websApp.getSDdir();
var abPath  = websApp.getSDdir("WebS/Cache");
```

### fileCopy
复制文件/文件夹

```javascript
var success = websApp.fileCopy(websApp.getSDdir("WebS/Cache"), websApp.getSDdir("B/Cache"));
```

### copyApkFileToSd
复制 Apk 文件到 SD 目录

```javascript
var success = websApp.copyApkFileToSd("index.html", "/sdcard/index.html");
```

### setLastModifiedTime
设置文件最后时间为当前

```javascript
var success = websApp.setLastModifiedTime(websApp.getSDdir("WebS/Cache"));
```

### zipFile
压缩文件

```javascript
var success = websApp.zipFile(websApp.getSDdir("WebS/Cache"), websApp.getSDdir("WebS/Cache.zip"));
```

### zipFileWithPassword
压缩文件，带密码

```javascript
var success = websApp.zipFileWithPassword(websApp.getSDdir("WebS/Cache"), websApp.getSDdir("WebS/Cache.zip"), "123456");
```

### unzipFile
解压文件

```javascript
var success = websApp.unzipFile(websApp.getSDdir("WebS/Cache.zip"), websApp.getSDdir("WebS/Cache"));
```

### unzipFileWithPassword
解压文件，带密码

```javascript
var success = websApp.unzipFileWithPassword(websApp.getSDdir("WebS/Cache.zip"), websApp.getSDdir("WebS/Cache"), "123456");
```

### getMediaMetadata
获取指定媒体文件的元数据

```javascript
var metadata = websApp.getMediaMetadata("/sdcard/Download/image.jpg");
```

### downloadFile
下载文件

```javascript
websApp.downloadFile("https://example.com/file.txt", "file.txt");
```

### clearCache
清理缓存目录

```javascript
websApp.clearCache();
```

## 四、App 操作相关

### exitApp
退出应用

```javascript
websApp.exitApp();
```

### restartApp
重启应用

```javascript
websApp.restartApp();
```

### getAppName
获取应用名称

```javascript
var appName = websApp.getAppName();
```

### getAppPackageName
获取应用的包名

```javascript
var packageName = websApp.getAppPackageName();
```

### getAppVersion
获取应用的版本号

```javascript
var appVersion = websApp.getAppVersion();
```

### getAppVersionCode
获取应用的版本号（code）

```javascript
var appVersionCode = websApp.getAppVersionCode();
```

### isAppInstalled
判断某个应用是否已安装

```javascript
var isInstalled = websApp.isAppInstalled("com.example.app");
```

### launchApp
启动另一个应用

```javascript
websApp.launchApp("com.example.app");
```

### installAPK
安装 APK 文件

```javascript
websApp.installAPK("/sdcard/download/app.apk");
```

### block
判断 Apk 状态栏通知是否被屏蔽

```javascript
var isBlocked = websApp.block();
```

### gotoApkNotice
跳转到 Apk 通知管理界面

```javascript
websApp.gotoApkNotice();
```

### openURLInBrowser
调用外部浏览器打开 URL

```javascript
websApp.openURLInBrowser("https://www.example.com");
```

## 六、设置相关

### getClipContent
获取剪切板内容

```javascript
var clipContent = websApp.getClipContent();
```

### setClipContent
设置剪切板内容

```javascript
websApp.setClipContent("需要复制到剪切板的文本");
```

### getOSID
获取安卓 OS 版本

```javascript
var osVersion = websApp.getOSID();
```

### getNetworkStatus
获取网络状态

```javascript
var networkStatus = websApp.getNetworkStatus();
```

### getEquipmentModel
获取设备型号

```javascript
var equipmentModel = websApp.getEquipmentModel();
```

### shockDevice
振动设备

```javascript
websApp.shockDevice(500); // 振动设备500毫秒
```

### getMediaVolume
获取音量

```javascript
var mediaVolume = websApp.getMediaVolume();
```

### setMediaVolume
设置系统音量

```javascript
websApp.setMediaVolume(5); // 设置系统音量为5
```

### getScreenBrightness
获取屏幕亮度

```javascript
var brightness = websApp.getScreenBrightness();
```

### setScreenBrightness
设置屏幕亮度

```javascript
websApp.setScreenBrightness(50);
```

### getMemoryUsage
获取应用的内存占用

```javascript
var memoryUsage = websApp.getMemoryUsage();
```

### getTotalMemory
获取设备的总内存

```javascript
var totalMemory = websApp.getTotalMemory();
```

### getDeviceStatus
获取设备的当前运行状态

```javascript
var deviceStatus = websApp.getDeviceStatus();
```

### getDeviceId
获取设备的唯一 ID

```javascript
var deviceId = websApp.getDeviceId();
```

### getIMEI
获取设备的 IMEI（需要权限）

```javascript
var imei = websApp.getIMEI();
```

### openSettings
打开设置界面

```javascript
websApp.openSettings();
```
