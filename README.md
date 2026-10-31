# 图片转 PDF · Pic2PDF

> 多图拼版 / 长图拼接 / 可见水印 / 隐形水印 —— 一个 Android 原生小工具，iOS 26 液态玻璃风界面，无第三方依赖，单文件即可编译。

![风格](https://img.shields.io/badge/UI-iOS%2026%20Liquid%20Glass-0A84FF) ![版本](https://img.shields.io/badge/version-1.0-brightgreen) ![minSdk](https://img.shields.io/badge/minSdk-26-blue)

## 它做什么

把相册里的图片合成一个 PDF，排版、页型、水印全部可控。

| 能力 | 说明 |
| --- | --- |
| 一页一图 | 每张图片独占一页，完整显示或裁剪填满 |
| 网格拼版 | 2 / 4 / 6 / 9 张一页，自动等分 |
| 长图拼接 | 多图竖向连成长页，超长自动换页 |
| 页面比例 | A4 竖、A4 横、Letter、16:9、1:1、跟随原图 |
| 细节调节 | 页边距、图片间距、圆角、100~300 DPI |
| 排序 | 点缩略图 → 上移 / 下移 / 删除 |
| 实时预览 | 首屏就有第一页效果预览 |

## 水印

**可见水印**
- 8 种内置字体：默认 / 细体 / 中粗 / 特黑 / 衬线 / 等宽 / 手写 / 圆润
- 支持导入本机 `.ttf` / `.otf` 字体文件
- 字号（页宽百分比）、不透明度、旋转角度、6 种颜色
- 布局：斜向平铺 / 居中单条 / 四角 / 底部 / 斜纹多行
- 图片水印：任意图片当 Logo，控制尺寸、透明度、五个落点

**隐形水印（三档，可叠加）**

| 模式 | 原理 | 谁能读到 |
| --- | --- | --- |
| 元数据 | PDF `/Subject` 写入 `P2PW1:<base64>` | 任何 PDF 信息查看器 |
| 同色隐藏层 | 以底色墨水的文字画在页边 | 取色放大 / OCR |
| 像素 LSB | base64 写进每像素 B 通道最低位，无损 Flate 存储 | 本 App 的「校验」功能或自写脚本 |

导出后可在 App 内点「校验一个 PDF 的隐形水印」，选中 PDF 即可把元数据和 LSB 内容原样读回来 —— 自己给自己的溯源链闭环。

## 安装

- 直接装 APK（Release 页或仓库附件）
- 包名 `com.starlab.pic2pdf`，minSdk 26，targetSdk 34

## 从源码构建

不需要 Gradle / AndroidX。仓库里的 `build-ondevice.sh` 是「在手机上直接编译」的脚本，依赖：

```
aapt2 · kotlinc · D8(R8) · apksig · android.jar(platform 34)
```

```bash
bash build-ondevice.sh
# 产物：build-p2p/pic2pdf-signed.apk
```

脚本默认从 `.probe/` 读取工具链，改 `P=` 前缀即可指向你自己的 build-tools 目录。

## 代码结构

```
app/src/main/java/com/starlab/pic2pdf/
├── MainActivity.kt   界面与交互（原生 View，无 XML 布局）
├── Glass.kt          液态玻璃主题：玻璃卡 / 胶囊 / 主按钮
├── Backdrop.kt       动态光斑背景（RenderEffect 软模糊）
├── Compositor.kt     排版引擎：页计划 + 画布合成
├── Watermarker.kt    可见水印 / 同色隐藏层 / Logo 水印
├── Lsb.kt            像素最低位隐写
├── Pdf.kt            自写最小 PDF 写出器（含 /Info）
├── PdfProbe.kt       从 PDF 取回隐形水印
├── Exporter.kt       渲染 → 编码 → 落盘，MediaStore 保存
└── Imgs.kt / Model.kt / UI.kt / PreviewView.kt / Fonts.kt
```

## 设计说明

- 背景是三层径向光斑 + 54px RenderEffect 模糊，缓慢漂移；卡片是半透明白玻璃 + 1px 高光描边，整体保持「清透」而不是「浓重」。
- 渲染全部走 Canvas，水印、圆角、裁剪都在合成阶段完成，PDF 只负责装页 —— 所以任意字体/任意布局都能原样进文件。
- PDF 结构手写：Catalog → Pages → (Page + Content + Image) × N → Info，交叉引用表自己算偏移，导出后可被任何阅读器打开。

## License

MIT

