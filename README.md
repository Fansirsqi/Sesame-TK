![Sesame-TK](https://socialify.git.ci/myblsky66/Sesame-TK/image?description=1&font=Source%20Code%20Pro&forks=1&issues=1&logo=https%3A%2F%2Fraw.githubusercontent.com%2Fmyblsky66%2FSesame-TK%2Frefs%2Fheads%2Fmain%2Fapp%2Fsrc%2Fmain%2Fassets%2Fweb%2FSesame-TK-logo.svg&name=1&owner=1&pattern=Circuit%20Board&pulls=1&stargazers=1&theme=Auto)




> 👉懒真人版本: [Sesame](https://github.com/LazyImmortal/Sesame)

> 👉本项目fork Fansirsqi: [SesameTK](https://github.com/Fansirsqi/Sesame)

## 💻本人不是专业的开发者，仅仅是一名热爱开源的爱好者

### 📢欢迎大家提出宝贵意见，共同完善此项目

> ### ~~墙内不再更新~~ 有缘再见~

> ### 非官方编译，别问安装的时候为啥签名冲突，因为我不会！

🤖 代码的大部分内容是通过 `GPT-4o` 模型的辅助来完成的。

🐛 `BUG`和功能建议和请提交 [ISSUE](https://github.com/Fansirsqi/Sesame-TK/issues/new/choose)

🙁 相关问题，我也可能不会修复,大家轻喷，因为我不是专业的开发者，我可能真的不会修复。

❓ 访问异常请手动开启 平衡网络延迟选项，设置适当的延迟时间以及查询时间

☀️ 对了，我自己用的支付宝版本是`10.6.88.9100`

💊 如果你想自己编译，请fork本项目

📕 开发者或用户请看[维基](https://github.com/Fansirsqi/Sesame-TK/wiki)

然后在仓库设置相关签名文件信息，使用GitHub Actions编译，下载编译好的APK文件，安装到手机上即可  
签名的生成以及转码请自行🔍解决 很简单滴~，你绝B可以
酷安搜懒真人，他发过教程！

| 仓库变量名                      | 变量值                          |  
| 签名变量名                      | 变量值                          |  
|----------------------------|------------------------------|  
| `ANDROID_SIGNING_KEY`      | `keystore.jks`文件的base64编码字符串 |  
| `ANDROID_KEY_ALIAS`        | `keystore.jks`文件别名           |  
| `ANDROID_SIGNING_PASSWORD` | `keystore.jks`文件密码           |  
| `ANDROID_KEY_PASSWORD`     | `keystore.jks`文件密码           |  

设置好这些后，去仓库新建一个release，随便新建一个tag，然后点击`Publish release`，GitHub Actions会自动编译并发布APK文件到release中，下载安装即可
<details> <summary>TG BOT配置教程</summary>   
<h3>创建 Telegram Bot</h3>  
1.私聊 @BotFather
<br>2.发送 /newbot 创建新 bot → 获取 TG_BOT_TOKEN
<br>3.获取 Chat ID：
<br>4.将 bot 添加到群组/频道
<br>访问:https://api.telegram.org/bot<TG_BOT_TOKEN>/getUpdates
<br>例如:https://api.telegram.org/bot123456:abcdefg/getUpdates
<br>找到"sender_chat": {"id": -这是一串负数, 或者 "chat": {"id": -这是一串负数,
<br>查找响应中的 "id" 字段 → 即 TG_CHAT_ID

| TG变量名                      | 变量值                          |  
|----------------------------|------------------------------|  
| `TG_CHAT_ID`      | `-100123456789`群ID |  
| `TG_BOT_TOKEN`        | `k123456：abcdefg`密钥           |  



</details>  
<details>  
<summary>Preview Images</summary>  

@@ -114,32 +128,30 @@

## 授权说明

本项目fork自  
基于 [constanline版XQuickEnergy](https://github.com/constanline/XQuickEnergy)  
与 [pansong291版XQuickEnergy](https://github.com/pansong291/XQuickEnergy)  
本项目fork自  基于 [constanline版XQuickEnergy](https://github.com/constanline/XQuickEnergy)  与 [pansong291版XQuickEnergy](https://github.com/pansong291/XQuickEnergy)  
开发的项目[Sesame-TK](https://github.com/TKaxv-7S/Sesame-TK)  
并且在其基础上进行了少量的功能改进与优化。得益于GPT4-o模型的强大能力使得本项目能有这么多提交  
但是不确定是否是有效修改，请自行斟酌考虑使用。

所有图片由 ༒激༙྇流༙྇泉༙྇༒ 授权使用

## 协议说明
自 **北京时间2024年7月15日开始** 提交的所有代码 ，遵循 GPLv3 协议，并禁止用于任何商业用途、禁止二次修改后闭源发布

## 鸣谢

<a href="https://github.com/Fansirsqi/Sesame-TK/graphs/contributors">  
  <img src="https://contrib.rocks/image?repo=Fansirsqi/Sesame-TK"  alt="contributors"/>  
</a>  

贡献名单使用 [contrib.rocks](https://contrib.rocks) 生成

## Star History

<a href="https://star-history.com/#Fansirsqi/Sesame-TK&Timeline">  
 <picture>  
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Fansirsqi/Sesame-TK&type=Timeline&theme=dark" />  More actions
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=Fansirsqi/Sesame-TK&type=Timeline" />  
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=Fansirsqi/Sesame-TK&type=Timeline" />  
 </picture>  
</a>
