# KaTeX 离线资源说明

需要把以下文件从 KaTeX 0.16.x release 复制到本目录：

- `katex.min.css`
- `katex.min.js`
- `fonts/` 目录（全部字体文件）

下载地址：https://github.com/KaTeX/KaTeX/releases/download/v0.16.11/katex.zip

解压后把 `katex/` 目录下的 `katex.min.css`、`katex.min.js`、`fonts/` 整体拷贝过来。

最终目录结构：
```
app/src/main/assets/katex/
├── render.html       # 已提供
├── katex.min.css     # 需下载
├── katex.min.js      # 需下载
└── fonts/            # 需下载
    ├── KaTeX_Main-Regular.woff2
    ├── KaTeX_Math-Italic.woff2
    └── ...
```

如果不下载这些文件，WebView 会加载不到 KaTeX，公式会以原始 LaTeX 源码显示（fallback 在 render.html 的 catch 分支里）。
