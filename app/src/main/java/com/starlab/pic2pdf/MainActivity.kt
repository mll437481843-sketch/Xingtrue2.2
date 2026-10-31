package com.starlab.pic2pdf

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private lateinit var s: S
    private lateinit var content: LinearLayout
    private lateinit var preview: PreviewView
    private lateinit var thumbRow: LinearLayout
    private lateinit var pageLabel: TextView
    private lateinit var exportBtn: TextView
    private val ui = Handler(Looper.getMainLooper())
    private var thumbVer = 0
    private var previewJob = 0

    companion object {
        const val REQ_PICS = 11
        const val REQ_LOGO = 12
        const val REQ_FONT = 13
        const val REQ_PDF = 14
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.setStatusBarColor(Color.TRANSPARENT)
        window.setNavigationBarColor(Color.TRANSPARENT)
        s = Prefs.load(this)
        buildUi()
    }

    // ---------------- UI ----------------

    private fun buildUi() {
        val root = FrameLayout(this)
        root.addView(Backdrop(this), FrameLayout.LayoutParams(-1, -1))

        val scroll = ScrollView(this)
        scroll.isFillViewport = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        val hp = G.dp(this, 16f)
        content.setPadding(hp, G.dp(this, 8f), hp, G.dp(this, 130f))
        scroll.addView(content, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        // 顶部标题
        val head = LinearLayout(this)
        head.orientation = LinearLayout.VERTICAL
        head.setPadding(G.dp(this, 4f), G.dp(this, 10f), G.dp(this, 4f), G.dp(this, 12f))
        val t1 = UI.text(this, "图片转 PDF", 27f, G.ink, true)
        head.addView(t1)
        val t2 = UI.text(this, "多图拼版 · 长图拼接 · 可见/隐形水印", 12.5f, G.ink2)
        t2.setPadding(0, G.dp(this, 6f), 0, 0)
        head.addView(t2)
        content.addView(head, LinearLayout.LayoutParams(-1, -2))

        buildPicsCard()
        buildLayoutCard()
        buildWatermarkCard()
        buildInvisibleCard()
        buildOutputCard()
        buildPreviewCard()

        buildBottomBar(root)
        setContentView(root)

        applyInsets(root)
        refreshAll()
    }

    private fun applyInsets(root: View) {
        root.setOnApplyWindowInsetsListener { v, insets ->
            var top = 0
            var bottom = 0
            if (Build.VERSION.SDK_INT >= 30) {
                val i = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                top = i.top
                bottom = i.bottom
            } else {
                top = insets.systemWindowInsetTop
                bottom = insets.systemWindowInsetBottom
            }
            v.setPadding(0, top, 0, 0)
            findViewById<View>(1001)?.setPadding(0, 0, 0, bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= 21) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        root.requestApplyInsets()
    }

    private fun addCard(v: View, top: Float = 12f) {
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = G.dp(this, top)
        content.addView(v, lp)
    }

    private fun buildPicsCard() {
        val card = UI.card(this, "图片", "最多几十张，点缩略图可排序或删除")
        val hsv = HorizontalScrollView(this)
        hsv.isHorizontalScrollBarEnabled = false
        hsv.clipToPadding = false
        hsv.setPadding(0, G.dp(this, 12f), 0, 0)
        thumbRow = LinearLayout(this)
        thumbRow.orientation = LinearLayout.HORIZONTAL
        hsv.addView(thumbRow, ViewGroup.LayoutParams(-2, -2))
        card.addView(hsv, LinearLayout.LayoutParams(-1, -2))
        addCard(card, 12f)
    }

    private fun buildLayoutCard() {
        val card = UI.card(this, "排版", "选页型、网格、边距与圆角")

        card.addView(head("模式"))
        val modes = UI.Seg(this, listOf("一页一图", "网格拼版", "长图拼接"))
        modes.selected = when (s.mode) { "grid" -> 1; "long" -> 2; else -> 0 }
        modes.onSelect = { i ->
            s.mode = when (i) { 1 -> "grid"; 2 -> "long"; else -> "single" }
            save(); refreshPreview()
        }
        card.addView(modes)

        card.addView(head("每页张数"))
        val per = UI.Seg(this, listOf("2", "4", "6", "9"))
        per.selected = when (s.perPage) { 2 -> 0; 6 -> 2; 9 -> 3; else -> 1 }
        per.visibility = if (s.mode == "grid") View.VISIBLE else View.GONE
        per.onSelect = { i ->
            s.perPage = intArrayOf(2, 4, 6, 9)[i]
            save(); refreshPreview()
        }
        card.addView(per)
        modes.onSelect = { i ->
            s.mode = when (i) { 1 -> "grid"; 2 -> "long"; else -> "single" }
            per.visibility = if (i == 1) View.VISIBLE else View.GONE
            save(); refreshPreview()
        }

        card.addView(head("页面比例"))
        val ratios = UI.Seg(this, listOf("A4 竖", "A4 横", "Letter", "16:9", "1:1", "跟随原图"))
        ratios.selected = when (s.ratio) {
            "a4l" -> 1; "letter" -> 2; "16x9" -> 3; "1x1" -> 4; "auto" -> 5; else -> 0
        }
        ratios.onSelect = { i ->
            s.ratio = listOf("a4p", "a4l", "letter", "16x9", "1x1", "auto")[i]
            save(); refreshPreview()
        }
        card.addView(ratios)

        card.addView(head("图片适配"))
        val fits = UI.Seg(this, listOf("完整显示", "裁剪填满"))
        fits.selected = if (s.fit == "cover") 1 else 0
        fits.onSelect = { i -> s.fit = if (i == 1) "cover" else "contain"; save(); refreshPreview() }
        card.addView(fits)

        val m = UI.Slider(this, "页边距", 0, 15, s.margin.toInt(), " %")
        m.onChange = { v -> s.margin = v.toFloat(); save(); refreshPreview() }
        card.addView(m, topMargin(6f))

        val gp = UI.Slider(this, "图片间距", 0, 10, s.gap.toInt(), " %")
        gp.onChange = { v -> s.gap = v.toFloat(); save(); refreshPreview() }
        card.addView(gp)

        val cr = UI.Slider(this, "圆角", 0, 8, (s.corner * 2).toInt(), " 档")
        cr.onChange = { v -> s.corner = v / 2f; save(); refreshPreview() }
        card.addView(cr)

        addCard(card)
    }

    private fun buildWatermarkCard() {
        val card = UI.card(this, "可见水印", "文字多字体 / 图片 Logo")
        val sw = Switch(this)
        sw.text = "开启可见水印"
        sw.isChecked = s.wmOn
        sw.setTextColor(G.ink)
        sw.setTextSize(13f)
        sw.setPadding(0, G.dp(this, 8f), 0, 0)
        sw.setOnCheckedChangeListener { _, v -> s.wmOn = v; save(); refreshPreview() }
        card.addView(sw)

        card.addView(head("水印文字"))
        val et = UI.input(this, "输入水印文字", s.wmText)
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) {
                s.wmText = e?.toString() ?: ""
                save(); refreshPreview()
            }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        card.addView(et, topMargin(8f))

        card.addView(head("字体"))
        val families = listOf("sans-serif", "light", "medium", "black", "serif", "mono", "cursive", "casual")
        val names = listOf("默认", "细体", "中粗", "特黑", "衬线", "等宽", "手写", "圆润")
        val fonts = families.map { Typeface.create(Fonts.familyOf(it), Typeface.NORMAL) }
        val fseg = UI.Seg(this, names, fonts)
        fseg.selected = families.indexOf(s.wmFont).coerceAtLeast(0)
        fseg.onSelect = { i -> s.wmFont = families[i]; s.wmFontPath = null; save(); refreshPreview(); rebuild() }
        card.addView(fseg)
        val importBtn = UI.smallButton(this, "导入字体文件 (.ttf/.otf)") { pickFont() }
        card.addView(importBtn, topMargin(10f))
        val curFont = UI.text(this, "当前字体：" + (s.wmFontPath?.let { File(it).name } ?: names[families.indexOf(s.wmFont).coerceAtLeast(0)]), 11.5f, G.ink3)
        curFont.setPadding(0, G.dp(this, 8f), 0, 0)
        card.addView(curFont)

        card.addView(head("水印样式"))
        val lays = UI.Seg(this, listOf("斜向平铺", "居中单条", "四角", "底部", "斜纹多行"))
        lays.selected = listOf("tile", "center", "corner", "bottom", "diag").indexOf(s.wmLayout).coerceAtLeast(0)
        lays.onSelect = { i -> s.wmLayout = listOf("tile", "center", "corner", "bottom", "diag")[i]; save(); refreshPreview() }
        card.addView(lays)

        val sz = UI.Slider(this, "字号", 2, 20, s.wmSize.toInt(), " %")
        sz.onChange = { v -> s.wmSize = v.toFloat(); save(); refreshPreview() }
        card.addView(sz, topMargin(6f))

        val al = UI.Slider(this, "不透明度", 5, 100, (s.wmAlpha * 100).toInt(), " %")
        al.onChange = { v -> s.wmAlpha = v / 100f; save(); refreshPreview() }
        card.addView(al)

        val ang = UI.Slider(this, "角度", -90, 90, s.wmAngle.toInt(), "°")
        ang.onChange = { v -> s.wmAngle = v.toFloat(); save(); refreshPreview() }
        card.addView(ang)

        card.addView(head("颜色"))
        val colors = listOf(Color.WHITE, Color.BLACK, 0xFF8E8E93.toInt(), 0xFF0A84FF.toInt(), 0xFFFF6FA5.toInt(), 0xFFD4AF37.toInt())
        val cnames = listOf("白", "黑", "灰", "蓝", "粉", "金")
        val cseg = UI.Seg(this, cnames)
        cseg.selected = colors.indexOf(s.wmColor).coerceAtLeast(0)
        cseg.onSelect = { i -> s.wmColor = colors[i]; save(); refreshPreview() }
        card.addView(cseg)

        card.addView(head("图片水印 (Logo)"))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(UI.smallButton(this, "选择图片") { pickLogo() })
        row.addView(UI.smallButton(this, "清除") { s.logoUri = null; save(); refreshPreview(); rebuild() }, topMargin(0f).also { it.leftMargin = G.dp(this, 8f) })
        val logoState = UI.text(this, if (s.logoUri == null) "未设置" else "已设置", 11.5f, G.ink3)
        logoState.setPadding(G.dp(this, 10f), 0, 0, 0)
        row.addView(logoState)
        card.addView(row, topMargin(10f))

        val lsz = UI.Slider(this, "Logo 尺寸", 5, 60, s.logoScale.toInt(), " %")
        lsz.onChange = { v -> s.logoScale = v.toFloat(); save(); refreshPreview() }
        card.addView(lsz, topMargin(6f))

        val lal = UI.Slider(this, "Logo 不透明度", 5, 100, (s.logoAlpha * 100).toInt(), " %")
        lal.onChange = { v -> s.logoAlpha = v / 100f; save(); refreshPreview() }
        card.addView(lal)

        val posSeg = UI.Seg(this, listOf("右下", "左下", "右上", "左上", "居中"))
        posSeg.selected = listOf("br", "bl", "tr", "tl", "center").indexOf(s.logoPos).coerceAtLeast(0)
        posSeg.onSelect = { i -> s.logoPos = listOf("br", "bl", "tr", "tl", "center")[i]; save(); refreshPreview() }
        card.addView(posSeg, topMargin(8f))

        addCard(card)
    }

    private fun buildInvisibleCard() {
        val card = UI.card(this, "隐形水印", "溯源防伪：可被读取，肉眼看不见")
        val sw = Switch(this)
        sw.text = "开启隐形水印"
        sw.isChecked = s.invisOn
        sw.setTextColor(G.ink)
        sw.setTextSize(13f)
        sw.setPadding(0, G.dp(this, 8f), 0, 0)
        sw.setOnCheckedChangeListener { _, v -> s.invisOn = v; save(); refreshPreview() }
        card.addView(sw)

        card.addView(head("模式"))
        val seg = UI.Seg(this, listOf("元数据", "同色隐藏层", "像素 LSB", "全部"))
        seg.selected = listOf("meta", "visual", "lsb", "both").indexOf(s.invisMode).coerceAtLeast(0)
        seg.onSelect = { i -> s.invisMode = listOf("meta", "visual", "lsb", "both")[i]; save(); refreshPreview() }
        card.addView(seg)
        val tip = UI.text(this, "元数据：PDF /Subject 写入 base64 水印，信息查看器可见。\n同色隐藏层：与底色同色的文字，肉眼不可见。\n像素 LSB：写进像素最低位，需无损保存（体积较大）。", 11f, G.ink3)
        tip.setPadding(0, G.dp(this, 8f), 0, 0)
        tip.setLineSpacing(G.dp(this, 3f).toFloat(), 1f)
        card.addView(tip)

        card.addView(head("水印内容"))
        val et = UI.input(this, "如：星球卑一号实验室 2026", s.invisText)
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) { s.invisText = e?.toString() ?: ""; save(); refreshPreview() }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        card.addView(et, topMargin(8f))

        addCard(card)
    }

    private fun buildOutputCard() {
        val card = UI.card(this, "输出", "保存到 Download/Pic2PDF")
        card.addView(head("文件名 / 标题"))
        val et = UI.input(this, "图片转PDF", s.title)
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) { s.title = e?.toString() ?: ""; save() }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        card.addView(et, topMargin(8f))

        card.addView(head("分辨率"))
        val dpis = UI.Seg(this, listOf("省流 100", "标准 150", "清晰 200", "印刷 300"))
        dpis.selected = when (s.dpi) { 100 -> 0; 200 -> 2; 300 -> 3; else -> 1 }
        dpis.onSelect = { i -> s.dpi = intArrayOf(100, 150, 200, 300)[i]; save(); refreshPreview() }
        card.addView(dpis)

        val vbtn = UI.smallButton(this, "校验一个 PDF 的隐形水印") { pickPdf() }
        card.addView(vbtn, topMargin(12f))

        addCard(card)
    }

    private fun buildPreviewCard() {
        val card = UI.card(this, "实时预览", "第一页效果")
        preview = PreviewView(this)
        card.addView(preview, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = G.dp(this, 6f) })
        pageLabel = UI.text(this, "", 11.5f, G.ink3)
        pageLabel.setPadding(0, G.dp(this, 8f), 0, 0)
        card.addView(pageLabel)
        addCard(card, 12f)
    }

    private fun buildBottomBar(root: FrameLayout) {
        val bar = LinearLayout(this)
        bar.id = 1001
        bar.orientation = LinearLayout.VERTICAL
        bar.setPadding(G.dp(this, 16f), G.dp(this, 10f), G.dp(this, 16f), G.dp(this, 14f))
        bar.background = G.glass(this, 26f)
        exportBtn = UI.text(this, "导出 PDF", 15.5f, Color.WHITE, true)
        exportBtn.gravity = Gravity.CENTER
        exportBtn.background = G.primary(this)
        exportBtn.elevation = G.dp(this, 3f).toFloat()
        exportBtn.setOnClickListener { doExport() }
        bar.addView(exportBtn, LinearLayout.LayoutParams(-1, G.dp(this, 52f)))
        val lp = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
        root.addView(bar, lp)
    }

    private fun head(t: String): TextView {
        val v = UI.text(this, t, 12f, G.ink3, true)
        v.setPadding(0, G.dp(this, 14f), 0, G.dp(this, 8f))
        return v
    }

    private fun topMargin(v: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-2, -2).also { it.topMargin = G.dp(this, v) }

    private fun rebuild() { recreate() }

    private fun buildUiContent() {
        // 仅用于字体/Logo 变更后刷新卡片文案：最省事的整页重建
    }

    // ---------------- 状态刷新 ----------------

    private fun refreshAll() {
        refreshThumbs()
        preview.bind(s)
        refreshBadges()
    }

    private fun refreshBadges() {
        val n = Compositor.plan(s, 150f).size
        pageLabel.text = if (s.pics.isEmpty()) "等待选择图片" else "共 " + s.pics.size + " 张图 · 生成 " + n + " 页"
        exportBtn.text = if (s.pics.isEmpty()) "先选择图片" else "导出 PDF · " + n + " 页"
        exportBtn.alpha = if (s.pics.isEmpty()) 0.5f else 1f
    }

    private fun refreshPreview() {
        val v = ++previewJob
        ui.removeCallbacksAndMessages(null)
        ui.postDelayed({
            if (v == previewJob) {
                preview.bind(s)
                refreshBadges()
            }
        }, 180)
    }

    private fun refreshThumbs() {
        val ver = ++thumbVer
        thumbRow.removeAllViews()
        for ((i, p) in s.pics.withIndex()) {
            val f = FrameLayout(this)
            val size = G.dp(this, 68f)
            val iv = ImageView(this)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.background = G.inner(this, 14f)
            iv.clipToOutline = true
            val outline = android.graphics.drawable.GradientDrawable()
            outline.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            outline.cornerRadius = G.dp(this, 14f).toFloat()
            iv.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: android.graphics.Outline) {
                    o.setRoundRect(0, 0, v.width, v.height, G.dp(this@MainActivity, 14f).toFloat())
                }
            }
            f.addView(iv, FrameLayout.LayoutParams(size, size))
            f.setOnClickListener { showPicMenu(i) }
            val badge = UI.text(this, "" + (i + 1), 10f, Color.WHITE, true)
            badge.gravity = Gravity.CENTER
            val bd = android.graphics.drawable.GradientDrawable()
            bd.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            bd.cornerRadius = G.dp(this, 8f).toFloat()
            bd.setColor(0x99000000.toInt())
            badge.background = bd
            val blp = FrameLayout.LayoutParams(G.dp(this, 20f), G.dp(this, 16f))
            blp.leftMargin = G.dp(this, 5f)
            blp.topMargin = G.dp(this, 5f)
            f.addView(badge, blp)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.rightMargin = G.dp(this, 10f)
            thumbRow.addView(f, lp)
            loadThumb(ver, i, p.uri, iv)
        }
        // 加号
        val add = TextView(this)
        add.text = "+"
        add.setTextSize(24f)
        add.gravity = Gravity.CENTER
        add.setTextColor(G.accent)
        add.background = G.soft(this, 14f)
        add.setOnClickListener { pickPics() }
        val size = G.dp(this, 68f)
        thumbRow.addView(add, LinearLayout.LayoutParams(size, size))
    }

    private fun loadThumb(ver: Int, idx: Int, uri: String, iv: ImageView) {
        Thread {
            val b = Imgs.load(this, uri, 240)
            ui.post {
                if (ver == thumbVer && b != null) iv.setImageBitmap(b)
            }
        }.start()
    }

    private fun showPicMenu(index: Int) {
        val items = arrayOf("上移", "下移", "删除", "查看")
        AlertDialog.Builder(this)
            .setTitle("第 " + (index + 1) + " 张")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> if (index > 0) { val t = s.pics[index]; s.pics[index] = s.pics[index - 1]; s.pics[index - 1] = t; save(); refreshAll() }
                    1 -> if (index < s.pics.size - 1) { val t = s.pics[index]; s.pics[index] = s.pics[index + 1]; s.pics[index + 1] = t; save(); refreshAll() }
                    2 -> { s.pics.removeAt(index); save(); refreshAll() }
                    else -> {
                        try {
                            val it = Intent(Intent.ACTION_VIEW)
                            it.setDataAndType(Uri.parse(s.pics[index].uri), "image/*")
                            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(it)
                        } catch (t: Throwable) { toast("无法打开") }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- 选择器 ----------------

    private fun pickPics() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "image/*"
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(i, REQ_PICS)
    }

    private fun pickLogo() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "image/*"
        startActivityForResult(i, REQ_LOGO)
    }

    private fun pickFont() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        i.putExtra("android.content.extra.MIME_TYPES", arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream"))
        startActivityForResult(i, REQ_FONT)
    }

    private fun pickPdf() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "application/pdf"
        startActivityForResult(i, REQ_PDF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            REQ_PICS -> {
                val uris = ArrayList<Uri>()
                val clip = data.clipData
                if (clip != null) {
                    for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
                } else {
                    data.data?.let { uris.add(it) }
                }
                for (u in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (t: Throwable) {}
                    val m = Imgs.meta(this, u)
                    if (m != null) s.pics.add(m)
                }
                save(); refreshAll()
            }
            REQ_LOGO -> {
                val u = data.data ?: return
                try { contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (t: Throwable) {}
                s.logoUri = u.toString()
                save(); refreshPreview(); toast("已设置图片水印")
            }
            REQ_FONT -> {
                val u = data.data ?: return
                try {
                    val f = File(filesDir, "wm_font.ttf")
                    contentResolver.openInputStream(u)?.use { ins -> f.outputStream().use { ins.copyTo(it) } }
                    s.wmFontPath = f.absolutePath
                    save(); refreshPreview(); toast("字体已导入")
                } catch (t: Throwable) { toast("导入失败") }
            }
            REQ_PDF -> {
                val u = data.data ?: return
                probePdf(u)
            }
        }
    }

    private fun probePdf(u: Uri) {
        Thread {
            try {
                val bytes = contentResolver.openInputStream(u)?.use { it.readBytes() }
                if (bytes == null) { ui.post { toast("读取失败") }; return@Thread }
                val r = PdfProbe.probe(bytes)
                ui.post {
                    val sb = StringBuilder()
                    sb.append("页数：").append(r.pages).append("\n\n")
                    sb.append("元数据水印：\n")
                    sb.append(r.subject ?: "（无）").append("\n\n")
                    var dec = "（无）"
                    val sub = r.subject
                    if (sub != null && sub.startsWith("P2PW1:")) {
                        try {
                            dec = String(android.util.Base64.decode(sub.substring(6), android.util.Base64.NO_WRAP), Charsets.UTF_8)
                        } catch (t: Throwable) {}
                    }
                    sb.append("解析：\n").append(dec).append("\n\n")
                    sb.append("LSB 水印：\n").append(r.lsb ?: "（无）")
                    showDialog("水印校验", sb.toString())
                }
            } catch (t: Throwable) {
                ui.post { toast("校验失败：" + t.message) }
            }
        }.start()
    }

    // ---------------- 导出 ----------------

    private fun doExport() {
        if (s.pics.isEmpty()) { toast("先选择图片"); return }
        val dlg = Dialog(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = G.sheet(this)
        box.setPadding(G.dp(this, 26f), G.dp(this, 24f), G.dp(this, 26f), G.dp(this, 24f))
        val t = UI.text(this, "正在生成 PDF", 16f, G.ink, true)
        box.addView(t)
        val p = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        p.max = 100
        box.addView(p, LinearLayout.LayoutParams(G.dp(this, 220f), G.dp(this, 30f)).also { it.topMargin = G.dp(this, 14f) })
        val note = UI.text(this, "准备中…", 12f, G.ink2)
        note.setPadding(0, G.dp(this, 6f), 0, 0)
        box.addView(note)
        dlg.setContentView(box)
        dlg.setCancelable(false)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.window?.setLayout(G.dp(this, 300f), -2)
        dlg.show()

        Thread {
            try {
                val r = Exporter.export(this, s) { done, total, nt ->
                    ui.post {
                        p.progress = (done * 100 / (total + 1)).coerceIn(0, 100)
                        note.text = nt
                    }
                }
                ui.post { dlg.dismiss(); showResult(r) }
            } catch (t: Throwable) {
                ui.post { dlg.dismiss(); toast("导出失败：" + (t.message ?: t.javaClass.simpleName)) }
            }
        }.start()
    }

    private fun showResult(r: Exporter.Result) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = G.sheet(this)
        box.setPadding(G.dp(this, 24f), G.dp(this, 22f), G.dp(this, 24f), G.dp(this, 18f))
        box.addView(UI.text(this, "已完成 ✓", 18f, G.ink, true))
        val info = "共 " + r.pages + " 页 · " + String.format("%.1f", r.bytes / 1024.0 / 1024.0) + " MB · " + (r.elapsedMs / 1000f) + "s"
        val i2 = UI.text(this, info, 12f, G.ink2)
        i2.setPadding(0, G.dp(this, 6f), 0, 0)
        box.addView(i2)
        val path = UI.text(this, r.path, 11.5f, G.ink3)
        path.setPadding(0, G.dp(this, 10f), 0, 0)
        box.addView(path)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, G.dp(this, 16f), 0, 0)
        val open = UI.text(this, "打开", 13.5f, Color.WHITE, true)
        open.gravity = Gravity.CENTER
        open.background = G.primary(this)
        row.addView(open, LinearLayout.LayoutParams(0, G.dp(this, 42f), 1f))
        val share = UI.text(this, "分享", 13.5f, G.accent, true)
        share.gravity = Gravity.CENTER
        share.background = G.soft(this, 14f)
        row.addView(share, LinearLayout.LayoutParams(0, G.dp(this, 42f), 1f).also { it.leftMargin = G.dp(this, 10f) })
        box.addView(row)
        val dlg = Dialog(this)
        dlg.setContentView(box)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.window?.setLayout(G.dp(this, 310f), -2)
        open.setOnClickListener {
            dlg.dismiss()
            try {
                val i = Intent(Intent.ACTION_VIEW)
                if (r.uri != null) {
                    i.setDataAndType(r.uri, "application/pdf")
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    i.setDataAndType(Uri.fromFile(File(r.path)), "application/pdf")
                }
                startActivity(i)
            } catch (t: Throwable) { toast("没有可用的 PDF 阅读器") }
        }
        share.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "application/pdf"
            if (r.uri != null) {
                i.putExtra(Intent.EXTRA_STREAM, r.uri)
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(i, "分享 PDF"))
            } else {
                toast("文件路径：" + r.path)
            }
        }
        dlg.show()
    }

    // ---------------- 工具 ----------------

    private fun showDialog(title: String, msg: String) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = G.sheet(this)
        box.setPadding(G.dp(this, 22f), G.dp(this, 20f), G.dp(this, 22f), G.dp(this, 14f))
        box.addView(UI.text(this, title, 16f, G.ink, true))
        val tv = TextView(this)
        tv.text = msg
        tv.setTextSize(12f)
        tv.setTextColor(G.ink2)
        tv.setPadding(0, G.dp(this, 12f), 0, 0)
        tv.setTextIsSelectable(true)
        box.addView(tv, LinearLayout.LayoutParams(-1, -2))
        val ok = UI.text(this, "好的", 13f, Color.WHITE, true)
        ok.gravity = Gravity.CENTER
        ok.background = G.primary(this)
        box.addView(ok, LinearLayout.LayoutParams(-1, G.dp(this, 40f)).also { it.topMargin = G.dp(this, 16f) })
        val dlg = Dialog(this)
        dlg.setContentView(box)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.window?.setLayout(G.dp(this, 310f), -2)
        ok.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    private fun save() = Prefs.save(this, s)
}

