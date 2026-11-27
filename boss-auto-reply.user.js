// ==UserScript==
// @name         BOSS直聘 招聘助手（自动回复 / 批量发送 · 半自动）
// @namespace    local.boss.assistant
// @version      0.1.0
// @description  在 BOSS直聘 招聘者网页端，对新消息自动起草回复，并支持批量发送。默认「演练模式」，不真正发送。请自行承担平台风控风险。
// @match        https://www.zhipin.com/web/*
// @match        https://www.zhipin.com/*
// @match        https://boss.zhipin.com/*
// @grant        GM_xmlhttpRequest
// @connect      *
// @run-at       document-idle
// @noframes
// ==/UserScript==

(function () {
  "use strict";

  /* =========================================================================
   * ⚠️ 使用前必读
   * 1. 默认 mode = "dry"（演练）：只识别新消息并生成草稿写进日志，绝不发送。
   *    确认无误后，在面板里切换到 "draft"（填入输入框，你手动发）或 "auto"（直接发）。
   * 2. 批量发送和 auto 模式会弹确认框。请务必先小批量、低频率测试。
   * 3. BOSS直聘 的 DOM 可能和你看到的不一样。若脚本识别不到，点面板「扫描元素」，
   *    在浏览器控制台/日志里看输出，把正确的选择器填到下面 SELECTORS 里。
   * 4. 自动化操作可能违反平台用户协议并导致账号受限，风险自负。
   * ========================================================================= */

  /* ----------------------------- 可调选择器 ------------------------------ */
  // 每个字段按顺序尝试，命中第一个可用元素。看不清就点「扫描元素」。
  const SELECTORS = {
    // 会话列表的每一项
    chatListItem: [
      ".chat-user-item",
      ".chat-user-list .item",
      ".chat-user-list > *",
      ".user-list .item",
      ".chat-user",
      ".conversation-list .item"
    ],
    // 会话列表里的未读角标（用于定位“新招呼”）
    unreadBadge: [
      ".badge",
      ".unread",
      ".num",
      ".msg-count",
      ".message-count"
    ],
    // 聊天区里的消息气泡（用于取最新一条）
    messageItem: [
      ".message-item",
      ".chat-message",
      ".msg-item",
      ".chat-record .item",
      ".im-message"
    ],
    // 对方（候选人）发来的消息气泡
    incomingMessage: [
      ".message-item.received",
      ".message-item.friend",
      ".message-item.left",
      ".message-item.other",
      ".msg-item.received",
      ".chat-message.friend"
    ],
    // 输入框（textarea 或 contenteditable）
    inputBox: [
      ".boss-chat-editor-input",
      ".chat-input",
      "#chat-input",
      ".chat-editor [contenteditable='true']",
      "div[contenteditable='true']",
      "textarea.input-area",
      "textarea"
    ],
    // 发送按钮
    sendBtn: [
      ".boss-chat-editor-send",
      ".chat-editor-send",
      ".btn-send",
      ".chat-op .btn-send",
      ".send-btn",
      ".send-message"
    ]
  };

  /* ------------------------------ 默认配置 ------------------------------- */
  const DEFAULTS = {
    enabled: false,          // 自动回复总开关
    mode: "dry",             // dry 仅记录 | draft 填入不发送 | auto 直接发送
    hourlyCap: 20,           // 每小时最多自动处理条数（含批量）
    intervalSec: 15,         // 扫描间隔（秒）
    // 未配置 LLM 时使用的兜底话术（可在此自定义增删）
    templates: [
      "你好，感谢你的关注～看了下你的背景，想先了解下你目前的期望岗位和到岗时间？",
      "你好，方便说下你现在看机会主要看重哪方面吗？我这边帮你匹配一下。",
      "你好，你的经历我们这边有兴趣，能简单说下最近一段工作的核心内容吗？"
    ],
    useLLM: false,
    llm: {
      baseUrl: "",           // 例：https://api.openai.com/v1/chat/completions
      apiKey: "",
      model: "",
      systemPrompt: "你是招聘负责人，回复候选人要简洁、礼貌、口语化，一次只问一个关键问题，不要超过60字。"
    },
    batchText: "",           // 批量发送内容
    batchMax: 10,            // 批量最多发多少人
    batchDelayMin: 6,        // 批量发送最小间隔（秒）
    batchDelayMax: 12        // 批量发送最大间隔（秒）
  };

  const LS_KEY = "boss_assistant_cfg_v1";
  const HANDLED_KEY = "boss_assistant_handled_v1";

  const cfg = Object.assign({}, DEFAULTS, loadCfg());
  const handled = new Set(loadHandled());

  let running = false;
  let stopFlag = false;

  /* ------------------------------- 基础工具 ------------------------------- */
  function loadCfg() {
    try { return JSON.parse(localStorage.getItem(LS_KEY) || "{}"); } catch (e) { return {}; }
  }
  function saveCfg() {
    try { localStorage.setItem(LS_KEY, JSON.stringify(cfg)); } catch (e) {}
  }
  function loadHandled() {
    try { return JSON.parse(localStorage.getItem(HANDLED_KEY) || "[]"); } catch (e) { return []; }
  }
  function saveHandled() {
    try { localStorage.setItem(HANDLED_KEY, JSON.stringify(Array.from(handled).slice(-500))); } catch (e) {}
  }

  // 排除脚本自己的悬浮面板，避免误把面板里的元素当成页面元素
  function inPanel(el) { return !!(el && el.closest && el.closest("#bzai-panel")); }

  function pick(list, root) {
    const scope = root || document;
    for (let i = 0; i < list.length; i++) {
      try {
        const nodes = scope.querySelectorAll(list[i]);
        for (let j = 0; j < nodes.length; j++) {
          if (!inPanel(nodes[j])) return nodes[j];
        }
      } catch (e) {}
    }
    return null;
  }
  function pickAll(list, root) {
    const scope = root || document;
    const out = [];
    for (let i = 0; i < list.length; i++) {
      try {
        const nodes = scope.querySelectorAll(list[i]);
        nodes.forEach((n) => { if (out.indexOf(n) === -1 && !inPanel(n)) out.push(n); });
      } catch (e) {}
    }
    return out;
  }
  function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }
  function rnd(a, b) { return a + Math.random() * (b - a); }
  function norm(s) { return String(s || "").replace(/\s+/g, " ").trim(); }

  const sentTimes = [];
  function withinCap() {
    const now = Date.now();
    while (sentTimes.length && now - sentTimes[0] > 3600 * 1000) sentTimes.shift();
    return sentTimes.length < cfg.hourlyCap;
  }
  function markSent() { sentTimes.push(Date.now()); }

  /* ------------------------------- 日志面板 ------------------------------- */
  let logEl = null;
  function log(msg, kind) {
    const line = "[" + new Date().toLocaleTimeString() + "] " + msg;
    console.log("[BOSS助手]", msg);
    if (!logEl) return;
    const d = document.createElement("div");
    d.textContent = line;
    if (kind === "err") d.style.color = "#ff8a8a";
    else if (kind === "ok") d.style.color = "#8affb0";
    else if (kind === "warn") d.style.color = "#ffd479";
    logEl.appendChild(d);
    logEl.scrollTop = logEl.scrollHeight;
    while (logEl.childNodes.length > 200) logEl.removeChild(logEl.firstChild);
  }

  function buildPanel() {
    const wrap = document.createElement("div");
    wrap.id = "bzai-panel";
    wrap.innerHTML = [
      '<div class="bzai-head">',
      '  <span>BOSS 招聘助手</span>',
      '  <button class="bzai-min" type="button">–</button>',
      "</div>",
      '<div class="bzai-body">',
      '  <div class="bzai-risk">默认演练模式，不发送。风险自负。</div>',
      '  <label class="bzai-row"><span>自动回复</span><input type="checkbox" id="bzai-enabled"></label>',
      '  <label class="bzai-row"><span>模式</span>',
      '    <select id="bzai-mode">',
      '      <option value="dry">演练（只记录）</option>',
      '      <option value="draft">填入草稿（手动发）</option>',
      '      <option value="auto">直接发送</option>',
      '    </select></label>',
      '  <label class="bzai-row"><span>每小时上限</span><input type="number" id="bzai-cap" min="1" max="200"></label>',
      '  <div class="bzai-sep">批量发送</div>',
      '  <textarea id="bzai-batch-text" placeholder="要群发的话术…"></textarea>',
      '  <label class="bzai-row"><span>最多发</span><input type="number" id="bzai-batch-max" min="1" max="200"> <span>人</span></label>',
      '  <div class="bzai-btns">',
      '    <button id="bzai-batch-start" type="button">开始批量</button>',
      '    <button id="bzai-batch-stop" type="button">停止</button>',
      "  </div>",
      '  <div class="bzai-sep">调试</div>',
      '  <div class="bzai-btns">',
      '    <button id="bzai-scan" type="button">扫描元素</button>',
      '    <button id="bzai-deep" type="button">深度扫描</button>',
      '    <button id="bzai-test" type="button">测试生成</button>',
      '    <button id="bzai-copy" type="button">复制日志</button>',
      '    <button id="bzai-clear" type="button">清空日志</button>',
      "  </div>",
      '  <div id="bzai-log" class="bzai-log"></div>',
      '  <div class="bzai-foot">选择器在脚本顶部 SELECTORS 处修改</div>',
      "</div>"
    ].join("");

    const style = document.createElement("style");
    style.textContent = [
      "#bzai-panel{position:fixed;right:16px;bottom:16px;width:320px;z-index:2147483646;",
      "background:#1b1d24;color:#e8e8ec;border:1px solid #333744;border-radius:12px;",
      "box-shadow:0 8px 30px rgba(0,0,0,.45);font:13px/1.6 -apple-system,'PingFang SC',sans-serif;}",
      "#bzai-panel .bzai-head{display:flex;justify-content:space-between;align-items:center;",
      "padding:10px 14px;border-bottom:1px solid #333744;font-weight:600;}",
      "#bzai-panel .bzai-min{background:none;border:none;color:#aaa;cursor:pointer;font-size:16px;}",
      "#bzai-panel .bzai-body{padding:12px 14px;max-height:70vh;overflow:auto;}",
      "#bzai-panel.bzai-collapsed .bzai-body{display:none;}",
      "#bzai-panel .bzai-risk{background:#3a2418;color:#ffcf9b;border-radius:8px;padding:6px 10px;",
      "font-size:12px;margin-bottom:10px;}",
      "#bzai-panel .bzai-row{display:flex;align-items:center;justify-content:space-between;margin:8px 0;gap:8px;}",
      "#bzai-panel .bzai-row input[type=number]{width:64px;}",
      "#bzai-panel input,#bzai-panel select,#bzai-panel textarea{background:#242733;color:#e8e8ec;",
      "border:1px solid #3a3f4e;border-radius:8px;padding:5px 8px;font:inherit;outline:none;}",
      "#bzai-panel textarea{width:100%;min-height:60px;resize:vertical;box-sizing:border-box;}",
      "#bzai-panel .bzai-sep{margin:12px 0 8px;font-size:12px;color:#8b90a0;border-top:1px solid #333744;padding-top:8px;}",
      "#bzai-panel .bzai-btns{display:flex;gap:8px;flex-wrap:wrap;}",
      "#bzai-panel .bzai-btns button{flex:1;min-width:80px;background:#2c3040;color:#e8e8ec;border:1px solid #3a3f4e;",
      "border-radius:8px;padding:7px 6px;cursor:pointer;font:inherit;}",
      "#bzai-panel .bzai-btns button:hover{border-color:#7f74ff;}",
      "#bzai-panel #bzai-batch-start{background:#4b3fe3;border-color:#4b3fe3;}",
      "#bzai-panel .bzai-log{margin-top:10px;height:130px;overflow:auto;background:#14161c;border-radius:8px;",
      "padding:8px;font:12px/1.5 ui-monospace,Menlo,monospace;white-space:pre-wrap;word-break:break-word;}",
      "#bzai-panel .bzai-foot{margin-top:8px;font-size:11px;color:#6f7484;}"
    ].join("");
    document.head.appendChild(style);
    document.body.appendChild(wrap);

    logEl = wrap.querySelector("#bzai-log");

    // 回填配置
    wrap.querySelector("#bzai-enabled").checked = cfg.enabled;
    wrap.querySelector("#bzai-mode").value = cfg.mode;
    wrap.querySelector("#bzai-cap").value = cfg.hourlyCap;
    wrap.querySelector("#bzai-batch-text").value = cfg.batchText || "";
    wrap.querySelector("#bzai-batch-max").value = cfg.batchMax;

    wrap.querySelector(".bzai-min").addEventListener("click", () => wrap.classList.toggle("bzai-collapsed"));

    wrap.querySelector("#bzai-enabled").addEventListener("change", (e) => {
      cfg.enabled = e.target.checked; saveCfg();
      log(cfg.enabled ? "自动回复已开启（模式：" + cfg.mode + "）" : "自动回复已关闭", "warn");
      if (cfg.enabled) startLoop();
    });
    wrap.querySelector("#bzai-mode").addEventListener("change", (e) => {
      cfg.mode = e.target.value; saveCfg();
      if (cfg.mode === "auto" && !confirm("「直接发送」会真的把消息发出去，确认开启？")) {
        cfg.mode = "dry"; e.target.value = "dry"; saveCfg();
      }
      log("模式切换为：" + cfg.mode);
    });
    wrap.querySelector("#bzai-cap").addEventListener("change", (e) => {
      cfg.hourlyCap = Math.max(1, parseInt(e.target.value, 10) || 20); saveCfg();
    });
    wrap.querySelector("#bzai-batch-text").addEventListener("input", (e) => { cfg.batchText = e.target.value; saveCfg(); });
    wrap.querySelector("#bzai-batch-max").addEventListener("change", (e) => {
      cfg.batchMax = Math.max(1, parseInt(e.target.value, 10) || 10); saveCfg();
    });
    wrap.querySelector("#bzai-batch-start").addEventListener("click", startBatch);
    wrap.querySelector("#bzai-batch-stop").addEventListener("click", () => { stopFlag = true; log("已请求停止", "warn"); });
    wrap.querySelector("#bzai-scan").addEventListener("click", scan);
    wrap.querySelector("#bzai-deep").addEventListener("click", deepScan);
    wrap.querySelector("#bzai-copy").addEventListener("click", copyLog);
    wrap.querySelector("#bzai-test").addEventListener("click", async () => {
      const txt = lastIncomingText() || "（当前会话没有识别到对方消息）";
      log("识别到的对方最新消息：" + txt);
      const reply = await genReply(txt);
      log("生成的草稿：" + reply, "ok");
    });
    wrap.querySelector("#bzai-clear").addEventListener("click", () => { if (logEl) logEl.innerHTML = ""; });
  }

  /* ----------------------------- 页面操作原语 ----------------------------- */
  function getChatItems() { return pickAll(SELECTORS.chatListItem); }
  function unreadBadgeOf(item) {
    for (let i = 0; i < SELECTORS.unreadBadge.length; i++) {
      try {
        const b = item.querySelector(SELECTORS.unreadBadge[i]);
        if (b) {
          const t = norm(b.textContent);
          if (/[0-9]|·|新/.test(t) || b.offsetParent !== null) return b;
        }
      } catch (e) {}
    }
    return null;
  }
  function lastIncomingText() {
    const nodes = pickAll(SELECTORS.incomingMessage);
    if (nodes.length) return norm(nodes[nodes.length - 1].textContent);
    const all = pickAll(SELECTORS.messageItem);
    return all.length ? norm(all[all.length - 1].textContent) : "";
  }

  function setInput(text) {
    const el = pick(SELECTORS.inputBox);
    if (!el) { log("找不到输入框，请在 SELECTORS.inputBox 校准", "err"); return false; }
    el.focus();
    try {
      if (el.tagName === "TEXTAREA" || el.tagName === "INPUT") {
        const proto = el.tagName === "TEXTAREA" ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
        const setter = Object.getOwnPropertyDescriptor(proto, "value").set;
        setter.call(el, text);
        el.dispatchEvent(new Event("input", { bubbles: true }));
        el.dispatchEvent(new Event("change", { bubbles: true }));
      } else {
        // contenteditable：先清空再插入，尽量触发框架的 input 事件
        el.innerText = "";
        document.execCommand("insertText", false, text);
        if (!el.innerText.trim()) el.innerText = text;
        el.dispatchEvent(new InputEvent("input", { bubbles: true, data: text, inputType: "insertText" }));
      }
      return true;
    } catch (e) {
      log("写入输入框失败：" + e.message, "err");
      return false;
    }
  }

  function clickSend() {
    const btn = pick(SELECTORS.sendBtn);
    if (btn) { btn.click(); return true; }
    // 兜底：在输入框按回车
    const el = pick(SELECTORS.inputBox);
    if (el) {
      const opts = { bubbles: true, cancelable: true, key: "Enter", code: "Enter", keyCode: 13, which: 13 };
      el.dispatchEvent(new KeyboardEvent("keydown", opts));
      el.dispatchEvent(new KeyboardEvent("keyup", opts));
      return true;
    }
    log("找不到发送按钮，请在 SELECTORS.sendBtn 校准", "err");
    return false;
  }

  async function openChatItem(item) {
    item.click();
    await sleep(rnd(900, 1600));
  }

  /* ------------------------------ 生成回复 ------------------------------- */
  async function genReply(incomingText) {
    if (cfg.useLLM && cfg.llm.apiKey && cfg.llm.baseUrl) {
      try {
        return await llmReply(incomingText);
      } catch (e) {
        log("LLM 调用失败，改用兜底话术：" + e.message, "warn");
      }
    }
    const list = cfg.templates && cfg.templates.length ? cfg.templates : DEFAULTS.templates;
    return list[Math.floor(Math.random() * list.length)];
  }

  function llmReply(incomingText) {
    return new Promise((resolve, reject) => {
      const body = JSON.stringify({
        model: cfg.llm.model,
        messages: [
          { role: "system", content: cfg.llm.systemPrompt },
          { role: "user", content: "候选人发来：" + incomingText + "\n请生成一句回复。" }
        ],
        temperature: 0.6
      });
      GM_xmlhttpRequest({
        method: "POST",
        url: cfg.llm.baseUrl,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + cfg.llm.apiKey },
        data: body,
        timeout: 20000,
        onload: (res) => {
          try {
            const j = JSON.parse(res.responseText);
            const t = j.choices && j.choices[0] && j.choices[0].message && j.choices[0].message.content;
            if (t) resolve(norm(t)); else reject(new Error("返回为空"));
          } catch (e) { reject(e); }
        },
        onerror: () => reject(new Error("网络错误")),
        ontimeout: () => reject(new Error("超时"))
      });
    });
  }

  /* ---------------------------- 自动回复主循环 ---------------------------- */
  let loopTimer = null;
  function startLoop() {
    if (loopTimer) return;
    log("自动回复循环启动，间隔 " + cfg.intervalSec + "s");
    loopTimer = setInterval(tick, cfg.intervalSec * 1000);
  }
  function stopLoop() {
    if (loopTimer) { clearInterval(loopTimer); loopTimer = null; }
  }

  let busy = false;
  async function tick() {
    if (!cfg.enabled || busy) return;
    if (!withinCap()) { log("已达每小时上限，暂停自动处理", "warn"); return; }

    // 场景一：当前打开的会话里有新的对方消息
    const incoming = lastIncomingText();
    if (incoming) {
      const key = hash(incoming);
      if (!handled.has(key)) {
        busy = true;
        try {
          handled.add(key); saveHandled();
          const reply = await genReply(incoming);
          log("新消息：" + incoming, "warn");
          log("草稿：" + reply, "ok");
          await act(reply);
        } finally { busy = false; }
      }
    }

    // 场景二：列表中有未读（新招呼），尝试逐个打开处理
    if (cfg.enabled && cfg.mode !== "dry" && withinCap()) {
      const items = getChatItems();
      for (const it of items) {
        if (!withinCap()) break;
        if (!unreadBadgeOf(it)) continue;
        busy = true;
        try {
          await openChatItem(it);
          const txt = lastIncomingText();
          if (!txt) continue;
          const key = hash(txt);
          if (handled.has(key)) continue;
          handled.add(key); saveHandled();
          const reply = await genReply(txt);
          log("未读会话：「" + norm(it.textContent).slice(0, 20) + "」→ " + reply, "ok");
          await act(reply);
          await sleep(rnd(3000, 6000));
        } finally { busy = false; }
      }
    }
  }

  async function act(reply) {
    if (cfg.mode === "dry") { log("演练模式：不发送", "warn"); return; }
    if (!withinCap()) { log("已达上限，跳过发送", "warn"); return; }
    if (!setInput(reply)) return;
    markSent();
    if (cfg.mode === "draft") {
      log("已填入输入框（draft 模式，请手动发送）", "ok");
      return;
    }
    await sleep(rnd(400, 900));
    if (clickSend()) log("已发送", "ok");
    await sleep(rnd(1500, 3000));
  }

  function hash(s) {
    let h = 0;
    for (let i = 0; i < s.length; i++) { h = (h * 31 + s.charCodeAt(i)) | 0; }
    return "m" + h;
  }

  /* ------------------------------ 批量发送 ------------------------------- */
  async function startBatch() {
    if (running) { log("已有任务在跑", "warn"); return; }
    stopFlag = false;
    const text = norm(cfg.batchText);
    if (!text) { log("请先填写批量发送内容", "err"); return; }
    if (!confirm("即将向最多 " + cfg.batchMax + " 个会话发送相同内容，确认开始？")) return;

    running = true;
    const items = getChatItems();
    log("批量发送开始，候选会话 " + items.length + " 个，上限 " + cfg.batchMax, "warn");

    let count = 0;
    for (let i = 0; i < items.length && count < cfg.batchMax; i++) {
      if (stopFlag) break;
      if (!withinCap()) { log("达到每小时上限，批量停止", "warn"); break; }
      try {
        await openChatItem(items[i]);
        if (!setInput(text)) continue;
        await sleep(rnd(500, 1000));
        if (clickSend()) {
          markSent(); count++;
          log("批量已发送 " + count + "/" + cfg.batchMax + " → " + norm(items[i].textContent).slice(0, 18), "ok");
        }
      } catch (e) {
        log("跳过一条：" + e.message, "err");
      }
      const delay = rnd(cfg.batchDelayMin, cfg.batchDelayMax) * 1000;
      await sleep(delay);
    }
    log("批量结束，共发送 " + count + " 条", "warn");
    running = false;
  }

  /* ------------------------------ 扫描元素 ------------------------------- */
  function scan() {
    log("===== 扫描开始 =====");
    log("当前网址：" + location.href);
    log("页面标题：" + document.title);

    const iframes = Array.from(document.querySelectorAll("iframe"));
    log("iframe 数量：" + iframes.length);
    iframes.slice(0, 5).forEach((f, i) => log("  iframe[" + i + "] src=" + (f.getAttribute("src") || "")));

    const ce = Array.from(document.querySelectorAll("[contenteditable='true']")).filter((n) => !inPanel(n));
    log("真实 contenteditable 数量：" + ce.length);
    ce.forEach((e, i) => log("  ce[" + i + "] <" + e.tagName + "> class=" + e.className));

    const ta = Array.from(document.querySelectorAll("textarea")).filter((n) => !inPanel(n));
    log("真实 textarea 数量：" + ta.length);
    ta.forEach((e, i) => log("  ta[" + i + "] class=" + e.className + " id=" + e.id));

    // 关键词元素：聊天/消息/输入/发送相关
    const KW = /(chat|message|msg|send|input|editor|user-list|userlist|conversation|im-|session)/i;
    const kwEls = Array.from(document.querySelectorAll("[class]")).filter((n) => {
      return !inPanel(n) && KW.test(String(n.className || ""));
    });
    log("含聊天关键词的元素：" + kwEls.length + " 个（下列前 30）");
    kwEls.slice(0, 30).forEach((e, i) => {
      log("  [" + e.tagName + "] class=" + e.className + (e.id ? " id=" + e.id : ""));
    });

    const btns = Array.from(document.querySelectorAll("button, a, div[role='button']")).filter((n) => !inPanel(n));
    const sendLike = btns.filter((b) => /发送|send/i.test(b.textContent || "") || /send/i.test(String(b.className || "")));
    log("疑似发送按钮：" + sendLike.length);
    sendLike.slice(0, 10).forEach((b, i) => log("  send[" + i + "] <" + b.tagName + "> class=" + b.className + " text=" + norm(b.textContent).slice(0, 12)));

    log("各 SELECTORS 当前命中（已排除脚本面板）：");
    Object.keys(SELECTORS).forEach((k) => {
      const ok = pick(SELECTORS[k]);
      log("  " + k + " → " + (ok ? "命中 <" + ok.tagName + "> " + ok.className : "未命中"), ok ? "ok" : "err");
    });

    if (!pick(SELECTORS.chatListItem)) {
      log("⚠️ 没找到会话列表：请确认已进入招聘者「消息/沟通」页，并点开了某个会话。", "warn");
    }
    log("===== 扫描结束。把以上日志整段复制发我即可 =====");
  }

  /* ------------------------------ 深度扫描 ------------------------------- */
  function deepScan() {
    log("===== 深度扫描开始 =====");
    log("网址：" + location.href);

    // 1) 消息条目：看每条是“自己发的”还是“对方发的”
    const items = pickAll([".message-item", ".chat-message", ".msg-item"]);
    log("消息条目数量：" + items.length + "（下列最后 6 条）");
    items.slice(-6).forEach((el, i) => {
      log("─ msg[" + i + "] class=" + el.className);
      Array.from(el.querySelectorAll("*")).slice(0, 6).forEach((c) => {
        const t = c.children.length ? "" : " text=" + norm(c.textContent).slice(0, 26);
        log("    <" + c.tagName + "> class=" + (c.className || "") + t);
      });
    });

    // 2) 会话列表候选
    const userish = Array.from(document.querySelectorAll("[class*='chat-user'], [class*='user-list'], [class*='conversation'], [class*='chat-list']"))
      .filter((n) => !inPanel(n));
    log("会话列表候选容器：" + userish.length);
    userish.slice(0, 6).forEach((el, i) => {
      log("─ list[" + i + "] <" + el.tagName + "> class=" + el.className + " 子元素=" + el.children.length);
      Array.from(el.children).slice(0, 3).forEach((c, j) => {
        log("    child[" + j + "] <" + c.tagName + "> class=" + (c.className || "") + " text=" + norm(c.textContent).slice(0, 26));
      });
    });

    // 3) 编辑器附近找发送按钮
    const ed = pick(SELECTORS.inputBox);
    if (ed) {
      let box = ed.closest(".boss-chat-editor") || ed.closest("[class*='editor']") || ed.parentElement;
      log("编辑器容器 class=" + (box ? box.className : "?"));
      const cand = Array.from((box || ed).querySelectorAll("*")).filter((c) => {
        const cls = String(c.className || "");
        return /send|submit|btn|button|icon/i.test(cls) || /发送/.test(c.textContent || "");
      });
      log("编辑器附近疑似发送元素：" + cand.length);
      cand.slice(0, 12).forEach((c, i) => {
        log("  send[" + i + "] <" + c.tagName + "> class=" + (c.className || "") + " text=" + norm(c.textContent).slice(0, 10));
      });
    } else {
      log("没找到输入框，先点开一个会话再扫描", "err");
    }

    // 4) 全页 send / submit
    const sendAll = Array.from(document.querySelectorAll("[class*='send'], [class*='submit']")).filter((n) => !inPanel(n));
    log("全页 class 含 send/submit：" + sendAll.length);
    sendAll.slice(0, 10).forEach((c, i) => log("  <" + c.tagName + "> class=" + (c.className || "")));

    log("===== 深度扫描结束。把这整段复制发我 =====");
  }

  function copyLog() {
    if (!logEl) return;
    const text = logEl.innerText || logEl.textContent;
    const done = () => toast("日志已复制");
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(done).catch(() => selectLog());
    } else {
      selectLog();
    }
  }
  function selectLog() {
    if (!logEl) return;
    const r = document.createRange(); r.selectNodeContents(logEl);
    const sel = window.getSelection(); sel.removeAllRanges(); sel.addRange(r);
    toast("已选中，按 Ctrl+C 复制");
  }
  let toastTimer = null;
  function toast(msg) {
    let t = document.getElementById("bzai-toast");
    if (!t) {
      t = document.createElement("div");
      t.id = "bzai-toast";
      t.style.cssText = "position:fixed;left:50%;bottom:40px;transform:translateX(-50%);background:#111;color:#fff;" +
        "padding:8px 16px;border-radius:999px;font-size:12px;z-index:2147483647;opacity:0;transition:opacity .2s;";
      document.body.appendChild(t);
    }
    t.textContent = msg; t.style.opacity = "1";
    clearTimeout(toastTimer); toastTimer = setTimeout(() => { t.style.opacity = "0"; }, 1500);
  }

  /* ------------------------------- 启动 ---------------------------------- */
  function boot() {
    if (document.getElementById("bzai-panel")) return;
    buildPanel();
    log("已加载。默认演练模式，未发送任何消息。", "ok");
    if (cfg.enabled) startLoop();
  }

  if (document.readyState === "complete" || document.readyState === "interactive") {
    setTimeout(boot, 1200);
  } else {
    window.addEventListener("DOMContentLoaded", () => setTimeout(boot, 1200));
  }
})();
