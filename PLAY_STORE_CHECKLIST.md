# 片屿 1.0 Google Play 发布检查清单

## A. 应用身份与发布资产

- [ ] 将临时应用 ID `com.example.newandroidapp` 改为开发者拥有的永久包名；首次发布后不要再更改。
- [ ] 确认应用名“片屿”、默认语言和目标国家/地区。
- [ ] 准备 512×512 PNG 商店图标、1024×500 特性图片、手机截图和简短/完整介绍。
- [ ] 创建正式上传密钥，启用 Play App Signing，并把密钥备份到安全位置。
- [ ] 将 `versionCode`、`versionName` 与发布说明更新为正式值。
- [ ] 在 Play Console 创建内部测试轨道，先发布 AAB，不直接推生产环境。

## B. 隐私政策与数据安全

- [ ] 将 `PRIVACY.md` 发布为无需登录即可访问的 HTTPS 页面，并填写到商店隐私政策字段。
- [ ] 填写开发者联系邮箱；确保应用内和商店页面能找到隐私说明。
- [ ] 数据安全初始建议：开发者不收集或共享照片、二维码内容、光册、收藏、标签或滤镜参数；所有业务处理在本机完成。
- [ ] 在提交当天重新检查 Google Play SDK Index 中 `play-services-code-scanner` 的声明要求。Google Play 服务可能联网下载扫码模块，不能把“应用含 INTERNET 权限”误写成“完全不联网”。
- [ ] 若未来加入云同步、崩溃统计、广告或账号，先更新隐私说明与数据安全表单，再发布新版。

## C. 权限与政策说明

- [x] Android 13+ 使用 `READ_MEDIA_IMAGES`。
- [x] Android 14+ 支持 `READ_MEDIA_VISUAL_USER_SELECTED` 的部分照片访问。
- [x] `READ_EXTERNAL_STORAGE` 限制到 Android 12L 及以下。
- [x] `WRITE_EXTERNAL_STORAGE` 只在 Android 9 及以下声明。
- [x] 不声明 `CAMERA`；扫码由 Google Play 服务 Code Scanner 提供。
- [x] 不请求“所有文件访问权限” (`MANAGE_EXTERNAL_STORAGE`)。
- [ ] 在 Play Console 照片和视频权限声明中说明：核心功能是浏览、整理与编辑用户照片，因此需要持续访问用户授权的照片库。
- [ ] 若 Play 政策审查要求改用系统 Photo Picker，重新评估“读取所有照片”是否仍属于应用不可替代的核心功能。

建议权限用途说明：

> 片屿的核心功能是持续浏览用户授权的照片库、按时间和光册整理、搜索收藏并进行本地编辑。没有照片读取权限，主要照片库、整理和编辑流程均无法工作。Android 14 及以上同时支持用户只授权部分照片。

## D. 第三方与外部应用

- [x] Snapseed 仅通过公开 Android Intent 接收用户主动选择的照片或官方 QR Look 链接。
- [x] 片屿不承诺在内部复制 Snapseed 私有滤镜参数，也不逆向其格式。
- [ ] 商店介绍中将 Snapseed 描述为“可选外部编辑”，避免暗示合作、授权或隶属关系。
- [ ] 在不同品牌 Android 设备上验证 ACTION_EDIT 和 ACTION_SEND 回退路径；没有 Snapseed 时验证商店跳转。

## E. 质量门槛

- [x] 单元测试覆盖照片分组/搜索/权限判断/滤镜码编解码/裁剪数学。
- [ ] 在 API 24、28、29、33、34、36 的真机或模拟器上完成关键流程。
- [ ] 测试无照片、1 张照片、上万张照片、超大照片、HEIC/PNG/JPEG、旋转方向和损坏文件。
- [ ] 测试完整授权、部分授权、拒绝、撤销权限和从设置页返回。
- [ ] 测试保存空间不足、MediaStore 写入失败、扫码模块不可用和分享目标不存在。
- [x] 运行 `testDebugUnitTest`、`connectedDebugAndroidTest`、`lintDebug`、`assembleDebug` 和 `bundleRelease`；当前预发布 AAB 尚未配置正式上传签名。
- [ ] 使用 Android Studio App Inspection/Profiler 检查快速滚动和连续导出时的内存峰值。

## F. 正式发布前人工决策

- [ ] 确认永久包名、公司/个人主体、客服邮箱、隐私政策网址。
- [ ] 确认商店图文素材拥有版权，测试照片不含未授权人物或地理信息。
- [ ] 由账号所有者接受 Play 开发者协议、内容分级、目标受众、广告和数据安全声明。
- [ ] 内部测试至少一轮无阻断问题后，再逐步扩大到封闭测试与生产分阶段发布。
