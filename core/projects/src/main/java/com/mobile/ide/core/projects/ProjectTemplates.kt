// Copyright 2025 Thomas Schmid
package com.mobile.ide.ui.projects

object ProjectTemplates {

    val normalIndexHtml =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>My Website</title>
            <link rel="stylesheet" href="css/style.css">
        </head>
        <body>
            <div class="container">
                <h1>Hello World</h1>
                <p>This is a standard Web project</p>
                <button id="clickBtn">Click Me</button>
                <p id="output"></p>
            </div>
            <script src="js/script.js"></script>
        </body>
        </html>
        """
            .trimIndent()

    val normalCss =
        """
        body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f0f2f5; }
        .container { background: white; padding: 2rem; border-radius: 8px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        button { background-color: #007bff; color: white; border: none; padding: 10px 20px; border-radius: 4px; font-size: 16px; }
        """
            .trimIndent()

    val normalJs =
        """
        document.getElementById('clickBtn').addEventListener('click', function() {
            document.getElementById('output').innerText = 'Time: ' + new Date().toLocaleTimeString();
        });
        """
            .trimIndent()

    val apiJs =
        """
        window.requestCallbacks = {};
        window.onAndroidResponse = function(id, b64) {
            const cb = window.requestCallbacks[id];
            if(!cb) return;
            try {
                const res = JSON.parse(decodeURIComponent(escape(window.atob(b64))));
                res.success ? cb.resolve(res.data) : cb.reject(res.data);
            } catch(e) { cb.reject(e.message); }
            delete window.requestCallbacks[id];
        };
        const call = (m, ...a) => new Promise((res, rej) => {
            if(!window.Android || !window.Android[m]) return rej("Native API not found");
            const id = 'cb_'+Math.random();
            window.requestCallbacks[id] = {resolve: res, reject: rej};
            window.Android[m](...a, id);
        });
        window.NativeAPI = {
            toast: (m) => window.Android?.showToast(m),
            vibrate: (ms=50) => window.Android?.vibrate(ms),
            share: (t) => window.Android?.shareText(t),
            openBrowser: (u) => window.Android?.openBrowser(u),
            info: () => { try { return JSON.parse(window.Android.getDeviceInfo()); } catch(e){ return null; } }
        };
        """
            .trimIndent()

    val webAppIndexHtml =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Universal Camera</title>
            <link rel="stylesheet" href="css/style.css">
            <style>
                #video-container { width: 100%; max-width: 400px; height: 300px; background: #000; margin: 10px auto; display: none; }
                video { width: 100%; height: 100%; object-fit: cover; }
                #fallback-container { display: none; margin: 20px; }
                .btn { padding: 10px 20px; background: #007bff; color: white; border-radius: 5px; text-decoration: none; display: inline-block; cursor: pointer; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Universal Camera Demo</h1>
                <p>Compatible with PC browsers, mobile browsers & Android WebView</p>
                
                <div id="video-container">
                    <video id="video" autoplay playsinline></video>
                </div>
                <button class="btn" onclick="startCamera()">Try opening camera (Live Stream)</button>
                
                <hr>

                <p>If the live stream above fails, please use the button below:</p>
                <label class="btn">
                    📷 Take Photo / Upload
                    <input type="file" accept="image/*" capture="environment" style="display:none" onchange="handleFile(this)">
                </label>
                
                <div id="preview-img" style="margin-top:10px"></div>
                <p id="log" style="color:red; font-size: 12px;"></p>
            </div>

            <script src="js/api.js"></script>
            <script>
                function log(msg) { document.getElementById('log').innerText = msg; console.log(msg); }

                async function startCamera() {
                    try {
                        const constraints = { video: { facingMode: "environment" } };
                        const stream = await navigator.mediaDevices.getUserMedia(constraints);
                        const video = document.getElementById('video');
                        video.srcObject = stream;
                        document.getElementById('video-container').style.display = 'block';
                        log("Camera started successfully (Stream Mode)");
                    } catch (err) {
                        log("Live stream failed to start: " + err.name + " - " + err.message + "\nIt is recommended to use the [Take Photo / Upload] button below");
                    }
                }

                function handleFile(input) {
                    if (input.files && input.files[0]) {
                        const reader = new FileReader();
                        reader.onload = function (e) {
                            const img = document.createElement('img');
                            img.src = e.target.result;
                            img.style.maxWidth = '100%';
                            img.style.marginTop = '10px';
                            const container = document.getElementById('preview-img');
                            container.innerHTML = '';
                            container.appendChild(img);
                            log("Image retrieved successfully (Input Mode)");
                        }
                        reader.readAsDataURL(input.files[0]);
                    }
                }
            </script>
        </body>
        </html>
        """
            .trimIndent()

    val webAppIndexJs =
        """
        const info = NativeAPI.info();
        if(info) document.getElementById('info').innerText = `Running on ${'$'}{info.model}`;
        """
            .trimIndent()

    val webAppCss =
        """
        body { font-family: sans-serif; padding: 20px; text-align: center; }
        button { margin: 10px; padding: 10px 20px; font-size: 16px; display: block; width: 100%; }
        """
            .trimIndent()

    fun getConfigFile(packageName: String, appName: String, targetUrl: String): String =
        """
{
  "name": "$appName",
  "package": "$packageName",
  "versionName": "1.0.0",
  "versionCode": 1,
  
  "orientation": "portrait",
  "fullscreen": false,
  "targetUrl": "$targetUrl",
  "icon": "icon.png",
  
  "statusBar": {
    "backgroundColor": "#FFFFFF",
    "style": "dark",
    "translucent": false,
    "hidden": false
  },
  
  "webview": {
    "zoomEnabled": false,
    "javascriptEnabled": true,
    "domStorageEnabled": true,
    "allowFileAccess": true,
    "textZoom": 100,
    "userAgent": ""
  },

  "permissions": [
    "android.permission.INTERNET",
    "android.permission.VIBRATE",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.CAMERA",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.RECORD_AUDIO"
  ]
}
    """
            .trimIndent()

    fun getSimpleConfigFile(packageName: String, appName: String, targetUrl: String): String =
        """
{
  "name": "$appName",
  "package": "$packageName",
  "versionName": "1.0.0",
  "versionCode": 1,
  
  "orientation": "portrait",
  "fullscreen": false,
  "targetUrl": "$targetUrl",
  
  "statusBar": {
    "backgroundColor": "#FFFFFF",
    "style": "dark",
    "translucent": false,
    "hidden": false
  },
  
  "webview": {
    "zoomEnabled": false,
    "javascriptEnabled": true,
    "domStorageEnabled": true,
    "allowFileAccess": true,
    "textZoom": 100
  },

  "permissions": [
    "android.permission.INTERNET"
  ]
}
    """
            .trimIndent()

    fun getStatusBarDemoConfig(packageName: String, appName: String): String =
        """
{
  "name": "$appName Status Bar Demo",
  "package": "$packageName",
  "versionName": "1.0.0",
  "versionCode": 1,
  
  "orientation": "portrait",
  "fullscreen": false,
  "targetUrl": "index.html",
  
  "statusBar": {
    "backgroundColor": "#FF5722",
    "style": "light",
    "translucent": true,
    "hidden": false
  },
  
  "webview": {
    "zoomEnabled": false,
    "javascriptEnabled": true,
    "domStorageEnabled": true,
    "allowFileAccess": true,
    "textZoom": 100
  },

  "permissions": [
    "android.permission.INTERNET"
  ]
}
    """
            .trimIndent()
}
